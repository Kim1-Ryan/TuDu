package com.kim.tudu_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class TuduApiApplication {

	static void main(String[] args) {
		SpringApplication.run(TuduApiApplication.class, args);
	}

}
