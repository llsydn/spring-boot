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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;
import org.springframework.util.CollectionUtils;

/**
 * Configuration properties for Spring for Apache Kafka.
 * <p>
 * Users should refer to Kafka documentation for complete descriptions of these
 * properties.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {

	/**
	 * Comma-delimited list of host:port pairs to use for establishing the initial
	 * connection to the Kafka cluster.
	 */
	private List<String> bootstrapServers = new ArrayList<String>(
			Collections.singletonList("localhost:9092"));

	/**
	 * Id to pass to the server when making requests; used for server-side logging.
	 */
	private String clientId;

	/**
	 * Additional properties used to configure the client.
	 */
	private Map<String, String> properties = new HashMap<String, String>();

	private final Consumer consumer = new Consumer();

	private final Producer producer = new Producer();

	private final Listener listener = new Listener();

	private final Ssl ssl = new Ssl();

	private final Template template = new Template();

	public List<String> getBootstrapServers() {
		return this.bootstrapServers;
	}

	public void setBootstrapServers(List<String> bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Producer getProducer() {
		return this.producer;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public Template getTemplate() {
		return this.template;
	}

	private Map<String, Object> buildCommonProperties() {
		Map<String, Object> properties = new HashMap<String, Object>();
		if (this.bootstrapServers != null) {
			properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
					this.bootstrapServers);
		}
		if (this.clientId != null) {
			properties.put(CommonClientConfigs.CLIENT_ID_CONFIG, this.clientId);
		}
		if (this.ssl.getKeyPassword() != null) {
			properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, this.ssl.getKeyPassword());
		}
		if (this.ssl.getKeystoreLocation() != null) {
			properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
					resourceToPath(this.ssl.getKeystoreLocation()));
		}
		if (this.ssl.getKeystorePassword() != null) {
			properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
					this.ssl.getKeystorePassword());
		}
		if (this.ssl.getTruststoreLocation() != null) {
			properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
					resourceToPath(this.ssl.getTruststoreLocation()));
		}
		if (this.ssl.getTruststorePassword() != null) {
			properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
					this.ssl.getTruststorePassword());
		}
		if (!CollectionUtils.isEmpty(this.properties)) {
			properties.putAll(this.properties);
		}
		return properties;
	}

	/**
	 * Create an initial map of consumer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaConsumerFactory bean.
	 * @return the consumer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildConsumerProperties() {
		Map<String, Object> properties = buildCommonProperties();
		properties.putAll(this.consumer.buildProperties());
		return properties;
	}

	/**
	 * Create an initial map of producer properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaProducerFactory bean.
	 * @return the producer properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildProducerProperties() {
		Map<String, Object> properties = buildCommonProperties();
		properties.putAll(this.producer.buildProperties());
		return properties;
	}

	private static String resourceToPath(Resource resource) {
		try {
			return resource.getFile().getAbsolutePath();
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Resource '" + resource + "' must be on a file system", ex);
		}
	}

	public static class Consumer {

		private final Ssl ssl = new Ssl();

		/**
		 * Frequency in milliseconds that the consumer offsets are auto-committed to Kafka
		 * if 'enable.auto.commit' true.
		 */
		private Integer autoCommitInterval;

		/**
		 * What to do when there is no initial offset in Kafka or if the current offset
		 * does not exist any more on the server.
		 */
		private String autoOffsetReset;

		/**
		 * Comma-delimited list of host:port pairs to use for establishing the initial
		 * connection to the Kafka cluster.
		 */
		private List<String> bootstrapServers;

		/**
		 * Id to pass to the server when making requests; used for server-side logging.
		 */
		private String clientId;

		/**
		 * If true the consumer's offset will be periodically committed in the background.
		 */
		private Boolean enableAutoCommit;

		/**
		 * Maximum amount of time in milliseconds the server will block before answering
		 * the fetch request if there isn't sufficient data to immediately satisfy the
		 * requirement given by "fetch.min.bytes".
		 */
		private Integer fetchMaxWait;

		/**
		 * Minimum amount of data the server should return for a fetch request in bytes.
		 */
		private Integer fetchMinSize;

		/**
		 * Unique string that identifies the consumer group this consumer belongs to.
		 */
		private String groupId;

		/**
		 * Expected time in milliseconds between heartbeats to the consumer coordinator.
		 */
		private Integer heartbeatInterval;

		/**
		 * Deserializer class for keys.
		 */
		private Class<?> keyDeserializer = StringDeserializer.class;

		/**
		 * Deserializer class for values.
		 */
		private Class<?> valueDeserializer = StringDeserializer.class;

		/**
		 * Maximum number of records returned in a single call to poll().
		 */
		private Integer maxPollRecords;

		public Ssl getSsl() {
			return this.ssl;
		}

		public Integer getAutoCommitInterval() {
			return this.autoCommitInterval;
		}

		public void setAutoCommitInterval(Integer autoCommitInterval) {
			this.autoCommitInterval = autoCommitInterval;
		}

		public String getAutoOffsetReset() {
			return this.autoOffsetReset;
		}

		public void setAutoOffsetReset(String autoOffsetReset) {
			this.autoOffsetReset = autoOffsetReset;
		}

		public List<String> getBootstrapServers() {
			return this.bootstrapServers;
		}

		public void setBootstrapServers(List<String> bootstrapServers) {
			this.bootstrapServers = bootstrapServers;
		}

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public Boolean getEnableAutoCommit() {
			return this.enableAutoCommit;
		}

		public void setEnableAutoCommit(Boolean enableAutoCommit) {
			this.enableAutoCommit = enableAutoCommit;
		}

		public Integer getFetchMaxWait() {
			return this.fetchMaxWait;
		}

		public void setFetchMaxWait(Integer fetchMaxWait) {
			this.fetchMaxWait = fetchMaxWait;
		}

		public Integer getFetchMinSize() {
			return this.fetchMinSize;
		}

		public void setFetchMinSize(Integer fetchMinSize) {
			this.fetchMinSize = fetchMinSize;
		}

		public String getGroupId() {
			return this.groupId;
		}

		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}

		public Integer getHeartbeatInterval() {
			return this.heartbeatInterval;
		}

		public void setHeartbeatInterval(Integer heartbeatInterval) {
			this.heartbeatInterval = heartbeatInterval;
		}

		public Class<?> getKeyDeserializer() {
			return this.keyDeserializer;
		}

		public void setKeyDeserializer(Class<?> keyDeserializer) {
			this.keyDeserializer = keyDeserializer;
		}

		public Class<?> getValueDeserializer() {
			return this.valueDeserializer;
		}

		public void setValueDeserializer(Class<?> valueDeserializer) {
			this.valueDeserializer = valueDeserializer;
		}

		public Integer getMaxPollRecords() {
			return this.maxPollRecords;
		}

		public void setMaxPollRecords(Integer maxPollRecords) {
			this.maxPollRecords = maxPollRecords;
		}

		public Map<String, Object> buildProperties() {
			Map<String, Object> properties = new HashMap<String, Object>();
			if (this.autoCommitInterval != null) {
				properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,
						this.autoCommitInterval);
			}
			if (this.autoOffsetReset != null) {
				properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
						this.autoOffsetReset);
			}
			if (this.bootstrapServers != null) {
				properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
						this.bootstrapServers);
			}
			if (this.clientId != null) {
				properties.put(ConsumerConfig.CLIENT_ID_CONFIG, this.clientId);
			}
			if (this.enableAutoCommit != null) {
				properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
						this.enableAutoCommit);
			}
			if (this.fetchMaxWait != null) {
				properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,
						this.fetchMaxWait);
			}
			if (this.fetchMinSize != null) {
				properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, this.fetchMinSize);
			}
			if (this.groupId != null) {
				properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
			}
			if (this.heartbeatInterval != null) {
				properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,
						this.heartbeatInterval);
			}
			if (this.keyDeserializer != null) {
				properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
						this.keyDeserializer);
			}
			if (this.ssl.getKeyPassword() != null) {
				properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG,
						this.ssl.getKeyPassword());
			}
			if (this.ssl.getKeystoreLocation() != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
						resourceToPath(this.ssl.getKeystoreLocation()));
			}
			if (this.ssl.getKeystorePassword() != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
						this.ssl.getKeystorePassword());
			}
			if (this.ssl.getTruststoreLocation() != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
						resourceToPath(this.ssl.getTruststoreLocation()));
			}
			if (this.ssl.getTruststorePassword() != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
						this.ssl.getTruststorePassword());
			}
			if (this.valueDeserializer != null) {
				properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
						this.valueDeserializer);
			}
			if (this.maxPollRecords != null) {
				properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
						this.maxPollRecords);
			}
			return properties;
		}

	}

	public static class Producer {

		private final Ssl ssl = new Ssl();

		/**
		 * Number of acknowledgments the producer requires the leader to have received
		 * before considering a request complete.
		 */
		private String acks;

		/**
		 * Number of records to batch before sending.
		 */
		private Integer batchSize;

		/**
		 * Comma-delimited list of host:port pairs to use for establishing the initial
		 * connection to the Kafka cluster.
		 */
		private List<String> bootstrapServers;

		/**
		 * Total bytes of memory the producer can use to buffer records waiting to be sent
		 * to the server.
		 */
		private Long bufferMemory;

		/**
		 * Id to pass to the server when making requests; used for server-side logging.
		 */
		private String clientId;

		/**
		 * Compression type for all data generated by the producer.
		 */
		private String compressionType;

		/**
		 * Serializer class for keys.
		 */
		private Class<?> keySerializer = StringSerializer.class;

		/**
		 * Serializer class for values.
		 */
		private Class<?> valueSerializer = StringSerializer.class;

		/**
		 * When greater than zero, enables retrying of failed sends.
		 */
		private Integer retries;

		public Ssl getSsl() {
			return this.ssl;
		}

		public String getAcks() {
			return this.acks;
		}

		public void setAcks(String acks) {
			this.acks = acks;
		}

		public Integer getBatchSize() {
			return this.batchSize;
		}

		public void setBatchSize(Integer batchSize) {
			this.batchSize = batchSize;
		}

		public List<String> getBootstrapServers() {
			return this.bootstrapServers;
		}

		public void setBootstrapServers(List<String> bootstrapServers) {
			this.bootstrapServers = bootstrapServers;
		}

		public Long getBufferMemory() {
			return this.bufferMemory;
		}

		public void setBufferMemory(Long bufferMemory) {
			this.bufferMemory = bufferMemory;
		}

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getCompressionType() {
			return this.compressionType;
		}

		public void setCompressionType(String compressionType) {
			this.compressionType = compressionType;
		}

		public Class<?> getKeySerializer() {
			return this.keySerializer;
		}

		public void setKeySerializer(Class<?> keySerializer) {
			this.keySerializer = keySerializer;
		}

		public Class<?> getValueSerializer() {
			return this.valueSerializer;
		}

		public void setValueSerializer(Class<?> valueSerializer) {
			this.valueSerializer = valueSerializer;
		}

		public Integer getRetries() {
			return this.retries;
		}

		public void setRetries(Integer retries) {
			this.retries = retries;
		}

		public Map<String, Object> buildProperties() {
			Map<String, Object> properties = new HashMap<String, Object>();
			if (this.acks != null) {
				properties.put(ProducerConfig.ACKS_CONFIG, this.acks);
			}
			if (this.batchSize != null) {
				properties.put(ProducerConfig.BATCH_SIZE_CONFIG, this.batchSize);
			}
			if (this.bootstrapServers != null) {
				properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
						this.bootstrapServers);
			}
			if (this.bufferMemory != null) {
				properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, this.bufferMemory);
			}
			if (this.clientId != null) {
				properties.put(ProducerConfig.CLIENT_ID_CONFIG, this.clientId);
			}
			if (this.compressionType != null) {
				properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
						this.compressionType);
			}
			if (this.keySerializer != null) {
				properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
						this.keySerializer);
			}
			if (this.retries != null) {
				properties.put(ProducerConfig.RETRIES_CONFIG, this.retries);
			}
			if (this.ssl.getKeyPassword() != null) {
				properties.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG,
						this.ssl.getKeyPassword());
			}
			if (this.ssl.getKeystoreLocation() != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
						resourceToPath(this.ssl.getKeystoreLocation()));
			}
			if (this.ssl.getKeystorePassword() != null) {
				properties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
						this.ssl.getKeystorePassword());
			}
			if (this.ssl.getTruststoreLocation() != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
						resourceToPath(this.ssl.getTruststoreLocation()));
			}
			if (this.ssl.getTruststorePassword() != null) {
				properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
						this.ssl.getTruststorePassword());
			}
			if (this.valueSerializer != null) {
				properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
						this.valueSerializer);
			}
			return properties;
		}

	}

	public static class Template {

		/**
		 * Default topic to which messages will be sent.
		 */
		private String defaultTopic;

		public String getDefaultTopic() {
			return this.defaultTopic;
		}

		public void setDefaultTopic(String defaultTopic) {
			this.defaultTopic = defaultTopic;
		}

	}

	public static class Listener {

		/**
		 * Listener AckMode; see the spring-kafka documentation.
		 */
		private AckMode ackMode;

		/**
		 * Number of threads to run in the listener containers.
		 */
		private Integer concurrency;

		/**
		 * Timeout in milliseconds to use when polling the consumer.
		 */
		private Long pollTimeout;

		/**
		 * Number of records between offset commits when ackMode is "COUNT" or
		 * "COUNT_TIME".
		 */
		private Integer ackCount;

		/**
		 * Time in milliseconds between offset commits when ackMode is "TIME" or
		 * "COUNT_TIME".
		 */
		private Long ackTime;

		public AckMode getAckMode() {
			return this.ackMode;
		}

		public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		public Integer getConcurrency() {
			return this.concurrency;
		}

		public void setConcurrency(Integer concurrency) {
			this.concurrency = concurrency;
		}

		public Long getPollTimeout() {
			return this.pollTimeout;
		}

		public void setPollTimeout(Long pollTimeout) {
			this.pollTimeout = pollTimeout;
		}

		public Integer getAckCount() {
			return this.ackCount;
		}

		public void setAckCount(Integer ackCount) {
			this.ackCount = ackCount;
		}

		public Long getAckTime() {
			return this.ackTime;
		}

		public void setAckTime(Long ackTime) {
			this.ackTime = ackTime;
		}

	}

	public static class Ssl {

		/**
		 * Password of the private key in the key store file.
		 */
		private String keyPassword;

		/**
		 * Location of the key store file.
		 */
		private Resource keystoreLocation;

		/**
		 * Store password for the key store file.
		 */
		private String keystorePassword;

		/**
		 * Location of the trust store file.
		 */
		private Resource truststoreLocation;

		/**
		 * Store password for the trust store file.
		 */
		private String truststorePassword;

		public String getKeyPassword() {
			return this.keyPassword;
		}

		public void setKeyPassword(String keyPassword) {
			this.keyPassword = keyPassword;
		}

		public Resource getKeystoreLocation() {
			return this.keystoreLocation;
		}

		public void setKeystoreLocation(Resource keystoreLocation) {
			this.keystoreLocation = keystoreLocation;
		}

		public String getKeystorePassword() {
			return this.keystorePassword;
		}

		public void setKeystorePassword(String keystorePassword) {
			this.keystorePassword = keystorePassword;
		}

		public Resource getTruststoreLocation() {
			return this.truststoreLocation;
		}

		public void setTruststoreLocation(Resource truststoreLocation) {
			this.truststoreLocation = truststoreLocation;
		}

		public String getTruststorePassword() {
			return this.truststorePassword;
		}

		public void setTruststorePassword(String truststorePassword) {
			this.truststorePassword = truststorePassword;
		}

	}

}
