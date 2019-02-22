/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import org.apache.catalina.core.ApplicationContext;

import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;

/**
 * EmbeddedServletContainerFactory接口是一个工厂接口，用于生产EmbeddedServletContainer
 *
 * Factory interface that can be used to create {@link EmbeddedServletContainer}s.
 * Implementations are encouraged to extend
 * {@link AbstractEmbeddedServletContainerFactory} when possible.
 *
 * @author Phillip Webb
 * @see EmbeddedServletContainer
 * @see AbstractEmbeddedServletContainerFactory
 * @see JettyEmbeddedServletContainerFactory
 * @see TomcatEmbeddedServletContainerFactory
 */
public interface EmbeddedServletContainerFactory {

	/**
	 *  获得一个已经配置好的内置Servlet容器，但是这个容器还没有监听端口。需要手动调用内置Servlet容器的start方法监听端口
	 *  参数是一群ServletContextInitializer，Servlet容器启动的时候会遍历这些ServletContextInitializer，并调用onStartup方法
	 *		例如：ServletContextInitializer表示Servlet初始化器，用于设置ServletContext中的一些配置，在使用EmbeddedServletContainerFactory接口的
	 *		getEmbeddedServletContainer方法获取Servlet内置容器并且容器启动的时候调用onStartup方法：
	 *
	 * EmbeddedServletContainerFactory是在EmbeddedServletContainerAutoConfiguration这个自动化配置类中被注册到Spring容器中的
	 * (前期是Spring容器中不存在EmbeddedServletContainerFactory类型的bean，可以自己定义EmbeddedServletContainerFactory类型的bean)
	 *
	 * Gets a new fully configured but paused {@link EmbeddedServletContainer} instance.
	 * Clients should not be able to connect to the returned server until
	 * {@link EmbeddedServletContainer#start()} is called (which happens when the
	 * {@link ApplicationContext} has been fully refreshed).
	 * @param initializers {@link ServletContextInitializer}s that should be applied as
	 * the container starts
	 * @return a fully configured and started {@link EmbeddedServletContainer}
	 * @see EmbeddedServletContainer#stop()
	 */
	EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers);

}
