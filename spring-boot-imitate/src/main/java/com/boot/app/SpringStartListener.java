package com.boot.app;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;

@Component
public class SpringStartListener implements ApplicationListener<ContextStartedEvent> {

	//将事件分配给监听器
	public void onApplicationEvent(ContextStartedEvent event) {
		System.out.println("spring application listener");
	}

}
