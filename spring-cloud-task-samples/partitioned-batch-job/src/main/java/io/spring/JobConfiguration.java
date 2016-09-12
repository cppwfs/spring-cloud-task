/*
 * Copyright 2016 the original author or authors.
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
package io.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.deployer.resource.docker.DockerResourceLoader;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.partition.DeployerPartitionHandler;
import org.springframework.cloud.task.batch.partition.DeployerStepExecutionHandler;
import org.springframework.cloud.task.batch.partition.NoOpEnvironmentVariablesProvider;
import org.springframework.cloud.task.batch.partition.PassThroughCommandLineArgsProvider;
import org.springframework.cloud.task.batch.partition.SimpleEnvironmentVariablesProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;

/**
 * @author Michael Minella
 */
@Configuration
public class JobConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public DataSource dataSource;

	@Autowired
	public JobRepository jobRepository;

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private DelegatingResourceLoader resourceLoader;

	@Autowired
	private Environment environment;

	private static final int GRID_SIZE = 4;

	@Bean
	public JobExplorerFactoryBean jobExplorer() {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();

		jobExplorerFactoryBean.setDataSource(this.dataSource);

		return jobExplorerFactoryBean;
	}

	@Bean
	public PartitionHandler partitionHandler(TaskLauncher taskLauncher, JobExplorer jobExplorer) throws Exception {
		DockerResourceLoader dockerResourceLoader = new DockerResourceLoader();
		Resource resource = dockerResourceLoader.getResource("docker:cppwfs/partition:latest");
		DeployerPartitionHandler partitionHandler = new DeployerPartitionHandler(taskLauncher, jobExplorer, resource, "workerStep");

		List<String> commandLineArgs = new ArrayList<>(3);
//		commandLineArgs.add("--spring.profiles.active=worker");
//		commandLineArgs.add("--spring.cloud.task.initialize.enable=false");
//		commandLineArgs.add("--spring.batch.initializer.enabled=false");
//		commandLineArgs.add("--spring.datasource.username=spring");
//		commandLineArgs.add("--spring.cloud.task.name=nnaaa");
//		commandLineArgs.add("--spring.datasource.url=jdbc:mysql://192.168.65.131:5360/test");
//		commandLineArgs.add("--spring.datasource.driverClassName=org.mariadb.jdbc.Driver");
//		commandLineArgs.add("--spring.datasource.password=secret");
//		commandLineArgs.add("--MESOS_MARAT HON_URI=http://m1.dcos/service/marathon");
//		commandLineArgs.add("--MESOS_CHRONOS_URI=http://m1.dcos/service/chronos");
		partitionHandler.setCommandLineArgsProvider(new PassThroughCommandLineArgsProvider(commandLineArgs));
		partitionHandler.setEnvironmentVariablesProvider(new SimpleEnvironmentVariablesProvider(getBasicEnvironment()));//new NoOpEnvironmentVariablesProvider());
		partitionHandler.setMaxWorkers(2);
		partitionHandler.setApplicationName("pbjt");

		return partitionHandler;
	}

	private Environment getBasicEnvironment() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map myMap = new HashMap();
		myMap.put("spring.profiles.active", "worker");
		myMap.put("spring.cloud.task.initialize.enable", "false");
		myMap.put("spring.batch.initializer.enabled", "false");
		myMap.put("spring.datasource.username", "spring");
		myMap.put("spring.cloud.task.name", "nnaaa");
		myMap.put("spring.datasource.url", "jdbc:mysql://192.168.65.131:5360/test");
		myMap.put("spring.datasource.driverClassName", "org.mariadb.jdbc.Driver");
		myMap.put("spring.datasource.password", "secret");
		propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
		return environment;
	}

	@Bean
	public Partitioner partitioner() {
		return new Partitioner() {
			@Override
			public Map<String, ExecutionContext> partition(int gridSize) {

				Map<String, ExecutionContext> partitions = new HashMap<>(gridSize);

				for(int i = 0; i < GRID_SIZE; i++) {
					ExecutionContext context1 = new ExecutionContext();
					context1.put("partitionNumber", i);

					partitions.put("partition" + i, context1);
				}

				return partitions;
			}
		};
	}

	@Bean
	@Profile("worker")
	public DeployerStepExecutionHandler stepExecutionHandler(JobExplorer jobExplorer) {
		return new DeployerStepExecutionHandler(this.context, jobExplorer, this.jobRepository);
	}

	@Bean
	@StepScope
	public Tasklet workerTasklet(
			final @Value("#{stepExecutionContext['partitionNumber']}")Integer partitionNumber) {

		return new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				System.out.println("This tasklet ran partition: " + partitionNumber);

				return RepeatStatus.FINISHED;
			}
		};
	}

	@Bean
	public Step step1(PartitionHandler partitionHandler) throws Exception {
		return stepBuilderFactory.get("step1")
				.partitioner(workerStep().getName(), partitioner())
				.step(workerStep())
				.partitionHandler(partitionHandler)
				.build();
	}

	@Bean
	public Step workerStep() {
		return stepBuilderFactory.get("workerStep")
				.tasklet(workerTasklet(null))
				.build();
	}

	@Bean
	@Profile("!worker")
	public Job partitionedJob(PartitionHandler partitionHandler) throws Exception {
		Random random = new Random();
		return jobBuilderFactory.get("pj")
				.start(step1(partitionHandler))
				.build();
	}
}
