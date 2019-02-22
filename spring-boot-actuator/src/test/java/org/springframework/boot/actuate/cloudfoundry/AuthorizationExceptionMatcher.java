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

package org.springframework.boot.actuate.cloudfoundry;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;

import org.springframework.boot.actuate.cloudfoundry.CloudFoundryAuthorizationException.Reason;

/**
 * Hamcrest matcher to check the {@link AuthorizationExceptionMatcher} {@link Reason}.
 *
 * @author Madhura Bhave
 */
final class AuthorizationExceptionMatcher {

	private AuthorizationExceptionMatcher() {
	}

	static Matcher<?> withReason(final Reason reason) {
		return new CustomMatcher<Object>(
				"CloudFoundryAuthorizationException with " + reason + " reason") {

			@Override
			public boolean matches(Object object) {
				return ((object instanceof CloudFoundryAuthorizationException)
						&& ((CloudFoundryAuthorizationException) object)
								.getReason() == reason);
			}

		};
	}

}
