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

package org.springframework.boot.context.embedded.tomcat;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.naming.directory.DirContext;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.core.StandardContext;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Abstraction to add resources that works with both Tomcat 8 and 7.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
abstract class TomcatResources {

	private final Context context;

	TomcatResources(Context context) {
		this.context = context;
	}

	void addResourceJars(List<URL> resourceJarUrls) {
		for (URL url : resourceJarUrls) {
			String file = url.getFile();
			if (file.endsWith(".jar") || file.endsWith(".jar!/")) {
				String jar = url.toString();
				if (!jar.startsWith("jar:")) {
					// A jar file in the file system. Convert to Jar URL.
					jar = "jar:" + jar + "!/";
				}
				addJar(jar);
			}
			else {
				addDir(file, url);
			}
		}
	}

	protected final Context getContext() {
		return this.context;
	}

	/**
	 * Called to add a JAR to the resources.
	 * @param jar the URL spec for the jar
	 */
	protected abstract void addJar(String jar);

	/**
	 * Called to add a dir to the resource.
	 * @param dir the dir
	 * @param url the URL
	 */
	protected abstract void addDir(String dir, URL url);

	/**
	 * Return a {@link TomcatResources} instance for the currently running Tomcat version.
	 * @param context the tomcat context
	 * @return a {@link TomcatResources} instance.
	 */
	public static TomcatResources get(Context context) {
		if (ClassUtils.isPresent("org.apache.catalina.deploy.ErrorPage", null)) {
			return new Tomcat7Resources(context);
		}
		return new Tomcat8Resources(context);
	}

	/**
	 * {@link TomcatResources} for Tomcat 7.
	 */
	private static class Tomcat7Resources extends TomcatResources {

		private final Method addResourceJarUrlMethod;

		Tomcat7Resources(Context context) {
			super(context);
			this.addResourceJarUrlMethod = ReflectionUtils.findMethod(context.getClass(),
					"addResourceJarUrl", URL.class);
		}

		@Override
		protected void addJar(String jar) {
			URL url = getJarUrl(jar);
			if (url != null) {
				try {
					this.addResourceJarUrlMethod.invoke(getContext(), url);
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}
		}

		private URL getJarUrl(String jar) {
			try {
				return new URL(jar);
			}
			catch (MalformedURLException ex) {
				// Ignore
				return null;
			}
		}

		@Override
		protected void addDir(String dir, URL url) {
			if (getContext() instanceof StandardContext) {
				try {
					Class<?> fileDirContextClass = Class
							.forName("org.apache.naming.resources.FileDirContext");
					Method setDocBaseMethod = ReflectionUtils
							.findMethod(fileDirContextClass, "setDocBase", String.class);
					Object fileDirContext = fileDirContextClass.newInstance();
					setDocBaseMethod.invoke(fileDirContext, dir);
					Method addResourcesDirContextMethod = ReflectionUtils.findMethod(
							StandardContext.class, "addResourcesDirContext",
							DirContext.class);
					addResourcesDirContextMethod.invoke(getContext(), fileDirContext);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Tomcat 7 reflection failed", ex);
				}
			}
		}

	}

	/**
	 * {@link TomcatResources} for Tomcat 8.
	 */
	static class Tomcat8Resources extends TomcatResources {

		Tomcat8Resources(Context context) {
			super(context);
		}

		@Override
		protected void addJar(String jar) {
			addResourceSet(jar);
		}

		@Override
		protected void addDir(String dir, URL url) {
			addResourceSet(url.toString());
		}

		private void addResourceSet(String resource) {
			try {
				if (isInsideNestedJar(resource)) {
					// It's a nested jar but we now don't want the suffix because Tomcat
					// is going to try and locate it as a root URL (not the resource
					// inside it)
					resource = resource.substring(0, resource.length() - 2);
				}
				URL url = new URL(resource);
				String path = "/META-INF/resources";
				getContext().getResources().createWebResourceSet(
						ResourceSetType.RESOURCE_JAR, "/", url, path);
			}
			catch (Exception ex) {
				// Ignore (probably not a directory)
			}
		}

		private boolean isInsideNestedJar(String dir) {
			return dir.indexOf("!/") < dir.lastIndexOf("!/");
		}

	}

}
