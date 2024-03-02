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
package com.sshtools.jenny.auth.api;

import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.sshtools.bootlace.api.Exceptions.InvalidCredentials;
import com.sshtools.bootlace.api.Exceptions.InvalidUsernameOrPasswordException;
import com.sshtools.jenny.api.Api;
import com.sshtools.jenny.api.XPoint;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;

public class Auth implements Plugin {
	
	public interface PasswordAuthProvider extends XPoint {
		Optional<ExtendedUserPrincipal> logon(String username, char[] password);
	}
	
	public interface ExternalAuthProvider extends XPoint {
		Optional<String> redirect(Optional<String> username, String returnTo);
		
		UserPrincipal complete(String response);
	}
	
	public interface DirectAuthProvider extends XPoint {
		Optional<UserPrincipal> logon(String path, Map<String, String> parameters);
	}
	
	public final static class ExternalAuthSession {
		private final UUID id = UUID.randomUUID();
		
		private final String redirect;
		private final ExternalAuthProvider provider;
		private final Instant created = Instant.now();
		
		private ExternalAuthSession(String redirect, ExternalAuthProvider provider) {
			this.redirect = redirect;
			this.provider = provider;
		}
		
		public Instant created() {
			return created;
		}
		
		public ExternalAuthProvider provider() {
			return provider;
		}
		
		public UUID id() {
			return id;
		}
		
		public String redirect() {
			return redirect;
		}
	}

	private Api api;
	private Map<UUID, ExternalAuthSession> sessions = new ConcurrentHashMap<>();
	private final static int AUTH_SESSION_TIMEOUT_MINUTES = Integer.parseInt(System.getProperty("jenny.auth.externalTimeout", "5"));

	@Override
	public void afterOpen(PluginContext context) {
		api = context.plugin(Api.class);
		api.globalTimerQueue().scheduleWithFixedDelay(() -> {
			synchronized(sessions) {
				var it = sessions.values().iterator();
				var now = Instant.now();
				while(it.hasNext()) {
					var session = it.next();
					if(now.isAfter(session.created().plus(AUTH_SESSION_TIMEOUT_MINUTES, ChronoUnit.MINUTES)))
						it.remove();
				}
			}
		}, 1, 1, TimeUnit.MINUTES);
	}
	
	public boolean isExternalSupported() {
		return !api.extensions().points(ExternalAuthProvider.class).isEmpty();
	}
	
	public boolean isDirectSupported() {
		return !api.extensions().points(DirectAuthProvider.class).isEmpty();
	}
	
	public UserPrincipal completeExternal(UUID uuid, String response) {
		var session = sessions.get(uuid);
		if(session == null) {			
			throw new InvalidCredentials();
		}
		return session.provider().complete(response); 
	}
	
	public Optional<UserPrincipal> direct(String path, Map<String, String> parameters) {
		for(var prov : api.extensions().points(DirectAuthProvider.class)) {
			var provInstance = prov.apply(null);
			var logon = provInstance.logon(path, parameters);
			if(logon.isPresent())
				return logon;
		}
		return Optional.empty();
	}
	
	public ExternalAuthSession external(Optional<String> username, String returnTo) {
		for(var prov : api.extensions().points(ExternalAuthProvider.class)) {
			var provInstance = prov.apply(null);
			var location = provInstance.redirect(username, returnTo);
			if(location.isPresent()) {
				var session = new ExternalAuthSession(location.get(), provInstance);
				sessions.put(session.id(), session);
				return session;
			}
		}
		throw new InvalidCredentials(username);
	}

	public UserPrincipal passwordLogon(String username, char[] password) {
		for(var prov : api.extensions().points(PasswordAuthProvider.class)) {
			var user = prov.apply(null).logon(username, password);
			if(user.isPresent())
				return user.get();
		}
		throw new InvalidUsernameOrPasswordException(username);
	}
}
