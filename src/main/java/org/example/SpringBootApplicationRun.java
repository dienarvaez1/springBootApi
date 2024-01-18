package org.example;

import org.example.config.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
//@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class SpringBootApplicationRun {
    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(LoggingUtil.class);

        SpringApplication.run(SpringBootApplicationRun.class, args);

        logger.debug(String.valueOf(true));

    }



}