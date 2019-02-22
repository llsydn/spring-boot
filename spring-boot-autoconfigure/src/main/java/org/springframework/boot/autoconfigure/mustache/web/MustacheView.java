/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.samskivert.mustache.Template;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * Spring MVC {@link View} using the Mustache template engine.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.2.2
 */
public class MustacheView extends AbstractTemplateView {

	private Template template;

	/**
	 * Create a new {@link MustacheView} instance.
	 * @since 1.2.5
	 * @see #setTemplate(Template)
	 */
	public MustacheView() {
	}

	/**
	 * Create a new {@link MustacheView} with the specified template.
	 * @param template the source template
	 */
	public MustacheView(Template template) {
		this.template = template;
	}

	/**
	 * Set the Mustache template that should actually be rendered.
	 * @param template the mustache template
	 * @since 1.2.5
	 */
	public void setTemplate(Template template) {
		this.template = template;
	}

	@Override
	protected void renderMergedTemplateModel(Map<String, Object> model,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (this.template != null) {
			this.template.execute(model, response.getWriter());
		}
	}

}
