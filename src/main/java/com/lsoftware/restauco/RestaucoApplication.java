package com.lsoftware.restauco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestaucoApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestaucoApplication.class, args);
	}

}
