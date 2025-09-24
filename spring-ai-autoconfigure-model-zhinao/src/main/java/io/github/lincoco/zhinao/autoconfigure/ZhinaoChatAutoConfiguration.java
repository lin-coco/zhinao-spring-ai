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

import io.github.lincoco.zhinao.ZhinaoChatModel;
import io.github.lincoco.zhinao.api.ZhinaoApi;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.SpringAIModelProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static io.github.lincoco.zhinao.api.ZhinaoConstants.ZHINAO_PROVIDER_NAME;

/**
 * @author xueyeshang
 */
@AutoConfiguration(after = {
        RestClientAutoConfiguration.class,
        WebClientAutoConfiguration.class,
        SpringAiRetryAutoConfiguration.class,
        ToolCallingAutoConfiguration.class
})
@ImportAutoConfiguration(classes = {
        RestClientAutoConfiguration.class,
        WebClientAutoConfiguration.class,
        SpringAiRetryAutoConfiguration.class,
        ToolCallingAutoConfiguration.class
})
@EnableConfigurationProperties({ZhinaoCommonProperties.class, ZhinaoChatProperties.class})
@ConditionalOnClass(ZhinaoApi.class)
@ConditionalOnProperty(name = SpringAIModelProperties.CHAT_MODEL, havingValue = ZHINAO_PROVIDER_NAME, matchIfMissing = true)
public class ZhinaoChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZhinaoChatModel zhinaoChatModel(ZhinaoCommonProperties commonProperties,
                                           ZhinaoChatProperties chatProperties,
                                           ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                                           ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                                           ToolCallingManager toolCallingManager,
                                           RetryTemplate retryTemplate,
                                           ResponseErrorHandler responseErrorHandler,
                                           ObjectProvider<ObservationRegistry> observationRegistryProvider,
                                           ObjectProvider<ChatModelObservationConvention> observationConventions,
                                           ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicateProvider) {

        ZhinaoApi zhinaoApi = zhinaoApi(commonProperties, chatProperties,
                restClientBuilderProvider.getIfAvailable(RestClient::builder),
                webClientBuilderProvider.getIfAvailable(WebClient::builder), responseErrorHandler);

        ZhinaoChatModel chatModel = ZhinaoChatModel.builder()
                .zhinaoApi(zhinaoApi)
                .defaultOptions(chatProperties.getOptions())
                .toolCallingManager(toolCallingManager)
                .toolExecutionEligibilityPredicate(
                        toolExecutionEligibilityPredicateProvider.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
        observationConventions.ifAvailable(chatModel::setObservationConvention);
        return chatModel;
    }

    @Bean
    @ConditionalOnMissingBean
    public RetryTemplate retryTemplate() {
        return new RetryTemplate();
    }

    private ZhinaoApi zhinaoApi(ZhinaoCommonProperties commonProperties,
                                ZhinaoChatProperties chatProperties,
                                RestClient.Builder restClientBuilder,
                                WebClient.Builder webClientBuilder,
                                ResponseErrorHandler responseErrorHandler) {

        String resolvedBaseUrl = StringUtils.hasText(chatProperties.getBaseUrl()) ? chatProperties.getBaseUrl() : commonProperties.getBaseUrl();
        Assert.hasText(resolvedBaseUrl, "Zhinao base url must be set");
        String resolvedApiKey = StringUtils.hasText(chatProperties.getApiKey()) ? chatProperties.getApiKey() : commonProperties.getApiKey();
        Assert.hasText(resolvedApiKey, "Zhinao api key must be set");

        return ZhinaoApi.builder()
                .baseUrl(resolvedBaseUrl)
                .apiKey(new SimpleApiKey(resolvedApiKey))
                .completionsPath(chatProperties.getCompletionsPath())
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .responseErrorHandler(responseErrorHandler)
                .build();
    }
}
