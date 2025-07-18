/*
 * Copyright 2015-2025 the original author or authors.
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

package org.springframework.cloud.task.repository.support;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TaskExecutionCreator;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for the SimpleTaskRepository that uses JDBC as a datastore.
 *
 * @author Glenn Renfro.
 * @author Michael Minella
 * @author Ilayaperumal Gopinathan
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { EmbeddedDataSourceConfiguration.class, SimpleTaskAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
@DirtiesContext
public class SimpleTaskRepositoryJdbcTests {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private DataSource dataSource;

	@Test
	@DirtiesContext
	public void testCreateEmptyExecution() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreEmptyTaskExecution(this.taskRepository);
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(this.dataSource,
				expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNoParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(this.dataSource,
				expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionWithParams(this.taskRepository);
		TaskExecution actualTaskExecution = TestDBUtils.getTaskExecutionFromDB(this.dataSource,
				expectedTaskExecution.getExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void startTaskExecutionWithParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreEmptyTaskExecution(this.taskRepository);

		expectedTaskExecution.setArguments(Collections.singletonList("foo=" + UUID.randomUUID().toString()));
		expectedTaskExecution.setStartTime(LocalDateTime.now());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		TaskExecution actualTaskExecution = this.taskRepository.startTaskExecution(
				expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void startTaskExecutionWithNoParam() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreEmptyTaskExecution(this.taskRepository);

		expectedTaskExecution.setStartTime(LocalDateTime.now());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		TaskExecution actualTaskExecution = this.taskRepository.startTaskExecution(
				expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	public void testUpdateExternalExecutionId() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExternalExecutionId(UUID.randomUUID().toString());
		this.taskRepository.updateExternalExecutionId(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExternalExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testUpdateNullExternalExecutionId() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExternalExecutionId(null);
		this.taskRepository.updateExternalExecutionId(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExternalExecutionId());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution,
				TestDBUtils.getTaskExecutionFromDB(this.dataSource, expectedTaskExecution.getExecutionId()));
	}

	@Test
	public void testInvalidExecutionIdForExternalExecutionIdUpdate() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExternalExecutionId(null);
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			this.taskRepository.updateExternalExecutionId(-1, expectedTaskExecution.getExternalExecutionId());
		});
	}

	@Test
	@DirtiesContext
	public void startTaskExecutionWithParent() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreEmptyTaskExecution(this.taskRepository);

		expectedTaskExecution.setStartTime(LocalDateTime.now());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());
		expectedTaskExecution.setParentExecutionId(12345L);

		TaskExecution actualTaskExecution = this.taskRepository.startTaskExecution(
				expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId(), expectedTaskExecution.getParentExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void testCompleteTaskExecution() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setEndTime(LocalDateTime.now());
		expectedTaskExecution.setExitCode(77);
		expectedTaskExecution.setExitMessage(UUID.randomUUID().toString());

		TaskExecution actualTaskExecution = TaskExecutionCreator.completeExecution(this.taskRepository,
				expectedTaskExecution);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNoParamMaxExitDefaultMessageSize() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExitMessage(new String(new char[SimpleTaskRepository.MAX_EXIT_MESSAGE_SIZE + 1]));
		expectedTaskExecution.setEndTime(LocalDateTime.now());
		expectedTaskExecution.setExitCode(0);
		TaskExecution actualTaskExecution = completeTaskExecution(expectedTaskExecution, this.taskRepository);
		assertThat(actualTaskExecution.getExitMessage().length()).isEqualTo(SimpleTaskRepository.MAX_EXIT_MESSAGE_SIZE);
	}

	@Test
	public void testCreateTaskExecutionNoParamMaxExitMessageSize() {
		SimpleTaskRepository simpleTaskRepository = new SimpleTaskRepository(
				new TaskExecutionDaoFactoryBean(this.dataSource));
		simpleTaskRepository.setMaxExitMessageSize(5);

		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(simpleTaskRepository);
		expectedTaskExecution.setExitMessage(new String(new char[SimpleTaskRepository.MAX_EXIT_MESSAGE_SIZE + 1]));
		expectedTaskExecution.setEndTime(LocalDateTime.now());
		expectedTaskExecution.setExitCode(0);
		TaskExecution actualTaskExecution = completeTaskExecution(expectedTaskExecution, simpleTaskRepository);
		assertThat(actualTaskExecution.getExitMessage().length()).isEqualTo(5);
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNoParamMaxErrorDefaultMessageSize() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setErrorMessage(new String(new char[SimpleTaskRepository.MAX_ERROR_MESSAGE_SIZE + 1]));
		expectedTaskExecution.setEndTime(LocalDateTime.now());
		expectedTaskExecution.setExitCode(0);
		TaskExecution actualTaskExecution = completeTaskExecution(expectedTaskExecution, this.taskRepository);
		assertThat(actualTaskExecution.getErrorMessage().length())
			.isEqualTo(SimpleTaskRepository.MAX_ERROR_MESSAGE_SIZE);
	}

	@Test
	public void testCreateTaskExecutionNoParamMaxErrorMessageSize() {
		SimpleTaskRepository simpleTaskRepository = new SimpleTaskRepository(
				new TaskExecutionDaoFactoryBean(this.dataSource));
		simpleTaskRepository.setMaxErrorMessageSize(5);

		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(simpleTaskRepository);
		expectedTaskExecution.setErrorMessage(new String(new char[SimpleTaskRepository.MAX_ERROR_MESSAGE_SIZE + 1]));
		expectedTaskExecution.setEndTime(LocalDateTime.now());
		expectedTaskExecution.setExitCode(0);
		TaskExecution actualTaskExecution = completeTaskExecution(expectedTaskExecution, simpleTaskRepository);
		assertThat(actualTaskExecution.getErrorMessage().length()).isEqualTo(5);
	}

	@Test
	public void testMaxTaskNameSizeForConstructor() {
		final int MAX_EXIT_MESSAGE_SIZE = 10;
		final int MAX_ERROR_MESSAGE_SIZE = 20;
		final int MAX_TASK_NAME_SIZE = 30;
		SimpleTaskRepository simpleTaskRepository = new SimpleTaskRepository(
				new TaskExecutionDaoFactoryBean(this.dataSource), MAX_EXIT_MESSAGE_SIZE, MAX_TASK_NAME_SIZE,
				MAX_ERROR_MESSAGE_SIZE);
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution.setTaskName(new String(new char[MAX_TASK_NAME_SIZE + 1]));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			simpleTaskRepository.createTaskExecution(expectedTaskExecution);
		});
	}

	@Test
	public void testDefaultMaxTaskNameSizeForConstructor() {
		SimpleTaskRepository simpleTaskRepository = new SimpleTaskRepository(
				new TaskExecutionDaoFactoryBean(this.dataSource), null, null, null);
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution.setTaskName(new String(new char[SimpleTaskRepository.MAX_TASK_NAME_SIZE + 1]));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			simpleTaskRepository.createTaskExecution(expectedTaskExecution);
		});
	}

	@Test
	public void testMaxSizeConstructor() {
		final int MAX_EXIT_MESSAGE_SIZE = 10;
		final int MAX_ERROR_MESSAGE_SIZE = 20;
		SimpleTaskRepository simpleTaskRepository = new SimpleTaskRepository(
				new TaskExecutionDaoFactoryBean(this.dataSource), MAX_EXIT_MESSAGE_SIZE, null, MAX_ERROR_MESSAGE_SIZE);
		verifyTaskRepositoryConstructor(MAX_EXIT_MESSAGE_SIZE, MAX_ERROR_MESSAGE_SIZE, simpleTaskRepository);
	}

	@Test
	public void testDefaultConstructor() {
		SimpleTaskRepository simpleTaskRepository = new SimpleTaskRepository(
				new TaskExecutionDaoFactoryBean(this.dataSource), null, null, null);
		verifyTaskRepositoryConstructor(SimpleTaskRepository.MAX_EXIT_MESSAGE_SIZE,
				SimpleTaskRepository.MAX_ERROR_MESSAGE_SIZE, simpleTaskRepository);
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNoParamMaxTaskName() {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName(new String(new char[SimpleTaskRepository.MAX_TASK_NAME_SIZE + 1]));
		taskExecution.setStartTime(LocalDateTime.now());
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			this.taskRepository.createTaskExecution(taskExecution);
		});
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNegativeException() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setEndTime(LocalDateTime.now());
		expectedTaskExecution.setExitCode(-1);

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			TaskExecution actualTaskExecution = TaskExecutionCreator.completeExecution(this.taskRepository,
					expectedTaskExecution);
			TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, actualTaskExecution);
		});
	}

	@Test
	@DirtiesContext
	public void testCreateTaskExecutionNullEndTime() {
		TaskExecution expectedTaskExecution = TaskExecutionCreator
			.createAndStoreTaskExecutionNoParams(this.taskRepository);
		expectedTaskExecution.setExitCode(-1);
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			TaskExecutionCreator.completeExecution(this.taskRepository, expectedTaskExecution);
		});
	}

	private TaskExecution completeTaskExecution(TaskExecution expectedTaskExecution, TaskRepository taskRepository) {
		return taskRepository.completeTaskExecution(expectedTaskExecution.getExecutionId(),
				expectedTaskExecution.getExitCode(), LocalDateTime.now(), expectedTaskExecution.getExitMessage(),
				expectedTaskExecution.getErrorMessage());
	}

	private void verifyTaskRepositoryConstructor(Integer maxExitMessage, Integer maxErrorMessage,
			TaskRepository taskRepository) {
		TaskExecution expectedTaskExecution = TaskExecutionCreator.createAndStoreTaskExecutionNoParams(taskRepository);
		expectedTaskExecution.setErrorMessage(new String(new char[maxErrorMessage + 1]));
		expectedTaskExecution.setExitMessage(new String(new char[maxExitMessage + 1]));
		expectedTaskExecution.setEndTime(LocalDateTime.now());
		expectedTaskExecution.setExitCode(0);

		TaskExecution actualTaskExecution = completeTaskExecution(expectedTaskExecution, taskRepository);
		assertThat(actualTaskExecution.getErrorMessage().length()).isEqualTo(maxErrorMessage.intValue());
		assertThat(actualTaskExecution.getExitMessage().length()).isEqualTo(maxExitMessage.intValue());
	}

}
