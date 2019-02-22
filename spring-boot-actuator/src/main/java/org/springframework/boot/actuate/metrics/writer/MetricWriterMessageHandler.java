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

package org.springframework.boot.actuate.metrics.writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

/**
 * A {@link MessageHandler} that updates {@link Metric} values through a
 * {@link MetricWriter}.
 *
 * @author Dave Syer
 * @see MessageChannelMetricWriter
 */
public final class MetricWriterMessageHandler implements MessageHandler {

	private static final Log logger = LogFactory.getLog(MetricWriterMessageHandler.class);

	private final MetricWriter observer;

	public MetricWriterMessageHandler(MetricWriter observer) {
		this.observer = observer;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		handleMessage(new MetricMessage(message));
	}

	private void handleMessage(MetricMessage message) {
		Object payload = message.getPayload();
		if (message.isReset()) {
			this.observer.reset(message.getMetricName());
		}
		else if (payload instanceof Delta) {
			Delta<?> value = (Delta<?>) payload;
			this.observer.increment(value);
		}
		else if (payload instanceof Metric) {
			Metric<?> value = (Metric<?>) payload;
			this.observer.set(value);
		}
		else {
			if (logger.isWarnEnabled()) {
				logger.warn("Unsupported metric payload "
						+ (payload == null ? "null" : payload.getClass().getName()));
			}
		}
	}

}
