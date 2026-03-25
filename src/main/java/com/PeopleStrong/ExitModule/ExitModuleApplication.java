package com.PeopleStrong.ExitModule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExitModuleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExitModuleApplication.class, args);
	}

}
