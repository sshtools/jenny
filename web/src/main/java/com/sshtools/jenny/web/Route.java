/**
 * Copyright © 2023 JAdaptive Limited (support@jadaptive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.jenny.web;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.function.Consumer;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.uhttpd.UHTTPD.AbstractContext;
import com.sshtools.uhttpd.UHTTPD.AbstractWebContextBuilder;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public final class Route extends AbstractContext {

	private final static Log LOG = Logs.of(WebLog.WEB);

	public final static class RouteBuilder extends AbstractWebContextBuilder<RouteBuilder, Route> {
		
		private final Consumer<Route> onBuild;
		private final Consumer<Route> onClose;

		RouteBuilder(Consumer<Route> onBuild, Consumer<Route> onClose) {
			this.onBuild = onBuild;
			this.onClose = onClose;
		}

		@Override
		public Route build() {
			return new Route(this);
		}
	}

	private final Consumer<Route> onClose;

	private Route(RouteBuilder builder) {
		super(builder);
		builder.onBuild.accept(this);
		onClose = builder.onClose;
	}

	@Override
	public void get(Transaction tx) throws Exception {
		for (var c : handlers.entrySet()) {
			if (c.getKey().matches(tx)) {
				tx.selector(c.getKey());
				try {
					c.getValue().get(tx);
				} catch (NoSuchFileException | FileNotFoundException fnfe) {
					if (LOG.debug())
						LOG.debug("File not found. {0}", fnfe.getMessage());
					tx.notFound();
				} catch (Exception ise) {
					LOG.error("Request handling failed.", ise);
					tx.error(ise);
				}

				if (tx.responded() || tx.hasResponse())
					break;
			}
		}
	}

	@Override
	protected void onClose() {
		onClose.accept(this);
	}

}
