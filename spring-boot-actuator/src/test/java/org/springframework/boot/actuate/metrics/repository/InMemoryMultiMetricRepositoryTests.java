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

package org.springframework.boot.actuate.metrics.repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InMemoryMultiMetricRepository}.
 *
 * @author Dave Syer
 */
public class InMemoryMultiMetricRepositoryTests {

	private final InMemoryMultiMetricRepository repository = new InMemoryMultiMetricRepository();

	@Test
	public void registeredPrefixCounted() {
		this.repository.increment("foo", new Delta<Number>("bar", 1));
		this.repository.increment("foo", new Delta<Number>("bar", 1));
		this.repository.increment("foo", new Delta<Number>("spam", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo")) {
			names.add(metric.getName());
		}
		assertThat(names).hasSize(2);
		assertThat(names.contains("foo.bar")).isTrue();
	}

	@Test
	public void prefixWithWildcard() {
		this.repository.increment("foo", new Delta<Number>("bar", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo.*")) {
			names.add(metric.getName());
		}
		assertThat(names).hasSize(1);
		assertThat(names.contains("foo.bar")).isTrue();
	}

	@Test
	public void prefixWithPeriod() {
		this.repository.increment("foo", new Delta<Number>("bar", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo.")) {
			names.add(metric.getName());
		}
		assertThat(names).hasSize(1);
		assertThat(names.contains("foo.bar")).isTrue();
	}

	@Test
	public void onlyRegisteredPrefixCounted() {
		this.repository.increment("foo", new Delta<Number>("bar", 1));
		this.repository.increment("foobar", new Delta<Number>("spam", 1));
		Set<String> names = new HashSet<String>();
		for (Metric<?> metric : this.repository.findAll("foo")) {
			names.add(metric.getName());
		}
		assertThat(names).hasSize(1);
		assertThat(names.contains("foo.bar")).isTrue();
	}

	@Test
	public void incrementGroup() {
		this.repository.increment("foo", new Delta<Number>("foo.bar", 1));
		this.repository.increment("foo", new Delta<Number>("foo.bar", 2));
		this.repository.increment("foo", new Delta<Number>("foo.spam", 1));
		Map<String, Metric<?>> metrics = new HashMap<String, Metric<?>>();
		for (Metric<?> metric : this.repository.findAll("foo")) {
			metrics.put(metric.getName(), metric);
		}
		assertThat(metrics).hasSize(2);
		assertThat(metrics).containsKeys("foo.bar", "foo.spam");
		assertThat(metrics.get("foo.bar").getValue()).isEqualTo(3L);
	}

}
