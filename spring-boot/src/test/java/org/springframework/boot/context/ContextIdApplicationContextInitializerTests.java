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

package org.springframework.boot.context;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextIdApplicationContextInitializer}.
 *
 * @author Dave Syer
 */
public class ContextIdApplicationContextInitializerTests {

	private final ContextIdApplicationContextInitializer initializer = new ContextIdApplicationContextInitializer();

	@Test
	public void testDefaults() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		assertThat(context.getId()).isEqualTo("application");
	}

	@Test
	public void testNameAndPort() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
				"spring.application.name=foo", "PORT=8080");
		this.initializer.initialize(context);
		assertThat(context.getId()).isEqualTo("foo:8080");
	}

	@Test
	public void testNameAndProfiles() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
				"spring.application.name=foo", "spring.profiles.active=spam,bar",
				"spring.application.index=12");
		this.initializer.initialize(context);
		assertThat(context.getId()).isEqualTo("foo:spam,bar:12");
	}

	@Test
	public void testCloudFoundry() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
				"spring.config.name=foo", "PORT=8080", "vcap.application.name=bar",
				"vcap.application.instance_index=2");
		this.initializer.initialize(context);
		assertThat(context.getId()).isEqualTo("bar:2");
	}

	@Test
	public void testExplicitNameIsChosenInFavorOfCloudFoundry() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
				"spring.application.name=spam", "spring.config.name=foo", "PORT=8080",
				"vcap.application.name=bar", "vcap.application.instance_index=2");
		this.initializer.initialize(context);
		assertThat(context.getId()).isEqualTo("spam:2");
	}

}
