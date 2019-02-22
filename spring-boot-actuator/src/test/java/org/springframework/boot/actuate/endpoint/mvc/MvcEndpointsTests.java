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

package org.springframework.boot.actuate.endpoint.mvc;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MvcEndpoints}.
 *
 * @author Dave Syer
 */
public class MvcEndpointsTests {

	private MvcEndpoints endpoints = new MvcEndpoints();

	private StaticApplicationContext context = new StaticApplicationContext();

	@Test
	public void picksUpEndpointDelegates() throws Exception {
		this.context.getDefaultListableBeanFactory().registerSingleton("endpoint",
				new TestEndpoint());
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertThat(this.endpoints.getEndpoints()).hasSize(1);
	}

	@Test
	public void picksUpEndpointDelegatesFromParent() throws Exception {
		StaticApplicationContext parent = new StaticApplicationContext();
		this.context.setParent(parent);
		parent.getDefaultListableBeanFactory().registerSingleton("endpoint",
				new TestEndpoint());
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertThat(this.endpoints.getEndpoints()).hasSize(1);
	}

	@Test
	public void picksUpMvcEndpoints() throws Exception {
		this.context.getDefaultListableBeanFactory().registerSingleton("endpoint",
				new EndpointMvcAdapter(new TestEndpoint()));
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertThat(this.endpoints.getEndpoints()).hasSize(1);
	}

	@Test
	public void changesPath() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.test.path=/foo/bar/");
		this.context.getDefaultListableBeanFactory().registerSingleton("endpoint",
				new TestEndpoint());
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertThat(this.endpoints.getEndpoints()).hasSize(1);
		assertThat(this.endpoints.getEndpoints().iterator().next().getPath())
				.isEqualTo("/foo/bar");
	}

	@Test
	public void getEndpointsForSpecifiedType() throws Exception {
		this.context.getDefaultListableBeanFactory().registerSingleton("endpoint-1",
				new TestMvcEndpoint(new TestEndpoint()));
		this.context.getDefaultListableBeanFactory().registerSingleton("endpoint-2",
				new OtherTestMvcEndpoint(new TestEndpoint()));
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertThat(this.endpoints.getEndpoints(TestMvcEndpoint.class)).hasSize(1);
	}

	@ConfigurationProperties("endpoints.test")
	protected static class TestEndpoint extends AbstractEndpoint<String> {

		TestEndpoint() {
			super("test");
		}

		@Override
		public String invoke() {
			return "foo";
		}

	}

	private static class TestMvcEndpoint extends EndpointMvcAdapter {

		TestMvcEndpoint(Endpoint<?> delegate) {
			super(delegate);
		}

	}

	private static class OtherTestMvcEndpoint extends EndpointMvcAdapter {

		OtherTestMvcEndpoint(Endpoint<?> delegate) {
			super(delegate);
		}

	}

}
