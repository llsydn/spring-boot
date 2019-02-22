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

package org.springframework.boot.autoconfigure.gson;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GsonAutoConfiguration}.
 *
 * @author David Liu
 */
public class GsonAutoConfigurationTests {

	AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void gsonRegistration() {
		this.context.register(GsonAutoConfiguration.class);
		this.context.refresh();
		Gson gson = this.context.getBean(Gson.class);
		assertThat(gson.toJson(new DataObject())).isEqualTo("{\"data\":\"hello\"}");
	}

	public class DataObject {

		@SuppressWarnings("unused")
		private String data = "hello";

	}

}
