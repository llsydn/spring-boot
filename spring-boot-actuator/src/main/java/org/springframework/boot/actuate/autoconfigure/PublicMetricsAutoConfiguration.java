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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.sql.DataSource;

import org.apache.catalina.startup.Tomcat;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.cache.CacheStatisticsProvider;
import org.springframework.boot.actuate.endpoint.CachePublicMetrics;
import org.springframework.boot.actuate.endpoint.DataSourcePublicMetrics;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RichGaugeReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.SystemPublicMetrics;
import org.springframework.boot.actuate.endpoint.TomcatPublicMetrics;
import org.springframework.boot.actuate.metrics.integration.SpringIntegrationMetricReader;
import org.springframework.boot.actuate.metrics.reader.CompositeMetricReader;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.JavaVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
import org.springframework.lang.UsesJava7;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link PublicMetrics}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Johannes Edmeier
 * @author Artem Bilan
 * @since 1.2.0
 */
@Configuration
@AutoConfigureBefore(EndpointAutoConfiguration.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, CacheAutoConfiguration.class,
		MetricRepositoryAutoConfiguration.class, CacheStatisticsAutoConfiguration.class,
		IntegrationAutoConfiguration.class })
public class PublicMetricsAutoConfiguration {

	private final List<MetricReader> metricReaders;

	public PublicMetricsAutoConfiguration(
			@ExportMetricReader ObjectProvider<List<MetricReader>> metricReaders) {
		this.metricReaders = metricReaders.getIfAvailable();
	}

	@Bean
	public SystemPublicMetrics systemPublicMetrics() {
		return new SystemPublicMetrics();
	}

	@Bean
	public MetricReaderPublicMetrics metricReaderPublicMetrics() {
		return new MetricReaderPublicMetrics(
				new CompositeMetricReader(this.metricReaders == null ? new MetricReader[0]
						: this.metricReaders
								.toArray(new MetricReader[this.metricReaders.size()])));
	}

	@Bean
	@ConditionalOnBean(RichGaugeReader.class)
	public RichGaugeReaderPublicMetrics richGaugePublicMetrics(
			RichGaugeReader richGaugeReader) {
		return new RichGaugeReaderPublicMetrics(richGaugeReader);
	}

	@Configuration
	@ConditionalOnClass(DataSource.class)
	@ConditionalOnBean(DataSource.class)
	static class DataSourceMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(DataSourcePoolMetadataProvider.class)
		public DataSourcePublicMetrics dataSourcePublicMetrics() {
			return new DataSourcePublicMetrics();
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
	@ConditionalOnWebApplication
	static class TomcatMetricsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public TomcatPublicMetrics tomcatPublicMetrics() {
			return new TomcatPublicMetrics();
		}

	}

	@Configuration
	@ConditionalOnClass(CacheManager.class)
	@ConditionalOnBean(CacheManager.class)
	static class CacheStatisticsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(CacheStatisticsProvider.class)
		public CachePublicMetrics cachePublicMetrics(
				Map<String, CacheManager> cacheManagers,
				Collection<CacheStatisticsProvider<?>> statisticsProviders) {
			return new CachePublicMetrics(cacheManagers, statisticsProviders);
		}

	}

	@Configuration
	@ConditionalOnClass(EnableIntegrationManagement.class)
	@ConditionalOnJava(JavaVersion.SEVEN)
	@UsesJava7
	static class IntegrationMetricsConfiguration {

		@Bean(name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)
		@ConditionalOnMissingBean(value = IntegrationManagementConfigurer.class, name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME, search = SearchStrategy.CURRENT)
		public IntegrationManagementConfigurer managementConfigurer() {
			IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
			configurer.setDefaultCountsEnabled(true);
			configurer.setDefaultStatsEnabled(true);
			return configurer;
		}

		@Bean
		@ConditionalOnMissingBean(name = "springIntegrationPublicMetrics")
		public MetricReaderPublicMetrics springIntegrationPublicMetrics(
				IntegrationManagementConfigurer managementConfigurer) {
			return new MetricReaderPublicMetrics(
					new SpringIntegrationMetricReader(managementConfigurer));
		}

	}

}
