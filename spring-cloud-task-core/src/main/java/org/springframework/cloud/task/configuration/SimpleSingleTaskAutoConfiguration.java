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

import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.locks.PassThruLockRegistry;
import org.springframework.util.CollectionUtils;

/**
 * Autoconfiguration of {@link SingleInstanceTaskListener}.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */

@Configuration
@ConditionalOnProperty(prefix = "spring.cloud.task", name = "singleInstanceEnabled")
public class SimpleSingleTaskAutoConfiguration {

	@Autowired(required = false)
	Collection<DataSource> dataSources;

	@Autowired
	private TaskProperties taskProperties;

	@Autowired
	private SimpleTaskNameResolver taskNameResolver;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private ConfigurableApplicationContext context;

	@Bean
	@ConditionalOnMissingBean
	public LockRegistry lockRegistry() {
		if (CollectionUtils.isEmpty(this.dataSources)) {
			return new PassThruLockRegistry();
		}
		verifyEnvironment();
		DefaultLockRepository lockRepository = new DefaultLockRepository(this.dataSources.iterator().next());
		lockRepository.setPrefix(taskProperties.getTablePrefix());
		lockRepository.setTimeToLive(taskProperties.getSingleInstanceLockTtl());
		lockRepository.afterPropertiesSet();
		return new JdbcLockRegistry(lockRepository);
	}

	@Bean
	public SingleInstanceTaskListener taskListener(LockRegistry lockRegistry) {
		return new SingleInstanceTaskListener(lockRegistry,
				this.taskNameResolver,
				this.taskRepository);
	}

	private void verifyEnvironment(){
		int dataSources = this.context.getBeanNamesForType(DataSource.class).length;

		if( dataSources > 1) {
			throw new IllegalStateException("To use the " +
					"SimpleSingleTaskAutoConfigurer with DefaultLockRepository," +
					" the context must contain no more than" +
					" one DataSource, found " + dataSources);
		}
	}

}
