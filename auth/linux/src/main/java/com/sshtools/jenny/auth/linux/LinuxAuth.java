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
import java.util.Optional;
import java.util.Set;

import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.api.Api;
import com.sshtools.jenny.auth.api.ExtendedUserPrincipal;
import com.sshtools.jenny.auth.api.Auth.PasswordAuthProvider;

public class LinuxAuth implements Plugin {

	public final static class Provider implements PasswordAuthProvider {
		@Override
		public Optional<ExtendedUserPrincipal> logon(String username, char[] password) {
			try {
				var pam = new PAM("Jenny");
				try {
					var user = pam.authenticate(username, new String(password));
					return Optional.of(new ExtendedUserPrincipal.LinuxUser() {
	
						@Override
						public String getName() {
							return user.getUserName();
						}
	
						@Override
						public int uid() {
							return user.getUID();
						}
	
						@Override
						public Optional<String> shell() {
							return Optional.of(user.getShell());
						}
	
						@Override
						public Set<String> groups() {
							return user.getGroups();
						}
	
						@Override
						public int gid() {
							return user.getGID();
						}
	
						@Override
						public Optional<String> gecos() {
							return Optional.of(user.getGecos());
						}
	
						@Override
						public Optional<Path> dir() {
							return Optional.of(user.getDir()).map(Paths::get);
						}
					});
				} catch (PAMException pe) {
					return Optional.empty();
				} finally {
					pam.dispose();
				}
			}
			catch(PAMException pe) {
				throw new IllegalStateException("Failed to configure PAM.", pe);
			}
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
				group().
					point(PasswordAuthProvider.class, (a) -> provider));
	}

}
