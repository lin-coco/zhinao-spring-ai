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
package io.github.lincoco.zhinao;

import io.github.lincoco.zhinao.api.ZhinaoApi;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@SpringBootConfiguration
public class ZhinaoTestConfiguration {

    @Bean
    public ZhinaoApi zhinaoApi() {
        return ZhinaoApi.builder().apiKey(getApiKey()).build();
    }

    @Bean
    public ZhinaoChatModel zhinaoChatModel(ZhinaoApi zhinaoApi) {
        return ZhinaoChatModel.builder().zhinaoApi(zhinaoApi).build();
    }

    private String getApiKey() {
        String apiKey = System.getenv("ZHINAO_API_KEY");
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("You must provide an API key.  Put it in an environment variable under the name MOONSHOT_API_KEY");
        }
        return apiKey;
    }
}
