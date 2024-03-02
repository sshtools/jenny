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
package com.sshtools.jenny.logging;

import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;

public class Logging implements Plugin {

	@Override
	public void afterOpen(PluginContext context) {
	}

	@Override
	public void open(PluginContext context) {
		LoggerFactory.getLogger(Logging.class).info("SLF4J Logging framework initialized.");
		Logger.getAnonymousLogger().info("JUL Logging framework initalized.");
	}

	@Override
	public void beforeClose(PluginContext context) {
	}
}
