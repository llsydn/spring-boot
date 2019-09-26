package com.boot.start;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

public class MyListener implements GenericApplicationListener {

	/**
	 * 事件类型
	 * 表示你对那些事件类型感不感兴趣
	 */
	public boolean supportsEventType(ResolvableType eventType) {
		Type type = eventType.getType();
		if (type instanceof Object) {

		}
		//表示都感兴趣
		return true;
	}

	/**
	 * 事件源
	 * 表示你对那些事件源感不感兴趣
	 */
	public boolean supportsSourceType(Class<?> sourceType) {
		//表示都感兴趣
		return true;
	}

	public void onApplicationEvent(ApplicationEvent event) {
		System.out.println("----------");
	}

	public int getOrder() {
		return 0;
	}
}
