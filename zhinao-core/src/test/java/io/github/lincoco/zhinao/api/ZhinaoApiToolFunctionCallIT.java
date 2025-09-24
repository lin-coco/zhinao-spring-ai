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
package io.github.lincoco.zhinao.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class ZhinaoApiToolFunctionCallIT {

    private final static Logger logger = LoggerFactory.getLogger(ZhinaoApiToolFunctionCallIT.class);

    private final ZhinaoApi zhinaoApi = ZhinaoApi.builder().apiKey(System.getenv("ZHINAO_API_KEY")).build();

    private final MockWeatherService weatherService = new MockWeatherService();

    private static final ZhinaoApi.FunctionTool FUNCTION_TOOL = new ZhinaoApi.FunctionTool(ZhinaoApi.FunctionTool.Type.FUNCTION, new ZhinaoApi.FunctionTool.Function(
            "Get the weather in location. Return temperature in 30°F or 30°C format. The location must include the English name.",
            "getCurrentWeather", """
					{
						"type": "object",
						"properties": {
							"location": {
								"type": "string",
								"description": "The city and state e.g. San Francisco, CA. Must include the English name."
							},
							"lat": {
								"type": "number",
								"description": "The city latitude"
							},
							"lon": {
								"type": "number",
								"description": "The city longitude"
							},
							"unit": {
								"type": "string",
								"enum": ["C", "F"]
							}
						},
						"required": ["location", "lat", "lon", "unit"]
					}
					"""));

    private static <T> T fromJson(String json, Class<T> targerClass) {
        try {
            return new ObjectMapper().readValue(json, targerClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void toolFunctionCall() {
        toolFunctionCall("What's the weather like in San Francisco? Return the temperature in Celsius.", "San Francisco");
    }

    @Test
    public void toolFunctionCallChinese() {
        toolFunctionCall("旧金山、东京和巴黎的气温怎么样? 返回摄氏度的温度", "旧金山");
    }


    private void toolFunctionCall(String userMessage, String cityName) {
        // 第一步: 发送消息获取大模型返回的可获得的工具列表
        ZhinaoApi.ChatCompletionMessage message = new ZhinaoApi.ChatCompletionMessage(userMessage, ZhinaoApi.ChatCompletionMessage.Role.USER);
        List<ZhinaoApi.ChatCompletionMessage> messages = new ArrayList<>(List.of(message));
        ZhinaoApi.ChatCompletionRequest chatCompletionRequest = new ZhinaoApi.ChatCompletionRequest(messages,
                ZhinaoApi.ChatModel.GPT_PRO.getValue(), List.of(FUNCTION_TOOL), ZhinaoApi.ToolChoiceBuilder.AUTO);

        ResponseEntity<ZhinaoApi.ChatCompletion> chatCompletion = this.zhinaoApi.chatCompletionEntity(chatCompletionRequest);
        assertThat(chatCompletion).isNotNull();
        assertThat(chatCompletion.getBody()).isNotNull();
        assertThat(chatCompletion.getBody().choices()).isNotNull();
        ZhinaoApi.ChatCompletionMessage responseMessage = chatCompletion.getBody().choices().get(0).message();
        assertThat(responseMessage).isNotNull();
        assertThat(responseMessage.role()).isEqualTo(ZhinaoApi.ChatCompletionMessage.Role.ASSISTANT);
        assertThat(responseMessage.toolCalls()).isNotNull();

        messages.add(responseMessage);

        // 第二步: 将每个函数调用和函数响应的信息发送到模型。
        for (ZhinaoApi.ChatCompletionMessage.ToolCall toolCall : responseMessage.toolCalls()) {
            String functionName = toolCall.function().name();
            String id = toolCall.id();
            if ("getCurrentWeather".equals(functionName)) {
                MockWeatherService.Request weatherRequest = fromJson(toolCall.function().arguments(), MockWeatherService.Request.class);
                MockWeatherService.Response weatherResponse = weatherService.apply(weatherRequest);

                messages.add(new ZhinaoApi.ChatCompletionMessage(ZhinaoApi.ChatCompletionMessage.Role.TOOL,
                        "" + weatherResponse.temp() + weatherRequest.unit(), functionName, id,null));
            }
        }
        ZhinaoApi.ChatCompletionRequest functionResponseRequest = new ZhinaoApi.ChatCompletionRequest(messages, ZhinaoApi.ChatModel.GPT_PRO.getValue(), 0.5);
        ResponseEntity<ZhinaoApi.ChatCompletion> functionResponse = this.zhinaoApi.chatCompletionEntity(functionResponseRequest);
        logger.info("Final request: {}", functionResponseRequest);
        logger.info("Final response: {}", functionResponse.getBody());
        assertThat(Objects.requireNonNull(functionResponse.getBody()).choices()).isNotEmpty();
        assertThat(functionResponse.getBody().choices().get(0).message().role()).isEqualTo(ZhinaoApi.ChatCompletionMessage.Role.ASSISTANT);
        assertThat(functionResponse.getBody().choices().get(0).message().content()).contains(cityName)
                .containsAnyOf("30");
    }
}
