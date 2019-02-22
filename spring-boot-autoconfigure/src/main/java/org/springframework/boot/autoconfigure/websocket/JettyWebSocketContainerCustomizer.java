/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket;

import org.eclipse.jetty.util.thread.ShutdownThread;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;

/**
 * {@link WebSocketContainerCustomizer} for {@link JettyEmbeddedServletContainerFactory}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public class JettyWebSocketContainerCustomizer
		extends WebSocketContainerCustomizer<JettyEmbeddedServletContainerFactory> {

	@Override
	protected void doCustomize(JettyEmbeddedServletContainerFactory container) {
		container.addConfigurations(new AbstractConfiguration() {

			@Override
			public void configure(WebAppContext context) throws Exception {
				ServerContainer serverContainer = WebSocketServerContainerInitializer
						.configureContext(context);
				ShutdownThread.deregister(serverContainer);
			}

		});
	}

}
