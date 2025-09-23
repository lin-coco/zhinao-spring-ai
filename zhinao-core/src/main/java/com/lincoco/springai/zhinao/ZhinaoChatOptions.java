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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lincoco.springai.zhinao.api.ZhinaoApi;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Options for Zhinao Chat completions.
 * @author xueyeshang
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZhinaoChatOptions implements ToolCallingChatOptions {

    private @JsonProperty("model") String model;
    private @JsonProperty("temperature") Double temperature;
    private @JsonProperty("max_tokens") Integer maxTokens;
    private @JsonProperty("top_p") Double topP;
    private @JsonProperty("top_k") Integer topK;
    private @JsonProperty("repetition_penalty") Double repetitionPenalty;
    private @JsonProperty("num_beams") Integer numBeams;
    private @JsonProperty("tools") List<ZhinaoApi.FunctionTool> tools;
    private @JsonProperty("tool_choice") String toolChoice;
    private @JsonProperty("user") String user;
    /**
     * Collection of {@link ToolCallback}s to be used for tool calling in the chat
     * completion requests.
     */
    @JsonIgnore
    private List<ToolCallback> toolCallbacks = new ArrayList<>();
    /**
     * Collection of tool names to be resolved at runtime and used for tool calling in the
     * chat completion requests.
     */
    @JsonIgnore
    private Set<String> toolNames = new HashSet<>();
    /**
     * Whether to enable the tool execution lifecycle internally in ChatModel.
     */
    @JsonIgnore
    private Boolean internalToolExecutionEnabled;
    @JsonIgnore
    private Map<String, Object> toolContext = new HashMap<>();

    public static Builder builder() {
        return new Builder();
    }

    public static ZhinaoChatOptions fromOptions(ZhinaoChatOptions options) {
        return ZhinaoChatOptions.builder()
                .model(options.getModel())
                .temperature(options.getTemperature())
                .maxTokens(options.getMaxTokens())
                .topP(options.getTopP())
                .topK(options.getTopK())
                .repetitionPenalty(options.getRepetitionPenalty())
                .numBeams(options.getNumBeams())
                .tools(options.getTools())
                .toolChoice(options.getToolChoice())
                .user(options.getUser())
                .toolCallbacks(options.getToolCallbacks() != null ? new ArrayList<>(options.getToolCallbacks()) : null)
                .toolNames(options.getToolNames() != null ? new HashSet<>(options.getToolNames()) : null)
                .internalToolExecutionEnabled(options.getInternalToolExecutionEnabled())
                .toolContext(options.getToolContext() != null ? new HashMap<>(options.getToolContext()) : null)
                .build();
    }


    @Override
    public List<ToolCallback> getToolCallbacks() {
        return this.toolCallbacks;
    }

    @Override
    public void setToolCallbacks(List<ToolCallback> toolCallbacks) {
        Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
        Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
        this.toolCallbacks = toolCallbacks;
    }

    @Override
    public Set<String> getToolNames() {
        return this.toolNames;
    }

    @Override
    public void setToolNames(Set<String> toolNames) {
        Assert.notNull(toolNames, "toolNames cannot be null");
        Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
        toolNames.forEach(tool -> Assert.hasText(tool, "toolNames cannot contain empty elements"));
        this.toolNames = toolNames;
    }

    @Override
    public Boolean getInternalToolExecutionEnabled() {
        return this.internalToolExecutionEnabled;
    }

    @Override
    public void setInternalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
        this.internalToolExecutionEnabled = internalToolExecutionEnabled;
    }

    @Override
    public Map<String, Object> getToolContext() {
        return this.toolContext;
    }

    @Override
    public void setToolContext(Map<String, Object> toolContext) {
        this.toolContext = toolContext;
    }

    @Override
    public String getModel() {
        return this.model;
    }
    public void setModel(String model) {
        this.model = model;
    }
    @Override
    public Double getTemperature() {
        return this.temperature;
    }
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    @Override
    public Integer getMaxTokens() {
        return this.maxTokens;
    }
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    @Override
    public Double getTopP() {
        return this.topP;
    }
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    @Override
    public Integer getTopK() {
        return this.topK;
    }
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    @Override
    public Double getFrequencyPenalty() {
        return repetitionPenalty;
    }
    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.repetitionPenalty = frequencyPenalty;
    }
    public Double getRepetitionPenalty() {
        return repetitionPenalty;
    }
    public void setRepetitionPenalty(Double repetitionPenalty) {
        this.repetitionPenalty = repetitionPenalty;
    }

    public Integer getNumBeams() {
        return numBeams;
    }
    public void setNumBeams(Integer numBeams) {
        this.numBeams = numBeams;
    }
    public List<ZhinaoApi.FunctionTool> getTools() {
        return tools;
    }
    public void setTools(List<ZhinaoApi.FunctionTool> tools) {
        this.tools = tools;
    }
    public String getToolChoice() {
        return toolChoice;
    }
    public void setToolChoice(String toolChoice) {
        this.toolChoice = toolChoice;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    @Override
    public Double getPresencePenalty() {
        return null;
    }
    public void setPresencePenalty(Double presencePenalty) {
        // no-op
    }

    @Override
    public List<String> getStopSequences() {
        return List.of();
    }
    public void setStopSequences(List<String> stopSequences) {
        // no-op
    }

    @Override
    public ZhinaoChatOptions copy() {
        return ZhinaoChatOptions.fromOptions(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ZhinaoChatOptions that = (ZhinaoChatOptions) o;
        return Objects.equals(model, that.model) && Objects.equals(temperature, that.temperature)
                && Objects.equals(maxTokens, that.maxTokens) && Objects.equals(topP, that.topP)
                && Objects.equals(topK, that.topK) && Objects.equals(repetitionPenalty, that.repetitionPenalty)
                && Objects.equals(numBeams, that.numBeams) && Objects.equals(tools, that.tools)
                && Objects.equals(toolChoice, that.toolChoice) && Objects.equals(user, that.user)
                && Objects.equals(toolCallbacks, that.toolCallbacks) && Objects.equals(toolNames, that.toolNames)
                && Objects.equals(internalToolExecutionEnabled, that.internalToolExecutionEnabled) && Objects.equals(toolContext, that.toolContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, temperature, maxTokens, topP, topK, repetitionPenalty, numBeams, tools, toolChoice, user, toolCallbacks, toolNames, internalToolExecutionEnabled, toolContext);
    }

    @Override
    public String toString() {
        return "ZhinaoChatOptions: " + ModelOptionsUtils.toJsonString(this);
    }

    public static class Builder {
        protected ZhinaoChatOptions options;
        public Builder() {
            this.options = new ZhinaoChatOptions();
        }
        public Builder(ZhinaoChatOptions options) {
            this.options = options;
        }
        public Builder model(String model) {
            this.options.model = model;
            return this;
        }
        public Builder mode(ZhinaoApi.ChatModel model) {
            this.options.model = model.getName();
            return this;
        }
        public Builder temperature(Double temperature) {
            this.options.temperature = temperature;
            return this;
        }
        public Builder maxTokens(Integer maxTokens) {
            this.options.maxTokens = maxTokens;
            return this;
        }
        public Builder topP(Double topP) {
            this.options.topP = topP;
            return this;
        }
        public Builder topK(Integer topK) {
            this.options.topK = topK;
            return this;
        }
        public Builder repetitionPenalty(Double repetitionPenalty) {
            this.options.repetitionPenalty = repetitionPenalty;
            return this;
        }
        public Builder numBeams(Integer numBeams) {
            this.options.numBeams = numBeams;
            return this;
        }
        public Builder tools(List<ZhinaoApi.FunctionTool> tools) {
            this.options.tools = tools;
            return this;
        }
        public Builder toolChoice(String toolChoice) {
            this.options.toolChoice = toolChoice;
            return this;
        }
        public Builder user(String user) {
            this.options.user = user;
            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
            this.options.setToolCallbacks(toolCallbacks);
            return this;
        }

        public Builder toolCallbacks(ToolCallback... toolCallbacks) {
            Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
            this.options.toolCallbacks.addAll(List.of(toolCallbacks));
            return this;
        }

        public Builder toolNames(Set<String> toolNames) {
            Assert.notNull(toolNames, "toolNames cannot be null");
            this.options.setToolNames(toolNames);
            return this;
        }

        public Builder toolNames(String... toolNames) {
            Assert.notNull(toolNames, "toolNames cannot be null");
            this.options.toolNames.addAll(Set.of(toolNames));
            return this;
        }

        public Builder internalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
            this.options.setInternalToolExecutionEnabled(internalToolExecutionEnabled);
            this.options.internalToolExecutionEnabled = internalToolExecutionEnabled;
            return this;
        }

        public Builder toolContext(Map<String, Object> toolContext) {
            if (this.options.toolContext == null) {
                this.options.toolContext = toolContext;
            } else {
                this.options.toolContext.putAll(toolContext);
            }
            return this;
        }

        public ZhinaoChatOptions build() {
            return this.options;
        }
    }
}
