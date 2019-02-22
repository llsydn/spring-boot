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

import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnWebApplication}.
 *
 * @author Dave Syer
 */
public class ConditionalOnWebApplicationTests {

	private final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Test
	public void testWebApplication() {
		this.context.register(BasicConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isTrue();
		assertThat(this.context.getBean("foo")).isEqualTo("foo");
	}

	@Test
	public void testNotWebApplication() {
		this.context.register(MissingConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		assertThat(this.context.containsBean("foo")).isFalse();
	}

	@Configuration
	@ConditionalOnNotWebApplication
	protected static class MissingConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

	@Configuration
	@ConditionalOnWebApplication
	protected static class BasicConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}

	}

}
