/*
Copyright 2025 lin-coco

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.lincoco.springai.zhinao.chat;

import com.lincoco.springai.zhinao.ZhinaoChatModel;
import com.lincoco.springai.zhinao.ZhinaoChatOptions;
import com.lincoco.springai.zhinao.api.ZhinaoApi;
import com.lincoco.springai.zhinao.api.ZhinaoConstants;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ZhinaoChatModelObservationIT.Config.class)
public class ZhinaoChatModelObservationIT {

    @Autowired
    TestObservationRegistry observationRegistry;

    @Autowired
    ZhinaoChatModel chatModel;

    @BeforeEach
    void beforeEach() {
        this.observationRegistry.clear();
    }

    @Test
    void observationForChatOperation() {

        var options = ZhinaoChatOptions.builder()
                .model(ZhinaoApi.ChatModel.GPT_PRO.getValue())
                .maxTokens(2048)
                .temperature(0.7)
                .topP(1.0)
                .topK(512)
                .repetitionPenalty(2.0)
                .numBeams(2)
                .user("lincoco")
                .build();

        Prompt prompt = new Prompt("Why does a raven look like a desk?", options);

        ChatResponse chatResponse = this.chatModel.call(prompt);
        assertThat(chatResponse.getResult().getOutput().getText()).isNotEmpty();

        ChatResponseMetadata responseMetadata = chatResponse.getMetadata();
        assertThat(responseMetadata).isNotNull();

        validate(responseMetadata);
    }

    private void validate(ChatResponseMetadata responseMetadata) {
        TestObservationRegistryAssert.assertThat(this.observationRegistry)
                .doesNotHaveAnyRemainingCurrentObservation()
                .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
                .that()
                .hasContextualNameEqualTo("chat " + ZhinaoApi.ChatModel.GPT_PRO.getValue())
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(),
                        AiOperationType.CHAT.value())
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER.asString(), ZhinaoConstants.ZHINAO_PROVIDER_NAME)
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL.asString(),
                        ZhinaoApi.ChatModel.GPT_PRO.getValue())
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL.asString(), responseMetadata.getModel())
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_MAX_TOKENS.asString(), "2048")
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TEMPERATURE.asString(), "0.7")
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TOP_P.asString(), "1.0")
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TOP_K.asString(), "512")
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.RESPONSE_ID.asString(), responseMetadata.getId())
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.RESPONSE_FINISH_REASONS.asString(), "[\"STOP\"]")
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(),
                        String.valueOf(responseMetadata.getUsage().getPromptTokens()))
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_OUTPUT_TOKENS.asString(),
                        String.valueOf(responseMetadata.getUsage().getCompletionTokens()))
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(),
                        String.valueOf(responseMetadata.getUsage().getTotalTokens()))
                .hasBeenStarted()
                .hasBeenStopped();
    }


    @SpringBootConfiguration
    static class Config {
        @Bean
        public TestObservationRegistry observationRegistry() {
            return TestObservationRegistry.create();
        }

        @Bean
        public ZhinaoApi zhinaoApi() {
            return ZhinaoApi.builder().apiKey(System.getenv("ZHINAO_API_KEY")).build();
        }

        @Bean
        public ZhinaoChatModel zhinaoChatModel(ZhinaoApi zhinaoApi, TestObservationRegistry observationRegistry) {
            return ZhinaoChatModel.builder()
                    .zhinaoApi(zhinaoApi)
                    .defaultOptions(ZhinaoChatOptions.builder().build())
                    .toolCallingManager(ToolCallingManager.builder().build())
                    .retryTemplate(RetryTemplate.defaultInstance())
                    .observationRegistry(observationRegistry)
                    .build();
        }
    }
}
