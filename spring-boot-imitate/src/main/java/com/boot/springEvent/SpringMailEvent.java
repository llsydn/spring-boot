package com.boot.springEvent;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

public class SpringMailEvent extends ApplicationEvent {
	private String content;

	public SpringMailEvent(ApplicationContext source) {
		//source事件源
		super(source);
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
