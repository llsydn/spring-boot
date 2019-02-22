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

package org.springframework.boot.web.servlet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Abstract base {@link ServletContextInitializer} to register {@link Filter}s in a
 * Servlet 3.0+ container.
 *
 * @author Phillip Webb
 */
abstract class AbstractFilterRegistrationBean extends RegistrationBean {

	/**
	 * Filters that wrap the servlet request should be ordered less than or equal to this.
	 */
	protected static final int REQUEST_WRAPPER_FILTER_MAX_ORDER = 0;

	private final Log logger = LogFactory.getLog(getClass());

	private static final EnumSet<DispatcherType> ASYNC_DISPATCHER_TYPES = EnumSet.of(
			DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST,
			DispatcherType.ASYNC);

	private static final EnumSet<DispatcherType> NON_ASYNC_DISPATCHER_TYPES = EnumSet
			.of(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST);

	private static final String[] DEFAULT_URL_MAPPINGS = { "/*" };

	private Set<ServletRegistrationBean> servletRegistrationBeans = new LinkedHashSet<ServletRegistrationBean>();

	private Set<String> servletNames = new LinkedHashSet<String>();

	private Set<String> urlPatterns = new LinkedHashSet<String>();

	private EnumSet<DispatcherType> dispatcherTypes;

	private boolean matchAfter = false;

	/**
	 * Create a new instance to be registered with the specified
	 * {@link ServletRegistrationBean}s.
	 * @param servletRegistrationBeans associate {@link ServletRegistrationBean}s
	 */
	AbstractFilterRegistrationBean(ServletRegistrationBean... servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
	}

	/**
	 * Set {@link ServletRegistrationBean}s that the filter will be registered against.
	 * @param servletRegistrationBeans the Servlet registration beans
	 */
	public void setServletRegistrationBeans(
			Collection<? extends ServletRegistrationBean> servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		this.servletRegistrationBeans = new LinkedHashSet<ServletRegistrationBean>(
				servletRegistrationBeans);
	}

	/**
	 * Return a mutable collection of the {@link ServletRegistrationBean} that the filter
	 * will be registered against. {@link ServletRegistrationBean}s.
	 * @return the Servlet registration beans
	 * @see #setServletNames
	 * @see #setUrlPatterns
	 */
	public Collection<ServletRegistrationBean> getServletRegistrationBeans() {
		return this.servletRegistrationBeans;
	}

	/**
	 * Add {@link ServletRegistrationBean}s for the filter.
	 * @param servletRegistrationBeans the servlet registration beans to add
	 * @see #setServletRegistrationBeans
	 */
	public void addServletRegistrationBeans(
			ServletRegistrationBean... servletRegistrationBeans) {
		Assert.notNull(servletRegistrationBeans,
				"ServletRegistrationBeans must not be null");
		Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
	}

	/**
	 * Set servlet names that the filter will be registered against. This will replace any
	 * previously specified servlet names.
	 * @param servletNames the servlet names
	 * @see #setServletRegistrationBeans
	 * @see #setUrlPatterns
	 */
	public void setServletNames(Collection<String> servletNames) {
		Assert.notNull(servletNames, "ServletNames must not be null");
		this.servletNames = new LinkedHashSet<String>(servletNames);
	}

	/**
	 * Return a mutable collection of servlet names that the filter will be registered
	 * against.
	 * @return the servlet names
	 */
	public Collection<String> getServletNames() {
		return this.servletNames;
	}

	/**
	 * Add servlet names for the filter.
	 * @param servletNames the servlet names to add
	 */
	public void addServletNames(String... servletNames) {
		Assert.notNull(servletNames, "ServletNames must not be null");
		this.servletNames.addAll(Arrays.asList(servletNames));
	}

	/**
	 * Set the URL patterns that the filter will be registered against. This will replace
	 * any previously specified URL patterns.
	 * @param urlPatterns the URL patterns
	 * @see #setServletRegistrationBeans
	 * @see #setServletNames
	 */
	public void setUrlPatterns(Collection<String> urlPatterns) {
		Assert.notNull(urlPatterns, "UrlPatterns must not be null");
		this.urlPatterns = new LinkedHashSet<String>(urlPatterns);
	}

