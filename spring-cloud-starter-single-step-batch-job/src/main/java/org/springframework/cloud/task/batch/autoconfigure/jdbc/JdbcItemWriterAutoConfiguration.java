/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure.jdbc;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.ItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Autconfiguration for a {@code JdbcBatchItemWriter}.
 *
 * @author Glenn Renfro
 * @since 2.3
 */
@Configuration
@EnableConfigurationProperties(JdbcItemWriterProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
public class JdbcItemWriterAutoConfiguration {

	@Autowired
	private DataSource dataSource;

	@Autowired(required = false)
	private ItemPreparedStatementSetter itemPreparedStatementSetter;

	@Autowired(required = false)
	private ItemSqlParameterSourceProvider itemSqlParameterSourceProvider;

	@Autowired(required = false)
	private NamedParameterJdbcOperations namedParameterJdbcTemplate;

	private JdbcItemWriterProperties properties;

	public JdbcItemWriterAutoConfiguration(JdbcItemWriterProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.jdbcwriter", name = "name")
	ItemWriter<Map<Object, Object>> itemWriter() {

		JdbcBatchItemWriterBuilder<Map<Object, Object>> jdbcBatchItemWriterBuilder = new JdbcBatchItemWriterBuilder<Map<Object, Object>>()
				.dataSource(this.dataSource).sql(this.properties.getSql());
		if (this.itemPreparedStatementSetter != null) {
			jdbcBatchItemWriterBuilder
					.itemPreparedStatementSetter(this.itemPreparedStatementSetter);
		}
		else if (this.itemSqlParameterSourceProvider != null) {
			jdbcBatchItemWriterBuilder
					.itemSqlParameterSourceProvider(this.itemSqlParameterSourceProvider);
		}
		else {
			jdbcBatchItemWriterBuilder.columnMapped();
		}
		jdbcBatchItemWriterBuilder.assertUpdates(this.properties.isAssertUpdates());
		return jdbcBatchItemWriterBuilder.build();
	}

}
