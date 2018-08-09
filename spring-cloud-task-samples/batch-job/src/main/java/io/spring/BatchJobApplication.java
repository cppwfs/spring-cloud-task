package io.spring;

import io.spring.configuration.JobConfiguration;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;

@SpringBootApplication
@EnableTask
@EnableBatchProcessing
public class BatchJobApplication {

	public static void main(String[] args) {
		//SpringApplication.run(BatchJobApplication.class, args);
		new SpringApplicationBuilder().sources(new Class[]{JobConfiguration.class}).build().run(args);
	}
}
