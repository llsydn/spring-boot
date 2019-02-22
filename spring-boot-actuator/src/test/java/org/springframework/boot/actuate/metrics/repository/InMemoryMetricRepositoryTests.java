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

import java.util.Date;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Tests for {@link InMemoryMetricRepository}.
 *
 * @author Dave Syer
 */
public class InMemoryMetricRepositoryTests {

	private final InMemoryMetricRepository repository = new InMemoryMetricRepository();

	@Test
	public void increment() {
		this.repository.increment(new Delta<Integer>("foo", 1, new Date()));
		assertThat(this.repository.findOne("foo").getValue().doubleValue()).isEqualTo(1.0,
				offset(0.01));
	}

	@Test
	public void set() {
		this.repository.set(new Metric<Double>("foo", 2.5, new Date()));
		assertThat(this.repository.findOne("foo").getValue().doubleValue()).isEqualTo(2.5,
				offset(0.01));
	}

}
