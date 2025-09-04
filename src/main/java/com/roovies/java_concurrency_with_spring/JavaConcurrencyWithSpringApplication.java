package com.roovies.java_concurrency_with_spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry // Spring Retry 활성화
public class JavaConcurrencyWithSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaConcurrencyWithSpringApplication.class, args);
	}
}
