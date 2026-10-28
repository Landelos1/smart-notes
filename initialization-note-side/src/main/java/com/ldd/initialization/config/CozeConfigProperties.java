package com.ldd.initialization.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.coze")
public class CozeConfigProperties {

    private Map<String, String> streamRunUrls;

    private String defaultStreamRunUrl;

    private Map<String, String> apiTokens;

    private Map<String, String> projectIds;

    private Map<String, String> sessionIds;
}
