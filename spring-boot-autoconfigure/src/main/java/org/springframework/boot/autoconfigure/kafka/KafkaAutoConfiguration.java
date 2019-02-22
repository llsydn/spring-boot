/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.kafka;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.LoggingProducerListener;
import org.springframework.kafka.support.ProducerListener;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Kafka.
 *
 * @author Gary Russell
 * @since 1.5.0
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
@Import(KafkaAnnotationDrivenConfiguration.class)
public class KafkaAutoConfiguration {

	private final KafkaProperties properties;

	public KafkaAutoConfiguration(KafkaProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(KafkaTemplate.class)
	public KafkaTemplate<?, ?> kafkaTemplate(
			ProducerFactory<Object, Object> kafkaProducerFactory,
			ProducerListener<Object, Object> kafkaProducerListener) {
		KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<Object, Object>(
				kafkaProducerFactory);
		kafkaTemplate.setProducerListener(kafkaProducerListener);
		kafkaTemplate.setDefaultTopic(this.properties.getTemplate().getDefaultTopic());
		return kafkaTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(ProducerListener.class)
	public ProducerListener<Object, Object> kafkaProducerListener() {
		return new LoggingProducerListener<Object, Object>();
	}

	@Bean
	@ConditionalOnMissingBean(ConsumerFactory.class)
	public ConsumerFactory<?, ?> kafkaConsumerFactory() {
		return new DefaultKafkaConsumerFactory<Object, Object>(
				this.properties.buildConsumerProperties());
	}

	@Bean
	@ConditionalOnMissingBean(ProducerFactory.class)
	public ProducerFactory<?, ?> kafkaProducerFactory() {
		return new DefaultKafkaProducerFactory<Object, Object>(
				this.properties.buildProducerProperties());
	}

}
