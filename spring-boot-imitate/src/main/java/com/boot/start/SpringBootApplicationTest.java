package com.boot.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringBootApplicationTest {

	public static void main(String[] args) {

		ConfigurableApplicationContext ac = SpringApplication.run(SpringBootApplicationTest.class, args);
		System.out.println(ac.getBean(MyProperties.class).getAge());

	}

}
