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

package org.springframework.boot.context.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ErrorHandler;

/**
 * {@link SpringApplicationRunListener} to publish {@link SpringApplicationEvent}s.
 * <p>
 * Uses an internal {@link ApplicationEventMulticaster} for the events that are fired
 * before the context is actually refreshed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {

	private final SpringApplication application;

	private final String[] args;

	//广播器initialMulticaster这个类的作用就是
	/**
	 * 这个类需要大篇幅介绍
	 * 这个类工作方式有点抽象，有点难以理解，我这里说清楚一点
	 * 1、首先他会广播一个事件
	 * 		对应for (final ApplicationListener<?> listener : getApplicationListeners(event, type))
	 * 		getApplicationListeners(event, type)干了两件事，首先传了两个参数
	 * 		这两个参数就是事件类型，意思告诉所有的监听器现在有了一个type类型的event
	 *
	 * 2、告诉所有的监听器
	 * 		getApplicationListeners告诉所有的监听器（遍历所有的监听器）
	 * 		然后监听器就会接受到这个事件，继而监听器会判断这个事件自己感兴趣不
	 * 		关键监听器如何知道自己感兴趣不？spring做的比较复杂，其实也不复杂，看源码就不复杂
	 * 		主要有两个步骤来确定
	 *
	 * 第一个步骤：两个方法确定
	 * 		suportsEventType(enentType)
	 * 		smartListener.suportsSourceType(sourceType)
	 * 		上面两个方法可以简单理解通过传入一个事件类型返回一个boolean
	 * 		任意一个返回false表示这个监听器对eventType的事件不敢兴趣
	 * 		如果感兴趣会被add到一个list当中，再后续的代码中依次执行方法调用
	 *
	 * 第二个步骤：在监听器回调的时候，还是可以进行事件类型判断的
	 * 		如果事件类型不感兴趣上面都不执行就可以
	 *
	 * 3.获得所有对这个事件感兴趣的监听器，遍历执行其onApplicationEvent方法
	 *		这里的代码传入了一个ApplicationStartingEvent的事件过去
	 *		那么在springboot当中定义的11个监听器，哪些监听器对这个事件感兴趣呢?
	 *		或者换句话说，哪些监听器就订阅了这个事件呢？
	 *		先看结果是4个监听器。
	 *		为什么是这四个？
	 *
	 * 根据上述第二点的第一个步骤，我们可以去查看源码
	 * 		1.org.springframework.boot.logging.LoggingApplicationListener
	 * 		2.org.springframework.boot.autoconfigure.BackgroundPreinitializer
	 * 		3.org.springframework.boot.context.config.DelegatingApplicationListener
	 * 		4.org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener
	 */
	private final SimpleApplicationEventMulticaster initialMulticaster;

	public EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		//广播器
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
		//将所有的监听器，给到这个广播器
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void starting() {
		//发布一个spring要启动事件：ApplicationStartedEvent
		this.initialMulticaster
				.multicastEvent(new ApplicationStartedEvent(this.application, this.args));
	}

	@Override
	public void environmentPrepared(ConfigurableEnvironment environment) {
		this.initialMulticaster.multicastEvent(new ApplicationEnvironmentPreparedEvent(
				this.application, this.args, environment));
	}

	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {

	}

	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {
		for (ApplicationListener<?> listener : this.application.getListeners()) {
			if (listener instanceof ApplicationContextAware) {
				((ApplicationContextAware) listener).setApplicationContext(context);
			}
			context.addApplicationListener(listener);
		}
		this.initialMulticaster.multicastEvent(
				new ApplicationPreparedEvent(this.application, this.args, context));
	}

	@Override
	public void finished(ConfigurableApplicationContext context, Throwable exception) {
		SpringApplicationEvent event = getFinishedEvent(context, exception);
		if (context != null && context.isActive()) {
			// Listeners have been registered to the application context so we should
			// use it at this point if we can
			context.publishEvent(event);
		}
		else {
			// An inactive context may not have a multicaster so we use our multicaster to
			// call all of the context's listeners instead
			if (context instanceof AbstractApplicationContext) {
				for (ApplicationListener<?> listener : ((AbstractApplicationContext) context)
						.getApplicationListeners()) {
					this.initialMulticaster.addApplicationListener(listener);
				}
			}
			if (event instanceof ApplicationFailedEvent) {
				this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
			}
			this.initialMulticaster.multicastEvent(event);
		}
	}

	private SpringApplicationEvent getFinishedEvent(
			ConfigurableApplicationContext context, Throwable exception) {
		if (exception != null) {
			return new ApplicationFailedEvent(this.application, this.args, context,
					exception);
		}
		return new ApplicationReadyEvent(this.application, this.args, context);
	}

	private static class LoggingErrorHandler implements ErrorHandler {

		private static Log logger = LogFactory.getLog(EventPublishingRunListener.class);

		@Override
		public void handleError(Throwable throwable) {
			logger.warn("Error calling ApplicationEventListener", throwable);
		}

	}

}
