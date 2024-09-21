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

import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.sshtools.uhttpd.UHTTPD.Session;

public final class WebState  {
	
	private final static String USER = "webstate.user";
	private final static String LOCALE = "webstate.locale";
	
	private final static Map<Session, WebState> map = new ConcurrentHashMap<>();
	
	private final Session session;
	private final Map<String, Object> env = new ConcurrentHashMap<String, Object>();
	
	public static WebState get() {
		return get(Session.get());
	}
	
	public static Optional<WebState> get(boolean create) {
		return Session.get(create).map(s -> get(s));
	}
	
	public static WebState get(Session session) {
		var state = map.get(session);
		if(state == null) {
			state = new WebState(session);
			map.put(session, state);
		}
		return state;
	}
	
	WebState(Session session) {
		this.session = session;
	}
	
	public Optional<Principal> user() {
		return Optional.ofNullable(get(USER));
	}
	
	public Optional<Locale> localeOr() {
		return Optional.ofNullable(get(LOCALE));
	}
	
	public Locale locale() {
		return localeOr().orElse(Locale.getDefault());
	}
	
	@SuppressWarnings("unchecked")
	public <V> V get(String key) {
		return (V)env.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <V> V get(String key, V defaultValue) {
		return env.containsKey(key) ? (V)env.get(key) : defaultValue;
	}
	

	@SuppressWarnings("unchecked")
	public <V> V get(String key, Supplier<V> defaultValue) {
		return env.containsKey(key) ? (V)env.get(key) : defaultValue.get();
	}
	
	@SuppressWarnings("unchecked")
	public <V> V set(String key, V val) {
		return (V)env.put(key, val);
	}
	
	public Map<String, Object> env() {
		return env;
	}
	
	public WebState locale(Locale locale) {
		env.put(LOCALE, locale);
		return this;
	}
	
	public void authenticate(Principal user) {
		user().ifPresentOrElse(u -> {
			throw new IllegalStateException("Already authenticated.");
		}, () -> set(USER, user));
	}
	
	public Session session() {
		return session;
	}

	public void invalidate() {
		env.clear();
		map.remove(session);
	}

	public boolean authenticated() {
		return user().isPresent();
	}
}
