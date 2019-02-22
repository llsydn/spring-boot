package com.boot.imitate.spring;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import javax.servlet.ServletException;
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
			Context ctx = tomcat.addWebapp("/", new File("tomcat.9090/work/Tomcat/localhost").getAbsolutePath());
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










