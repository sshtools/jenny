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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.sshtools.jenny.web.Route.RouteBuilder;
import com.sshtools.uhttpd.UHTTPD.AbstractContext;
import com.sshtools.uhttpd.UHTTPD.AbstractWebContextBuilder;
import com.sshtools.uhttpd.UHTTPD.Handler;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class Router extends AbstractContext {

	public final static class RouterBuilder extends AbstractWebContextBuilder<RouterBuilder, Router> {

		public Router build() {
			return new Router(this);
		}
	}

	private final Set<Route> routes = new LinkedHashSet<>();
	private final static ThreadLocal<Set<WebModule>> requires = new ThreadLocal<>();

	private Router(RouterBuilder builder) {
		super(builder);
	}

	public RouteBuilder route() {
		return new RouteBuilder(routes::add, routes::remove);
	}
	
	public static void requires(WebModule handle) {
		var reqs = requires.get();
		if(reqs == null) {
			reqs = new LinkedHashSet<>();
			requires.set(reqs);
		}
		reqs.add(handle);
	}
	
	public static Set<WebModule> requires() {
		var reqs = requires.get();
		if(reqs == null)
			return Collections.emptySet();
		else
			return reqs;
	}

	@Override
	public void get(Transaction tx) throws Exception {
		try {
			handleMultiple(tx, routes);
		}
		finally {
			requires.remove();
		}
	}

	@Override
	protected Collection<? extends Handler> handlers() {
		return routes;
	}
}
