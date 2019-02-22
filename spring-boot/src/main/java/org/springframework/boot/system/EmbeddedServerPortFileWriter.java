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

package org.springframework.boot.system;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationListener} that saves embedded server port and management port into
 * file. This application listener will be triggered whenever the servlet container
 * starts, and the file name can be overridden at runtime with a System property or
 * environment variable named "PORTFILE" or "portfile".
 *
 * @author David Liu
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class EmbeddedServerPortFileWriter
		implements ApplicationListener<EmbeddedServletContainerInitializedEvent> {

	private static final String DEFAULT_FILE_NAME = "application.port";

	private static final String[] PROPERTY_VARIABLES = { "PORTFILE", "portfile" };

	private static final Log logger = LogFactory
			.getLog(EmbeddedServerPortFileWriter.class);

	private final File file;

	/**
	 * Create a new {@link EmbeddedServerPortFileWriter} instance using the filename
	 * 'application.port'.
	 */
	public EmbeddedServerPortFileWriter() {
		this(new File(DEFAULT_FILE_NAME));
	}

	/**
	 * Create a new {@link EmbeddedServerPortFileWriter} instance with a specified
	 * filename.
	 * @param filename the name of file containing port
	 */
	public EmbeddedServerPortFileWriter(String filename) {
		this(new File(filename));
	}

	/**
	 * Create a new {@link EmbeddedServerPortFileWriter} instance with a specified file.
	 * @param file the file containing port
	 */
	public EmbeddedServerPortFileWriter(File file) {
		Assert.notNull(file, "File must not be null");
		String override = SystemProperties.get(PROPERTY_VARIABLES);
		if (override != null) {
			this.file = new File(override);
		}
		else {
			this.file = file;
		}
	}

	@Override
	public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
		File portFile = getPortFile(event.getApplicationContext());
		try {
			String port = String.valueOf(event.getEmbeddedServletContainer().getPort());
			createParentFolder(portFile);
			FileCopyUtils.copy(port.getBytes(), portFile);
			portFile.deleteOnExit();
		}
		catch (Exception ex) {
			logger.warn(String.format("Cannot create port file %s", this.file));
		}
	}

	/**
	 * Return the actual port file that should be written for the given application
	 * context. The default implementation builds a file from the source file and the
	 * application context namespace.
	 * @param applicationContext the source application context
	 * @return the file that should be written
	 */
	protected File getPortFile(EmbeddedWebApplicationContext applicationContext) {
		String contextName = applicationContext.getNamespace();
		if (StringUtils.isEmpty(contextName)) {
			return this.file;
		}
		String name = this.file.getName();
		String extension = StringUtils.getFilenameExtension(this.file.getName());
		name = name.substring(0, name.length() - extension.length() - 1);
		if (isUpperCase(name)) {
			name = name + "-" + contextName.toUpperCase();
		}
		else {
			name = name + "-" + contextName.toLowerCase();
		}
		if (StringUtils.hasLength(extension)) {
			name = name + "." + extension;
		}
		return new File(this.file.getParentFile(), name);
	}

	private boolean isUpperCase(String name) {
		for (int i = 0; i < name.length(); i++) {
			if (Character.isLetter(name.charAt(i))
					&& !Character.isUpperCase(name.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private void createParentFolder(File file) {
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
	}

}
