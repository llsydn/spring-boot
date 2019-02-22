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

import javax.servlet.Servlet;

import io.undertow.Undertow;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.xnio.SslClientAuthMode;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration.BeanPostProcessorsRegistrar;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for an embedded servlet containers.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Ivan Sopov
 * @author Stephane Nicoll
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication  // 在Web环境下才会起作用
@Import(BeanPostProcessorsRegistrar.class) // 会Import一个内部类BeanPostProcessorsRegistrar
public class EmbeddedServletContainerAutoConfiguration {

	/**
	 * Nested configuration if Tomcat is being used.
	 */
	@Configuration
	// Tomcat类和Servlet类必须在classloader中存在
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
	// 当前Spring容器中不存在EmbeddedServletContainerFactory类型的实例
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
	public static class EmbeddedTomcat {
		// 上述条件注解成立的话就会构造TomcatEmbeddedServletContainerFactory这个EmbeddedServletContainerFactory
		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

	}

	/**
	 * Nested configuration if Jetty is being used.
	 */
	@Configuration
	// Server类、Servlet类、Loader类以及WebAppContext类必须在classloader中存在
	@ConditionalOnClass({ Servlet.class, Server.class, Loader.class,
			WebAppContext.class })
	// 当前Spring容器中不存在EmbeddedServletContainerFactory类型的实例
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
	public static class EmbeddedJetty {
		// 上述条件注解成立的话就会构造JettyEmbeddedServletContainerFactory这个EmbeddedServletContainerFactory
		@Bean
		public JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

	}

	/**
	 * Nested configuration if Undertow is being used.
	 */
	@Configuration
	// Undertow类、Servlet类、以及SslClientAuthMode类必须在classloader中存在
	@ConditionalOnClass({ Servlet.class, Undertow.class, SslClientAuthMode.class })
	// 当前Spring容器中不存在EmbeddedServletContainerFactory类型的实例
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
	public static class EmbeddedUndertow {
		// 上述条件注解成立的话就会构造JettyEmbeddedServletContainerFactory这个EmbeddedServletContainerFactory
		@Bean
		public UndertowEmbeddedServletContainerFactory undertowEmbeddedServletContainerFactory() {
			return new UndertowEmbeddedServletContainerFactory();
		}

	}

	// 在EmbeddedServletContainerAutoConfiguration自动化配置类中被导入，实现了BeanFactoryAware接口(BeanFactory会被自动注入进来)和
	// ImportBeanDefinitionRegistrar接口(会被ConfigurationClassBeanDefinitionReader解析并注册到Spring容器中)
	/**
	 * Registers a {@link EmbeddedServletContainerCustomizerBeanPostProcessor}. Registered
	 * via {@link ImportBeanDefinitionRegistrar} for early registration.
	 */
	public static class BeanPostProcessorsRegistrar
			implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

		private ConfigurableListableBeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (beanFactory instanceof ConfigurableListableBeanFactory) {
				this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
			}
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (this.beanFactory == null) {
				return;
			}
			// 如果Spring容器中不存在EmbeddedServletContainerCustomizerBeanPostProcessor类型的bean
			// 注册一个EmbeddedServletContainerCustomizerBeanPostProcessor
			// EmbeddedServletContainerCustomizerBeanPostProcessor是一个BeanPostProcessor，它在postProcessBeforeInitialization过程中去
			// 寻找Spring容器中EmbeddedServletContainerCustomizer类型的bean，并依次调用EmbeddedServletContainerCustomizer接口的customize方法做一些定制化：

			registerSyntheticBeanIfMissing(registry,
					"embeddedServletContainerCustomizerBeanPostProcessor",
					EmbeddedServletContainerCustomizerBeanPostProcessor.class);
			registerSyntheticBeanIfMissing(registry,
					"errorPageRegistrarBeanPostProcessor",
					ErrorPageRegistrarBeanPostProcessor.class);
		}

		private void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry,
				String name, Class<?> beanClass) {
			if (ObjectUtils.isEmpty(
					this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
				RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
				beanDefinition.setSynthetic(true);
				registry.registerBeanDefinition(name, beanDefinition);
			}
		}

	}

}
