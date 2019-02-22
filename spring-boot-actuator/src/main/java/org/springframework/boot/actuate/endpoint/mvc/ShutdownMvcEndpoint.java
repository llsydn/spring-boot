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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Adapter to expose {@link ShutdownEndpoint} as an {@link MvcEndpoint}.
 *
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "endpoints.shutdown")
public class ShutdownMvcEndpoint extends EndpointMvcAdapter {

	public ShutdownMvcEndpoint(ShutdownEndpoint delegate) {
		super(delegate);
	}

	@PostMapping(produces = { ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON_VALUE,
			MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	@Override
	public Object invoke() {
		if (!getDelegate().isEnabled()) {
			return new ResponseEntity<Map<String, String>>(
					Collections.singletonMap("message", "This endpoint is disabled"),
					HttpStatus.NOT_FOUND);
		}
		return super.invoke();
	}

}
