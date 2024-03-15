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

import java.util.concurrent.Callable;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;

public enum WebLog implements Logs.Category {
	WEB, JOBS, ALERTS, TEMPLATES;
	
	
	private final static Log JOB_LOG = Logs.of(WebLog.JOBS);
	
	public final static <T> Callable<T> logTask(Callable<T> task) {
		if(JOB_LOG.debug()) {
			JOB_LOG.debug("Queueing task: {0}", task);
		}
		return new Callable<T>() {

			@Override
			public T call() throws Exception {
				try {
					if(JOB_LOG.debug()) {
						JOB_LOG.debug("Executing task: {0}", task);
					}
					return task.call();
				}
				catch(Throwable t) {
					JOB_LOG.error("Task failed.", t); 
					throw t;
				}
				finally {
					if(JOB_LOG.debug()) {
						JOB_LOG.debug("Completed task: {0}", task);
					}
				}
			}
		};
	}
	
	public final static Runnable logTask(Runnable task) {
		if(JOB_LOG.debug()) {
			JOB_LOG.debug("Queueing task: {0}", task);
		}
		return new Runnable() {

			@Override
			public void run() {
				try {
					if(JOB_LOG.debug()) {
						JOB_LOG.debug("Executing task: {0}", task);
					}
					task.run();
				}
				catch(Throwable t) {
					JOB_LOG.error("Task failed.", t); 
				}
				finally {
					if(JOB_LOG.debug()) {
						JOB_LOG.debug("Completed task: {0}", task);
					}
				}
			}
		};
	}
}
