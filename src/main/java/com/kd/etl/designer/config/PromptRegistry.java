package com.kd.etl.designer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

@Data
@Configuration
@PropertySource(value = "classpath:prompts.yml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "etl")
public class PromptRegistry {

    private Map<String, String> prompts;

    public String get(String key){
        return this.prompts.get(key);
    }
}
