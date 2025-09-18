package com.lincoco.springai.zhinao;

import com.lincoco.springai.zhinao.ZhinaoChatModel;
import com.lincoco.springai.zhinao.api.ZhinaoApi;
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
