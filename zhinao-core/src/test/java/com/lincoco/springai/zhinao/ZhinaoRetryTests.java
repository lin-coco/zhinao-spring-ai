package com.lincoco.springai.zhinao;

import com.lincoco.springai.zhinao.ZhinaoChatModel;
import com.lincoco.springai.zhinao.ZhinaoChatOptions;
import com.lincoco.springai.zhinao.api.ZhinaoApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ZhinaoRetryTests {

    private TestRetryListener retryListener;

    private @Mock ZhinaoApi zhinaoApi;

    private ZhinaoChatModel chatModel;

    @BeforeEach
    public void beforeEach() {
        RetryTemplate retryTemplate = RetryUtils.SHORT_RETRY_TEMPLATE;
        this.retryListener = new TestRetryListener();
        retryTemplate.registerListener(this.retryListener);

        this.chatModel = ZhinaoChatModel.builder()
                .zhinaoApi(this.zhinaoApi)
                .defaultOptions(ZhinaoChatOptions.builder().build())
                .retryTemplate(retryTemplate)
                .build();
    }

    @Test
    public void zhinaoChatTransientError() {

        var choice = new ZhinaoApi.Choice(new ZhinaoApi.ChatCompletionMessage("Response", ZhinaoApi.ChatCompletionMessage.Role.ASSISTANT), ZhinaoApi.Choice.ChatCompletionFinishReason.STOP, 0);
        ZhinaoApi.ChatCompletion expectedChatCompletion = new ZhinaoApi.ChatCompletion(List.of(choice), 789L, "id", "model",
                "chat.completion", new ZhinaoApi.Usage(10, 10, 10));

        given(this.zhinaoApi.chatCompletionEntity(isA(ZhinaoApi.ChatCompletionRequest.class)))
                .willThrow(new TransientAiException("Transient Error 1"))
                .willThrow(new TransientAiException("Transient Error 2"))
                .willReturn(ResponseEntity.of(Optional.of(expectedChatCompletion)));

        var result = this.chatModel.call(new Prompt("text"));

        assertThat(result).isNotNull();
        assertThat(result.getResult().getOutput().getText()).isSameAs("Response");
        assertThat(this.retryListener.onSuccessRetryCount).isEqualTo(2);
        assertThat(this.retryListener.onErrorRetryCount).isEqualTo(2);
    }

    @Test
    public void zhinaoChatNonTransientError() {
        given(this.zhinaoApi.chatCompletionEntity(isA(ZhinaoApi.ChatCompletionRequest.class)))
                .willThrow(new RuntimeException("Non Transient Error"));
        assertThrows(RuntimeException.class, () -> this.chatModel.call(new Prompt("text")));
    }

    @Test
    public void zhinaoChatStreamTransientError() {

        var choice = new ZhinaoApi.Choice(new ZhinaoApi.ChatCompletionMessage("Response", ZhinaoApi.ChatCompletionMessage.Role.ASSISTANT),
                ZhinaoApi.Choice.ChatCompletionFinishReason.STOP, 0);
        ZhinaoApi.ChatCompletion expectedChatCompletion = new ZhinaoApi.ChatCompletion(List.of(choice), 666L, "id", "model",
                "chat.completion", new ZhinaoApi.Usage(10, 10, 10));

        given(this.zhinaoApi.chatCompletionEntity(isA(ZhinaoApi.ChatCompletionRequest.class)))
                .willThrow(new TransientAiException("Transient Error 1"))
                .willThrow(new TransientAiException("Transient Error 2"))
                .willReturn(ResponseEntity.of(Optional.of(expectedChatCompletion)));

        var result = this.chatModel.call(new Prompt("text"));

        assertThat(result).isNotNull();
        assertThat(result.getResult().getOutput().getText()).isSameAs("Response");
        assertThat(this.retryListener.onSuccessRetryCount).isEqualTo(2);
        assertThat(this.retryListener.onErrorRetryCount).isEqualTo(2);
    }

    @Test
    public void zhinaoChatStreamNonTransientError() {
        given(this.zhinaoApi.chatCompletionStream(isA(ZhinaoApi.ChatCompletionRequest.class)))
                .willThrow(new RuntimeException("Non Transient Error"));
        assertThrows(RuntimeException.class, () -> this.chatModel.stream(new Prompt("text")).collectList().block());
    }

    private static class TestRetryListener implements RetryListener {

        int onErrorRetryCount = 0;

        int onSuccessRetryCount = 0;

        @Override
        public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
            this.onSuccessRetryCount = context.getRetryCount();
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                                                     Throwable throwable) {
            this.onErrorRetryCount = context.getRetryCount();
        }

    }
}
