package com.boot.springEvent;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

//监听SpringMailEvent事件
@Component
public class SpringMailListener
		implements ApplicationListener<SpringMailEvent> {

	public void onApplicationEvent(SpringMailEvent event) {
		String content = event.getContent();
		if ("xxx".equals(content)) {
			//响应
		}
		System.out.println("springmail event");
	}


}
