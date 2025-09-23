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

package com.lincoco.springai.zhinao.autoconfigure.tool;

import com.lincoco.springai.zhinao.ZhinaoChatModel;
import com.lincoco.springai.zhinao.ZhinaoChatOptions;
import com.lincoco.springai.zhinao.autoconfigure.ZhinaoChatAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Geng Rong
 * @author Alexandros Pappas
 */
@EnabledIfEnvironmentVariable(named = "ZHINAO_API_KEY", matches = ".*")
public class ZhinaoFunctionCallbackIT {

	private final Logger logger = LoggerFactory.getLogger(ZhinaoFunctionCallbackIT.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.ai.zhinao.apiKey=" + System.getenv("ZHINAO_API_KEY"))
		.withConfiguration(AutoConfigurations.of(ZhinaoChatAutoConfiguration.class))
		.withUserConfiguration(Config.class);

	@Test
	void functionCallTest() {
		this.contextRunner.run(context -> {

			ZhinaoChatModel chatModel = context.getBean(ZhinaoChatModel.class);

			UserMessage userMessage = new UserMessage(
					"What's the weather like in San Francisco, Tokyo, and Paris? Return the temperature in Celsius");

			ChatResponse response = chatModel
				.call(new Prompt(List.of(userMessage), ZhinaoChatOptions.builder().toolNames("WeatherInfo").build()));

			logger.info("Response: {}", response);

			assertThat(response.getResult().getOutput().getText()).contains("30", "10", "15");

		});
	}

	/**
	 * 依据官方文档：工具调用模式，仅支持非流式
	 */
	//@Test
	void streamFunctionCallTest() {
		this.contextRunner.run(context -> {

			ZhinaoChatModel chatModel = context.getBean(ZhinaoChatModel.class);

			UserMessage userMessage = new UserMessage(
					"What's the weather like in San Francisco, Tokyo, and Paris? Return the temperature in Celsius");

			Flux<ChatResponse> response = chatModel.stream(
					new Prompt(List.of(userMessage), ZhinaoChatOptions.builder().toolNames("WeatherInfo").build()));

			String content = response.collectList()
				.block()
				.stream()
				.map(ChatResponse::getResults)
				.flatMap(List::stream)
				.map(Generation::getOutput)
				.map(AssistantMessage::getText)
				.filter(Objects::nonNull)
				.collect(Collectors.joining());
			logger.info("Response: {}", content);

			assertThat(content).containsAnyOf("30.0", "30");
			assertThat(content).containsAnyOf("10.0", "10");
			assertThat(content).containsAnyOf("15.0", "15");

		});
	}

	@Configuration
	static class Config {

		@Bean
		public ToolCallback weatherFunctionInfo() {

			return FunctionToolCallback.builder("WeatherInfo", new MockWeatherService())
				.description("Get the weather in location")
				.inputType(MockWeatherService.Request.class)
				.build();
		}

	}

}
