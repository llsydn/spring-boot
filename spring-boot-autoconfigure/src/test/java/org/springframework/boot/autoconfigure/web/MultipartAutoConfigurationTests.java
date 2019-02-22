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

package org.springframework.boot.autoconfigure.web;

import java.net.URI;
import java.net.URL;

import javax.servlet.MultipartConfigElement;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MultipartAutoConfiguration}. Tests an empty configuration, no
 * multipart configuration, and a multipart configuration (with both Jetty and Tomcat).
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Josh Long
 * @author Ivan Sopov
 * @author Toshiaki Maki
 */
public class MultipartAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@BeforeClass
	@AfterClass
	public static void uninstallUrlStreamHandlerFactory() {
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance",
				null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
	}

	@Test
	public void containerWithNothing() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithNothing.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		verify404();
		assertThat(servlet.getMultipartResolver()).isNotNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
	}

	@Test
	public void containerWithNoMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithNoMultipartJetty.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		assertThat(servlet.getMultipartResolver()).isNotNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
		verifyServletWorks();
	}

	@Test
	public void containerWithNoMultipartUndertowConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithNoMultipartUndertow.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		verifyServletWorks();
		assertThat(servlet.getMultipartResolver()).isNotNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
	}

	@Test
	public void containerWithNoMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithNoMultipartTomcat.class, BaseConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		assertThat(servlet.getMultipartResolver()).isNull();
		assertThat(this.context.getBeansOfType(StandardServletMultipartResolver.class))
				.hasSize(1);
		assertThat(this.context.getBeansOfType(MultipartResolver.class)).hasSize(1);
		verifyServletWorks();
	}

	@Test
	public void containerWithAutomatedMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithEverythingJetty.class, BaseConfiguration.class);
		this.context.getBean(MultipartConfigElement.class);
		assertThat(this.context.getBean(StandardServletMultipartResolver.class)).isSameAs(
				this.context.getBean(DispatcherServlet.class).getMultipartResolver());
		verifyServletWorks();
	}

	@Test
	public void containerWithAutomatedMultipartTomcatConfiguration() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithEverythingTomcat.class, BaseConfiguration.class);
		new RestTemplate().getForObject("http://localhost:"
				+ this.context.getEmbeddedServletContainer().getPort() + "/",
				String.class);
		this.context.getBean(MultipartConfigElement.class);
		assertThat(this.context.getBean(StandardServletMultipartResolver.class)).isSameAs(
				this.context.getBean(DispatcherServlet.class).getMultipartResolver());
		verifyServletWorks();
	}

	@Test
	public void containerWithAutomatedMultipartUndertowConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithEverythingUndertow.class, BaseConfiguration.class);
		this.context.getBean(MultipartConfigElement.class);
		verifyServletWorks();
		assertThat(this.context.getBean(StandardServletMultipartResolver.class)).isSameAs(
				this.context.getBean(DispatcherServlet.class).getMultipartResolver());
	}

	@Test
	public void containerWithMultipartConfigDisabled() {
		testContainerWithCustomMultipartConfigEnabledSetting("false", 0);
	}

	@Test
	public void containerWithMultipartConfigEnabled() {
		testContainerWithCustomMultipartConfigEnabledSetting("true", 1);
	}

	private void testContainerWithCustomMultipartConfigEnabledSetting(
			final String propertyValue, int expectedNumberOfMultipartConfigElementBeans) {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.http.multipart.enabled=" + propertyValue);
		this.context.register(ContainerWithNoMultipartTomcat.class,
				BaseConfiguration.class);
		this.context.refresh();
		this.context.getBean(MultipartProperties.class);
		assertThat(this.context.getBeansOfType(MultipartConfigElement.class))
				.hasSize(expectedNumberOfMultipartConfigElementBeans);
	}

	@Test
	public void containerWithCustomMultipartResolver() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithCustomMultipartResolver.class, BaseConfiguration.class);
		MultipartResolver multipartResolver = this.context
				.getBean(MultipartResolver.class);
		assertThat(multipartResolver)
				.isNotInstanceOf(StandardServletMultipartResolver.class);
	}

	@Test
	public void configureResolveLazily() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.http.multipart.resolve-lazily=true");
		this.context.register(ContainerWithNothing.class, BaseConfiguration.class);
		this.context.refresh();
		StandardServletMultipartResolver multipartResolver = this.context
				.getBean(StandardServletMultipartResolver.class);
		boolean resolveLazily = (Boolean) ReflectionTestUtils.getField(multipartResolver,
				"resolveLazily");
		assertThat(resolveLazily).isTrue();
	}

	private void verify404() throws Exception {
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		ClientHttpRequest request = requestFactory.createRequest(
				new URI("http://localhost:"
						+ this.context.getEmbeddedServletContainer().getPort() + "/"),
				HttpMethod.GET);
		ClientHttpResponse response = request.execute();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private void verifyServletWorks() {
		RestTemplate restTemplate = new RestTemplate();
		String url = "http://localhost:"
				+ this.context.getEmbeddedServletContainer().getPort() + "/";
		assertThat(restTemplate.getForObject(url, String.class)).isEqualTo("Hello");
	}

	@Configuration
	public static class ContainerWithNothing {

	}

	@Configuration
	public static class ContainerWithNoMultipartJetty {

		@Bean
		JettyEmbeddedServletContainerFactory containerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}

	}

	@Configuration
	public static class ContainerWithNoMultipartUndertow {

		@Bean
		UndertowEmbeddedServletContainerFactory containerFactory() {
			return new UndertowEmbeddedServletContainerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}

	}

	@Configuration
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, MultipartAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class })
	@EnableConfigurationProperties(MultipartProperties.class)
	protected static class BaseConfiguration {

		@Bean
		public ServerProperties serverProperties() {
			ServerProperties properties = new ServerProperties();
			properties.setPort(0);
			return properties;
		}

	}

	@Configuration
	public static class ContainerWithNoMultipartTomcat {

		@Bean
		TomcatEmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}

	}

	@Configuration
	public static class ContainerWithEverythingJetty {

		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		JettyEmbeddedServletContainerFactory containerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}

	}

	@Configuration
	@EnableWebMvc
	public static class ContainerWithEverythingTomcat {

		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		TomcatEmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}

	}

	@Configuration
	@EnableWebMvc
	public static class ContainerWithEverythingUndertow {

		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		UndertowEmbeddedServletContainerFactory containerFactory() {
			return new UndertowEmbeddedServletContainerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}

	}

	public static class ContainerWithCustomMultipartResolver {

		@Bean
		MultipartResolver multipartResolver() {
			return mock(MultipartResolver.class);
		}

	}

	@Controller
	public static class WebController {

		@RequestMapping("/")
		@ResponseBody
		public String index() {
			return "Hello";
		}

	}

}
