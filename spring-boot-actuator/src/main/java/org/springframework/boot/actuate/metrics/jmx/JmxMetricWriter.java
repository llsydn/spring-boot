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

package org.springframework.boot.actuate.metrics.jmx;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;

/**
 * A {@link MetricWriter} for MBeans. Each metric is registered as an individual MBean, so
 * (for instance) it can be graphed and monitored. The object names are provided by an
 * {@link ObjectNamingStrategy}, where the default is a
 * {@link DefaultMetricNamingStrategy} which provides {@code type}, {@code name} and
 * {@code value} keys by splitting up the metric name on periods.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@ManagedResource(description = "MetricWriter for pushing metrics to JMX MBeans.")
public class JmxMetricWriter implements MetricWriter {

	private static final Log logger = LogFactory.getLog(JmxMetricWriter.class);

	private final ConcurrentMap<String, MetricValue> values = new ConcurrentHashMap<String, MetricValue>();

	private final MBeanExporter exporter;

	private ObjectNamingStrategy namingStrategy = new DefaultMetricNamingStrategy();

	private String domain = "org.springframework.metrics";

	public JmxMetricWriter(MBeanExporter exporter) {
		this.exporter = exporter;
	}

	public void setNamingStrategy(ObjectNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	@ManagedOperation
	public void increment(String name, long value) {
		increment(new Delta<Long>(name, value));
	}

	@Override
	public void increment(Delta<?> delta) {
		MetricValue counter = getValue(delta.getName());
		counter.increment(delta.getValue().longValue());
	}

	@ManagedOperation
	public void set(String name, double value) {
		set(new Metric<Double>(name, value));
	}

	@Override
	public void set(Metric<?> value) {
		MetricValue metric = getValue(value.getName());
		metric.setValue(value.getValue().doubleValue());
	}

	@Override
	@ManagedOperation
	public void reset(String name) {
		MetricValue value = this.values.remove(name);
		if (value != null) {
			try {
				// We can unregister the MBean, but if this writer is on the end of an
				// Exporter the chances are it will be re-registered almost immediately.
				this.exporter.unregisterManagedResource(getName(name, value));
			}
			catch (MalformedObjectNameException ex) {
				logger.warn("Could not unregister MBean for " + name);
			}
		}
	}

	private MetricValue getValue(String name) {
		MetricValue value = this.values.get(name);
		if (value == null) {
			value = new MetricValue();
			MetricValue oldValue = this.values.putIfAbsent(name, value);
			if (oldValue != null) {
				value = oldValue;
			}
			try {
				this.exporter.registerManagedResource(value, getName(name, value));
			}
			catch (Exception ex) {
				// Could not register mbean, maybe just a race condition
			}
		}
		return value;
	}

	private ObjectName getName(String name, MetricValue value)
			throws MalformedObjectNameException {
		String key = String.format(this.domain + ":type=MetricValue,name=%s", name);
		return this.namingStrategy.getObjectName(value, key);
	}

	/**
	 * A single metric value.
	 */
	@ManagedResource
	public static class MetricValue {

		private double value;

		private long lastUpdated = 0;

		public void setValue(double value) {
			if (this.value != value) {
				this.lastUpdated = System.currentTimeMillis();
			}
			this.value = value;
		}

		public void increment(long value) {
			this.lastUpdated = System.currentTimeMillis();
			this.value += value;
		}

		@ManagedAttribute
		public double getValue() {
			return this.value;
		}

		@ManagedAttribute
		public Date getLastUpdated() {
			return new Date(this.lastUpdated);
		}

	}

}
