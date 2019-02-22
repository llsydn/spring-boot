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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Common {@link Layout}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public final class Layouts {

	private Layouts() {
	}

	/**
	 * Return a layout for the given source file.
	 * @param file the source file
	 * @return a {@link Layout}
	 */
	public static Layout forFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		if (file.getName().toLowerCase().endsWith(".jar")) {
			return new Jar();
		}
		if (file.getName().toLowerCase().endsWith(".war")) {
			return new War();
		}
		if (file.isDirectory() || file.getName().toLowerCase().endsWith(".zip")) {
			return new Expanded();
		}
		throw new IllegalStateException("Unable to deduce layout for '" + file + "'");
	}

	/**
	 * Executable JAR layout.
	 */
	public static class Jar implements RepackagingLayout {

		@Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.JarLauncher";
		}

		@Override
		public String getLibraryDestination(String libraryName, LibraryScope scope) {
			return "BOOT-INF/lib/";
		}

		@Override
		public String getClassesLocation() {
			return "";
		}

		@Override
		public String getRepackagedClassesLocation() {
			return "BOOT-INF/classes/";
		}

		@Override
		public boolean isExecutable() {
			return true;
		}

	}

	/**
	 * Executable expanded archive layout.
	 */
	public static class Expanded extends Jar {

		@Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.PropertiesLauncher";
		}

	}

	/**
	 * No layout.
	 */
	public static class None extends Jar {

		@Override
		public String getLauncherClassName() {
			return null;
		}

		@Override
		public boolean isExecutable() {
			return false;
		}

	}

	/**
	 * Executable WAR layout.
	 */
	public static class War implements Layout {

		private static final Map<LibraryScope, String> scopeDestinations;

		static {
			Map<LibraryScope, String> map = new HashMap<LibraryScope, String>();
			map.put(LibraryScope.COMPILE, "WEB-INF/lib/");
			map.put(LibraryScope.CUSTOM, "WEB-INF/lib/");
			map.put(LibraryScope.RUNTIME, "WEB-INF/lib/");
			map.put(LibraryScope.PROVIDED, "WEB-INF/lib-provided/");
			scopeDestinations = Collections.unmodifiableMap(map);
		}

		@Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.WarLauncher";
		}

		@Override
		public String getLibraryDestination(String libraryName, LibraryScope scope) {
			return scopeDestinations.get(scope);
		}

		@Override
		public String getClassesLocation() {
			return "WEB-INF/classes/";
		}

		@Override
		public boolean isExecutable() {
			return true;
		}

	}

	/**
	 * Module layout (designed to be used as a "plug-in").
	 * @deprecated as of 1.5 in favor of a custom {@link LayoutFactory}
	 */
	@Deprecated
	public static class Module implements Layout {

		private static final Set<LibraryScope> LIB_DESTINATION_SCOPES = new HashSet<LibraryScope>(
				Arrays.asList(LibraryScope.COMPILE, LibraryScope.RUNTIME,
						LibraryScope.CUSTOM));

		@Override
		public String getLauncherClassName() {
			return null;
		}

		@Override
		public String getLibraryDestination(String libraryName, LibraryScope scope) {
			if (LIB_DESTINATION_SCOPES.contains(scope)) {
				return "lib/";
			}
			return null;
		}

		@Override
		public String getClassesLocation() {
			return "";
		}

		@Override
		public boolean isExecutable() {
			return false;
		}

	}

}
