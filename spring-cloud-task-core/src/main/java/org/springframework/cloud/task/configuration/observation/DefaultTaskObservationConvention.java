/*
 * Copyright 2013-2022 the original author or authors.
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

package org.springframework.cloud.task.configuration.observation;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;

/**
 * {@link Observation.ObservationConvention} for Spring Cloud Task.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public class DefaultTaskObservationConvention implements TaskObservationConvention {

	@Override
	public KeyValues getLowCardinalityKeyValues(TaskObservationContext context) {
		return KeyValues.of(TaskDocumentedObservation.TaskRunnerTags.BEAN_NAME.of(context.getBeanName()));
	}

	@Override
	public String getName() {
		return "spring.cloud.task.runner";
	}
}
