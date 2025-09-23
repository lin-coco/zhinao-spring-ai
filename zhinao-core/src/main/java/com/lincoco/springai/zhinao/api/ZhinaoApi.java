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
package com.lincoco.springai.zhinao.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ChatModelDescription;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Single-class, Java Client library for Zhinao platform. Provides implementation for
 * the <a href="https://ai.360.com/platform/docs/overview">Chat Completion</a> APIs.
 * <p>
 * Implements <b>Synchronous</b> and <b>Streaming</b> chat completion.
 * </p>
 *
 * @author Geng Rong
 * @author Thomas Vitale
 */
public class ZhinaoApi {

    public static final String DEFAULT_CHAT_MODEL = ChatModel.GPT_PRO.getName();

    private static Predicate<String> SSE_DOME_PREDICATE = "[DONE]"::equals;

    private final String completionsPath;

    private final RestClient restClient;

    private final WebClient webClient;

    private final ZhinaoStreamFunctionCallingHelper chunkMerge = new ZhinaoStreamFunctionCallingHelper();

    /**
     * Create a new chat completion api.
     * @param baseUrl api base URL.
     * @param apiKey Zhinao API key.
     * @param headers the http headers to be added to the request.
     * @param completionsPath the path to the completions endpoint.
     * @param restClientBuilder the rest client builder.
     * @param webClientBuilder the web client builder.
     * @param responseErrorHandler the response error handler.
     */
    public ZhinaoApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> headers, String completionsPath,
                     RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
                     ResponseErrorHandler responseErrorHandler) {
        Assert.hasText(completionsPath, "Completions Path must not be null");
        Assert.notNull(headers, "Headers must not be null");

        this.completionsPath = completionsPath;

        // @formatter:off
        Consumer<HttpHeaders> finalHeaders = h -> {
            h.setBearerAuth(apiKey.getValue());
            h.setContentType(MediaType.APPLICATION_JSON);
            h.addAll(headers);
        };
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(finalHeaders)
                .defaultStatusHandler(responseErrorHandler)
                .build();
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(finalHeaders)
                .build();
    }

    /**
     * Creates a model response for the given chat conversation.
     * @param chatRequest the chat request.
     * @return the model response.
     */
    public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest) {
        Assert.notNull(chatRequest, "The request body can not be null.");
        Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");
        return this.restClient.post()
                .uri(completionsPath)
                .body(chatRequest)
                .retrieve().toEntity(ChatCompletion.class);
    }

    public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest) {
        Assert.notNull(chatRequest, "The request body can not be null.");
        Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");
        AtomicBoolean isInsideTool = new AtomicBoolean(false);

        return this.webClient.post()
                .uri(completionsPath)
                .body(Mono.just(chatRequest), ChatCompletionRequest.class)
                .retrieve()
                .bodyToFlux(String.class)
                .takeUntil(SSE_DOME_PREDICATE)
                .filter(SSE_DOME_PREDICATE.negate())
                .map(content -> ModelOptionsUtils.jsonToObject(content, ChatCompletionChunk.class))
                .map(chunk -> {
                    if (this.chunkMerge.isStreamingToolFunctionCall(chunk)) {
                        isInsideTool.set(true);
                    }
                    return chunk;
                })
                .windowUntil(chunk -> {
                    if (isInsideTool.get() && this.chunkMerge.isStreamingToolFunctionCallFinish(chunk)) {
                        isInsideTool.set(false);
                        return true;
                    }
                    return !isInsideTool.get();
                })
                .concatMapIterable(window -> {
                    Mono<ChatCompletionChunk> monoChunk = window.reduce(
                            new ChatCompletionChunk(null, null, null, null, null, null),
                            this.chunkMerge::merge);
                    return List.of(monoChunk);
                })
                .flatMap(mono -> mono);
    }

    public enum ChatModel implements ChatModelDescription {
        /**
         * 360gpt-pro
         */
        GPT_PRO("360gpt-pro");

        private final String value;
        ChatModel(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }

        @Override
        public String getName() {
            return value;
        }
    }

    /**
     * Chat completion request.
     * @param model 模型类型. 必填
     * @param messages 当前会话记录信息. 必填
     * @param stream 是否流式输出，默认是 false
     * @param temperature 取值应大于等于 0 小于等于 1，默认值是 0.9，更高的值代表结果更随机，较低的值代表结果更聚焦
     * @param maxTokens 取值应大于等于 1 小于等于 2048，默认值是 2048，代表输出结果的最大 token 数
     * @param topP 取值应大于等于 0 小于等于 1，默认值是 0.5
     * @param topK 取值应大于等于 0 小于等于 1024，默认值是 0
     * @param repetitionPenalty 取值应大于等于 1 小于等于 2，默认值是 1.05
     * @param numBeams 取值应大于等于 1 小于等于 5，默认值是 1
     * @param tools 控制哪个函数被调用，取值none代表不调用函数，auto代表由模型自主选择，
     * {"type": "function", "function": {"name": "my_function"}}代表强制模型调用指定函数
     * @param toolChoice tool choice
     * @param user 标记业务方用户 id，便于业务方区分不同用户
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionRequest(
            @JsonProperty("model") String model,
            @JsonProperty("messages") List<ChatCompletionMessage> messages,
            @JsonProperty("stream") Boolean stream,
            @JsonProperty("temperature") Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens,
            @JsonProperty("top_p") Double topP,
            @JsonProperty("top_k") Integer topK,
            @JsonProperty("repetition_penalty") Double repetitionPenalty,
            @JsonProperty("num_beams") Integer numBeams,
            @JsonProperty("tools") List<FunctionTool> tools,
            @JsonProperty("tool_choice") Object toolChoice,
            @JsonProperty("user") String user
    ) {
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, Boolean stream) {
            this(DEFAULT_CHAT_MODEL, messages, stream, null, null, null, null, null, null, null, null, null);
        }

        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Double temperature) {
            this(model, messages, false, temperature, null, null, null, null, null, null, null, null);
        }

        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Double temperature, Boolean stream) {
            this(model, messages, stream, temperature, null, null, null, null, null, null, null, null);
        }

        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, List<FunctionTool> tools, Object toolChoice) {
            this(model, messages, false, null, null, null, null, null, null, tools, toolChoice, null);
        }

    }

    /**
     * 聊天结果
     * @param choices 返回结果，一个结构体数组，目前不支持批量，因此正常返回时数组内含一个元素
     * @param created 时间戳，值是服务端接收到请求时的时间戳，以秒计
     * @param id 服务端生成的 uuid，代表本次请求 id，业务方可以记录方便排查问题
     * @param model 本次调用使用的模型名
     * @param object 暂未使用
     * @param usage token 消耗量
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletion(
            @JsonProperty("choices") List<Choice> choices,
            @JsonProperty("created") Long created,
            @JsonProperty("id") String id,
            @JsonProperty("model") String model,
            @JsonProperty("object") String object,
            @JsonProperty("usage") Usage usage
    ) {

    }

    /**
     * stream调用模式的聊天结果分片
     * @param choices 返回结果，一个结构体数组，目前不支持批量，因此正常返回时数组内含一个元素
     * @param created 时间戳，值是服务端接收到请求时的时间戳，以秒计
     * @param id 服务端生成的 uuid，代表本次请求 id，业务方可以记录方便排查问题
     * @param model 本次调用使用的模型名
     * @param object 暂未使用
     * @param usage token 消耗量
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionChunk(
            @JsonProperty("choices") List<ChunkChoice> choices,
            @JsonProperty("created") Long created,
            @JsonProperty("id") String id,
            @JsonProperty("model") String model,
            @JsonProperty("object") String object,
            @JsonProperty("usage") Usage usage
    ) {

    }

    /**
     * 聊天消息
     * @param role role 的取值有system,assistant,user. 必填
     * @param content content 为具体的对话内容. 必填
     * @param toolName 调用的工具名称，请求时不填，springai构造tool消息时使用
     * @param toolCallId 调用工具时返回的tool_call_id，请求时不填，apringai构造tool消息时使用，大模型调用响应会包含toolCallId
     * @param toolCalls 调用的工具列表，请求时不填，响应可能存在
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionMessage(
            @JsonProperty("role") Role role,
            @JsonProperty("content") String content,
            @JsonProperty("tool_name") String toolName,
            @JsonProperty("tool_call_id") String toolCallId,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls) {

        public ChatCompletionMessage(String content, Role role) {
            this(role, content, null, null, null);
        }

        public enum Role {
            @JsonProperty("system")
            SYSTEM,
            @JsonProperty("user")
            USER,
            @JsonProperty("assistant")
            ASSISTANT,
            /**
             * zhinao大模型目前返回tool类型的消息，tool消息由工具调用结果构建
             */
            @JsonProperty("tool")
            TOOL
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ToolCall(
                @JsonProperty("id") String id,
                @JsonProperty("type") String type,
                @JsonProperty("function") ChatCompletionFunction function) {
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ChatCompletionFunction(
                @JsonProperty("name") String name,
                @JsonProperty("arguments") String arguments) {
        }
    }

    /**
     * 模型可以调用的工具列表
     * @param type 工具类型，当前只支持function. 必填
     * @param function 函数相关配置. 必填
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunctionTool (
            @JsonProperty("type") Type type,
            @JsonProperty("function") Function function
    ){
        public FunctionTool(Function function) {
            this(Type.FUNCTION, function);
        }
        public enum Type {
            @JsonProperty("function")
            FUNCTION
        }
        /**
         * 函数相关配置
         * @param description 函数描述
         * @param name 函数名称. 必填
         * @param parameters 函数参数. 要描述不接受参数的函数，请提供{"type": "object", "properties": {}}
         */
        public record Function (
                @JsonProperty("description") String description,
                @JsonProperty("name") String name,
                @JsonProperty("parameters") Map<String, Object> parameters
        ){
            public Function(String description, String name, String jsonSchema) {
                this(description, name, ModelOptionsUtils.jsonToMap(jsonSchema));
            }
        }
    }

    /**
     * Helper factory that creates a tool_choice of type 'none', 'auto' or selected
     */
    public static class ToolChoiceBuilder {
        public static final String AUTO = "auto";
        public static final String AUTO_TOOL_CALL = "none";
        public static Object function(String functionName) {
            return Map.of("type", "function", "function", Map.of("name", functionName));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            @JsonProperty("message") ChatCompletionMessage message,
            @JsonProperty("finish_reason") ChatCompletionFinishReason finishReason,
            @JsonProperty("index") Integer index) {

        public enum ChatCompletionFinishReason {
            @JsonProperty("stop")
            STOP,
            @JsonProperty("content_filter")
            CONTENT_FILTER,
            @JsonProperty("tool_calls")
            TOOL_CALLS
        }

    }

    /**
     * token 消耗量
     * @param promptTokens 输入 token 消耗量
     * @param completionTokens 输出 token 消耗量
     * @param totalTokens 总 token 消耗量
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChunkChoice(
            @JsonProperty("delta") ChatCompletionMessage delta,
            @JsonProperty("finish_reason") Choice.ChatCompletionFinishReason finishReason,
            @JsonProperty("index") Integer index
    ) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl = ZhinaoConstants.DEFAULT_BASE_URL;
        private ApiKey apiKey;
        private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        private String completionsPath = ZhinaoConstants.DEFAULT_COMPLETIONS_PATH;
        private RestClient.Builder restClientBuilder = RestClient.builder();
        private WebClient.Builder webClientBuilder = WebClient.builder();
        private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

        public Builder baseUrl(String baseUrl) {
            Assert.hasText(baseUrl, "baseUrl cannot be null or empty");
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(ApiKey apiKey) {
            Assert.notNull(apiKey, "apiKey cannot be null");
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiKey(String simpleApikey) {
            Assert.notNull(simpleApikey, "simpleApikey cannot be null");
            this.apiKey = new SimpleApiKey(simpleApikey);
            return this;
        }

        public Builder headers(MultiValueMap<String, String> headers) {
            Assert.notNull(headers, "headers cannot be null");
            this.headers = headers;
            return this;
        }

        public Builder completionsPath(String completionsPath) {
            Assert.hasText(completionsPath, "completionsPath cannot be null or empty");
            this.completionsPath = completionsPath;
            return this;
        }

        public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
            Assert.notNull(restClientBuilder, "restClientBuilder cannot be null");
            this.restClientBuilder = restClientBuilder;
            return this;
        }

        public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
            Assert.notNull(webClientBuilder, "webClientBuilder cannot be null");
            this.webClientBuilder = webClientBuilder;
            return this;
        }

        public Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
            Assert.notNull(responseErrorHandler, "errorHandler cannot be null");
            this.responseErrorHandler = responseErrorHandler;
            return this;
        }

        public ZhinaoApi build() {
            Assert.notNull(apiKey, "apiKey cannot be null");
            return new ZhinaoApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath,
                    this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
        }
    }

}
