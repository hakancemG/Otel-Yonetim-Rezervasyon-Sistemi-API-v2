package com.otelyonetim.rezervasyon.config;

import com.iyzipay.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IyzicoConfig {
    @Bean
    public Options iyzicoOptions(
            @Value("${iyzico.api-key}") String apiKey,
            @Value("${iyzico.secret-key}") String secret,
            @Value("${iyzico.base-url}") String baseUrl) {
        Options options = new Options();
        options.setApiKey(apiKey);
        options.setSecretKey(secret);
        options.setBaseUrl(baseUrl);
        return options;
    }
}
