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
package com.sshtools.jenny.auth.linux;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.api.Api;
import com.sshtools.jenny.api.ExtendedUserPrincipal;
import com.sshtools.jenny.auth.api.Auth.AuthResult;
import com.sshtools.jenny.auth.api.Auth.AuthState;
import com.sshtools.jenny.auth.api.Auth.PasswordAuthProvider;

import uk.co.bithatch.linid.Linid;
import uk.co.bithatch.linid.LinuxUserId;

public class LinidAuth implements Plugin {

	public final static class Provider implements PasswordAuthProvider {
		@Override
		public AuthResult logon(String username, char[] password) {
			return Linid.get().authenticate(username, password).map(id -> {
				var linId = (LinuxUserId)id;
				return new AuthResult(AuthState.COMPLETE, new ExtendedUserPrincipal.LinuxUser() {

					private String name;
					private Optional<String> shell;
					private int uid;
					private Set<String> groups;
					private int gid;
					private Optional<String> gecos;
					private Optional<Path> dir;

					{
						name = id.getName();
						shell = Optional.of(linId.shell());
						uid = linId.uid();
						groups = new LinkedHashSet<>(Arrays.asList(linId.collectionNames()));
						gid = linId.gid();
						gecos = Optional.of(String.join(",", linId.gecos()));
						dir = Optional.of(linId.home()).map(Paths::get);
					}
					
					
					@Override
					public String getName() {
						return name;
					}

					@Override
					public int uid() {
						return uid;
					}

					@Override
					public Optional<String> shell() {
						return shell;
					}

					@Override
					public Set<String> groups() {
						return groups;
					}

					@Override
					public int gid() {
						return gid;
					}

					@Override
					public Optional<String> gecos() {
						return gecos;
					}

					@Override
					public Optional<Path> dir() {
						return dir;
					}
				});
			}).orElseGet(() -> new AuthResult(AuthState.DENY));
		}

		@Override
		public int weight() {
			return 0;
		}
	}

	private Api api;

	@Override
	public void afterOpen(PluginContext context) {
		api = context.plugin(Api.class);
		
		var provider = new Provider();

		context.autoClose(api.extensions().
				group().point(PasswordAuthProvider.class, (a) -> provider));
	}

}
