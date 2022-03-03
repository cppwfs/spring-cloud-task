/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.cloud.task.listener;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.observation.Observation;

import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Utility class for publishing Spring Cloud Task specific metrics via Micrometer.
 * Intended for internal use only.
 *
 * @author Christian Tzolov
 * @since 2.2
 */
public class TaskMetrics {
	private MeterRegistry registry;

	public TaskMetrics(MeterRegistry meterRegistry) {
		this.registry = meterRegistry;
	}
	/**
	 * Task timer measurements. Records information about task duration and status.
	 */
	public static final String SPRING_CLOUD_TASK_METER = "spring.cloud.task";

	/**
	 * LongTask timer measurement. Records the run-time status of long-time lasting tasks.
	 */
	public static final String SPRING_CLOUD_TASK_ACTIVE_METER = "spring.cloud.task.active";

	/**
	 * Successful task execution status indicator.
	 */
	public static final String STATUS_SUCCESS = "success";

	/**
	 * Failing task execution status indicator.
	 */
	public static final String STATUS_FAILURE = "failure";

	/**
	 * task name measurement tag.
	 */
	public static final String TASK_NAME_TAG = "task.name";

	/**
	 * task execution id tag.
	 */
	public static final String TASK_EXECUTION_ID_TAG = "task.execution.id";

	/**
	 * task parent execution id tag.
	 */
	public static final String TASK_PARENT_EXECUTION_ID_TAG = "task.parent.execution.id";

	/**
	 * task external execution id tag.
	 */
	public static final String TASK_EXTERNAL_EXECUTION_ID_TAG = "task.external.execution.id";

	/**
	 * task exit code tag.
	 */
	public static final String TASK_EXIT_CODE_TAG = "task.exit.code";

	/**
	 * task status tag. Can be either STATUS_SUCCESS or STATUS_FAILURE
	 */
	public static final String TASK_STATUS_TAG = "task.status";

	/**
	 * task exception tag. Contains the name of the exception class in case of error or
	 * none otherwise.
	 */
	public static final String TASK_EXCEPTION_TAG = "task.exception";

	private LongTaskTimer.Sample longTaskSample;

	private Throwable exception;

	private Observation observation;

	public void onTaskStartup(TaskExecution taskExecution) {
		this.observation = Observation.start(SPRING_CLOUD_TASK_METER, this.registry)
			.lowCardinalityTag(TASK_EXIT_CODE_TAG, String.valueOf(taskExecution.getExitCode()))
			.lowCardinalityTag(TASK_EXCEPTION_TAG,
				(this.exception == null) ? "none"
					: this.exception.getClass().getSimpleName())
			.lowCardinalityTag(TASK_STATUS_TAG,
				(this.exception == null) ? STATUS_SUCCESS : STATUS_FAILURE)
			.lowCardinalityTag(TASK_NAME_TAG, taskExecution.getTaskName())
			.lowCardinalityTag(TASK_EXECUTION_ID_TAG, "" + taskExecution.getExecutionId())
			.lowCardinalityTag(TASK_PARENT_EXECUTION_ID_TAG,
				"" + taskExecution.getParentExecutionId());

		LongTaskTimer longTaskTimer = LongTaskTimer
				.builder(SPRING_CLOUD_TASK_ACTIVE_METER).description("Long task duration")
				.tags(commonTags(taskExecution)).register(this.registry);

		this.longTaskSample = longTaskTimer.start();
	}

	public void onTaskFailed(Throwable throwable) {
		this.exception = throwable;
	}

	public void onTaskEnd(TaskExecution taskExecution) {
		if (this.observation != null) {
			this.observation.stop();
			this.observation = null;
		}

		if (this.longTaskSample != null) {
			this.longTaskSample.stop();
			this.longTaskSample = null;
		}
	}

	private Tags commonTags(TaskExecution taskExecution) {
		return Tags.of(TASK_NAME_TAG, taskExecution.getTaskName())
				.and(TASK_EXECUTION_ID_TAG, "" + taskExecution.getExecutionId())
				.and(TASK_PARENT_EXECUTION_ID_TAG,
						"" + taskExecution.getParentExecutionId())
				.and(TASK_EXTERNAL_EXECUTION_ID_TAG,
						(taskExecution.getExternalExecutionId() == null) ? "unknown"
								: "" + taskExecution.getExternalExecutionId());
	}

}
