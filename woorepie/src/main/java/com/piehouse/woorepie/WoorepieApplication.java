package com.piehouse.woorepie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WoorepieApplication {

    public static void main(String[] args) {
        SpringApplication.run(WoorepieApplication.class, args);
    }

}
