package com.boot.app;

import com.boot.springEvent.MailBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class StringTest {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(Appconfig.class);
		ac.getBean(MailBean.class).sendMail();
	}
}
