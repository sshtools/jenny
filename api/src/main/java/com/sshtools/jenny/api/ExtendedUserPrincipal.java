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
package com.sshtools.jenny.api;

import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.util.Optional;
import java.util.Set;

public interface ExtendedUserPrincipal extends UserPrincipal {
	
	default String getRealName() {
		return getName();
	}
	
	default boolean canSignOut() {
		return true;
	}
	
	default boolean isVirtual() {
		return false;
	}
	
	default String getEmail() {
		return null;
	}
	
	default String getUuid() {
		return getName();
	}
	
	default String getMobilePhone() {
		return null;
	}

	public interface LinuxUser extends ExtendedUserPrincipal {

		Optional<String> gecos();

		Optional<String> shell();

		Optional<Path> dir();

		int uid();

		int gid();

		Set<String> groups();
	}
}
