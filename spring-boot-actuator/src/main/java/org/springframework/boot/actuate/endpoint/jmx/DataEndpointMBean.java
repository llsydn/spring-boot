/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * Simple wrapper around {@link Endpoint} implementations that provide actuator data of
 * some sort.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 */
public class DataEndpointMBean extends EndpointMBean {

	/**
	 * Create a new {@link DataEndpointMBean} instance.
	 * @param beanName the bean name
	 * @param endpoint the endpoint to wrap
	 * @param objectMapper the {@link ObjectMapper} used to convert the payload
	 */
	public DataEndpointMBean(String beanName, Endpoint<?> endpoint,
			ObjectMapper objectMapper) {
		super(beanName, endpoint, objectMapper);
	}

	@ManagedAttribute(description = "Invoke the underlying endpoint")
	public Object getData() {
		return convert(getEndpoint().invoke());
	}

}
