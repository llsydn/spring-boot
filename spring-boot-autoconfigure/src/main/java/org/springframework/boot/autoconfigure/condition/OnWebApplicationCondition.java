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

package org.springframework.boot.autoconfigure.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

/**
 * {@link Condition} that checks for the presence or absence of
 * {@link WebApplicationContext}.
 *
 * @author Dave Syer
 * @see ConditionalOnWebApplication
 * @see ConditionalOnNotWebApplication
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
class OnWebApplicationCondition extends SpringBootCondition {

	private static final String WEB_CONTEXT_CLASS = "org.springframework.web.context."
			+ "support.GenericWebApplicationContext";

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		// 检查是否有@ConditionOnWebApplication注解
		boolean required = metadata
				.isAnnotated(ConditionalOnWebApplication.class.getName());
		// 判断是否是WebApplication
		ConditionOutcome outcome = isWebApplication(context, metadata, required);
		if (required && !outcome.isMatch()) {
			// 3.如果有@ConditionOnWebApplication注解，但是不是webApplication环境，则返回不匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage());
		}
		if (!required && outcome.isMatch()) {
			// 4.如果没有有@ConditionOnWebApplication注解，但是是webApplication环境，则返回不匹配
			return ConditionOutcome.noMatch(outcome.getConditionMessage());
		}
		// 5.有@ConditionOnWebApplication注解，是webApplication环境，则返回匹配
		return ConditionOutcome.match(outcome.getConditionMessage());
	}

	// 判断是否为webApplication环境
	private ConditionOutcome isWebApplication(ConditionContext context,
			AnnotatedTypeMetadata metadata, boolean required) {
		ConditionMessage.Builder message = ConditionMessage.forCondition(
				ConditionalOnWebApplication.class, required ? "(required)" : "");
		// 1.判断GenericWebApplication是否在类路径中，如果不存在，则返回不匹配
		if (!ClassUtils.isPresent(WEB_CONTEXT_CLASS, context.getClassLoader())) {
			return ConditionOutcome
					.noMatch(message.didNotFind("web application classes").atAll());
		}
		// 2.容器是否有名为session的scope，如果存在，则匹配返回
		if (context.getBeanFactory() != null) {
			String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
			if (ObjectUtils.containsElement(scopes, "session")) {
				return ConditionOutcome.match(message.foundExactly("'session' scope"));
			}
		}
		// 3.ConditionContext中的Environment是否为StandardServletEnvironment，如果是的话，则返回匹配
		if (context.getEnvironment() instanceof StandardServletEnvironment) {
			return ConditionOutcome
					.match(message.foundExactly("StandardServletEnvironment"));
		}
		// 4.当前ResourceLoader是否为WebApplication，如果是，则返回匹配
		if (context.getResourceLoader() instanceof WebApplicationContext) {
			return ConditionOutcome.match(message.foundExactly("WebApplicationContext"));
		}
		// 5.其他情况，返回不匹配
		return ConditionOutcome.noMatch(message.because("not a web application"));
	}

}
