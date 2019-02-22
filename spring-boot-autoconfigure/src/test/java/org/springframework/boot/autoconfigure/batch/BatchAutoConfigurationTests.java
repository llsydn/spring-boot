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

package org.springframework.boot.autoconfigure.batch;

import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BatchAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Kazuki Shimizu
 */
public class BatchAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultContext() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		assertThat(this.context.getBean(JobExplorer.class)).isNotNull();
		assertThat(
				this.context.getBean(BatchProperties.class).getInitializer().isEnabled())
						.isTrue();
		assertThat(new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from BATCH_JOB_EXECUTION")).isEmpty();
	}

	@Test
	public void testNoDatabase() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestCustomConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		JobExplorer explorer = this.context.getBean(JobExplorer.class);
		assertThat(explorer).isNotNull();
		assertThat(explorer.getJobInstances("job", 0, 100)).isEmpty();
	}

	@Test
	public void testNoBatchConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmptyConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(JobLauncher.class).length)
				.isEqualTo(0);
		assertThat(this.context.getBeanNamesForType(JobRepository.class).length)
				.isEqualTo(0);
	}

	@Test
	public void testDefinesAndLaunchesJob() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JobConfiguration.class,
				EmbeddedDataSourceConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		this.context.getBean(JobLauncherCommandLineRunner.class).run();
		assertThat(this.context.getBean(JobRepository.class).getLastJobExecution("job",
				new JobParameters())).isNotNull();
	}

	@Test
	public void testDefinesAndLaunchesNamedJob() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.batch.job.names:discreteRegisteredJob");
		this.context.register(NamedJobConfigurationWithRegisteredJob.class,
				EmbeddedDataSourceConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JobRepository repository = this.context.getBean(JobRepository.class);
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		this.context.getBean(JobLauncherCommandLineRunner.class).run();
		assertThat(repository.getLastJobExecution("discreteRegisteredJob",
				new JobParameters())).isNotNull();
	}

	@Test
	public void testDefinesAndLaunchesLocalJob() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.batch.job.names:discreteLocalJob");
		this.context.register(NamedJobConfigurationWithLocalJob.class,
				EmbeddedDataSourceConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		this.context.getBean(JobLauncherCommandLineRunner.class).run();
		assertThat(this.context.getBean(JobRepository.class)
				.getLastJobExecution("discreteLocalJob", new JobParameters()))
						.isNotNull();
	}

	@Test
	public void testDisableLaunchesJob() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.batch.job.enabled:false");
		this.context.register(JobConfiguration.class,
				EmbeddedDataSourceConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		assertThat(this.context.getBeanNamesForType(CommandLineRunner.class).length)
				.isEqualTo(0);
	}

	@Test
	public void testDisableSchemaLoader() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.name:batchtest",
				"spring.batch.initializer.enabled:false");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		assertThat(
				this.context.getBean(BatchProperties.class).getInitializer().isEnabled())
						.isFalse();
		this.expected.expect(BadSqlGrammarException.class);
		new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from BATCH_JOB_EXECUTION");
	}

	@Test
	public void testUsingJpa() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		// The order is very important here: DataSource -> Hibernate -> Batch
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		PlatformTransactionManager transactionManager = this.context
				.getBean(PlatformTransactionManager.class);
		// It's a lazy proxy, but it does render its target if you ask for toString():
		assertThat(transactionManager.toString().contains("JpaTransactionManager"))
				.isTrue();
		assertThat(this.context.getBean(EntityManagerFactory.class)).isNotNull();
		// Ensure the JobRepository can be used (no problem with isolation level)
		assertThat(this.context.getBean(JobRepository.class).getLastJobExecution("job",
				new JobParameters())).isNull();
	}

	@Test
	public void testRenamePrefix() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.name:batchtest",
				"spring.batch.schema:classpath:batch/custom-schema-hsql.sql",
				"spring.batch.tablePrefix:PREFIX_");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		assertThat(
				this.context.getBean(BatchProperties.class).getInitializer().isEnabled())
						.isTrue();
		assertThat(new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from PREFIX_JOB_EXECUTION")).isEmpty();
		JobExplorer jobExplorer = this.context.getBean(JobExplorer.class);
		assertThat(jobExplorer.findRunningJobExecutions("test")).isEmpty();
		JobRepository jobRepository = this.context.getBean(JobRepository.class);
		assertThat(jobRepository.getLastJobExecution("test", new JobParameters()))
				.isNull();
	}

	@Test
	public void testCustomTablePrefixWithDefaultSchemaDisablesInitializer()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.name:batchtest", "spring.batch.tablePrefix:PREFIX_");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JobLauncher.class)).isNotNull();
		assertThat(
				this.context.getBean(BatchProperties.class).getInitializer().isEnabled())
						.isFalse();
		this.expected.expect(BadSqlGrammarException.class);
		new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForList("select * from BATCH_JOB_EXECUTION");
	}

	@Test
	public void testCustomizeJpaTransactionManagerUsingProperties() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.transaction.default-timeout:30",
				"spring.transaction.rollback-on-commit-failure:true");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(BatchConfigurer.class);
		JpaTransactionManager transactionManager = JpaTransactionManager.class.cast(
				this.context.getBean(BatchConfigurer.class).getTransactionManager());
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
	}

	@Test
	public void testCustomizeDataSourceTransactionManagerUsingProperties()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.transaction.default-timeout:30",
				"spring.transaction.rollback-on-commit-failure:true");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class, BatchAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(BatchConfigurer.class);
		DataSourceTransactionManager transactionManager = DataSourceTransactionManager.class
				.cast(this.context.getBean(BatchConfigurer.class)
						.getTransactionManager());
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
	}

	@Configuration
	protected static class EmptyConfiguration {

	}

	@EnableBatchProcessing
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@EnableBatchProcessing
	@TestAutoConfigurationPackage(City.class)
	protected static class TestCustomConfiguration implements BatchConfigurer {

		private JobRepository jobRepository;

		private MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();

		@Override
		public JobRepository getJobRepository() throws Exception {
			if (this.jobRepository == null) {
				this.factory.afterPropertiesSet();
				this.jobRepository = this.factory.getObject();
			}
			return this.jobRepository;
		}

		@Override
		public PlatformTransactionManager getTransactionManager() throws Exception {
			return new ResourcelessTransactionManager();
		}

		@Override
		public JobLauncher getJobLauncher() throws Exception {
			SimpleJobLauncher launcher = new SimpleJobLauncher();
			launcher.setJobRepository(this.jobRepository);
			return launcher;
		}

		@Override
		public JobExplorer getJobExplorer() throws Exception {
			MapJobExplorerFactoryBean explorer = new MapJobExplorerFactoryBean(
					this.factory);
			explorer.afterPropertiesSet();
			return explorer.getObject();
		}

	}

	@EnableBatchProcessing
	protected static class NamedJobConfigurationWithRegisteredJob {

		@Autowired
		private JobRegistry jobRegistry;

		@Autowired
		private JobRepository jobRepository;

		@Bean
		public JobRegistryBeanPostProcessor registryProcessor() {
			JobRegistryBeanPostProcessor processor = new JobRegistryBeanPostProcessor();
			processor.setJobRegistry(this.jobRegistry);
			return processor;
		}

		@Bean
		public Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteRegisteredJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution)
						throws JobExecutionException {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@EnableBatchProcessing
	protected static class NamedJobConfigurationWithLocalJob {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteLocalJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution)
						throws JobExecutionException {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@EnableBatchProcessing
	protected static class JobConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job job() {
			AbstractJob job = new AbstractJob() {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution)
						throws JobExecutionException {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

}
