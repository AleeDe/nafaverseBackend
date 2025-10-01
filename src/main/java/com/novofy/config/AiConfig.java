package com.novofy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AiConfig {
    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public ChatClient chatClient(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${spring.ai.openai.organization-id:}") String orgId
    ) {
        String tail = apiKey != null && apiKey.length() > 6 ? apiKey.substring(apiKey.length() - 6) : "unknown";
        log.info("OpenAI config -> model={}, orgId={}, key=***{}",
                model, (orgId.isEmpty() ? "(default)" : orgId), tail);

        var api = new org.springframework.ai.openai.api.OpenAiApi(apiKey /* uses default base URL */);
        var options = org.springframework.ai.openai.OpenAiChatOptions.builder().withModel(model).build();
        var openAiModel = new OpenAiChatModel(api, options);
        return ChatClient.builder(openAiModel).build();
    }
}
