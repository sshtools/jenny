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
package com.sshtools.jenny.tunnels;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;

public class Tunnels implements Plugin {
	public final static Log LOG = Logs.of(Logs.Category.ofSimpleName(Tunnels.class));

	private TunnelProvider provider;
	private boolean obtained;

	@Override
	public void afterOpen(PluginContext context) throws Exception {
	}

	public void provider(TunnelProvider provider) {
		if (this.provider == null && provider != null) {
			if (obtained) {
				throw new IllegalStateException("Cannot set provider after bus has been obtained.");
			}
			this.provider = provider;
		} else if (this.provider != null && provider == null) {
			this.provider = null;
		} else {
			throw new IllegalStateException();
		}
	}

	public TunnelInstance aquire(TunnelRequest request) {
		try {
			if(provider == null) {
				throw new IllegalStateException("No tunnel provider.");
			}
			return provider.acquire(request);
		}
		finally {
			obtained = true;
		}
	}

	public boolean hasProvider() {
		return provider != null;
	}

}
