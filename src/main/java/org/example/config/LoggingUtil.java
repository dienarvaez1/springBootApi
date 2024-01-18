package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggingUtil {

    Logger logger = LoggerFactory.getLogger(LoggingUtil.class);

    @RequestMapping("/")
    public String index() {
        //logger.trace("A TRACE Message");
        //logger.debug("A DEBUG Message");
        //logger.info("An INFO Message");
        //logger.warn("A WARN Message");
        //logger.error("An ERROR Message");

        return "{\"message\": \"Welcome to the home of this app.\"}";
    }
}
