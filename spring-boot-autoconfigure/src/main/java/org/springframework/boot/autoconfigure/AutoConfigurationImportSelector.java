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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
 * auto-configuration}. This class can also be subclassed if a custom variant of
 * {@link EnableAutoConfiguration @EnableAutoConfiguration}. is needed.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.3.0
 * @see EnableAutoConfiguration
 */
public class AutoConfigurationImportSelector
		implements DeferredImportSelector, BeanClassLoaderAware, ResourceLoaderAware,
		BeanFactoryAware, EnvironmentAware, Ordered {

	private static final String[] NO_IMPORTS = {};

	private static final Log logger = LogFactory
			.getLog(AutoConfigurationImportSelector.class);

	private ConfigurableListableBeanFactory beanFactory;

	private Environment environment;

	private ClassLoader beanClassLoader;

	private ResourceLoader resourceLoader;

	@Override
	public String[] selectImports(AnnotationMetadata annotationMetadata) {
		if (!isEnabled(annotationMetadata)) {
			return NO_IMPORTS;
		}
		try {
			AutoConfigurationMetadata autoConfigurationMetadata = AutoConfigurationMetadataLoader
					.loadMetadata(this.beanClassLoader);
			AnnotationAttributes attributes = getAttributes(annotationMetadata);
			List<String> configurations = getCandidateConfigurations(annotationMetadata,
					attributes);
			configurations = removeDuplicates(configurations);
			configurations = sort(configurations, autoConfigurationMetadata);
			Set<String> exclusions = getExclusions(annotationMetadata, attributes);
			checkExcludedClasses(configurations, exclusions);
			configurations.removeAll(exclusions);
			configurations = filter(configurations, autoConfigurationMetadata);
			fireAutoConfigurationImportEvents(configurations, exclusions);
			return configurations.toArray(new String[configurations.size()]);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected boolean isEnabled(AnnotationMetadata metadata) {
		return true;
	}

	/**
	 * Return the appropriate {@link AnnotationAttributes} from the
	 * {@link AnnotationMetadata}. By default this method will return attributes for
	 * {@link #getAnnotationClass()}.
	 * @param metadata the annotation metadata
	 * @return annotation attributes
	 */
	protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
		String name = getAnnotationClass().getName();
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(name, true));
		Assert.notNull(attributes,
				"No auto-configuration attributes found. Is " + metadata.getClassName()
						+ " annotated with " + ClassUtils.getShortName(name) + "?");
		return attributes;
	}

	/**
	 * Return the source annotation class used by the selector.
	 * @return the annotation class
	 */
	protected Class<?> getAnnotationClass() {
		return EnableAutoConfiguration.class;
	}

	/**
	 * Return the auto-configuration class names that should be considered. By default
	 * this method will load candidates using {@link SpringFactoriesLoader} with
	 * {@link #getSpringFactoriesLoaderFactoryClass()}.
	 * @param metadata the source metadata
	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
	 * attributes}
	 * @return a list of candidate configurations
	 */
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		List<String> configurations = SpringFactoriesLoader.loadFactoryNames(
				getSpringFactoriesLoaderFactoryClass(), getBeanClassLoader());
		Assert.notEmpty(configurations,
				"No auto configuration classes found in META-INF/spring.factories. If you "
						+ "are using a custom packaging, make sure that file is correct.");
		return configurations;
	}

	/**
	 * Return the class used by {@link SpringFactoriesLoader} to load configuration
	 * candidates.
	 * @return the factory class
	 */
	protected Class<?> getSpringFactoriesLoaderFactoryClass() {
		return EnableAutoConfiguration.class;
	}

	private void checkExcludedClasses(List<String> configurations,
			Set<String> exclusions) {
		List<String> invalidExcludes = new ArrayList<String>(exclusions.size());
		for (String exclusion : exclusions) {
			if (ClassUtils.isPresent(exclusion, getClass().getClassLoader())
					&& !configurations.contains(exclusion)) {
				invalidExcludes.add(exclusion);
			}
		}
		if (!invalidExcludes.isEmpty()) {
			handleInvalidExcludes(invalidExcludes);
		}
	}

	/**
	 * Handle any invalid excludes that have been specified.
	 * @param invalidExcludes the list of invalid excludes (will always have at least one
	 * element)
	 */
	protected void handleInvalidExcludes(List<String> invalidExcludes) {
		StringBuilder message = new StringBuilder();
		for (String exclude : invalidExcludes) {
			message.append("\t- ").append(exclude).append(String.format("%n"));
		}
		throw new IllegalStateException(String
				.format("The following classes could not be excluded because they are"
						+ " not auto-configuration classes:%n%s", message));
	}

	/**
	 * Return any exclusions that limit the candidate configurations.
	 * @param metadata the source metadata
	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
	 * attributes}
	 * @return exclusions or an empty set
	 */
	protected Set<String> getExclusions(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		Set<String> excluded = new LinkedHashSet<String>();
		excluded.addAll(asList(attributes, "exclude"));
		excluded.addAll(Arrays.asList(attributes.getStringArray("excludeName")));
		excluded.addAll(getExcludeAutoConfigurationsProperty());
		return excluded;
	}

	private List<String> getExcludeAutoConfigurationsProperty() {
		if (getEnvironment() instanceof ConfigurableEnvironment) {
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
					this.environment, "spring.autoconfigure.");
			Map<String, Object> properties = resolver.getSubProperties("exclude");
			if (properties.isEmpty()) {
				return Collections.emptyList();
			}
			List<String> excludes = new ArrayList<String>();
			for (Map.Entry<String, Object> entry : properties.entrySet()) {
				String name = entry.getKey();
				Object value = entry.getValue();
				if (name.isEmpty() || name.startsWith("[") && value != null) {
					excludes.addAll(new HashSet<String>(Arrays.asList(StringUtils
							.tokenizeToStringArray(String.valueOf(value), ","))));
				}
			}
			return excludes;
		}
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(getEnvironment(),
				"spring.autoconfigure.");
		String[] exclude = resolver.getProperty("exclude", String[].class);
		return (Arrays.asList(exclude == null ? new String[0] : exclude));
	}

	private List<String> sort(List<String> configurations,
			AutoConfigurationMetadata autoConfigurationMetadata) throws IOException {
		configurations = new AutoConfigurationSorter(getMetadataReaderFactory(),
				autoConfigurationMetadata).getInPriorityOrder(configurations);
		return configurations;
	}

	private List<String> filter(List<String> configurations,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		long startTime = System.nanoTime();
		String[] candidates = configurations.toArray(new String[configurations.size()]);
		boolean[] skip = new boolean[candidates.length];
		boolean skipped = false;
		for (AutoConfigurationImportFilter filter : getAutoConfigurationImportFilters()) {
			invokeAwareMethods(filter);
			boolean[] match = filter.match(candidates, autoConfigurationMetadata);
			for (int i = 0; i < match.length; i++) {
				if (!match[i]) {
					skip[i] = true;
					skipped = true;
				}
			}
		}
		if (!skipped) {
			return configurations;
		}
		List<String> result = new ArrayList<String>(candidates.length);
		for (int i = 0; i < candidates.length; i++) {
			if (!skip[i]) {
				result.add(candidates[i]);
			}
		}
		if (logger.isTraceEnabled()) {
			int numberFiltered = configurations.size() - result.size();
			logger.trace("Filtered " + numberFiltered + " auto configuration class in "
					+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
					+ " ms");
		}
		return new ArrayList<String>(result);
	}

	protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportFilter.class,
				this.beanClassLoader);
	}

	private MetadataReaderFactory getMetadataReaderFactory() {
		try {
			return getBeanFactory().getBean(
					SharedMetadataReaderFactoryContextInitializer.BEAN_NAME,
					MetadataReaderFactory.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return new CachingMetadataReaderFactory(this.resourceLoader);
		}
	}

	protected final <T> List<T> removeDuplicates(List<T> list) {
		return new ArrayList<T>(new LinkedHashSet<T>(list));
	}

	protected final List<String> asList(AnnotationAttributes attributes, String name) {
		String[] value = attributes.getStringArray(name);
		return Arrays.asList(value == null ? new String[0] : value);
	}

	private void fireAutoConfigurationImportEvents(List<String> configurations,
			Set<String> exclusions) {
		List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
		if (!listeners.isEmpty()) {
			AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this,
					configurations, exclusions);
			for (AutoConfigurationImportListener listener : listeners) {
				invokeAwareMethods(listener);
				listener.onAutoConfigurationImportEvent(event);
			}
		}
	}

	protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportListener.class,
				this.beanClassLoader);
	}

	private void invokeAwareMethods(Object instance) {
		if (instance instanceof Aware) {
			if (instance instanceof BeanClassLoaderAware) {
				((BeanClassLoaderAware) instance)
						.setBeanClassLoader(this.beanClassLoader);
			}
			if (instance instanceof BeanFactoryAware) {
				((BeanFactoryAware) instance).setBeanFactory(this.beanFactory);
			}
			if (instance instanceof EnvironmentAware) {
				((EnvironmentAware) instance).setEnvironment(this.environment);
			}
			if (instance instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) instance).setResourceLoader(this.resourceLoader);
			}
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	protected final ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	protected final Environment getEnvironment() {
		return this.environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	protected final ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

}
