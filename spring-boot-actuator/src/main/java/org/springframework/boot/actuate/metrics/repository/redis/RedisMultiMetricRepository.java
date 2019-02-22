/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.repository.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MultiMetricRepository;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.util.Assert;

/**
 * {@link MultiMetricRepository} implementation backed by a redis store. Metric values are
 * stored as zset values and the timestamps as regular values, both against a key composed
 * of the group name prefixed with a constant prefix (default "spring.groups."). The group
 * names are stored as a zset under "keys." + {@code [prefix]}.
 *
 * @author Dave Syer
 */
public class RedisMultiMetricRepository implements MultiMetricRepository {

	private static final String DEFAULT_METRICS_PREFIX = "spring.groups.";

	private final String prefix;

	private final String keys;

	private final BoundZSetOperations<String, String> zSetOperations;

	private final RedisOperations<String, String> redisOperations;

	public RedisMultiMetricRepository(RedisConnectionFactory redisConnectionFactory) {
		this(redisConnectionFactory, DEFAULT_METRICS_PREFIX);
	}

	public RedisMultiMetricRepository(RedisConnectionFactory redisConnectionFactory,
			String prefix) {
		Assert.notNull(redisConnectionFactory, "RedisConnectionFactory must not be null");
		this.redisOperations = RedisUtils.stringTemplate(redisConnectionFactory);
		if (!prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		this.prefix = prefix;
		this.keys = "keys." + this.prefix.substring(0, prefix.length() - 1);
		this.zSetOperations = this.redisOperations.boundZSetOps(this.keys);
	}

	@Override
	public Iterable<Metric<?>> findAll(String group) {

		BoundZSetOperations<String, String> zSetOperations = this.redisOperations
				.boundZSetOps(keyFor(group));

		Set<String> keys = zSetOperations.range(0, -1);
		Iterator<String> keysIt = keys.iterator();

		List<Metric<?>> result = new ArrayList<Metric<?>>(keys.size());
		List<String> values = this.redisOperations.opsForValue().multiGet(keys);
		for (String v : values) {
			String key = keysIt.next();
			result.add(deserialize(group, key, v, zSetOperations.score(key)));
		}
		return result;

	}

	@Override
	public void set(String group, Collection<Metric<?>> values) {
		String groupKey = keyFor(group);
		trackMembership(groupKey);
		BoundZSetOperations<String, String> zSetOperations = this.redisOperations
				.boundZSetOps(groupKey);
		for (Metric<?> metric : values) {
			String raw = serialize(metric);
			String key = keyFor(metric.getName());
			zSetOperations.add(key, metric.getValue().doubleValue());
			this.redisOperations.opsForValue().set(key, raw);
		}
	}

	@Override
	public void increment(String group, Delta<?> delta) {
		String groupKey = keyFor(group);
		trackMembership(groupKey);
		BoundZSetOperations<String, String> zSetOperations = this.redisOperations
				.boundZSetOps(groupKey);
		String key = keyFor(delta.getName());
		double value = zSetOperations.incrementScore(key, delta.getValue().doubleValue());
		String raw = serialize(
				new Metric<Double>(delta.getName(), value, delta.getTimestamp()));
		this.redisOperations.opsForValue().set(key, raw);
	}

	@Override
	public Iterable<String> groups() {
		Set<String> range = this.zSetOperations.range(0, -1);
		Collection<String> result = new ArrayList<String>();
		for (String key : range) {
			result.add(key.substring(this.prefix.length()));
		}
		return result;
	}

	@Override
	public long countGroups() {
		return this.zSetOperations.size();
	}

	@Override
	public void reset(String group) {
		String groupKey = keyFor(group);
		if (this.redisOperations.hasKey(groupKey)) {
			BoundZSetOperations<String, String> zSetOperations = this.redisOperations
					.boundZSetOps(groupKey);
			Set<String> keys = zSetOperations.range(0, -1);
			for (String key : keys) {
				this.redisOperations.delete(key);
			}
			this.redisOperations.delete(groupKey);
		}
		this.zSetOperations.remove(groupKey);
	}

	private Metric<?> deserialize(String group, String redisKey, String v, Double value) {
		Date timestamp = new Date(Long.valueOf(v));
		return new Metric<Double>(nameFor(redisKey), value, timestamp);
	}

	private String serialize(Metric<?> entity) {
		return String.valueOf(entity.getTimestamp().getTime());
	}

	private String keyFor(String name) {
		return this.prefix + name;
	}

	private String nameFor(String redisKey) {
		Assert.state(redisKey != null && redisKey.startsWith(this.prefix),
				"Invalid key does not start with prefix: " + redisKey);
		return redisKey.substring(this.prefix.length());
	}

	private void trackMembership(String redisKey) {
		this.zSetOperations.incrementScore(redisKey, 0.0D);
	}

}
