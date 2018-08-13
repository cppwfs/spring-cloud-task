package io.spring;

import io.spring.configuration.JobConfiguration;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.task.configuration.EnableTask;

@SpringBootApplication
@EnableTask
@EnableBatchProcessing
public class BatchJobApplication {

	public static void main(String[] args) {
		//SpringApplication.run(BatchJobApplication.class, args); //works
		new SpringApplicationBuilder().sources(new Class[]{BatchJobApplication.class, JobConfiguration.class}).build().run(args); //ehh not so much
	}
}
