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

import java.io.File;
import java.net.URL;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServerPropertiesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Ivan Sopov
 */
public class ServerPropertiesAutoConfigurationTests {

	private static AbstractEmbeddedServletContainerFactory containerFactory;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Before
	public void init() {
		containerFactory = mock(AbstractEmbeddedServletContainerFactory.class);
	}

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
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "server.port:9000");
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		assertThat(server.getPort().intValue()).isEqualTo(9000);
		verify(containerFactory).setPort(9000);
	}

	@Test
	public void tomcatProperties() throws Exception {
		containerFactory = mock(TomcatEmbeddedServletContainerFactory.class);
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"server.tomcat.basedir:target/foo", "server.port:9000");
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		assertThat(server.getTomcat().getBasedir()).isEqualTo(new File("target/foo"));
		verify(containerFactory).setPort(9000);
	}

	@Test
	public void customizeWithJettyContainerFactory() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(CustomJettyContainerConfig.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		containerFactory = this.context
				.getBean(AbstractEmbeddedServletContainerFactory.class);
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		// The server.port environment property was not explicitly set so the container
		// factory should take precedence...
		assertThat(containerFactory.getPort()).isEqualTo(3000);
	}

	@Test
	public void customizeWithUndertowContainerFactory() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(CustomUndertowContainerConfig.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		containerFactory = this.context
				.getBean(AbstractEmbeddedServletContainerFactory.class);
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		assertThat(containerFactory.getPort()).isEqualTo(3000);
	}

	@Test
	public void customizeTomcatWithCustomizer() throws Exception {
		containerFactory = mock(TomcatEmbeddedServletContainerFactory.class);
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, CustomizeConfig.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		// The server.port environment property was not explicitly set so the container
		// customizer should take precedence...
		verify(containerFactory).setPort(3000);
	}

	@Test
	public void testAccidentalMultipleServerPropertiesBeans() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, MultiServerPropertiesBeanConfig.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.thrown.expect(ApplicationContextException.class);
		this.thrown.expectMessage("Multiple ServerProperties");
		this.context.refresh();
	}

	@Configuration
	protected static class Config {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return ServerPropertiesAutoConfigurationTests.containerFactory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	protected static class CustomJettyContainerConfig {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
			factory.setPort(3000);
			return factory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	protected static class CustomUndertowContainerConfig {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			UndertowEmbeddedServletContainerFactory factory = new UndertowEmbeddedServletContainerFactory();
			factory.setPort(3000);
			return factory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	protected static class CustomizeConfig {

		@Bean
		public EmbeddedServletContainerCustomizer containerCustomizer() {
			return new EmbeddedServletContainerCustomizer() {

				@Override
				public void customize(ConfigurableEmbeddedServletContainer container) {
					container.setPort(3000);
				}

			};
		}

	}

	@Configuration
	protected static class MultiServerPropertiesBeanConfig {

		@Bean
		public ServerProperties serverPropertiesOne() {
			return new ServerProperties();
		}

		@Bean
		public ServerProperties serverPropertiesTwo() {
			return new ServerProperties();
		}

	}

}
