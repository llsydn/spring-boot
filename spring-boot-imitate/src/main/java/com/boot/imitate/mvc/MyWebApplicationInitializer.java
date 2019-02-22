package com.boot.imitate.mvc;

import com.boot.imitate.app.AppConfig;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

/**
 * @author lilinshen
 * @title 请填写标题
 * @description 请填写相关描述
 * @date 2019/2/22 14:39
 */
public class MyWebApplicationInitializer implements WebApplicationInitializer {

	/**
	 * 去掉web.xml
	 *
	 * @param servletContext
	 * @throws ServletException
	 */
	public void onStartup(ServletContext servletContext) throws ServletException {
		// spring--- applicationContext.xml
		// 加载spring容器，完成springbean扫描（Load Spring web application configuration）
		AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
		ac.register(AppConfig.class);
		ac.refresh();

		// spring----spring-mvc.xml
		// web.xml配置一个servlet，@WebServlet，自己new一个servlet
		// 创建并注册一个DispatcherServlet
		DispatcherServlet servlet = new DispatcherServlet(ac);
		// 把一个servlet注册给tomcat
		ServletRegistration.Dynamic registration = servletContext.addServlet("app", servlet);
		// 设置一启动加载
		registration.setLoadOnStartup(1);
		// 设置访问路径
		registration.addMapping("/");
	}
}
