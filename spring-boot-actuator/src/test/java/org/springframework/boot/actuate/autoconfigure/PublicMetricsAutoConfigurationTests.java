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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.CachePublicMetrics;
import org.springframework.boot.actuate.endpoint.DataSourcePublicMetrics;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RichGaugeReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.SystemPublicMetrics;
import org.springframework.boot.actuate.endpoint.TomcatPublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PublicMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Dave Syer
 * @author Phillip Webb
 */
public class PublicMetricsAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void systemPublicMetrics() throws Exception {
		load();
		assertThat(this.context.getBeansOfType(SystemPublicMetrics.class)).hasSize(1);
	}

	@Test
	public void metricReaderPublicMetrics() throws Exception {
		load();
		assertThat(this.context.getBeansOfType(MetricReaderPublicMetrics.class))
				.hasSize(2);
	}

	@Test
	public void richGaugePublicMetrics() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				RichGaugeReaderConfig.class, MetricRepositoryAutoConfiguration.class,
				PublicMetricsAutoConfiguration.class);
		RichGaugeReader richGaugeReader = context.getBean(RichGaugeReader.class);
		assertThat(richGaugeReader).isNotNull();
		given(richGaugeReader.findAll())
				.willReturn(Collections.singletonList(new RichGauge("bar", 3.7d)));
		RichGaugeReaderPublicMetrics publicMetrics = context
				.getBean(RichGaugeReaderPublicMetrics.class);
		assertThat(publicMetrics).isNotNull();
		Collection<Metric<?>> metrics = publicMetrics.metrics();
		assertThat(metrics).isNotNull();
		assertThat(6).isEqualTo(metrics.size());
		assertHasMetric(metrics, new Metric<Double>("bar.val", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.avg", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.min", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.max", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.alpha", -1.d));
		assertHasMetric(metrics, new Metric<Long>("bar.count", 1L));
		context.close();
	}

	@Test
	public void noDataSource() {
		load();
		assertThat(this.context.getBeansOfType(DataSourcePublicMetrics.class)).isEmpty();
	}

	@Test
	public void autoDataSource() {
		load(DataSourceAutoConfiguration.class);
		PublicMetrics bean = this.context.getBean(DataSourcePublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "datasource.primary.active", "datasource.primary.usage");
	}

	@Test
	public void multipleDataSources() {
		load(MultipleDataSourcesConfig.class);
		PublicMetrics bean = this.context.getBean(DataSourcePublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "datasource.tomcat.active", "datasource.tomcat.usage",
				"datasource.commonsDbcp.active", "datasource.commonsDbcp.usage");

		// Hikari won't work unless a first connection has been retrieved
		JdbcTemplate jdbcTemplate = new JdbcTemplate(
				this.context.getBean("hikariDS", DataSource.class));
		jdbcTemplate.execute(new ConnectionCallback<Void>() {
			@Override
			public Void doInConnection(Connection connection)
					throws SQLException, DataAccessException {
				return null;
			}
		});

		Collection<Metric<?>> anotherMetrics = bean.metrics();
		assertMetrics(anotherMetrics, "datasource.tomcat.active",
				"datasource.tomcat.usage", "datasource.hikariDS.active",
				"datasource.hikariDS.usage", "datasource.commonsDbcp.active",
				"datasource.commonsDbcp.usage");
	}

	@Test
	public void multipleDataSourcesWithPrimary() {
		load(MultipleDataSourcesWithPrimaryConfig.class);
		PublicMetrics bean = this.context.getBean(DataSourcePublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "datasource.primary.active", "datasource.primary.usage",
				"datasource.commonsDbcp.active", "datasource.commonsDbcp.usage");
	}

	@Test
	public void multipleDataSourcesWithCustomPrimary() {
		load(MultipleDataSourcesWithCustomPrimaryConfig.class);
		PublicMetrics bean = this.context.getBean(DataSourcePublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "datasource.primary.active", "datasource.primary.usage",
				"datasource.dataSource.active", "datasource.dataSource.usage");
	}

	@Test
	public void customPrefix() {
		load(MultipleDataSourcesWithPrimaryConfig.class,
				CustomDataSourcePublicMetrics.class);
		PublicMetrics bean = this.context.getBean(DataSourcePublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "ds.first.active", "ds.first.usage", "ds.second.active",
				"ds.second.usage");
	}

	@Test
	public void tomcatMetrics() throws Exception {
		loadWeb(TomcatConfiguration.class);
		assertThat(this.context.getBeansOfType(TomcatPublicMetrics.class)).hasSize(1);
	}

	@Test
	public void noCacheMetrics() {
		load();
		assertThat(this.context.getBeansOfType(CachePublicMetrics.class)).isEmpty();
	}

	@Test
	public void autoCacheManager() {
		load(CacheConfiguration.class);
		CachePublicMetrics bean = this.context.getBean(CachePublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "cache.books.size", "cache.speakers.size");
	}

	@Test
	public void multipleCacheManagers() {
		load(MultipleCacheConfiguration.class);
		CachePublicMetrics bean = this.context.getBean(CachePublicMetrics.class);
		Collection<Metric<?>> metrics = bean.metrics();
		assertMetrics(metrics, "cache.books.size", "cache.second_speakers.size",
				"cache.first_speakers.size", "cache.users.size");
	}

	private void assertHasMetric(Collection<Metric<?>> metrics, Metric<?> metric) {
		for (Metric<?> m : metrics) {
			if (m.getValue().equals(metric.getValue())
					&& m.getName().equals(metric.getName())) {
				return;
			}
		}
		fail("Metric " + metric.toString() + " not found in " + metrics.toString());
	}

	private void assertMetrics(Collection<Metric<?>> metrics, String... keys) {
		Map<String, Number> content = new HashMap<String, Number>();
		for (Metric<?> metric : metrics) {
			content.put(metric.getName(), metric.getValue());
		}
		for (String key : keys) {
			assertThat(content).containsKey(key);
		}
	}

	private void loadWeb(Class<?>... config) {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		if (config.length > 0) {
			context.register(config);
		}
		context.register(DataSourcePoolMetadataProvidersConfiguration.class,
				CacheStatisticsAutoConfiguration.class,
				PublicMetricsAutoConfiguration.class,
				MockEmbeddedServletContainerFactory.class);
		context.refresh();
		this.context = context;
	}

	private void load(Class<?>... config) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (config.length > 0) {
			context.register(config);
		}
		context.register(DataSourcePoolMetadataProvidersConfiguration.class,
				CacheStatisticsAutoConfiguration.class,
				PublicMetricsAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

	@Configuration
	static class MultipleDataSourcesConfig {

		@Bean
		public DataSource tomcatDataSource() {
			return InitializedBuilder.create()
					.type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
		}

		@Bean
		public DataSource hikariDS() {
			return InitializedBuilder.create().type(HikariDataSource.class).build();
		}

		@Bean
		public DataSource commonsDbcpDataSource() {
			return InitializedBuilder.create().type(BasicDataSource.class).build();
		}

	}

	@Configuration
	static class MultipleDataSourcesWithPrimaryConfig {

		@Bean
		@Primary
		public DataSource myDataSource() {
			return InitializedBuilder.create()
					.type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
		}

		@Bean
		public DataSource commonsDbcpDataSource() {
			return InitializedBuilder.create().type(BasicDataSource.class).build();
		}

	}

	@Configuration
	static class MultipleDataSourcesWithCustomPrimaryConfig {

		@Bean
		@Primary
		public DataSource myDataSource() {
			return InitializedBuilder.create()
					.type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
		}

		@Bean
		public DataSource dataSource() {
			return InitializedBuilder.create().type(BasicDataSource.class).build();
		}

	}

	@Configuration
	static class CustomDataSourcePublicMetrics {

		@Bean
		public DataSourcePublicMetrics myDataSourcePublicMetrics() {
			return new DataSourcePublicMetrics() {
				@Override
				protected String createPrefix(String dataSourceName,
						DataSource dataSource, boolean primary) {
					return (primary ? "ds.first." : "ds.second");
				}
			};
		}

	}

	@Configuration
	static class RichGaugeReaderConfig {

		@Bean
		public RichGaugeReader richGaugeReader() {
			return mock(RichGaugeReader.class);
		}

	}

	@Configuration
	static class TomcatConfiguration {

		@Bean
		public TomcatEmbeddedServletContainerFactory containerFactory() {
			TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
			factory.setPort(SocketUtils.findAvailableTcpPort(40000));
			return factory;
		}

	}

	@Configuration
	static class CacheConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("books", "speakers");
		}

	}

	@Configuration
	static class MultipleCacheConfiguration {

		@Bean
		@Order(1)
		public CacheManager first() {
			return new ConcurrentMapCacheManager("books", "speakers");
		}

		@Bean
		@Order(2)
		public CacheManager second() {
			return new ConcurrentMapCacheManager("users", "speakers");
		}

	}

	private static class InitializedBuilder {

		public static DataSourceBuilder create() {
			return DataSourceBuilder.create()
					.driverClassName("org.hsqldb.jdbc.JDBCDriver")
					.url("jdbc:hsqldb:mem:test").username("sa");
		}

	}

}
