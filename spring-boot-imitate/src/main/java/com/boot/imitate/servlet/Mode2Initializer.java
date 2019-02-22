package com.boot.imitate.servlet;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.util.Set;

/**
 * @author lilinshen
 * @title 请填写标题
 * @description 请填写相关描述
 * @date 2019/2/22 15:55
 */
public class Mode2Initializer implements ServletContainerInitializer {
	/**
	 * 把这两个类，IndexServlet和Mode2Initializer打成一个jar包，那个模块需要，引入到pom.xml文件即可
	 * 需要在
	 */
	public void onStartup(Set<Class<?>> set, ServletContext servletContext) throws ServletException {
		System.out.println("-----------------");

		ServletRegistration.Dynamic registration = servletContext.addServlet("index", new IndexServlet());
		// 设置一启动加载
		registration.setLoadOnStartup(1);
		// 设置访问路径
		registration.addMapping("/indexS");
	}
}
