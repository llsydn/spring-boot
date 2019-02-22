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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Map;

import io.searchbox.client.JestClient;
import org.elasticsearch.client.Client;

import org.springframework.boot.actuate.health.ElasticsearchHealthIndicator;
import org.springframework.boot.actuate.health.ElasticsearchHealthIndicatorProperties;
import org.springframework.boot.actuate.health.ElasticsearchJestHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actual Elasticsearch health indicator configurations imported by
 * {@link HealthIndicatorAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ElasticsearchHealthIndicatorConfiguration {

	@Configuration
	@ConditionalOnBean(Client.class)
	@ConditionalOnEnabledHealthIndicator("elasticsearch")
	@EnableConfigurationProperties(ElasticsearchHealthIndicatorProperties.class)
	static class ElasticsearchClientHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<ElasticsearchHealthIndicator, Client> {

		private final Map<String, Client> clients;

		private final ElasticsearchHealthIndicatorProperties properties;

		ElasticsearchClientHealthIndicatorConfiguration(Map<String, Client> clients,
				ElasticsearchHealthIndicatorProperties properties) {
			this.clients = clients;
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean(name = "elasticsearchHealthIndicator")
		public HealthIndicator elasticsearchHealthIndicator() {
			return createHealthIndicator(this.clients);
		}

		@Override
		protected ElasticsearchHealthIndicator createHealthIndicator(Client client) {
			return new ElasticsearchHealthIndicator(client, this.properties);
		}

	}

	@Configuration
	@ConditionalOnBean(JestClient.class)
	@ConditionalOnEnabledHealthIndicator("elasticsearch")
	static class ElasticsearchJestHealthIndicatorConfiguration extends
			CompositeHealthIndicatorConfiguration<ElasticsearchJestHealthIndicator, JestClient> {

		private final Map<String, JestClient> clients;

		ElasticsearchJestHealthIndicatorConfiguration(Map<String, JestClient> clients) {
			this.clients = clients;
		}

		@Bean
		@ConditionalOnMissingBean(name = "elasticsearchHealthIndicator")
		public HealthIndicator elasticsearchHealthIndicator() {
			return createHealthIndicator(this.clients);
		}

		@Override
		protected ElasticsearchJestHealthIndicator createHealthIndicator(
				JestClient client) {
			return new ElasticsearchJestHealthIndicator(client);
		}

	}

}
