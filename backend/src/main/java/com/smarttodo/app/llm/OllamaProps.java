package com.smarttodo.app.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama.api")
public record OllamaProps(
        String baseUrl,
        String model,
        int timeoutSeconds
) {}