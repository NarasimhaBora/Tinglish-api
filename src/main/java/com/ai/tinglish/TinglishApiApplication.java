package com.ai.tinglish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.ai.tinglish.dto.LlamaRequest;

@SpringBootApplication
@EnableConfigurationProperties(LlamaRequest.class)
public class TinglishApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinglishApiApplication.class, args);
    }

}
