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

package org.springframework.boot.actuate.endpoint;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiquibaseEndpoint}.
 *
 * @author Eddú Meléndez
 */
public class LiquibaseEndpointTests extends AbstractEndpointTests<LiquibaseEndpoint> {

	public LiquibaseEndpointTests() {
		super(Config.class, LiquibaseEndpoint.class, "liquibase", true,
				"endpoints.liquibase");
	}

	@Test
	public void invoke() throws Exception {
		assertThat(getEndpointBean().invoke()).hasSize(1);
	}

	@Test
	public void invokeWithCustomSchema() throws Exception {
		this.context.close();
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.default-schema=CUSTOMSCHEMA",
				"spring.datasource.generate-unique-name=true",
				"spring.datasource.schema=classpath:/db/create-custom-schema.sql");
		this.context.register(CustomSchemaConfig.class);
		this.context.refresh();
		assertThat(getEndpointBean().invoke()).hasSize(1);
	}

	@Configuration
	@Import({ EmbeddedDataSourceConfiguration.class, LiquibaseAutoConfiguration.class })
	public static class Config {

		private final SpringLiquibase liquibase;

		public Config(SpringLiquibase liquibase) {
			this.liquibase = liquibase;
		}

		@Bean
		public LiquibaseEndpoint endpoint() {
			return new LiquibaseEndpoint(this.liquibase);
		}

	}

	@Configuration
	@Import({ DataSourceAutoConfiguration.class, LiquibaseAutoConfiguration.class })
	public static class CustomSchemaConfig {

		private final SpringLiquibase liquibase;

		public CustomSchemaConfig(SpringLiquibase liquibase) {
			this.liquibase = liquibase;
		}

		@Bean
		public LiquibaseEndpoint endpoint() {
			return new LiquibaseEndpoint(this.liquibase);
		}

	}

}
