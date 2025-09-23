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
package com.lincoco.springai.zhinao;

import com.lincoco.springai.zhinao.api.ZhinaoApi;
import com.lincoco.springai.zhinao.api.ZhinaoConstants;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.support.UsageCalculator;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZhinaoChatModel is a {@link ChatModel} implementation that uses the Zhinao
 */
public class ZhinaoChatModel implements ChatModel, StreamingChatModel {

    private static final Logger logger = LoggerFactory.getLogger(ZhinaoChatModel.class);

    private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

    private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

    private final ZhinaoChatOptions defaultOptions;
    private final ZhinaoApi zhinaoApi;
    private final RetryTemplate retryTemplate;
    private final ObservationRegistry observationRegistry;
    private final ToolCallingManager toolCallingManager;
    private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;

    private ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;


    public ZhinaoChatModel(ZhinaoApi zhinaoApi, ZhinaoChatOptions defaultOptions,
                           ToolCallingManager toolCallingManager,RetryTemplate retryTemplate,
                           ObservationRegistry observationRegistry) {
        this(zhinaoApi, defaultOptions, DEFAULT_TOOL_CALLING_MANAGER, retryTemplate, observationRegistry, new DefaultToolExecutionEligibilityPredicate());
    }

    public ZhinaoChatModel(ZhinaoApi zhinaoApi, ZhinaoChatOptions defaultOptions,
                           ToolCallingManager toolCallingManager,RetryTemplate retryTemplate,
                            ObservationRegistry observationRegistry, ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
        Assert.notNull(zhinaoApi, "zhinaoApi cannot be null");
        Assert.notNull(defaultOptions, "defaultOptions cannot be null");
        Assert.notNull(toolCallingManager, "toolCallingManager cannot be null");
        Assert.notNull(retryTemplate, "retryTemplate cannot be null");
        Assert.notNull(observationRegistry, "observationRegistry cannot be null");
        Assert.notNull(toolExecutionEligibilityPredicate, "toolExecutionEligibilityPredicate cannot be null");
        this.zhinaoApi = zhinaoApi;
        this.defaultOptions = defaultOptions;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
        this.toolCallingManager = toolCallingManager;
        this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Prompt requestPrompt = buildRequestPrompt(prompt);
        return this.internalCall(requestPrompt, null);
    }

