package com.smarttodo.app.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "max.api")
public record MaxProps(
        String baseUrl,    // Базовый URL Bot API (например, https://api.max.ru)
        String token      // Токен бота (из ENV/секретов)
) {}
