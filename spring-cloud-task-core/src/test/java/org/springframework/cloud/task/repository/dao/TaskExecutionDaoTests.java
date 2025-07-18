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

package org.springframework.cloud.task.repository.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.task.configuration.TestConfiguration;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.util.TestDBUtils;
import org.springframework.cloud.task.util.TestVerifierUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Executes unit tests on JdbcTaskExecutionDao.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Michael Minella
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
public class TaskExecutionDaoTests extends BaseTaskExecutionDaoTestCases {

	@Autowired
	TaskRepository repository;

	@Autowired
	private DataSource dataSource;

	@BeforeEach
	public void setup() {
		final JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(this.dataSource);
		dao.setTaskIncrementer(TestDBUtils.getIncrementer(this.dataSource));
		super.dao = dao;
	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void testStartTaskExecutionGeneric(String testType) {
		getDao(testType);
		TaskExecution expectedTaskExecution = this.dao.createTaskExecution(null, null, new ArrayList<>(0), null);

		expectedTaskExecution.setArguments(Collections.singletonList("foo=" + UUID.randomUUID().toString()));
		expectedTaskExecution.setStartTime(LocalDateTime.now());
		expectedTaskExecution.setTaskName(UUID.randomUUID().toString());

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, getTaskExecution(testType, expectedTaskExecution));
	}

	private TaskExecutionDao getDao(String type) {
		if (type.equals("db")) {
			final JdbcTaskExecutionDao jdbcDao = new JdbcTaskExecutionDao(this.dataSource);
			jdbcDao.setTaskIncrementer(TestDBUtils.getIncrementer(this.dataSource));
			this.dao = jdbcDao;
		}
		else {
			this.dao = new MapTaskExecutionDao();
		}
		return this.dao;

	}

