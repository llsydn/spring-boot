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

package org.springframework.boot.actuate.metrics.export;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Iterables;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMultiMetricRepository;
import org.springframework.boot.actuate.metrics.rich.InMemoryRichGaugeRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RichGaugeExporter}.
 *
 * @author Dave Syer
 */
public class RichGaugeExporterTests {

	private final InMemoryRichGaugeRepository reader = new InMemoryRichGaugeRepository();

	private final InMemoryMultiMetricRepository writer = new InMemoryMultiMetricRepository();

	private final RichGaugeExporter exporter = new RichGaugeExporter(this.reader,
			this.writer);

	@Test
	public void prefixedMetricsCopied() {
		this.reader.set(new Metric<Number>("foo", 2.3));
		this.exporter.export();
		assertThat(Iterables.collection(this.writer.groups())).hasSize(1);
	}

}
