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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;

public class Api implements Plugin {

	private final XPoints extensions;
	private final ScheduledExecutorService queue;

	public Api() {
		queue = Executors.newScheduledThreadPool(1);
		extensions = new XPoints();
	}

	/**
	 * Intended only for scheduling tasks off at a certain time, should return as
	 * quickly as possible.
	 * 
	 * @return global timer queue
	 */
	public ScheduledExecutorService globalTimerQueue() {
		return queue;
	}

	public XPoints extensions() {
		return extensions;
	}

	@Override
	public void open(PluginContext context) {
	}

}
