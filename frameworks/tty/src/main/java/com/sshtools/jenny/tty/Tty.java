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
package com.sshtools.jenny.tty;

import java.text.MessageFormat;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;

public class Tty implements Plugin {
	public final static Log LOG = Logs.of(Logs.Category.ofSimpleName(Tty.class));

	private TtyProvider provider;
	private boolean obtained;

	@Override
	public void afterOpen(PluginContext context) throws Exception {
	}

	public void provider(TtyProvider provider) {
		if (this.provider == null && provider != null) {
			if (obtained) {
				throw new IllegalStateException("Cannot set provider after bus has been obtained.");
			}
			this.provider = provider;
		} else if (this.provider != null && provider == null) {
			this.provider = null;
		} else if(this.provider != null) {
			throw new IllegalStateException(MessageFormat.format("Cannot be more than one Tty provider. {0} and {1}", this.provider.getClass().getName(), provider.getClass().getName()));
		}
	}

	public TtyInstance aquire(TtyRequest request) {
		try {
			if(provider == null) {
				throw new IllegalStateException("No TTY provider.");
			}
			return provider.allocate(request);
		}
		finally {
			obtained = true;
		}
	}

}
