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
package com.sshtools.jenny.mail;

import static com.sshtools.bootlace.api.PluginContext.$;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.api.Api;
import com.sshtools.jenny.config.Config;
import com.sshtools.jenny.messaging.MessageDeliveryProvider;
import com.sshtools.jini.config.INISet;

public class Mail implements Plugin {
	
	private final Config config					= $().plugin(Config.class);
	private final Api api						= $().plugin(Api.class);
	
	private final INISet mailConfig 		    = config.configBuilder("mail").build();
	
	@Override
	public void afterOpen(PluginContext context) {
		
		var provider = new SimpleMailDeliveryProvider(mailConfig);

		context.autoClose(api.extensions().
				group().point(MessageDeliveryProvider.class, (a) -> provider));
	}
}
