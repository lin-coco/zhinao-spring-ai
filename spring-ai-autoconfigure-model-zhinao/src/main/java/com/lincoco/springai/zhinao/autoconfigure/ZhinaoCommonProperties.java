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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(ZhinaoCommonProperties.CONFIG_PREFIX)
public class ZhinaoCommonProperties extends ZhinaoParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.zhinao";

    public static final String DEFAULT_BASE_URL = "https://api.360.cn";

    public ZhinaoCommonProperties() {
        super.setBaseUrl(DEFAULT_BASE_URL);
    }
}
