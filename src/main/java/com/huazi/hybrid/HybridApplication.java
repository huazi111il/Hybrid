package com.huazi.hybrid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class HybridApplication {

    public static void main(String[] args) {
        SpringApplication.run(HybridApplication.class, args);
    }

}
