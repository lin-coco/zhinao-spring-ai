package com.lincoco.springai.zhinao;

import com.lincoco.springai.zhinao.ZhinaoChatModel;
import com.lincoco.springai.zhinao.ZhinaoChatOptions;
import com.lincoco.springai.zhinao.api.ZhinaoApi;
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
