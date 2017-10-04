/*
 *  Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import java.util.concurrent.locks.Lock;

import javax.sql.DataSource;

import org.springframework.cloud.task.listener.TaskExecutionException;
import org.springframework.cloud.task.listener.annotation.AfterTask;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.listener.annotation.FailedTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * When singleInstanceEnabled is set to true this listener will create a lock for the task
 * based on the spring.cloud.task.name. If a lock already exists this Listener will throw
 * a TaskExecutionException.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
public class SingleInstanceTaskListener {

	private LockRegistry lockRegistry;

	private Lock singleInstanceLock;

	private TaskNameResolver taskNameResolver;

	public SingleInstanceTaskListener(LockRegistry lockRegistry,
			TaskNameResolver taskNameResolver) {
		this.lockRegistry = lockRegistry;
		this.taskNameResolver = taskNameResolver;
	}

	public SingleInstanceTaskListener(DataSource dataSource,
			TaskNameResolver taskNameResolver,
			TaskProperties taskProperties) {
		DefaultLockRepository lockRepository = new DefaultLockRepository(dataSource);
		lockRepository.setPrefix(taskProperties.getTablePrefix());
		lockRepository.setTimeToLive(taskProperties.getSingleInstanceLockTtl());
		lockRepository.afterPropertiesSet();
		this.lockRegistry = new JdbcLockRegistry(lockRepository);
		this.taskNameResolver = taskNameResolver;
	}

	@BeforeTask
	public void lockTask(TaskExecution taskExecution) {
		this.singleInstanceLock = this.lockRegistry.obtain(this.taskNameResolver.getTaskName());
		if (!singleInstanceLock.tryLock()) {
			String errorMessage = String.format(
					"Task with name \"%s\" is already running.",
					this.taskNameResolver.getTaskName());
			throw new TaskExecutionException(errorMessage);
		}
	}

	@AfterTask
	public void unlockTaskOnEnd(TaskExecution taskExecution) {
		this.singleInstanceLock.unlock();
	}

	@FailedTask
	public void unlockTaskOnError(TaskExecution taskExecution) {
		this.singleInstanceLock.unlock();
	}

}