	/**
	 * Return a mutable collection of URL patterns that the filter will be registered
	 * against.
	 * @return the URL patterns
	 */
	public Collection<String> getUrlPatterns() {
		return this.urlPatterns;
	}

	/**
	 * Add URL patterns that the filter will be registered against.
	 * @param urlPatterns the URL patterns
	 */
	public void addUrlPatterns(String... urlPatterns) {
		Assert.notNull(urlPatterns, "UrlPatterns must not be null");
		Collections.addAll(this.urlPatterns, urlPatterns);
	}

	/**
	 * Convenience method to {@link #setDispatcherTypes(EnumSet) set dispatcher types}
	 * using the specified elements.
	 * @param first the first dispatcher type
	 * @param rest additional dispatcher types
	 */
	public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
		this.dispatcherTypes = EnumSet.of(first, rest);
	}

	/**
	 * Sets the dispatcher types that should be used with the registration. If not
	 * specified the types will be deduced based on the value of
	 * {@link #isAsyncSupported()}.
	 * @param dispatcherTypes the dispatcher types
	 */
	public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
		this.dispatcherTypes = dispatcherTypes;
	}

	/**
	 * Set if the filter mappings should be matched after any declared filter mappings of
	 * the ServletContext. Defaults to {@code false} indicating the filters are supposed
	 * to be matched before any declared filter mappings of the ServletContext.
	 * @param matchAfter if filter mappings are matched after
	 */
	public void setMatchAfter(boolean matchAfter) {
		this.matchAfter = matchAfter;
	}

	/**
	 * Return if filter mappings should be matched after any declared Filter mappings of
	 * the ServletContext.
	 * @return if filter mappings are matched after
	 */
	public boolean isMatchAfter() {
		return this.matchAfter;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		Filter filter = getFilter();
		Assert.notNull(filter, "Filter must not be null");
		String name = getOrDeduceName(filter);
		if (!isEnabled()) {
			this.logger.info("Filter " + name + " was not registered (disabled)");
			return;
		}
		FilterRegistration.Dynamic added = servletContext.addFilter(name, filter);
		if (added == null) {
			this.logger.info("Filter " + name + " was not registered "
					+ "(possibly already registered?)");
			return;
		}
		configure(added);
	}

	/**
	 * Return the {@link Filter} to be registered.
	 * @return the filter
	 */
	public abstract Filter getFilter();

	/**
	 * Configure registration settings. Subclasses can override this method to perform
	 * additional configuration if required.
	 * @param registration the registration
	 */
	protected void configure(FilterRegistration.Dynamic registration) {
		super.configure(registration);
		EnumSet<DispatcherType> dispatcherTypes = this.dispatcherTypes;
		if (dispatcherTypes == null) {
			dispatcherTypes = (isAsyncSupported() ? ASYNC_DISPATCHER_TYPES
					: NON_ASYNC_DISPATCHER_TYPES);
		}
		Set<String> servletNames = new LinkedHashSet<String>();
		for (ServletRegistrationBean servletRegistrationBean : this.servletRegistrationBeans) {
			servletNames.add(servletRegistrationBean.getServletName());
		}
		servletNames.addAll(this.servletNames);
		if (servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
			this.logger.info("Mapping filter: '" + registration.getName() + "' to: "
					+ Arrays.asList(DEFAULT_URL_MAPPINGS));
			registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
					DEFAULT_URL_MAPPINGS);
		}
		else {
			if (!servletNames.isEmpty()) {
				this.logger.info("Mapping filter: '" + registration.getName()
						+ "' to servlets: " + servletNames);
				registration.addMappingForServletNames(dispatcherTypes, this.matchAfter,
						servletNames.toArray(new String[servletNames.size()]));
			}
			if (!this.urlPatterns.isEmpty()) {
				this.logger.info("Mapping filter: '" + registration.getName()
						+ "' to urls: " + this.urlPatterns);
				registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter,
						this.urlPatterns.toArray(new String[this.urlPatterns.size()]));
			}
		}
	}

}
