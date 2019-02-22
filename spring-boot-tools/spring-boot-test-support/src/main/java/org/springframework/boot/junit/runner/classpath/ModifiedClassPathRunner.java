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

package org.springframework.boot.junit.runner.classpath;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * A custom {@link BlockJUnit4ClassRunner} that runs tests using a modified class path.
 * Entries are excluded from the class path using {@link ClassPathExclusions} and
 * overridden using {@link ClassPathOverrides} on the test class. A class loader is
 * created with the customized class path and is used both to load the test class and as
 * the thread context class loader while the test is being run.
 *
 * @author Andy Wilkinson
 */
public class ModifiedClassPathRunner extends BlockJUnit4ClassRunner {

	public ModifiedClassPathRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	protected TestClass createTestClass(Class<?> testClass) {
		try {
			ClassLoader classLoader = createTestClassLoader(testClass);
			return new ModifiedClassPathTestClass(classLoader, testClass.getName());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	protected Object createTest() throws Exception {
		ModifiedClassPathTestClass testClass = (ModifiedClassPathTestClass) getTestClass();
		return testClass.doWithModifiedClassPathThreadContextClassLoader(
				new ModifiedClassPathTestClass.ModifiedClassPathTcclAction<Object, Exception>() {

					@Override
					public Object perform() throws Exception {
						return ModifiedClassPathRunner.super.createTest();
					}
				});
	}

	private URLClassLoader createTestClassLoader(Class<?> testClass) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
		return new ModifiedClassPathClassLoader(
				processUrls(extractUrls(classLoader), testClass), classLoader.getParent(),
				classLoader);
	}

	private URL[] extractUrls(URLClassLoader classLoader) throws Exception {
		List<URL> extractedUrls = new ArrayList<URL>();
		for (URL url : classLoader.getURLs()) {
			if (isSurefireBooterJar(url)) {
				extractedUrls.addAll(extractUrlsFromManifestClassPath(url));
			}
			else {
				extractedUrls.add(url);
			}
		}
		return extractedUrls.toArray(new URL[extractedUrls.size()]);
	}

	private boolean isSurefireBooterJar(URL url) {
		return url.getPath().contains("surefirebooter");
	}

	private List<URL> extractUrlsFromManifestClassPath(URL booterJar) throws Exception {
		List<URL> urls = new ArrayList<URL>();
		for (String entry : getClassPath(booterJar)) {
			urls.add(new URL(entry));
		}
		return urls;
	}

	private String[] getClassPath(URL booterJar) throws Exception {
		JarFile jarFile = new JarFile(new File(booterJar.toURI()));
		try {
			return StringUtils.delimitedListToStringArray(jarFile.getManifest()
					.getMainAttributes().getValue(Attributes.Name.CLASS_PATH), " ");
		}
		finally {
			jarFile.close();
		}
	}

	private URL[] processUrls(URL[] urls, Class<?> testClass) throws Exception {
		ClassPathEntryFilter filter = new ClassPathEntryFilter(testClass);
		List<URL> processedUrls = new ArrayList<URL>();
		processedUrls.addAll(getAdditionalUrls(testClass));
		for (URL url : urls) {
			if (!filter.isExcluded(url)) {
				processedUrls.add(url);
			}
		}
		return processedUrls.toArray(new URL[processedUrls.size()]);
	}

	private List<URL> getAdditionalUrls(Class<?> testClass) throws Exception {
		ClassPathOverrides overrides = AnnotationUtils.findAnnotation(testClass,
				ClassPathOverrides.class);
		if (overrides == null) {
			return Collections.emptyList();
		}
		return resolveCoordinates(overrides.value());
	}

	private List<URL> resolveCoordinates(String[] coordinates) throws Exception {
		DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils
				.newServiceLocator();
		serviceLocator.addService(RepositoryConnectorFactory.class,
				BasicRepositoryConnectorFactory.class);
		serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		RepositorySystem repositorySystem = serviceLocator
				.getService(RepositorySystem.class);
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		LocalRepository localRepository = new LocalRepository(
				System.getProperty("user.home") + "/.m2/repository");
		session.setLocalRepositoryManager(
				repositorySystem.newLocalRepositoryManager(session, localRepository));
		CollectRequest collectRequest = new CollectRequest(null,
				Arrays.asList(new RemoteRepository.Builder("central", "default",
						"http://central.maven.org/maven2").build()));

		collectRequest.setDependencies(createDependencies(coordinates));
		DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
		DependencyResult result = repositorySystem.resolveDependencies(session,
				dependencyRequest);
		List<URL> resolvedArtifacts = new ArrayList<URL>();
		for (ArtifactResult artifact : result.getArtifactResults()) {
			resolvedArtifacts.add(artifact.getArtifact().getFile().toURI().toURL());
		}
		return resolvedArtifacts;
	}

	private List<Dependency> createDependencies(String[] allCoordinates) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for (String coordinate : allCoordinates) {
			dependencies.add(new Dependency(new DefaultArtifact(coordinate), null));
		}
		return dependencies;
	}

