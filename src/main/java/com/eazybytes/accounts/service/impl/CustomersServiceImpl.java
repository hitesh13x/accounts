package com.eazybytes.accounts.service.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.dto.CardsDto;
import com.eazybytes.accounts.dto.CustomerDetailsDto;
import com.eazybytes.accounts.dto.LoansDto;
import com.eazybytes.accounts.entity.Accounts;
import com.eazybytes.accounts.entity.Customer;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.repository.CustomerRepository;
import com.eazybytes.accounts.service.ICustomersService;
import com.eazybytes.accounts.service.client.CardsFeignClient;
import com.eazybytes.accounts.service.client.LoansFeignClient;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CustomersServiceImpl implements ICustomersService {

    private final CustomerRepository customerRepository;
    private final AccountsRepository accountsRepository;
    private final LoansFeignClient loansFeignClient;
    private final CardsFeignClient cardsFeignClient;

    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> {
            return new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber);
        });
        Accounts account = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(() -> {
            return new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString());
        });

        ResponseEntity<LoansDto> loanResponse = loansFeignClient.fetchLoanDetails(correlationId, customer.getMobileNumber());
        ResponseEntity<CardsDto> cardsResponse = cardsFeignClient.fetchCardDetails(correlationId, customer.getMobileNumber());
        
        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(account, new AccountsDto()));
        customerDetailsDto.setLoansDto(loanResponse != null ? loanResponse.getBody() : null);
        customerDetailsDto.setCardsDto(cardsResponse != null ? cardsResponse.getBody() : null);
        return customerDetailsDto;

    }

}
