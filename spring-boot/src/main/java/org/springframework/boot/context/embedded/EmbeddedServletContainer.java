/*
 * Copyright 2012-2015 the original author or authors.
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

/**
 * 它目前有4个实现类，分别是JettyEmbeddedServletContainer、TomcatEmbeddedServletContainer和UndertowEmbeddedServletContainer，MockEmbeddedServletContainer
 * 分别对应Jetty、Tomcat和Undertow，Mock这4个Servlet容器。
 */
/**
 * Simple interface that represents a fully configured embedded servlet container (for
 * example Tomcat or Jetty). Allows the container to be {@link #start() started} and
 * {@link #stop() stopped}.
 * <p>
 * Instances of this class are usually obtained via a
 * {@link EmbeddedServletContainerFactory}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @see EmbeddedServletContainerFactory
 */
public interface EmbeddedServletContainer {

	// 启动内置的Servlet容器，如果容器已经启动，则不影响
	/**
	 * Starts the embedded servlet container. Calling this method on an already started
	 * container has no effect.
	 * @throws EmbeddedServletContainerException if the container cannot be started
	 */
	void start() throws EmbeddedServletContainerException;

	// 关闭内置的Servlet容器，如果容器已经关系，则不影响
	/**
	 * Stops the embedded servlet container. Calling this method on an already stopped
	 * container has no effect.
	 * @throws EmbeddedServletContainerException if the container cannot be stopped
	 */
	void stop() throws EmbeddedServletContainerException;

	// 内置的Servlet容器监听的端口
	/**
	 * Return the port this server is listening on.
	 * @return the port (or -1 if none)
	 */
	int getPort();

}
