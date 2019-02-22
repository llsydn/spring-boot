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

package org.springframework.boot.context.embedded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} that applies all {@link EmbeddedServletContainerCustomizer}s
 * from the bean factory to {@link ConfigurableEmbeddedServletContainer} beans.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class EmbeddedServletContainerCustomizerBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware {

	private ListableBeanFactory beanFactory;

	private List<EmbeddedServletContainerCustomizer> customizers;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory,
				"EmbeddedServletContainerCustomizerBeanPostProcessor can only be used "
						+ "with a ListableBeanFactory");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		// 在Spring容器中寻找ConfigurableEmbeddedServletContainer类型的bean，SpringBoot内部的3种内置Servlet容器工厂都实现了这个接口，
		// 该接口的作用就是进行Servlet容器的配置：
		// 比如添加Servlet初始化器addInitializers、添加错误页addErrorPages、设置session超时时间setSessionTimeout、设置端口setPort等等
		// SpringBoot内置了一些EmbeddedServletContainerCustomizer，比如ErrorPageCustomizer、ServerProperties、TomcatWebSocketContainerCustomizer等
		if (bean instanceof ConfigurableEmbeddedServletContainer) {
			postProcessBeforeInitialization((ConfigurableEmbeddedServletContainer) bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	private void postProcessBeforeInitialization(
			ConfigurableEmbeddedServletContainer bean) {
		for (EmbeddedServletContainerCustomizer customizer : getCustomizers()) {
			// 遍历获取的每个定制化器，并调用customize方法进行一些定制
			customizer.customize(bean);
		}
	}

	private Collection<EmbeddedServletContainerCustomizer> getCustomizers() {
		if (this.customizers == null) {
			// Look up does not include the parent context
			this.customizers = new ArrayList<EmbeddedServletContainerCustomizer>(
					// 找出Spring容器中EmbeddedServletContainerCustomizer类型的bean
					this.beanFactory
							.getBeansOfType(EmbeddedServletContainerCustomizer.class,
									false, false)
							.values());
			// 定制化器做排序
			Collections.sort(this.customizers, AnnotationAwareOrderComparator.INSTANCE);
			// 设置定制化器到属性中
			this.customizers = Collections.unmodifiableList(this.customizers);
		}
		return this.customizers;
	}

}
