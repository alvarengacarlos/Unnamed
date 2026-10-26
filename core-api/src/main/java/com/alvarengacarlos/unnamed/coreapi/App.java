package com.alvarengacarlos.unnamed.coreapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    private static final String ENABLED = "true";

    static {
        // jOOQ prints an ASCII-art logo and a "tip of the day" when it starts, which are noise in the logs.
        System.setProperty("org.jooq.no-logo", ENABLED);
        System.setProperty("org.jooq.no-tips", ENABLED);
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
