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
package com.sshtools.jenny.jobs;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Function;

public interface Job<RESULT> {
	
	public record Handle<RESULT>(Future<RESULT> result, JobState state) {}
	
	public interface Queue {
		int threads();
	}
	
	public enum StandardQueues implements Queue {
		GENERIC, IO, SYSTEM;

		@Override
		public int threads() {
			return 1;
		}
	}

	public interface JobContext {
		void result(Object result);
		
		void indeterminate();

		void max(long max);

		void val(long val);
		
		void title(String message, Object... args);
		
		void i18NTitle(String key, Object... args);
		
		void text(String message, Object... args);
		
		void i18n(String key, Object... args);
		
		boolean cancelled();
		
		void onCancel(Function<Job<?>, Boolean> r);
	}
	
	public static class JobState {
		Queue queue;
		long max;
		long val;
		String text;
		boolean cancelled;
		String title;
		UUID uuid = UUID.randomUUID();
		Object result;
		String category;
		Function<Job<?>, Boolean> onCancel;
		
		public UUID uuid() {
			return uuid;
		}
		
		public Queue queue() {
			return queue;
		}
		
		public long max() {
			return max;
		}
		
		public long val() {
			return val;
		}
		
		public String text() {
			return text;
		}
		
		public String category() {
			return category;
		}
		
		public String title() {
			return title;
		}
		
		public boolean cancelled() {
			return cancelled;
		}
		
		public int percent() { 
			if(max == 0)
				return 0;
			else {
				return (int)(((double)val / (double)max) * 100d);
			}
		}
	}
	
	void apply(JobContext ctx) throws Exception;
}
