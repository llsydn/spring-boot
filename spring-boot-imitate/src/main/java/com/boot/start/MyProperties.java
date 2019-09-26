package com.boot.start;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "my", ignoreUnknownFields = false)
public class MyProperties  {

	private Integer age;
	private String name;

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
