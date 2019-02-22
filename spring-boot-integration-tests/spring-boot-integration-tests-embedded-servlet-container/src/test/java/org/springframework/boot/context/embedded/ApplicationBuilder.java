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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import com.samskivert.mustache.Mustache;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.rules.TemporaryFolder;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds a Spring Boot application using Maven. To use this class, the {@code maven.home}
 * system property must be set.
 *
 * @author Andy Wilkinson
 */
class ApplicationBuilder {

	private final TemporaryFolder temp;

	private final String packaging;

	private final String container;

	private final String containerVersion;

	ApplicationBuilder(TemporaryFolder temp, String packaging, String container,
			String containerVersion) {
		this.temp = temp;
		this.packaging = packaging;
		this.container = container;
		this.containerVersion = containerVersion;
	}

	File buildApplication() throws Exception {
		File containerFolder = new File(this.temp.getRoot(),
				this.container + "-" + this.containerVersion);
		if (containerFolder.exists()) {
			return new File(containerFolder, "app/target/app-0.0.1." + this.packaging);
		}
		return doBuildApplication(containerFolder);
	}

	private File doBuildApplication(File containerFolder)
			throws IOException, FileNotFoundException, MavenInvocationException {
		File resourcesJar = createResourcesJar();
		File appFolder = new File(containerFolder, "app");
		appFolder.mkdirs();
		writePom(appFolder, resourcesJar);
		copyApplicationSource(appFolder);
		packageApplication(appFolder);
		return new File(appFolder, "target/app-0.0.1." + this.packaging);
	}

	private File createResourcesJar() throws IOException, FileNotFoundException {
		File resourcesJar = new File(this.temp.getRoot(), "resources.jar");
		if (resourcesJar.exists()) {
			return resourcesJar;
		}
		JarOutputStream resourcesJarStream = new JarOutputStream(
				new FileOutputStream(resourcesJar));
		resourcesJarStream.putNextEntry(new ZipEntry("META-INF/resources/"));
		resourcesJarStream.closeEntry();
		resourcesJarStream.putNextEntry(
				new ZipEntry("META-INF/resources/nested-meta-inf-resource.txt"));
		resourcesJarStream.write("nested".getBytes());
		resourcesJarStream.closeEntry();
		resourcesJarStream.close();
		return resourcesJar;
	}

	private void writePom(File appFolder, File resourcesJar)
			throws FileNotFoundException, IOException {
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("packaging", this.packaging);
		context.put("container", this.container);
		context.put("bootVersion", Versions.getBootVersion());
		context.put("resourcesJarPath", resourcesJar.getAbsolutePath());
		context.put("containerVersion",
				"current".equals(this.containerVersion) ? ""
						: String.format("<%s.version>%s</%s.version>", this.container,
								this.containerVersion, this.container));
		context.put("additionalDependencies", getAdditionalDependencies());
		FileWriter out = new FileWriter(new File(appFolder, "pom.xml"));
		Mustache.compiler().escapeHTML(false)
				.compile(new FileReader("src/test/resources/pom-template.xml"))
				.execute(context, out);
		out.close();
	}

	private List<Map<String, String>> getAdditionalDependencies() {
		List<Map<String, String>> additionalDependencies = new ArrayList<Map<String, String>>();
		if ("tomcat".equals(this.container) && !"current".equals(this.containerVersion)) {
			Map<String, String> juli = new HashMap<String, String>();
			juli.put("groupId", "org.apache.tomcat");
			juli.put("artifactId", "tomcat-juli");
			juli.put("version", "${tomcat.version}");
			additionalDependencies.add(juli);
		}
		return additionalDependencies;
	}

	private void copyApplicationSource(File appFolder) throws IOException {
		File examplePackage = new File(appFolder, "src/main/java/com/example");
		examplePackage.mkdirs();
		FileCopyUtils.copy(
				new File("src/test/java/com/example/ResourceHandlingApplication.java"),
				new File(examplePackage, "ResourceHandlingApplication.java"));
		if ("war".equals(this.packaging)) {
			File srcMainWebapp = new File(appFolder, "src/main/webapp");
			srcMainWebapp.mkdirs();
			FileCopyUtils.copy("webapp resource",
					new FileWriter(new File(srcMainWebapp, "webapp-resource.txt")));
		}
	}

	private void packageApplication(File appFolder) throws MavenInvocationException {
		InvocationRequest invocation = new DefaultInvocationRequest();
		invocation.setBaseDirectory(appFolder);
		invocation.setGoals(Collections.singletonList("package"));
		InvocationResult execute = new DefaultInvoker().execute(invocation);
		assertThat(execute.getExitCode()).isEqualTo(0);
	}

}
