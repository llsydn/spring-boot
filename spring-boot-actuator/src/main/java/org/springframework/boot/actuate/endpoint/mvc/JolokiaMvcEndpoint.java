/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.jolokia.http.AgentServlet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ServletWrappingController;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link MvcEndpoint} to expose Jolokia.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 */
@ConfigurationProperties(prefix = "endpoints.jolokia", ignoreUnknownFields = false)
@HypermediaDisabled
public class JolokiaMvcEndpoint extends AbstractNamedMvcEndpoint implements
		InitializingBean, ApplicationContextAware, ServletContextAware, DisposableBean {

	private final ServletWrappingController controller = new ServletWrappingController();

	public JolokiaMvcEndpoint() {
		super("jolokia", "/jolokia", true);
		this.controller.setServletClass(AgentServlet.class);
		this.controller.setServletName("jolokia");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.controller.afterPropertiesSet();
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.controller.setServletContext(servletContext);
	}

	public void setInitParameters(Properties initParameters) {
		this.controller.setInitParameters(initParameters);
	}

	@Override
	public final void setApplicationContext(ApplicationContext context)
			throws BeansException {
		this.controller.setApplicationContext(context);
	}

	@Override
	public void destroy() {
		this.controller.destroy();
	}

	@RequestMapping("/**")
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		return this.controller.handleRequest(new PathStripper(request, getPath()),
				response);
	}

	private static class PathStripper extends HttpServletRequestWrapper {

		private final String path;

		private final UrlPathHelper urlPathHelper;

		PathStripper(HttpServletRequest request, String path) {
			super(request);
			this.path = path;
			this.urlPathHelper = new UrlPathHelper();
		}

		@Override
		public String getPathInfo() {
			String value = this.urlPathHelper.decodeRequestString(
					(HttpServletRequest) getRequest(), super.getRequestURI());
			if (value.contains(this.path)) {
				value = value.substring(value.indexOf(this.path) + this.path.length());
			}
			int index = value.indexOf("?");
			if (index > 0) {
				value = value.substring(0, index);
			}
			while (value.startsWith("/")) {
				value = value.substring(1);
			}
			return value;
		}

	}

}
