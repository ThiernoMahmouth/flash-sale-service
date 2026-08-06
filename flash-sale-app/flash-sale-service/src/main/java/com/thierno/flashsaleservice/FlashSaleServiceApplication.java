package com.thierno.flashsaleservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FlashSaleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSaleServiceApplication.class, args);
    }

}
