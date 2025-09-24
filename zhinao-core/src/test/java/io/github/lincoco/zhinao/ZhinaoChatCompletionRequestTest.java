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
package io.github.lincoco.zhinao;

import io.github.lincoco.zhinao.ZhinaoChatModel;
import io.github.lincoco.zhinao.ZhinaoChatOptions;
import io.github.lincoco.zhinao.api.ZhinaoApi;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

public class ZhinaoChatCompletionRequestTest {

    @Test
    public void createRequestWithChatOptions() {

        var client = ZhinaoChatModel.builder()
                .zhinaoApi(ZhinaoApi.builder().apiKey("TEST").build())
                .defaultOptions(ZhinaoChatOptions.builder().model("DEFAULT_MODEL").temperature(66.6).build())
                .build();

        var prompt = client.buildRequestPrompt(new Prompt("Test message content"));

        var request = client.createRequest(prompt, false);

        assertThat(request.messages()).hasSize(1);
        assertThat(request.stream()).isFalse();

        assertThat(request.model()).isEqualTo("DEFAULT_MODEL");
        assertThat(request.temperature()).isEqualTo(66.6D);

        request = client.createRequest(new Prompt("Test message content",
                ZhinaoChatOptions.builder().model("PROMPT_MODEL").temperature(99.9D).build()), true);

        assertThat(request.messages()).hasSize(1);
        assertThat(request.stream()).isTrue();

        assertThat(request.model()).isEqualTo("PROMPT_MODEL");
        assertThat(request.temperature()).isEqualTo(99.9D);
    }
}
