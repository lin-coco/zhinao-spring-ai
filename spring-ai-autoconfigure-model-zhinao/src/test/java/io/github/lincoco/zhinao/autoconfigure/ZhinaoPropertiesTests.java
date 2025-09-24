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

package io.github.lincoco.zhinao.autoconfigure;

import io.github.lincoco.zhinao.autoconfigure.ZhinaoChatAutoConfiguration;
import io.github.lincoco.zhinao.autoconfigure.ZhinaoChatProperties;
import io.github.lincoco.zhinao.autoconfigure.ZhinaoCommonProperties;
import io.github.lincoco.zhinao.ZhinaoChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class ZhinaoPropertiesTests {

	@Test
	public void chatProperties() {

		new ApplicationContextRunner().withPropertyValues(
				"spring.ai.zhinao.base-url=TEST_BASE_URL",
						"spring.ai.zhinao.api-key=abc123",
						"spring.ai.zhinao.chat.options.model=MODEL_XYZ",
						"spring.ai.zhinao.chat.options.temperature=0.55")
			.withConfiguration(AutoConfigurations.of(SpringAiRetryAutoConfiguration.class,
					RestClientAutoConfiguration.class, ZhinaoChatAutoConfiguration.class))
			.run(context -> {
				var chatProperties = context.getBean(ZhinaoChatProperties.class);
				var connectionProperties = context.getBean(ZhinaoCommonProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(chatProperties.getApiKey()).isNull();
				assertThat(chatProperties.getBaseUrl()).isNull();

				assertThat(chatProperties.getOptions().getModel()).isEqualTo("MODEL_XYZ");
				assertThat(chatProperties.getOptions().getTemperature()).isEqualTo(0.55);
			});
	}

	@Test
	public void chatOverrideConnectionProperties() {

		new ApplicationContextRunner().withPropertyValues(
				"spring.ai.zhinao.base-url=TEST_BASE_URL",
						"spring.ai.zhinao.api-key=abc123",
						"spring.ai.zhinao.chat.base-url=TEST_BASE_URL2",
						"spring.ai.zhinao.chat.api-key=456",
						"spring.ai.zhinao.chat.options.model=MODEL_XYZ",
						"spring.ai.zhinao.chat.options.temperature=0.55")
			.withConfiguration(AutoConfigurations.of(SpringAiRetryAutoConfiguration.class,
					RestClientAutoConfiguration.class, ZhinaoChatAutoConfiguration.class))
			.run(context -> {
				var chatProperties = context.getBean(ZhinaoChatProperties.class);
				var connectionProperties = context.getBean(ZhinaoCommonProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(chatProperties.getApiKey()).isEqualTo("456");
				assertThat(chatProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL2");

				assertThat(chatProperties.getOptions().getModel()).isEqualTo("MODEL_XYZ");
				assertThat(chatProperties.getOptions().getTemperature()).isEqualTo(0.55);
			});
	}

	@Test
	public void chatOptionsTest() {

		new ApplicationContextRunner().withPropertyValues(
				"spring.ai.zhinao.api-key=API_KEY",
						"spring.ai.zhinao.base-url=TEST_BASE_URL",
						"spring.ai.zhinao.chat.options.model=MODEL_XYZ",
						"spring.ai.zhinao.chat.options.frequencyPenalty=-1.5",
						"spring.ai.zhinao.chat.options.logitBias.myTokenId=-5",
						"spring.ai.zhinao.chat.options.maxTokens=123",
						"spring.ai.zhinao.chat.options.n=10",
						"spring.ai.zhinao.chat.options.responseFormat.type=json",
						"spring.ai.zhinao.chat.options.seed=66",
						"spring.ai.zhinao.chat.options.stop=boza,koza",
						"spring.ai.zhinao.chat.options.temperature=0.55",
						"spring.ai.zhinao.chat.options.topP=0.56",
						"spring.ai.zhinao.chat.options.topK=512",
						"spring.ai.zhinao.chat.options.user=userXYZ"
				)
			.withConfiguration(AutoConfigurations.of(SpringAiRetryAutoConfiguration.class,
					RestClientAutoConfiguration.class, ZhinaoChatAutoConfiguration.class))
			.run(context -> {
				var chatProperties = context.getBean(ZhinaoChatProperties.class);
				var connectionProperties = context.getBean(ZhinaoCommonProperties.class);

				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");
				assertThat(connectionProperties.getApiKey()).isEqualTo("API_KEY");

				assertThat(chatProperties.getOptions().getModel()).isEqualTo("MODEL_XYZ");
				assertThat(chatProperties.getOptions().getFrequencyPenalty()).isEqualTo(-1.5);
				assertThat(chatProperties.getOptions().getMaxTokens()).isEqualTo(123);
				assertThat(chatProperties.getOptions().getTemperature()).isEqualTo(0.55);
				assertThat(chatProperties.getOptions().getTopP()).isEqualTo(0.56);
				assertThat(chatProperties.getOptions().getTopK()).isEqualTo(512);

				assertThat(chatProperties.getOptions().getUser()).isEqualTo("userXYZ");
			});
	}

	@Test
	void chatActivation() {
		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.zhinao.api-key=API_KEY", "spring.ai.zhinao.base-url=TEST_BASE_URL",
					"spring.ai.model.chat=none")
			.withConfiguration(AutoConfigurations.of(SpringAiRetryAutoConfiguration.class,
					RestClientAutoConfiguration.class, ZhinaoChatAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(ZhinaoChatProperties.class)).isEmpty();
				assertThat(context.getBeansOfType(ZhinaoChatModel.class)).isEmpty();
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.zhinao.api-key=API_KEY", "spring.ai.zhinao.base-url=TEST_BASE_URL")
			.withConfiguration(AutoConfigurations.of(SpringAiRetryAutoConfiguration.class,
					RestClientAutoConfiguration.class, ZhinaoChatAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(ZhinaoChatProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(ZhinaoChatModel.class)).isNotEmpty();
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.zhinao.api-key=API_KEY", "spring.ai.zhinao.base-url=TEST_BASE_URL",
					"spring.ai.model.chat=zhinao")
			.withConfiguration(AutoConfigurations.of(SpringAiRetryAutoConfiguration.class,
					RestClientAutoConfiguration.class, ZhinaoChatAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(ZhinaoChatProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(ZhinaoChatModel.class)).isNotEmpty();
			});
	}

}
