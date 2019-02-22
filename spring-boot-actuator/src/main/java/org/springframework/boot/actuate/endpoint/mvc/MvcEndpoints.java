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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;

/**
 * A registry for all {@link MvcEndpoint} beans, and a factory for a set of generic ones
 * wrapping existing {@link Endpoint} instances that are not already exposed as MVC
 * endpoints.
 *
 * @author Dave Syer
 */
public class MvcEndpoints implements ApplicationContextAware, InitializingBean {

	private ApplicationContext applicationContext;

	private final Set<MvcEndpoint> endpoints = new HashSet<MvcEndpoint>();

	private Set<Class<?>> customTypes;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Collection<MvcEndpoint> existing = BeanFactoryUtils
				.beansOfTypeIncludingAncestors(this.applicationContext, MvcEndpoint.class)
				.values();
		this.endpoints.addAll(existing);
		this.customTypes = findEndpointClasses(existing);
		@SuppressWarnings("rawtypes")
		Collection<Endpoint> delegates = BeanFactoryUtils
				.beansOfTypeIncludingAncestors(this.applicationContext, Endpoint.class)
				.values();
		for (Endpoint<?> endpoint : delegates) {
			if (isGenericEndpoint(endpoint.getClass()) && endpoint.isEnabled()) {
				EndpointMvcAdapter adapter = new EndpointMvcAdapter(endpoint);
				String path = determinePath(endpoint,
						this.applicationContext.getEnvironment());
				if (path != null) {
					adapter.setPath(path);
				}
				this.endpoints.add(adapter);
			}
		}
	}

	private Set<Class<?>> findEndpointClasses(Collection<MvcEndpoint> existing) {
		Set<Class<?>> types = new HashSet<Class<?>>();
		for (MvcEndpoint endpoint : existing) {
			Class<?> type = endpoint.getEndpointType();
			if (type != null) {
				types.add(type);
			}
		}
		return types;
	}

	public Set<MvcEndpoint> getEndpoints() {
		return this.endpoints;
	}

	/**
	 * Return the endpoints of the specified type.
	 * @param <E> the Class type of the endpoints to be returned
	 * @param type the endpoint type
	 * @return the endpoints
	 */
	@SuppressWarnings("unchecked")
	public <E extends MvcEndpoint> Set<E> getEndpoints(Class<E> type) {
		Set<E> result = new HashSet<E>(this.endpoints.size());
		for (MvcEndpoint candidate : this.endpoints) {
			if (type.isInstance(candidate)) {
				result.add((E) candidate);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	private boolean isGenericEndpoint(Class<?> type) {
		return !this.customTypes.contains(type)
				&& !MvcEndpoint.class.isAssignableFrom(type);
	}

	private String determinePath(Endpoint<?> endpoint, Environment environment) {
		ConfigurationProperties configurationProperties = AnnotationUtils
				.findAnnotation(endpoint.getClass(), ConfigurationProperties.class);
		if (configurationProperties != null) {
			return environment.getProperty(configurationProperties.prefix() + ".path");
		}
		return null;
	}

}
