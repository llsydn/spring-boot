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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.JolokiaAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link JolokiaMvcEndpoint}.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "management.security.enabled=false")
public class JolokiaMvcEndpointIntegrationTests {

	@Autowired
	private MvcEndpoints endpoints;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		EnvironmentTestUtils.addEnvironment((ConfigurableApplicationContext) this.context,
				"foo:bar");
	}

	@Test
	public void endpointRegistered() throws Exception {
		Set<? extends MvcEndpoint> values = this.endpoints.getEndpoints();
		assertThat(values).hasAtLeastOneElementOfType(JolokiaMvcEndpoint.class);
	}

	@Test
	public void search() throws Exception {
		this.mvc.perform(get("/jolokia/search/java.lang:*")).andExpect(status().isOk())
				.andExpect(content().string(containsString("GarbageCollector")));
	}

	@Test
	public void read() throws Exception {
		this.mvc.perform(get("/jolokia/read/java.lang:type=Memory"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("NonHeapMemoryUsage")));
	}

	@Test
	public void list() throws Exception {
		this.mvc.perform(get("/jolokia/list/java.lang/type=Memory/attr"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("NonHeapMemoryUsage")));
	}

	@Configuration
	@EnableConfigurationProperties
	@EnableWebMvc
	@Import({ JacksonAutoConfiguration.class, AuditAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, JolokiaAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	public static class Config {

	}

}
