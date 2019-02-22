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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for endpoint tests.
 *
 * @param <T> the endpoint type
 * @author Phillip Webb
 */
public abstract class AbstractEndpointTests<T extends Endpoint<?>> {

	protected AnnotationConfigApplicationContext context;

	protected final Class<?> configClass;

	private final Class<?> type;

	private final String id;

	private final boolean sensitive;

	private final String property;

	public AbstractEndpointTests(Class<?> configClass, Class<?> type, String id,
			boolean sensitive, String property) {
		this.configClass = configClass;
		this.type = type;
		this.id = id;
		this.sensitive = sensitive;
		this.property = property;
	}

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JacksonAutoConfiguration.class, this.configClass);
		this.context.refresh();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void getId() throws Exception {
		assertThat(getEndpointBean().getId()).isEqualTo(this.id);
	}

	@Test
	public void isSensitive() throws Exception {
		assertThat(getEndpointBean().isSensitive()).isEqualTo(this.sensitive);
	}

	@Test
	public void idOverride() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, this.property + ".id:myid");
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().getId()).isEqualTo("myid");
	}

	@Test
	public void isSensitiveOverride() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test",
				Collections.<String, Object>singletonMap(this.property + ".sensitive",
						String.valueOf(!this.sensitive)));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isSensitive()).isEqualTo(!this.sensitive);
	}

	@Test
	public void isSensitiveOverrideWithGlobal() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("endpoint.sensitive", this.sensitive);
		properties.put(this.property + ".sensitive", String.valueOf(!this.sensitive));
		PropertySource<?> propertySource = new MapPropertySource("test", properties);
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isSensitive()).isEqualTo(!this.sensitive);
	}

	@Test
	public void isEnabledByDefault() throws Exception {
		assertThat(getEndpointBean().isEnabled()).isTrue();
	}

	@Test
	public void isEnabledFallbackToEnvironment() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test", Collections
				.<String, Object>singletonMap(this.property + ".enabled", false));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isEnabled()).isFalse();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void isExplicitlyEnabled() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test", Collections
				.<String, Object>singletonMap(this.property + ".enabled", false));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		((AbstractEndpoint) getEndpointBean()).setEnabled(true);
		assertThat(getEndpointBean().isEnabled()).isTrue();
	}

	@Test
	public void isAllEndpointsDisabled() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test",
				Collections.<String, Object>singletonMap("endpoints.enabled", false));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isEnabled()).isFalse();
	}

	@Test
	public void isOptIn() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("endpoints.enabled", false);
		source.put(this.property + ".enabled", true);
		PropertySource<?> propertySource = new MapPropertySource("test", source);
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isEnabled()).isTrue();
	}

	@Test
	public void serialize() throws Exception {
		Object result = getEndpointBean().invoke();
		if (result != null) {
			this.context.getBean(ObjectMapper.class).writeValue(System.out, result);
		}
	}

	@Test
	public void isAllEndpointsSensitive() throws Exception {
		testGlobalEndpointsSensitive(true);
	}

	@Test
	public void isAllEndpointsNotSensitive() throws Exception {
		testGlobalEndpointsSensitive(false);
	}

	private void testGlobalEndpointsSensitive(boolean sensitive) {
		this.context = new AnnotationConfigApplicationContext();
		PropertySource<?> propertySource = new MapPropertySource("test", Collections
				.<String, Object>singletonMap("endpoints.sensitive", sensitive));
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.register(this.configClass);
		this.context.refresh();
		assertThat(getEndpointBean().isSensitive()).isEqualTo(sensitive);
	}

	@SuppressWarnings("unchecked")
	protected T getEndpointBean() {
		return (T) this.context.getBean(this.type);
	}

}