	/**
	 * Filter for class path entries.
	 */
	private static final class ClassPathEntryFilter {

		private final List<String> exclusions;

		private final AntPathMatcher matcher = new AntPathMatcher();

		private ClassPathEntryFilter(Class<?> testClass) throws Exception {
			ClassPathExclusions exclusions = AnnotationUtils.findAnnotation(testClass,
					ClassPathExclusions.class);
			this.exclusions = exclusions == null ? Collections.<String>emptyList()
					: Arrays.asList(exclusions.value());
		}

		private boolean isExcluded(URL url) throws Exception {
			if (!"file".equals(url.getProtocol())) {
				return false;
			}
			String name = new File(url.toURI()).getName();
			for (String exclusion : this.exclusions) {
				if (this.matcher.match(exclusion, name)) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * Custom {@link TestClass} that uses a modified class path.
	 */
	private static final class ModifiedClassPathTestClass extends TestClass {

		private final ClassLoader classLoader;

		ModifiedClassPathTestClass(ClassLoader classLoader, String testClassName)
				throws ClassNotFoundException {
			super(classLoader.loadClass(testClassName));
			this.classLoader = classLoader;
		}

		@Override
		public List<FrameworkMethod> getAnnotatedMethods(
				Class<? extends Annotation> annotationClass) {
			try {
				return getAnnotatedMethods(annotationClass.getName());
			}
			catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}

		@SuppressWarnings("unchecked")
		private List<FrameworkMethod> getAnnotatedMethods(String annotationClassName)
				throws ClassNotFoundException {
			Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) this.classLoader
					.loadClass(annotationClassName);
			List<FrameworkMethod> methods = super.getAnnotatedMethods(annotationClass);
			return wrapFrameworkMethods(methods);
		}

		private List<FrameworkMethod> wrapFrameworkMethods(
				List<FrameworkMethod> methods) {
			List<FrameworkMethod> wrapped = new ArrayList<FrameworkMethod>(
					methods.size());
			for (FrameworkMethod frameworkMethod : methods) {
				wrapped.add(new ModifiedClassPathFrameworkMethod(
						frameworkMethod.getMethod()));
			}
			return wrapped;
		}

		private <T, E extends Throwable> T doWithModifiedClassPathThreadContextClassLoader(
				ModifiedClassPathTcclAction<T, E> action) throws E {
			ClassLoader originalClassLoader = Thread.currentThread()
					.getContextClassLoader();
			Thread.currentThread().setContextClassLoader(this.classLoader);
			try {
				return action.perform();
			}
			finally {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}

		/**
		 * An action to be performed with the {@link ModifiedClassPathClassLoader} set as
		 * the thread context class loader.
		 */
		private interface ModifiedClassPathTcclAction<T, E extends Throwable> {

			T perform() throws E;

		}

		/**
		 * Custom {@link FrameworkMethod} that runs methods with
		 * {@link ModifiedClassPathClassLoader} as the thread context class loader.
		 */
		private final class ModifiedClassPathFrameworkMethod extends FrameworkMethod {

			private ModifiedClassPathFrameworkMethod(Method method) {
				super(method);
			}

			@Override
			public Object invokeExplosively(final Object target, final Object... params)
					throws Throwable {
				return doWithModifiedClassPathThreadContextClassLoader(
						new ModifiedClassPathTcclAction<Object, Throwable>() {

							@Override
							public Object perform() throws Throwable {
								return ModifiedClassPathFrameworkMethod.super.invokeExplosively(
										target, params);
							}

						});
			}

		}

	}

	/**
	 * Custom {@link URLClassLoader} that modifies the class path.
	 */
	private static final class ModifiedClassPathClassLoader extends URLClassLoader {

		private final ClassLoader junitLoader;

		ModifiedClassPathClassLoader(URL[] urls, ClassLoader parent,
				ClassLoader junitLoader) {
			super(urls, parent);
			this.junitLoader = junitLoader;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.startsWith("org.junit") || name.startsWith("org.hamcrest")) {
				return this.junitLoader.loadClass(name);
			}
			return super.loadClass(name);
		}

	}

}
