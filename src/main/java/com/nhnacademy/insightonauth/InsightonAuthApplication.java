package com.nhnacademy.insightonauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class InsightonAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsightonAuthApplication.class, args);
    }

}
