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
