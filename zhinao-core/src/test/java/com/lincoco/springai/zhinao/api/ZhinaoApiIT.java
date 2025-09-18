package com.lincoco.springai.zhinao.api;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ZhinaoApiIT {

    private static final Logger logger = LoggerFactory.getLogger(ZhinaoApiIT.class);

    private final ZhinaoApi zhinaoApi = ZhinaoApi.builder().apiKey(System.getenv("ZHINAO_API_KEY")).build();

    @Test
    void chatCompletionEntity() {
        ZhinaoApi.ChatCompletionMessage chatCompletionMessage = new ZhinaoApi.ChatCompletionMessage("Hello World", ZhinaoApi.ChatCompletionMessage.Role.USER);
        ResponseEntity<ZhinaoApi.ChatCompletion> response = this.zhinaoApi.chatCompletionEntity(new ZhinaoApi.ChatCompletionRequest(
                List.of(chatCompletionMessage), ZhinaoApi.ChatModel.GPT_PRO.getValue(), 0.8, false));
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        logger.info("Response status: {}", response.getBody());
    }

    @Test
    void chatCompletionEntityWithSystemMessage() {
        ZhinaoApi.ChatCompletionMessage userMessage = new ZhinaoApi.ChatCompletionMessage(
                "Tell me about 3 famous pirates from thw Folden Age of Piracy and why they did?", ZhinaoApi.ChatCompletionMessage.Role.USER);
        ZhinaoApi.ChatCompletionMessage systemMessage = new ZhinaoApi.ChatCompletionMessage("""
                You are an AI assistant that helps people find information.
                Your name is Bob.
                You should reply to the user's request with your name and also in the style of a pirate.
                """, ZhinaoApi.ChatCompletionMessage.Role.SYSTEM);

        ResponseEntity<ZhinaoApi.ChatCompletion> response = this.zhinaoApi.chatCompletionEntity(new ZhinaoApi.ChatCompletionRequest(
                List.of(systemMessage, userMessage), ZhinaoApi.ChatModel.GPT_PRO.getValue(), 0.8, false));
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        logger.info("Response status: {}",response.getBody());
    }

    @Test
    void chatCompletionStream() {

        ZhinaoApi.ChatCompletionMessage chatCompletionMessage = new ZhinaoApi.ChatCompletionMessage("Hello World", ZhinaoApi.ChatCompletionMessage.Role.USER);
        Flux<ZhinaoApi.ChatCompletionChunk> response = this.zhinaoApi.chatCompletionStream(new ZhinaoApi.ChatCompletionRequest(
                List.of(chatCompletionMessage), ZhinaoApi.ChatModel.GPT_PRO.getValue(), 0.8, true));

        assertThat(response).isNotNull();

        StringBuilder fullResponse = new StringBuilder();
        response.doOnNext(chunk -> {
            logger.info("Streaming response: {}", chunk);
            if (chunk.choices() != null && !chunk.choices().isEmpty()) {
                ZhinaoApi.ChunkChoice choice = chunk.choices().get(0);
                if (choice.delta() != null && choice.delta().content() != null) {
                    fullResponse.append(choice.delta().content());
                }
            }
        }).blockLast(); // 等待流完成

        logger.info("Full streamed response: {}", fullResponse);
        assertThat(fullResponse.toString()).isNotEmpty();
    }
}