    private ChatResponse internalCall(Prompt prompt, ChatResponse previousChatResponse) {
        ZhinaoApi.ChatCompletionRequest request = createRequest(prompt, false);
        ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(ZhinaoConstants.ZHINAO_PROVIDER_NAME)
                .build();
        ChatResponse response = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
                .observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry)
                .observe(() -> {
                    ResponseEntity<ZhinaoApi.ChatCompletion> completionEntity = this.retryTemplate.execute(ctx -> this.zhinaoApi.chatCompletionEntity(request));

                    ZhinaoApi.ChatCompletion chatCompletion = completionEntity.getBody();
                    if (chatCompletion == null) {
                        logger.warn("No chat completion returned for prompt: {}", prompt);
                        return new ChatResponse(List.of());
                    }
                    List<ZhinaoApi.Choice> choices = chatCompletion.choices();
                    if (choices == null) {
                        logger.warn("No choices returned for prompt: {}", prompt);
                        return new ChatResponse(List.of());
                    }
                    List<Generation> generations = choices.stream().map(choice -> {
                        Map<String, Object> metadata = Map.of(
                                "id", chatCompletion.id() != null ? chatCompletion.id() : "",
                                "role", choice.message().role() != null ? choice.message().role().name() : "",
                                "index", choice.index(),
                                "finishReason", choice.finishReason() != null ? choice.finishReason().name() : ""
                        );
                        return buildGeneration(choice, metadata);
                    }).toList();
                    // 当前的token消耗量
                    ZhinaoApi.Usage usage = chatCompletion.usage();
                    Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
                    Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage, previousChatResponse);
                    ChatResponse chatResponse = new ChatResponse(generations, from(chatCompletion, accumulatedUsage));
                    observationContext.setResponse(chatResponse);
                    return chatResponse;
                });
        if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response)) {
            ToolExecutionResult toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
            if (toolExecutionResult.returnDirect()) {
                // Return tool execution result directly to the client.
                return ChatResponse.builder()
                        .from(response)
                        .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                        .build();
            } else {
                // Send the tool execution result back to the model.
                return this.internalCall(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()), response);
            }
        }
        return response;
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return this.defaultOptions.copy();
    }

    public DefaultUsage getDefaultUsage(ZhinaoApi.Usage usage) {
        return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        Prompt requestPrompt = buildRequestPrompt(prompt);
        return internalStream(requestPrompt, null);
    }

    public Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse previousChatResponse) {
        return Flux.deferContextual(contextView -> {
            ZhinaoApi.ChatCompletionRequest request = createRequest(prompt, true);
            Flux<ZhinaoApi.ChatCompletionChunk> completionChunks = this.zhinaoApi.chatCompletionStream(request);

            ConcurrentHashMap<String, String> roleMap = new ConcurrentHashMap<>();

            ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                    .prompt(prompt)
                    .provider(ZhinaoConstants.ZHINAO_PROVIDER_NAME)
                    .build();

            Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(
                    this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry
            );
            observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

            Flux<ChatResponse> chatResponse = completionChunks.map(this::chunkToChatCompletion)
                    .switchMap(chatCompletion -> Mono.just(chatCompletion).map(chatCompletion2 -> {
                        try {
                            String id = chatCompletion2.id();
                            List<Generation> generations = chatCompletion2.choices().stream().map(choice -> {
                                if (choice.message().role() != null) {
                                    roleMap.putIfAbsent(id, choice.message().role().name());
                                }
                                Map<String, Object> metadata = Map.of(
                                        "id", chatCompletion2.id(),
                                        "role", roleMap.getOrDefault(id, ""),
                                        "finishReason", choice.finishReason() != null ? choice.finishReason().name() : ""
                                );
                                return buildGeneration(choice, metadata);
                            }).toList();
                            ZhinaoApi.Usage usage = chatCompletion2.usage();
                            Usage currentUsage = (usage != null) ? getDefaultUsage(usage) : new EmptyUsage();
                            Usage cumulativeUsage = UsageCalculator.getCumulativeUsage(currentUsage, previousChatResponse);
                            return new ChatResponse(generations, from(chatCompletion2, cumulativeUsage));
                        } catch (Exception e) {
                            logger.error("Error processing chat completion: ", e);
                            return new ChatResponse(List.of());
                        }
                    }));

            Flux<ChatResponse> flux = chatResponse.flatMap(response -> {
                if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response)) {
                    return Flux.defer(() -> {
                        ToolExecutionResult toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
                        if (toolExecutionResult.returnDirect()) {
                            return Flux.just(ChatResponse.builder().from(response)
                                    .generations(ToolExecutionResult.buildGenerations(toolExecutionResult)).build());
                        } else {
                            return this.internalStream(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()), response);
                        }
                    }).subscribeOn(Schedulers.boundedElastic());
                } else {
                    return Flux.just(response);
                }
            }).doOnError(observation::error).doFinally(s -> observation.stop())
                    .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
            return new MessageAggregator().aggregate(flux, observationContext::setResponse);
        });
    }

    private ChatResponseMetadata from(ZhinaoApi.ChatCompletion result, Usage usage) {
        Assert.notNull(result, "Zhinao ChatCompletion must not be null");
        return ChatResponseMetadata.builder()
                .id(result.id() != null ? result.id() : "")
                .usage(usage)
                .model(result.model() != null ? result.model() : "")
                .keyValue("created", result.created() != null ? result.created() : 0L)
                .build();
    }

    private ZhinaoApi.ChatCompletion chunkToChatCompletion(ZhinaoApi.ChatCompletionChunk chunk) {
        List<ZhinaoApi.Choice> choices = chunk.choices().stream().map(cc -> {
            ZhinaoApi.ChatCompletionMessage delta = cc.delta();
            if (delta == null) {
                delta = new ZhinaoApi.ChatCompletionMessage("", ZhinaoApi.ChatCompletionMessage.Role.ASSISTANT);
            }
            return new ZhinaoApi.Choice(delta, cc.finishReason(), cc.index());
        }).toList();
        return new ZhinaoApi.ChatCompletion(choices, chunk.created(), chunk.id(), chunk.model(), chunk.object(), chunk.usage());
    }

    /**
     * 构建请求使用的Prompt对象，合并运行时选项和默认选项
     *
     * @param prompt 原始Prompt对象，包含指令和可选的运行时选项
     * @return 合并了默认选项和运行时选项的新Prompt对象
     */
    protected Prompt buildRequestPrompt(Prompt prompt) {
        ZhinaoChatOptions runtimeOptions = null;
        if (prompt.getOptions() != null) {
            // 根据选项类型进行转换，优先处理ToolCallingChatOptions类型
            if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
                runtimeOptions = ModelOptionsUtils.copyToTarget(toolCallingChatOptions, ToolCallingChatOptions.class, ZhinaoChatOptions.class);
            } else {
                runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(), ChatOptions.class, ZhinaoChatOptions.class);
            }
        }

        // 合并运行时选项和默认选项
        ZhinaoChatOptions requestOptions = ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions, ZhinaoChatOptions.class);

        if (runtimeOptions != null) {
            // 当存在运行时选项时，合并各个工具相关配置项
            requestOptions.setInternalToolExecutionEnabled(
                    ModelOptionsUtils.mergeOption(runtimeOptions.getInternalToolExecutionEnabled(),
                            this.defaultOptions.getInternalToolExecutionEnabled()));
            requestOptions.setToolNames(ToolCallingChatOptions.mergeToolNames(runtimeOptions.getToolNames(),
                    this.defaultOptions.getToolNames()));
            requestOptions.setToolCallbacks(ToolCallingChatOptions.mergeToolCallbacks(runtimeOptions.getToolCallbacks(),
                    this.defaultOptions.getToolCallbacks()));
            requestOptions.setToolContext(ToolCallingChatOptions.mergeToolContext(runtimeOptions.getToolContext(),
                    this.defaultOptions.getToolContext()));
        } else {
            // 当不存在运行时选项时，直接使用默认配置项
            requestOptions.setInternalToolExecutionEnabled(this.defaultOptions.getInternalToolExecutionEnabled());
            requestOptions.setToolNames(this.defaultOptions.getToolNames());
            requestOptions.setToolCallbacks(this.defaultOptions.getToolCallbacks());
            requestOptions.setToolContext(this.defaultOptions.getToolContext());
        }

        // 验证工具回调配置的有效性
        ToolCallingChatOptions.validateToolCallbacks(requestOptions.getToolCallbacks());

        return new Prompt(prompt.getInstructions(), requestOptions);
    }


    protected ZhinaoApi.ChatCompletionRequest createRequest(Prompt prompt, boolean stream) {
        List<ZhinaoApi.ChatCompletionMessage> chatCompletionMessages = prompt.getInstructions().stream().map(message -> {
            if (message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.SYSTEM) {
                return List.of(new ZhinaoApi.ChatCompletionMessage(message.getText(), ZhinaoApi.ChatCompletionMessage.Role.valueOf(message.getMessageType().name())));
            } else if (message.getMessageType() == MessageType.ASSISTANT) {
                AssistantMessage assistantMessage = (AssistantMessage) message;
                List<ZhinaoApi.ChatCompletionMessage.ToolCall> toolCalls = null;
                if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                    toolCalls = assistantMessage.getToolCalls().stream().map(toolCall -> {
                        ZhinaoApi.ChatCompletionMessage.ChatCompletionFunction function = new ZhinaoApi.ChatCompletionMessage.ChatCompletionFunction(toolCall.name(), toolCall.arguments());
                        return new ZhinaoApi.ChatCompletionMessage.ToolCall(toolCall.id(), toolCall.type(), function);
                    }).toList();
                }
                return List.of(new ZhinaoApi.ChatCompletionMessage(ZhinaoApi.ChatCompletionMessage.Role.ASSISTANT, message.getText(), null,null, toolCalls));
            } else if (message.getMessageType() == MessageType.TOOL){
                ToolResponseMessage toolMessage = (ToolResponseMessage) message;
                toolMessage.getResponses()
                        .forEach(response -> Assert.isTrue(response.id() != null, "ToolResponseMessage must have an id"));
                return toolMessage.getResponses().stream()
                        .map(tr -> new ZhinaoApi.ChatCompletionMessage(ZhinaoApi.ChatCompletionMessage.Role.TOOL, tr.responseData(), tr.name(), tr.id(), null))
                        .toList();
            } else {
                throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
            }
        }).flatMap(List::stream).toList();

        ZhinaoApi.ChatCompletionRequest request = new ZhinaoApi.ChatCompletionRequest(chatCompletionMessages, stream);

        ZhinaoChatOptions requestOptions = (ZhinaoChatOptions) prompt.getOptions();
        request = ModelOptionsUtils.merge(requestOptions, request, ZhinaoApi.ChatCompletionRequest.class);

        List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
        if (!CollectionUtils.isEmpty(toolDefinitions)) {
            request = ModelOptionsUtils.merge(ZhinaoChatOptions.builder().tools(this.getFunctionTools(toolDefinitions)).build(),
                    request, ZhinaoApi.ChatCompletionRequest.class);
        }
        return request;
    }

    private List<ZhinaoApi.FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
        return toolDefinitions.stream().map(toolDefinition -> {
            ZhinaoApi.FunctionTool.Function function = new ZhinaoApi.FunctionTool.Function(toolDefinition.description(), toolDefinition.name(), toolDefinition.inputSchema());
            return new ZhinaoApi.FunctionTool(function);
        }).toList();
    }

    private static Generation buildGeneration(ZhinaoApi.Choice choice, Map<String, Object> metadata) {
        List<AssistantMessage.ToolCall> toolCalls = choice.message().toolCalls() == null ? List.of() : choice.message().toolCalls().stream()
                .map(toolCall -> new AssistantMessage.ToolCall(toolCall.id(), toolCall.type(),
                        toolCall.function().name(), toolCall.function().arguments())).toList();
        AssistantMessage assistantMessage = new AssistantMessage(choice.message().content(), metadata, toolCalls);
        String finishReason = choice.finishReason() != null ? choice.finishReason().name() : "";
        ChatGenerationMetadata generationMetadata = ChatGenerationMetadata.builder().finishReason(finishReason).build();
        return new Generation(assistantMessage, generationMetadata);
    }

    public void setObservationConvention(ChatModelObservationConvention chatModelObservationConvention) {
        this.observationConvention = chatModelObservationConvention;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ZhinaoApi zhinaoApi;

        private ZhinaoChatOptions defaultOptions = ZhinaoChatOptions.builder()
                .model(ZhinaoApi.DEFAULT_CHAT_MODEL)
                .temperature(0.7)
                .build();

        private ToolCallingManager toolCallingManager;

        private ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();

        private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

        private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

        private Builder() {
        }

        public Builder zhinaoApi(ZhinaoApi zhinaoApi) {
            this.zhinaoApi = zhinaoApi;
            return this;
        }

        public Builder defaultOptions(ZhinaoChatOptions defaultOptions) {
            this.defaultOptions = defaultOptions;
            return this;
        }

        public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
            this.toolCallingManager = toolCallingManager;
            return this;
        }

        public Builder toolExecutionEligibilityPredicate(ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
            this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
            return this;
        }

        public Builder retryTemplate(RetryTemplate retryTemplate) {
            this.retryTemplate = retryTemplate;
            return this;
        }

        public Builder observationRegistry(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        public ZhinaoChatModel build() {
            return new ZhinaoChatModel(this.zhinaoApi, this.defaultOptions, Objects.requireNonNullElse(this.toolCallingManager, DEFAULT_TOOL_CALLING_MANAGER),
                    this.retryTemplate, this.observationRegistry, this.toolExecutionEligibilityPredicate);
        }
    }
}
