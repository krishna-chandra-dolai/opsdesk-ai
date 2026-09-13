package com.opsdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpsDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpsDeskApplication.class, args);
    }
}
