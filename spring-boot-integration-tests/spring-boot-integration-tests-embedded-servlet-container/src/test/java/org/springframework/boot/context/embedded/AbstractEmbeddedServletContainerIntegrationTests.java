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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.StringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Base class for embedded servlet container integration tests.
 *
 * @author Andy Wilkinson
 */
public abstract class AbstractEmbeddedServletContainerIntegrationTests {

	@ClassRule
	public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public final AbstractApplicationLauncher launcher;

	protected final RestTemplate rest = new RestTemplate();

	public static Object[] parameters(String packaging,
			List<Class<? extends AbstractApplicationLauncher>> applicationLaunchers) {
		List<Object> parameters = new ArrayList<Object>();
		parameters.addAll(createParameters(packaging, "jetty",
				Arrays.asList("current", "9.3.16.v20170120"), applicationLaunchers));
		parameters.addAll(createParameters(packaging, "tomcat",
				Arrays.asList("current", "8.0.41", "7.0.75"), applicationLaunchers));
		parameters.addAll(createParameters(packaging, "undertow",
				Collections.singletonList("current"), applicationLaunchers));
		return parameters.toArray(new Object[parameters.size()]);
	}

	private static List<Object> createParameters(String packaging, String container,
			List<String> versions,
			List<Class<? extends AbstractApplicationLauncher>> applicationLaunchers) {
		List<Object> parameters = new ArrayList<Object>();
		for (String version : versions) {
			ApplicationBuilder applicationBuilder = new ApplicationBuilder(
					temporaryFolder, packaging, container, version);
			for (Class<? extends AbstractApplicationLauncher> launcherClass : applicationLaunchers) {
				try {
					AbstractApplicationLauncher launcher = launcherClass
							.getDeclaredConstructor(ApplicationBuilder.class)
							.newInstance(applicationBuilder);
					String name = StringUtils.capitalise(container) + " " + version + ": "
							+ launcher.getDescription(packaging);
					parameters.add(new Object[] { name, launcher });
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		return parameters;
	}

	protected AbstractEmbeddedServletContainerIntegrationTests(String name,
			AbstractApplicationLauncher launcher) {
		this.launcher = launcher;
		this.rest.setErrorHandler(new ResponseErrorHandler() {

			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				return false;
			}

			@Override
			public void handleError(ClientHttpResponse response) throws IOException {

			}

		});
		this.rest.setUriTemplateHandler(new UriTemplateHandler() {

			@Override
			public URI expand(String uriTemplate, Object... uriVariables) {
				return URI.create(
						"http://localhost:" + launcher.getHttpPort() + uriTemplate);
			}

			@Override
			public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
				return URI.create(
						"http://localhost:" + launcher.getHttpPort() + uriTemplate);
			}

		});
	}

}
