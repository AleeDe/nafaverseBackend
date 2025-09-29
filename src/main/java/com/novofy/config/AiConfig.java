package com.novofy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(@Value("${spring.ai.openai.api-key}") String apiKey,
                                 @Value("${spring.ai.openai.chat.options.model}") String model) {
        // OpenAI model create karo
        var openAiModel = new OpenAiChatModel(new org.springframework.ai.openai.api.OpenAiApi(apiKey),
                org.springframework.ai.openai.OpenAiChatOptions.builder().withModel(model).build());

        // ChatClient banao aur return karo
        return ChatClient.builder(openAiModel).build();
    }
}
