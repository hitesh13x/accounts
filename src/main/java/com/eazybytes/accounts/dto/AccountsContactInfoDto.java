package com.eazybytes.accounts.dto;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contact")
public record AccountsContactInfoDto(
        String message,
        Map<String, String> contactDetails,
        List<String> onCallSupport) {

}
