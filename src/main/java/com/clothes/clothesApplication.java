package com.clothes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class clothesApplication {
    public static void main(String[] args) {
        SpringApplication.run(clothesApplication.class, args);

        System.out.println("Hello World");
    }
}
