package com.eazybytes.accounts.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import com.eazybytes.accounts.dto.AccountsMsgDto;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import com.eazybytes.accounts.constants.AccountsConstants;
import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.dto.CustomerDto;
import com.eazybytes.accounts.entity.Accounts;
import com.eazybytes.accounts.entity.Customer;
import com.eazybytes.accounts.exception.CustomerAlreadyExistsException;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.repository.CustomerRepository;
import com.eazybytes.accounts.service.IAccountsService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class AccountsServiceImpl implements IAccountsService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private final StreamBridge streamBridge;

    @Override
    public void createAccount(CustomerDto customerDto) {
        log.info("INSIDE createAccount() ---->");
        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());

        Optional<Customer> optCustomer = customerRepository.findByMobileNumber(customer.getMobileNumber());
        if (optCustomer.isPresent()) {
            log.info("Customer already exist with mobile number -----> {}", customer.getMobileNumber());
            throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber "
                    + customerDto.getMobileNumber());
        }

        customer.setCreatedAt(LocalDateTime.now());
        customer.setCreatedBy("Anonymous");
        Customer savedCustomer = customerRepository.save(customer);
        Accounts savedAccount = accountsRepository.save(createNewAccount(customer));

        sendCommunication(savedAccount, savedCustomer);
    }

    private void sendCommunication(Accounts savedAccount, Customer savedCustomer) {
        var accountsMsgDto = new AccountsMsgDto(savedAccount.getAccountNumber(), savedCustomer.getName(), savedCustomer.getEmail(),
                savedCustomer.getMobileNumber());

        log.info("Sending Communication request for the details: {}", accountsMsgDto);
        boolean result = streamBridge.send("sendCommunication-out-0", accountsMsgDto);
        log.info("Is the Communication request successfully triggered ? : {}", result);
    }

    /**
     * @param customer - Customer Object
     * @return the new account details
     */
    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();
        newAccount.setCustomerId(customer.getCustomerId());
        long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

        newAccount.setAccountNumber(randomAccNumber);
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);
        newAccount.setCreatedAt(LocalDateTime.now());
        newAccount.setCreatedBy("Anonymous");
        return newAccount;

    }

    @Override
    public CustomerDto fetchAccount(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Cutomer", "mobileNumber", mobileNumber));
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "CustomerId", customer.getCustomerId().toString()));
        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));
        return customerDto;
    }

    @Override
    public Boolean updateAccountDetails(CustomerDto customerDto) {
        Boolean isUpdated = false;
        AccountsDto accountsDto = customerDto.getAccountsDto();
        if (accountsDto != null) {
            Accounts accounts = accountsRepository.findById(accountsDto.getAccountNumber()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber",
                            accountsDto.getAccountNumber().toString()));
            accounts = AccountsMapper.mapToAccounts(accountsDto, accounts);
            accounts = accountsRepository.save(accounts);

            Long customerId = accounts.getCustomerId();
            Customer customer = customerRepository.findById(customerId).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "CustomerId", customerId.toString()));
            CustomerMapper.mapToCustomer(customerDto, customer);
            customerRepository.save(customer);
            isUpdated = true;
        }
        return isUpdated;
    }

    @Override
    public Boolean deleteAccountDetails(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));
        // Accounts accounts =
        // accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
        // () -> new ResourceNotFoundException("Accounts", "CustomerId",
        // customer.getCustomerId().toString()));
        // accountsRepository.delete(accounts);
        // customerRepository.delete(customer);
        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepository.deleteById(customer.getCustomerId());
        return true;
    }

    @Override
    public boolean updateCommunicationStatus(Long accountNumber) {
        boolean isUpdated = false;
        if (accountNumber != null) {
            Accounts accounts = accountsRepository.findByAccountNumber(accountNumber).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountNumber.toString())
            );
            accounts.setCommunicationSw(true);
            accountsRepository.save(accounts);
            isUpdated = true;
        }
        return isUpdated;
    }

}
