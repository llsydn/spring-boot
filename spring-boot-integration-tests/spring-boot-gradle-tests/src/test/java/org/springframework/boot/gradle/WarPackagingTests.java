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

package org.springframework.boot.gradle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for war packaging with Gradle to ensure that only the Servlet container and its
 * dependencies are packaged in WEB-INF/lib-provided
 *
 * @author Andy Wilkinson
 */
public class WarPackagingTests {

	private static final String WEB_INF_LIB_PROVIDED_PREFIX = "WEB-INF/lib-provided/";

	private static final String WEB_INF_LIB_PREFIX = "WEB-INF/lib/";

	private static final Set<String> TOMCAT_EXPECTED_IN_WEB_INF_LIB_PROVIDED = new HashSet<String>(
			Arrays.asList("spring-boot-starter-tomcat-", "tomcat-annotations",
					"tomcat-embed-core-", "tomcat-embed-el-", "tomcat-embed-websocket-"));

	private static final Set<String> JETTY_EXPECTED_IN_WEB_INF_LIB_PROVIDED = new HashSet<String>(
			Arrays.asList("spring-boot-starter-jetty-", "jetty-continuation",
					"jetty-util-", "javax.servlet-", "jetty-client", "jetty-io-",
					"jetty-http-", "jetty-server-", "jetty-security-", "jetty-servlet-",
					"jetty-servlets", "jetty-webapp-", "websocket-api",
					"javax.annotation-api", "jetty-plus", "javax-websocket-server-impl-",
					"apache-el", "asm-", "javax.websocket-api-", "asm-tree-",
					"asm-commons-", "websocket-common-", "jetty-annotations-",
					"javax-websocket-client-impl-", "websocket-client-",
					"websocket-server-", "jetty-xml-", "websocket-servlet-"));

	private static final String BOOT_VERSION = Versions.getBootVersion();

	private static ProjectConnection project;

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("war-packaging");
	}

	@Test
	public void onlyTomcatIsPackagedInWebInfLibProvided() throws IOException {
		checkWebInfEntriesForServletContainer("tomcat",
				TOMCAT_EXPECTED_IN_WEB_INF_LIB_PROVIDED);
	}

	@Test
	public void onlyJettyIsPackagedInWebInfLibProvided() throws IOException {
		checkWebInfEntriesForServletContainer("jetty",
				JETTY_EXPECTED_IN_WEB_INF_LIB_PROVIDED);
	}

	private void checkWebInfEntriesForServletContainer(String servletContainer,
			Set<String> expectedLibProvidedEntries) throws IOException {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION,
						"-PservletContainer=" + servletContainer)
				.run();

		JarFile war = new JarFile("target/war-packaging/build/libs/war-packaging.war");

		checkWebInfLibProvidedEntries(war, expectedLibProvidedEntries);

		checkWebInfLibEntries(war, expectedLibProvidedEntries);

		try {
			war.close();
		}
		catch (IOException ex) {
			// Ignore
		}
	}

	private void checkWebInfLibProvidedEntries(JarFile war, Set<String> expectedEntries)
			throws IOException {
		Set<String> entries = getWebInfLibProvidedEntries(war);
		assertThat(entries).hasSameSizeAs(expectedEntries);
		List<String> unexpectedLibProvidedEntries = new ArrayList<String>();
		for (String entry : entries) {
			if (!isExpectedInWebInfLibProvided(entry, expectedEntries)) {
				unexpectedLibProvidedEntries.add(entry);
			}
		}
		assertThat(unexpectedLibProvidedEntries.isEmpty());
	}

	private void checkWebInfLibEntries(JarFile war, Set<String> entriesOnlyInLibProvided)
			throws IOException {
		Set<String> entries = getWebInfLibEntries(war);
		List<String> unexpectedLibEntries = new ArrayList<String>();
		for (String entry : entries) {
			if (!isExpectedInWebInfLib(entry, entriesOnlyInLibProvided)) {
				unexpectedLibEntries.add(entry);
			}
		}
		assertThat(unexpectedLibEntries.isEmpty());
	}

	private Set<String> getWebInfLibProvidedEntries(JarFile war) throws IOException {
		Set<String> webInfLibProvidedEntries = new HashSet<String>();
		Enumeration<JarEntry> entries = war.entries();
		while (entries.hasMoreElements()) {
			String name = entries.nextElement().getName();
			if (isWebInfLibProvidedEntry(name)) {
				webInfLibProvidedEntries.add(name);
			}
		}
		return webInfLibProvidedEntries;
	}

	private Set<String> getWebInfLibEntries(JarFile war) throws IOException {
		Set<String> webInfLibEntries = new HashSet<String>();
		Enumeration<JarEntry> entries = war.entries();
		while (entries.hasMoreElements()) {
			String name = entries.nextElement().getName();
			if (isWebInfLibEntry(name)) {
				webInfLibEntries.add(name);
			}
		}
		return webInfLibEntries;
	}

	private boolean isWebInfLibProvidedEntry(String name) {
		return name.startsWith(WEB_INF_LIB_PROVIDED_PREFIX)
				&& !name.equals(WEB_INF_LIB_PROVIDED_PREFIX);
	}

	private boolean isWebInfLibEntry(String name) {
		return name.startsWith(WEB_INF_LIB_PREFIX) && !name.equals(WEB_INF_LIB_PREFIX);
	}

	private boolean isExpectedInWebInfLibProvided(String name,
			Set<String> expectedEntries) {
		for (String expected : expectedEntries) {
			if (name.startsWith(WEB_INF_LIB_PROVIDED_PREFIX + expected)) {
				return true;
			}
		}
		return false;
	}

	private boolean isExpectedInWebInfLib(String name, Set<String> unexpectedEntries) {
		for (String unexpected : unexpectedEntries) {
			if (name.startsWith(WEB_INF_LIB_PREFIX + unexpected)) {
				return false;
			}
		}
		return true;
	}

}
