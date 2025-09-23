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

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xueyeshang
 */
public class ZhinaoStreamFunctionCallingHelper {

    public ZhinaoApi.ChatCompletionChunk merge(ZhinaoApi.ChatCompletionChunk previous, ZhinaoApi.ChatCompletionChunk current) {

        if (previous == null) {
            return current;
        }

        String id = (current.id() != null ? current.id() : previous.id());
        Long created = (current.created() != null ? current.created() : previous.created());
        String model = (current.model() != null ? current.model() : previous.model());
        String object = (current.object() != null ? current.object() : previous.object());
        ZhinaoApi.Usage usage = (current.usage() != null ? current.usage() : previous.usage());

        ZhinaoApi.ChunkChoice previousChoice0 = (CollectionUtils.isEmpty(previous.choices()) ? null : previous.choices().get(0));
        ZhinaoApi.ChunkChoice currentChoice0 = (CollectionUtils.isEmpty(current.choices()) ? null : current.choices().get(0));

        ZhinaoApi.ChunkChoice choice = merge(previousChoice0, currentChoice0);
        List<ZhinaoApi.ChunkChoice> chunkChoices = choice == null ? List.of() : List.of(choice);

        return new ZhinaoApi.ChatCompletionChunk(chunkChoices, created, id, model, object, usage);
    }

    private ZhinaoApi.ChunkChoice merge(ZhinaoApi.ChunkChoice previous, ZhinaoApi.ChunkChoice current) {
        if (previous == null) {
            return current;
        }

        ZhinaoApi.Choice.ChatCompletionFinishReason finishReason = (current.finishReason() != null ? current.finishReason()
                : previous.finishReason());
        Integer index = (current.index() != null ? current.index() : previous.index());

        ZhinaoApi.ChatCompletionMessage message = merge(previous.delta(), current.delta());
        return new ZhinaoApi.ChunkChoice(message, finishReason, index);
    }

    private ZhinaoApi.ChatCompletionMessage merge(ZhinaoApi.ChatCompletionMessage previous, ZhinaoApi.ChatCompletionMessage current) {
        String content = (current.content() != null ? current.content()
                : (previous.content() != null) ? previous.content() : "");
        ZhinaoApi.ChatCompletionMessage.Role role = (current.role() != null ? current.role() : previous.role());
        // default to ASSISTANT if null
        role = (role != null ? role : ZhinaoApi.ChatCompletionMessage.Role.ASSISTANT);

        List<ZhinaoApi.ChatCompletionMessage.ToolCall> toolCalls = new ArrayList<>();
        ZhinaoApi.ChatCompletionMessage.ToolCall lastPreviousTooCall = null;
        if (previous.toolCalls() != null) {
            lastPreviousTooCall = previous.toolCalls().get(previous.toolCalls().size() - 1);
            if (previous.toolCalls().size() > 1) {
                toolCalls.addAll(previous.toolCalls().subList(0, previous.toolCalls().size() - 1));
            }
        }
        if (current.toolCalls() != null) {
            if (current.toolCalls().size() > 1) {
                throw new IllegalStateException("Currently only one tool call is supported per message!");
            }
            var currentToolCall = current.toolCalls().iterator().next();
            if (currentToolCall.id() != null) {
                if (lastPreviousTooCall != null) {
                    toolCalls.add(lastPreviousTooCall);
                }
                toolCalls.add(currentToolCall);
            }
            else {
                toolCalls.add(merge(lastPreviousTooCall, currentToolCall));
            }
        }
        else {
            if (lastPreviousTooCall != null) {
                toolCalls.add(lastPreviousTooCall);
            }
        }
        return new ZhinaoApi.ChatCompletionMessage(role, content,  null, null,toolCalls);
    }

    private ZhinaoApi.ChatCompletionMessage.ToolCall merge(ZhinaoApi.ChatCompletionMessage.ToolCall previous, ZhinaoApi.ChatCompletionMessage.ToolCall current) {
        if (previous == null) {
            return current;
        }
        String id = (current.id() != null ? current.id() : previous.id());
        String type = (current.type() != null ? current.type() : previous.type());
        ZhinaoApi.ChatCompletionMessage.ChatCompletionFunction function = merge(previous.function(), current.function());
        return new ZhinaoApi.ChatCompletionMessage.ToolCall(id, type, function);
    }

    private ZhinaoApi.ChatCompletionMessage.ChatCompletionFunction merge(ZhinaoApi.ChatCompletionMessage.ChatCompletionFunction previous, ZhinaoApi.ChatCompletionMessage.ChatCompletionFunction current) {
        if (previous == null) {
            return current;
        }
        String name = (current.name() != null ? current.name() : previous.name());
        StringBuilder parameters = new StringBuilder();
        if (previous.arguments() != null) {
            parameters.append(previous.arguments());
        }
        if (current.arguments() != null) {
            parameters.append(current.arguments());
        }
        return new ZhinaoApi.ChatCompletionMessage.ChatCompletionFunction(name, parameters.toString());
    }

    /**
     * @param chatCompletion the ChatCompletionChunk to check
     * @return true if the ChatCompletionChunk is a streaming tool function call.
     */
    public boolean isStreamingToolFunctionCall(ZhinaoApi.ChatCompletionChunk chatCompletion) {

        if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.choices())) {
            return false;
        }

        var choice = chatCompletion.choices().get(0);
        if (choice == null || choice.delta() == null) {
            return false;
        }
        return !CollectionUtils.isEmpty(choice.delta().toolCalls());
    }

    /**
     * @param chatCompletion the ChatCompletionChunk to check
     * @return true if the ChatCompletionChunk is a streaming tool function call and it is
     * the last one.
     */
    public boolean isStreamingToolFunctionCallFinish(ZhinaoApi.ChatCompletionChunk chatCompletion) {

        if (chatCompletion == null || CollectionUtils.isEmpty(chatCompletion.choices())) {
            return false;
        }

        var choice = chatCompletion.choices().get(0);
        if (choice == null || choice.delta() == null) {
            return false;
        }
        return choice.finishReason() == ZhinaoApi.Choice.ChatCompletionFinishReason.TOOL_CALLS;
    }
}
