/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.cloud.task.batch.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.TaskEventProperties;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Provides a {@link StepExecutionEvent} at the start and end of each step indicating the
 * step's status. The {@link StepExecutionListener#afterStep(StepExecution)} returns the
 * {@link ExitStatus} of the inputted {@link StepExecution}.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Ali Shahbour
 */
public class EventEmittingStepExecutionListener implements StepExecutionListener, Ordered {

	private final MessagePublisher<StepExecutionEvent> messagePublisher;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private TaskEventProperties properties;

	public EventEmittingStepExecutionListener(MessagePublisher messagePublisher, TaskEventProperties properties) {
		Assert.notNull(messagePublisher, "messagePublisher is required");
		Assert.notNull(properties, "properties is required");

		this.messagePublisher = messagePublisher;
		this.properties = properties;
	}

	public EventEmittingStepExecutionListener(MessagePublisher messagePublisher, int order,
			TaskEventProperties properties) {
		this(messagePublisher, properties);
		this.order = order;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.messagePublisher.publish(this.properties.getStepExecutionEventBindingName(),
				new StepExecutionEvent(stepExecution));
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		this.messagePublisher.publish(this.properties.getStepExecutionEventBindingName(),
				new StepExecutionEvent(stepExecution));

		return stepExecution.getExitStatus();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
