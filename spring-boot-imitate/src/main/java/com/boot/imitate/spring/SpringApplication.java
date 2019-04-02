package com.boot.imitate.spring;

import com.boot.imitate.app.AppConfig;
import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import java.io.File;

/**
 * @author lilinshen
 * @title 请填写标题
 * @description 请填写相关描述
 * @date 2019/2/22 15:22
 */
public class SpringApplication {

	/**
	 * 启动tomcat
	 */
	public static void run() {
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(9090);

		try {
			//设置你要发布的项目
			String sourcePath = SpringApplication.class.getResource("/").getPath();
			//告诉tomcat的webapp路径src/main/webapp
			//（这里告诉tomcat，这是一个web项目，这样的话就需要加入：tomcat-embed-jasper依赖解析jsp，但是springboot可以有多个视图解析，所以不使用addWebapp）
			// Context ctx = tomcat.addWebapp("/", new File("tomcat.9090/work/Tomcat/localhost").getAbsolutePath());


			/**
			 * springboot底层的实现，addContext
			 */
			Context ctx = tomcat.addContext("/", new File("tomcat.9090/work/Tomcat/localhost").getAbsolutePath());
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
			Wrapper app = tomcat.addServlet("/", "app", servlet);
			// 设置一启动加载
			app.setLoadOnStartup(1);
			// 设置访问路径
			app.addMapping("/");


			//告诉tomcat你的classes路径
			WebResourceRoot resourceRoot = new StandardRoot(ctx);
			resourceRoot.addPreResources(new DirResourceSet(resourceRoot, "/WEB-INF/classes", sourcePath, "/"));
			ctx.setResources(resourceRoot);
			tomcat.start();
			// 让tomcat等待你的连接
			tomcat.getServer().await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * tomcat如何调用你自己的 MyWebApplicationInitializer 的onStartup方法（servlet3.0的知识）
	 * springboot的项目怎么可以访问到static/index.html静态资源
	 * DispatcherServlet接收到请求，通过类读取到static/index.html，写入到io，再写给浏览器。
	 */

}










