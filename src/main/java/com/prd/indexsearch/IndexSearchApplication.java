package com.prd.indexsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cache.annotation.EnableCaching
public class IndexSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndexSearchApplication.class, args);
    }

}
