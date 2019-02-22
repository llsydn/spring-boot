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

package org.springframework.boot.actuate.endpoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestMappingEndpoint}.
 *
 * @author Dave Syer
 */
public class RequestMappingEndpointTests {

	private RequestMappingEndpoint endpoint = new RequestMappingEndpoint();

	@Test
	public void concreteUrlMappings() {
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setUrlMap(Collections.singletonMap("/foo", new Object()));
		mapping.setApplicationContext(new StaticApplicationContext());
		mapping.initApplicationContext();
		this.endpoint.setHandlerMappings(
				Collections.<AbstractUrlHandlerMapping>singletonList(mapping));
		Map<String, Object> result = this.endpoint.invoke();
		assertThat(result).hasSize(1);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) result.get("/foo");
		assertThat(map.get("type")).isEqualTo("java.lang.Object");
	}

	@Test
	public void beanUrlMappings() {
		StaticApplicationContext context = new StaticApplicationContext();
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setUrlMap(Collections.singletonMap("/foo", new Object()));
		mapping.setApplicationContext(context);
		mapping.initApplicationContext();
		context.getDefaultListableBeanFactory().registerSingleton("mapping", mapping);
		this.endpoint.setApplicationContext(context);
		Map<String, Object> result = this.endpoint.invoke();
		assertThat(result).hasSize(1);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) result.get("/foo");
		assertThat(map.get("bean")).isEqualTo("mapping");
	}

	@Test
	public void beanUrlMappingsProxy() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MappingConfiguration.class);
		this.endpoint.setApplicationContext(context);
		Map<String, Object> result = this.endpoint.invoke();
		assertThat(result).hasSize(1);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) result.get("/foo");
		assertThat(map.get("bean")).isEqualTo("scopedTarget.mapping");
	}

	@Test
	public void beanMethodMappings() {
		StaticApplicationContext context = new StaticApplicationContext();
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(new EndpointMvcAdapter(new DumpEndpoint())));
		mapping.setApplicationContext(new StaticApplicationContext());
		mapping.afterPropertiesSet();
		context.getDefaultListableBeanFactory().registerSingleton("mapping", mapping);
		this.endpoint.setApplicationContext(context);
		Map<String, Object> result = this.endpoint.invoke();
		assertThat(result).hasSize(1);
		assertThat(result.keySet().iterator().next().contains("/dump")).isTrue();
		@SuppressWarnings("unchecked")
		Map<String, Object> handler = (Map<String, Object>) result.values().iterator()
				.next();
		assertThat(handler.containsKey("method")).isTrue();
	}

	@Test
	public void concreteMethodMappings() {
		EndpointHandlerMapping mapping = new EndpointHandlerMapping(
				Arrays.asList(new EndpointMvcAdapter(new DumpEndpoint())));
		mapping.setApplicationContext(new StaticApplicationContext());
		mapping.afterPropertiesSet();
		this.endpoint.setMethodMappings(
				Collections.<AbstractHandlerMethodMapping<?>>singletonList(mapping));
		Map<String, Object> result = this.endpoint.invoke();
		assertThat(result).hasSize(1);
		assertThat(result.keySet().iterator().next().contains("/dump")).isTrue();
		@SuppressWarnings("unchecked")
		Map<String, Object> handler = (Map<String, Object>) result.values().iterator()
				.next();
		assertThat(handler.containsKey("method")).isTrue();
	}

	@Configuration
	protected static class MappingConfiguration {

		@Bean
		@Lazy
		@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
		public AbstractUrlHandlerMapping mapping() {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setUrlMap(Collections.singletonMap("/foo", new Object()));
			return mapping;
		}

	}

}
