/*
 * Copyright 2018-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.batch.handler;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchProperties;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Glenn Renfro
 */
public class TaskJobLauncherApplicationRunnerCoreTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class))
		.withUserConfiguration(BatchConfiguration.class);

	@Test
	void basicExecution() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			jobLauncherContext.executeJob(new JobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
			jobLauncherContext.executeJob(new JobParametersBuilder().addLong("id", 1L).toJobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
		});
	}

	@Test
	void incrementExistingExecution() {
		this.contextRunner.run((context) -> {
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.configureJob().incrementer(new RunIdIncrementer()).build();
			jobLauncherContext.runner.execute(job, new JobParameters());
			jobLauncherContext.runner.execute(job, new JobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
		});
	}

	@Test
	void runDifferentInstances() {
		this.contextRunner.run((context) -> {
			PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.jobBuilder()
				.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet(), transactionManager).build())
				.build();
			// start a job instance
			JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo").toJobParameters();
			runFailedJob(jobLauncherContext, job, jobParameters);
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
			// start a different job instance
			JobParameters otherJobParameters = new JobParametersBuilder().addString("name", "bar").toJobParameters();
			runFailedJob(jobLauncherContext, job, otherJobParameters);

			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
		});
	}

	@Test
	void retryFailedExecutionOnNonRestartableJob() {
		this.contextRunner.run((context) -> {
			PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.jobBuilder()
				.preventRestart()
				.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet(), transactionManager).build())
				.incrementer(new RunIdIncrementer())
				.build();
			runFailedJob(jobLauncherContext, job, new JobParameters());
			runFailedJob(jobLauncherContext, job, new JobParameters());
			// A failed job that is not restartable does not re-use the job params of
			// the last execution, but creates a new job instance when running it again.
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
			assertThatExceptionOfType(TaskException.class).isThrownBy(() -> {
				// try to re-run a failed execution
				// In this case the change from the previous behavior is that a new job
				// instance is created
				// https://github.com/spring-projects/spring-batch/issues/4910
				jobLauncherContext.runner.execute(job,
						new JobParametersBuilder().addLong("run.id", 1L).toJobParameters());
				fail("expected TaskException");
			}).withMessageContaining("Job job failed during execution for job instance id 3 with jobExecutionId of 3 ");
		});
	}

	@Test
	void retryFailedExecutionWithNonIdentifyingParameters() {
		this.contextRunner.run((context) -> {
			PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
			JobLauncherApplicationRunnerContext jobLauncherContext = new JobLauncherApplicationRunnerContext(context);
			Job job = jobLauncherContext.jobBuilder()
				.start(jobLauncherContext.stepBuilder().tasklet(throwingTasklet(), transactionManager).build())
				.incrementer(new RunIdIncrementer())
				.build();
			JobParameters jobParameters = new JobParametersBuilder().addLong("id", 1L, false)
				.addLong("foo", 2L, false)
				.toJobParameters();
			runFailedJob(jobLauncherContext, job, jobParameters);
			assertThat(jobLauncherContext.jobInstances()).hasSize(1);
			// try to re-run a failed execution with non identifying parameters
			// Updated to expect a new 2 instances are created because of this change
			// https://github.com/spring-projects/spring-batch/issues/4910
			runFailedJob(jobLauncherContext, job,
					new JobParametersBuilder(jobParameters).addLong("run.id", 1L).toJobParameters());
			assertThat(jobLauncherContext.jobInstances()).hasSize(2);
		});
	}

	private Tasklet throwingTasklet() {
		return (contribution, chunkContext) -> {
			throw new RuntimeException("Planned");
		};
	}

	private void runFailedJob(JobLauncherApplicationRunnerContext jobLauncherContext, Job job,
			JobParameters jobParameters) throws Exception {
		boolean isExceptionThrown = false;
		try {
			jobLauncherContext.runner.execute(job, jobParameters);
		}
		catch (TaskException taskException) {
			isExceptionThrown = true;
		}
		assertThat(isExceptionThrown).isTrue();
	}

	static class JobLauncherApplicationRunnerContext {

		private final TaskJobLauncherApplicationRunner runner;

		private final JobRepository jobRepository;

		private final JobBuilder jobBuilder;

		private final Job job;

		private final StepBuilder stepBuilder;

		private final Step step;

		JobLauncherApplicationRunnerContext(ApplicationContext context) {
			JobOperator jobOperator = context.getBean(JobOperator.class);
			jobRepository = context.getBean(JobRepository.class);
			PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
			this.stepBuilder = new StepBuilder("step", jobRepository);
			this.step = this.stepBuilder.tasklet((contribution, chunkContext) -> null, transactionManager).build();
			this.jobBuilder = new JobBuilder("job", jobRepository);
			this.job = this.jobBuilder.start(this.step).build();
			this.runner = new TaskJobLauncherApplicationRunner(jobOperator, jobRepository, new TaskBatchProperties());
		}

		List<JobInstance> jobInstances() {
			return this.jobRepository.getJobInstances("job", 0, 100);
		}

		void executeJob(JobParameters jobParameters) throws JobExecutionException {
			this.runner.execute(this.job, jobParameters);
		}

		JobBuilder jobBuilder() {
			return this.jobBuilder;
		}

		StepBuilder stepBuilder() {
			return this.stepBuilder;
		}

		SimpleJobBuilder configureJob() {
			return this.jobBuilder.start(this.step);
		}

	}

	@EnableBatchProcessing
	@EnableJdbcJobRepository
	@Configuration(proxyBeanMethods = false)
	static class BatchConfiguration {

		private final DataSource dataSource;

		protected BatchConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		DataSourceScriptDatabaseInitializer batchDataSourceInitializer() {
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setSchemaLocations(Arrays.asList("classpath:org/springframework/batch/core/schema-h2.sql"));
			return new DataSourceScriptDatabaseInitializer(this.dataSource, settings);
		}

	}

}
