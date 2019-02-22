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

package org.springframework.boot.autoconfigure.mobile;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mobile.device.DeviceResolver;
import org.springframework.mobile.device.site.SitePreferenceHandler;
import org.springframework.mobile.device.site.SitePreferenceHandlerInterceptor;
import org.springframework.mobile.device.site.SitePreferenceHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Mobile's
 * {@link SitePreferenceHandler}. The site preference feature depends on a
 * {@link DeviceResolver} first being registered.
 *
 * @author Roy Clarkson
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ SitePreferenceHandlerInterceptor.class,
		SitePreferenceHandlerMethodArgumentResolver.class })
@AutoConfigureAfter(DeviceResolverAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.mobile.sitepreference", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
public class SitePreferenceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(SitePreferenceHandlerInterceptor.class)
	public SitePreferenceHandlerInterceptor sitePreferenceHandlerInterceptor() {
		return new SitePreferenceHandlerInterceptor();
	}

	@Bean
	public SitePreferenceHandlerMethodArgumentResolver sitePreferenceHandlerMethodArgumentResolver() {
		return new SitePreferenceHandlerMethodArgumentResolver();
	}

	@Configuration
	protected static class SitePreferenceMvcConfiguration
			extends WebMvcConfigurerAdapter {

		private final SitePreferenceHandlerInterceptor sitePreferenceHandlerInterceptor;

		private final SitePreferenceHandlerMethodArgumentResolver sitePreferenceHandlerMethodArgumentResolver;

		protected SitePreferenceMvcConfiguration(
				SitePreferenceHandlerInterceptor sitePreferenceHandlerInterceptor,
				org.springframework.mobile.device.site.SitePreferenceHandlerMethodArgumentResolver sitePreferenceHandlerMethodArgumentResolver) {
			this.sitePreferenceHandlerInterceptor = sitePreferenceHandlerInterceptor;
			this.sitePreferenceHandlerMethodArgumentResolver = sitePreferenceHandlerMethodArgumentResolver;
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(this.sitePreferenceHandlerInterceptor);
		}

		@Override
		public void addArgumentResolvers(
				List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(this.sitePreferenceHandlerMethodArgumentResolver);
		}

	}

}
