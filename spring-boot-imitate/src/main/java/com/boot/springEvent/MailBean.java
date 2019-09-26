package com.boot.springEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MailBean {

	@Autowired
	ApplicationContext applicationContext;

	//发送邮件
	public void sendMail() {
		//发布一个事件
		applicationContext.publishEvent(
				new SpringMailEvent(applicationContext)
		);
	}

}
