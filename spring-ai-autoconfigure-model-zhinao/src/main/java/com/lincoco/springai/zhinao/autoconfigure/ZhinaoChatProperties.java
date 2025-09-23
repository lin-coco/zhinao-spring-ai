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
package com.lincoco.springai.zhinao.autoconfigure;

import com.lincoco.springai.zhinao.ZhinaoChatOptions;
import com.lincoco.springai.zhinao.api.ZhinaoApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.lincoco.springai.zhinao.api.ZhinaoConstants.DEFAULT_COMPLETIONS_PATH;

/**
 * @author xueyeshang
 */
@ConfigurationProperties(ZhinaoChatProperties.CONFIG_PREFIX)
public class ZhinaoChatProperties extends ZhinaoParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhinao.chat";

    public static final String DEFAULT_CHAT_MODEL = ZhinaoApi.ChatModel.GPT_PRO.getValue();

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    private String completionsPath = DEFAULT_COMPLETIONS_PATH;

    @NestedConfigurationProperty
    private ZhinaoChatOptions options = ZhinaoChatOptions.builder()
            .model(DEFAULT_CHAT_MODEL)
            .temperature(DEFAULT_TEMPERATURE)
            .build();

    public String getCompletionsPath() {
        return completionsPath;
    }

    public void setCompletionsPath(String completionsPath) {
        this.completionsPath = completionsPath;
    }

    public ZhinaoChatOptions getOptions() {
        return options;
    }

    public void setOptions(ZhinaoChatOptions options) {
        this.options = options;
    }
}
