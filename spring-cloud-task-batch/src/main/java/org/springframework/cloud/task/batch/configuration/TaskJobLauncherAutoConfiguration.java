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

package org.springframework.cloud.task.batch.configuration;

import java.util.List;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * Provides auto configuration for the
 * {@link org.springframework.cloud.task.batch.handler.TaskJobLauncherApplicationRunner}.
 *
 * @author Glenn Renfro
 */
@AutoConfiguration
@Conditional(JobLaunchCondition.class)
@EnableConfigurationProperties(TaskBatchProperties.class)
@AutoConfigureBefore(BatchAutoConfiguration.class)
public class TaskJobLauncherAutoConfiguration {

	@Autowired
	private TaskBatchProperties properties;

	@Bean
	@ConditionalOnClass(name = "org.springframework.boot.batch.autoconfigure.JobLauncherApplicationRunner")
	public TaskJobLauncherApplicationRunnerFactoryBean taskJobLauncherApplicationRunner(JobOperator jobLauncher,
			List<Job> jobs, JobRegistry jobRegistry, JobRepository jobRepository, BatchProperties batchProperties) {
		TaskJobLauncherApplicationRunnerFactoryBean taskJobLauncherApplicationRunnerFactoryBean;
		taskJobLauncherApplicationRunnerFactoryBean = new TaskJobLauncherApplicationRunnerFactoryBean(jobLauncher, jobs,
				this.properties, jobRegistry, jobRepository, batchProperties);

		return taskJobLauncherApplicationRunnerFactoryBean;
	}

}
