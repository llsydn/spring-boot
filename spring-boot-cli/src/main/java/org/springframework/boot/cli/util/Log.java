/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.cli.util;

/**
 * Simple logger used by the CLI.
 *
 * @author Phillip Webb
 */
public abstract class Log {

	public static void info(String message) {
		System.out.println(message);
	}

	public static void infoPrint(String message) {
		System.out.print(message);
	}

	public static void error(String message) {
		System.err.println(message);
	}

	public static void error(Exception ex) {
		ex.printStackTrace(System.err);
	}

}