	private TaskExecution getTaskExecution(String type, TaskExecution expectedTaskExecution) {
		TaskExecution taskExecution;
		if (type.equals("db")) {
			taskExecution = TestDBUtils.getTaskExecutionFromDB(this.dataSource, expectedTaskExecution.getExecutionId());
		}
		else {
			Map<Long, TaskExecution> taskExecutionMap = ((MapTaskExecutionDao) dao).getTaskExecutions();

			taskExecution = taskExecutionMap.get(expectedTaskExecution.getExecutionId());
		}
		return taskExecution;

	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void createTaskExecution(String testType) {
		getDao(testType);
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		expectedTaskExecution = this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, getTaskExecution(testType, expectedTaskExecution));
	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void createEmptyTaskExecution(String testType) {
		getDao(testType);
		TaskExecution expectedTaskExecution = this.dao.createTaskExecution(null, null, new ArrayList<>(0), null);

		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, getTaskExecution(testType, expectedTaskExecution));
	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void completeTaskExecution(String testType) {
		getDao(testType);
		TaskExecution expectedTaskExecution = TestVerifierUtils.endSampleTaskExecutionNoArg();
		expectedTaskExecution = this.dao.createTaskExecution(expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(),
				expectedTaskExecution.getExternalExecutionId());
		this.dao.completeTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getExitCode(),
				expectedTaskExecution.getEndTime(), expectedTaskExecution.getExitMessage());
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, getTaskExecution(testType, expectedTaskExecution));
	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void completeTaskExecutionWithNoCreate(String testType) {
		getDao(testType);
		JdbcTaskExecutionDao dao = new JdbcTaskExecutionDao(this.dataSource);

		TaskExecution expectedTaskExecution = TestVerifierUtils.endSampleTaskExecutionNoArg();
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			dao.completeTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getExitCode(),
					expectedTaskExecution.getEndTime(), expectedTaskExecution.getExitMessage());
		});
	}

	@Test
	@DirtiesContext
	public void testFindAllPageableSort() {
		initializeRepositoryNotInOrder();
		Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC, "EXTERNAL_EXECUTION_ID"));
		Iterator<TaskExecution> iter = getPageIterator(0, 2, sort);
		TaskExecution taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO2");
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO3");

		iter = getPageIterator(1, 2, sort);
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO1");
	}

	@Test
	@DirtiesContext
	public void testFindAllDefaultSort() {
		initializeRepository();
		Iterator<TaskExecution> iter = getPageIterator(0, 2, null);
		TaskExecution taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO1");
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO2");

		iter = getPageIterator(1, 2, null);
		taskExecution = iter.next();
		assertThat(taskExecution.getTaskName()).isEqualTo("FOO3");
	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void testStartExecutionWithNullExternalExecutionIdExisting(String testType) {
		getDao(testType);
		TaskExecution expectedTaskExecution = initializeTaskExecutionWithExternalExecutionId();

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(), null);
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, getTaskExecution(testType, expectedTaskExecution));
	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void testStartExecutionWithNullExternalExecutionIdNonExisting(String testType) {
		getDao(testType);
		TaskExecution expectedTaskExecution = initializeTaskExecutionWithExternalExecutionId();

		this.dao.startTaskExecution(expectedTaskExecution.getExecutionId(), expectedTaskExecution.getTaskName(),
				expectedTaskExecution.getStartTime(), expectedTaskExecution.getArguments(), "BAR");
		expectedTaskExecution.setExternalExecutionId("BAR");
		TestVerifierUtils.verifyTaskExecution(expectedTaskExecution, getTaskExecution(testType, expectedTaskExecution));
	}

	@ParameterizedTest
	@DirtiesContext
	@ValueSource(strings = { "db", "map" })
	public void testFindRunningTaskExecutions(String testType) {
		getDao(testType);
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		assertThat(this.dao.findRunningTaskExecutions("FOO1", PageRequest.of(1, 4, Sort.by("START_TIME")))
			.getTotalElements()).isEqualTo(4);
	}

	@Test
	@DirtiesContext
	public void testFindRunningTaskExecutionsIllegalSort() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		assertThatThrownBy(() -> this.dao
			.findRunningTaskExecutions("FOO1", PageRequest.of(1, Integer.MAX_VALUE, Sort.by("ILLEGAL_SORT")))
			.getTotalElements()).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Invalid sort option selected: ILLEGAL_SORT");
	}

	@Test
	@DirtiesContext
	public void testFindRunningTaskExecutionsSortWithDifferentCase() {
		initializeRepositoryNotInOrderWithMultipleTaskExecutions();
		assertThat(
				this.dao.findRunningTaskExecutions("FOO1", PageRequest.of(1, Integer.MAX_VALUE, Sort.by("StArT_TiMe")))
					.getTotalElements())
			.isEqualTo(4);
	}

	private TaskExecution initializeTaskExecutionWithExternalExecutionId() {
		TaskExecution expectedTaskExecution = TestVerifierUtils.createSampleTaskExecutionNoArg();
		return this.dao.createTaskExecution(expectedTaskExecution.getTaskName(), expectedTaskExecution.getStartTime(),
				expectedTaskExecution.getArguments(), "FOO1");
	}

	private Iterator<TaskExecution> getPageIterator(int pageNum, int pageSize, Sort sort) {
		Pageable pageable = (sort == null) ? PageRequest.of(pageNum, pageSize)
				: PageRequest.of(pageNum, pageSize, sort);
		Page<TaskExecution> page = this.dao.findAll(pageable);
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getTotalPages()).isEqualTo(2);
		return page.iterator();
	}

	private void initializeRepository() {
		this.repository.createTaskExecution(getTaskExecution("FOO3", "externalA"));
		this.repository.createTaskExecution(getTaskExecution("FOO2", "externalB"));
		this.repository.createTaskExecution(getTaskExecution("FOO1", "externalC"));
	}

	private void initializeRepositoryNotInOrder() {
		this.repository.createTaskExecution(getTaskExecution("FOO1", "externalC"));
		this.repository.createTaskExecution(getTaskExecution("FOO2", "externalA"));
		this.repository.createTaskExecution(getTaskExecution("FOO3", "externalB"));
	}

}
