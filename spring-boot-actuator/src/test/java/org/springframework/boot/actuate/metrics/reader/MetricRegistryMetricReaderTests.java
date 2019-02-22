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

package org.springframework.boot.actuate.metrics.reader;

import java.util.HashSet;
import java.util.Set;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricRegistryMetricReader}.
 *
 * @author Andy Wilkinson
 */
public class MetricRegistryMetricReaderTests {

	private final MetricRegistry metricRegistry = new MetricRegistry();

	private final MetricRegistryMetricReader metricReader = new MetricRegistryMetricReader(
			this.metricRegistry);

	@Test
	public void nonNumberGaugesAreTolerated() {
		this.metricRegistry.register("test", new Gauge<Set<String>>() {

			@Override
			public Set<String> getValue() {
				return new HashSet<String>();
			}

		});
		assertThat(this.metricReader.findOne("test")).isNull();
		this.metricRegistry.remove("test");
		assertThat(this.metricReader.findOne("test")).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void numberGauge() {
		this.metricRegistry.register("test", new Gauge<Number>() {

			@Override
			public Number getValue() {
				return Integer.valueOf(5);
			}

		});
		Metric<Integer> metric = (Metric<Integer>) this.metricReader.findOne("test");
		assertThat(metric.getValue()).isEqualTo(Integer.valueOf(5));
		this.metricRegistry.remove("test");
		assertThat(this.metricReader.findOne("test")).isNull();
	}

}
