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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import javax.json.Json;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.bootstrap5.Bootstrap5;
import com.sshtools.jenny.io.Io;
import com.sshtools.jenny.io.Io.Contributor;
import com.sshtools.jenny.io.Io.IoChannel;
import com.sshtools.jenny.io.Io.Sender;
import com.sshtools.jenny.jobs.Job.Handle;
import com.sshtools.jenny.jobs.Job.Queue;
import com.sshtools.jenny.jobs.Job.StandardQueues;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebLog;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebModule.WebModulesRef;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Status;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class Jobs implements Plugin {
	final static Log LOG = Logs.of(WebLog.JOBS);
	
	
	public record JobOptions<RESULT>(Queue queue, Optional<ResourceBundle> bundle, Job<RESULT> job, Optional<String> category, boolean exclusive) {}
	
	public final static class JobBuilder<RESULT> {
		private Queue queue = StandardQueues.GENERIC;
		private Optional<ResourceBundle> bundle = Optional.empty();
		private final Job<RESULT> job;
		private Optional<String> category = Optional.empty();
		private boolean exclusive = true;
		
		public JobBuilder(Job<RESULT> job) {
			this.job = job;
		}
		
		public JobBuilder(Class<?> categoryAndBundle, Locale locale, Job<RESULT> job) {
			this.job = job;
			withCategory(categoryAndBundle);
			withBundle(categoryAndBundle, locale);
		}
		
		public JobBuilder<RESULT> withCategory(Class<?> category) {
			return withCategory(category.getName());
		}
		
		public JobBuilder<RESULT> withCategory(String category) {
			this.category = Optional.of(category);
			return this;
		}
		
		public JobBuilder<RESULT> withoutExclusive() {
			return withExclusive(false);
		}
		
		public JobBuilder<RESULT> withExclusive(boolean exclusive) {
			this.exclusive = exclusive;
			return this;
		}
		
		public JobBuilder<RESULT> withQueue(Queue queue) {
			this.queue = queue;
			return this;
		}
		
		public JobBuilder<RESULT> withBundle(ResourceBundle bundle) {
			this.bundle = Optional.of(bundle);
			return this;
		}
		
		public JobBuilder<RESULT> withBundle(Class<?> bundle, Locale locale) {
			return withBundle(ResourceBundle.getBundle(bundle.getName(), locale, bundle.getClassLoader()));
		}
		
		public JobOptions<RESULT> build() {
			return new JobOptions<>(queue, bundle, job, category, exclusive);
		}
		
	}
	
	private Web web;
	private Io io;
	private Map<Queue, ScheduledExecutorService> queues = new HashMap<>();
	private Map<String, List<Handle<?>>> jobs = new ConcurrentHashMap<>();
	private Map<UUID, Handle<?>> jobsByUuid = new ConcurrentHashMap<>();
	private Map<String, Contributor> ioContributors = new ConcurrentHashMap<>();
	private Map<String, Sender> ioSenders = new ConcurrentHashMap<>();
	private WebModule jobModule;
	private WebModulesRef modulesRef;
	
	@Override
	public void afterOpen(PluginContext context) {
		web = context.plugin(Web.class);
		io = context.plugin(Io.class);
		
		context.autoClose(
			modulesRef = web.modules(
				jobModule = WebModule.of(
					"/job-progress.frag.js", 
					Jobs.class, 
					"job-progress.frag.js", 
					Bootstrap5.MODULE_JQUERY, Bootstrap5.MODULE_BOOTSTRAP5, io.webModule())
			),
			web.router().route().
				handle("/job-cancel", this::actionCancel).
				build());
	}
	
	public WebModule webModule() {
		return jobModule;
	}
	
	public List<Handle<?>> jobs(Class<?> jobCategory) {
		return jobs(jobCategory.getName());
	}
	
	public List<Handle<?>> jobs(String jobCategory) {
		synchronized(jobs) {
			var j = jobs.get(jobCategory);
			if(j == null)
				return Collections.emptyList();
			return Collections.unmodifiableList(j);
		}
	}
	
	
	public boolean running(Class<?> jobCategory) {
		return running(jobCategory.getName());
	}
	
	public boolean running(String jobCategory) {
		synchronized(jobs) {
			var j = jobs.get(jobCategory);
			return j!= null && !j.isEmpty();
		}
	}
	
	public <RESULT> Handle<RESULT> run(JobOptions<RESULT> options) {
		var jobCategory = options.category.orElse("default");
		var q = options.queue;
		var job = options.job;
		
		if(jobs.containsKey(jobCategory) && options.exclusive)
			throw new IllegalStateException(MessageFormat.format("Job with ID {0}", jobCategory));
		ScheduledExecutorService queue;
		synchronized(queues) {
			queue = queues.get(q);
			if(queue == null) {
				queue = Executors.newScheduledThreadPool(q.threads());
				queues.put(q, queue);
			}
		}
		
		var state = new Job.JobState();
		state.category = jobCategory;
		
		var ctx = new Job.JobContext() {
			
			@Override
			public void val(long val) {
				state.val = val;
				sendUpdate();
			}
			
			@Override
			public void text(String message, Object... args) {
				state.text = args.length == 0 ? message : MessageFormat.format(message, args);
				sendUpdate();
				
			}
			
			@Override
			public void max(long max) {
				state.max = max;
				sendUpdate();
			}
			
			@Override
			public void indeterminate() {
				state.max = state.val = 0;
				sendUpdate();
			}
			
			@Override
			public void i18n(String key, Object... args) {
				if(args.length == 0) 
					state.text = options.bundle.orElseThrow(() -> new IllegalStateException("No bundle.")).getString(key);
				else 
					state.text = MessageFormat.format(options.bundle.orElseThrow(() -> new IllegalStateException("No bundle.")).getString(key), args);
				sendUpdate();
				
			}
			
			@Override
			public boolean cancelled() {
				return state.cancelled;
			}

			@Override
			public void title(String message, Object... args) {
				state.title = args.length == 0 ? message : MessageFormat.format(message, args);
				sendUpdate();
			}

			@Override
			public void i18NTitle(String key, Object... args) {
				if(args.length == 0) 
					state.title = options.bundle.orElseThrow(() -> new IllegalStateException("No bundle.")).getString(key);
				else 
					state.title = MessageFormat.format(options.bundle.orElseThrow(() -> new IllegalStateException("No bundle.")).getString(key), args);
				sendUpdate();
			}

			@Override
			public void result(Object result) {
				state.result = result;
				sendUpdate();
			}

			private void sendUpdate() {
				var sndr = ioSenders.get(jobCategory);
				if(sndr == null)
					LOG.warning("Attempt to send update before sender was ready for {0}", jobCategory);
				else {
					try {
						sndr.send(Json.createObjectBuilder().
							add("type", "update").
							add("uuid", state.uuid.toString()).
							add("val", state.val).
							add("percent", state.percent()).
							add("max", state.max).
							add("text", state.text == null ? "" : state.text).
							add("title", state.title == null ? "" : state.title).
							build());
					}
					catch(Exception e) {
						LOG.warning("Failed to send job update.", e);
					}
				}
			}

			@Override
			public void onCancel(Function<Job<?>, Boolean> r) {
				state.onCancel = r;
			}
		};
		
		var rawFuture = queue.submit(() -> {
			try {
				job.apply(ctx);
				return state.result;
			}
			catch(Throwable e) {
				LOG.error("Job failure.", e);
				throw e;
			}
			finally {
				var hndl = jobsByUuid.remove(state.uuid());
				synchronized(jobs) {
					var l = jobs.get(jobCategory);
					l.remove(hndl);
					if(l.isEmpty())
						jobs.remove(jobCategory);
				}
				ioContributors.remove(jobCategory).close();
				
				var sender = ioSenders.remove(jobCategory);
				sender.send(Json.createObjectBuilder().
						add("uuid", state.uuid.toString()).
						add("type", "complete").
						build());
			}
		});
		
		var handle = new Handle<RESULT>(new Future<RESULT>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				state.cancelled = true;
				if(state.onCancel == null || !state.onCancel.apply(job)) {
					return rawFuture.cancel(mayInterruptIfRunning);
				}
				return true;
				
			}

			@Override
			public boolean isCancelled() {
				return state.cancelled;
			}

			@Override
			public boolean isDone() {
				return rawFuture.isDone();
			}

			@SuppressWarnings("unchecked")
			@Override
			public RESULT get() throws InterruptedException, ExecutionException {
				return (RESULT) rawFuture.get();
			}

			@SuppressWarnings("unchecked")
			@Override
			public RESULT get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				return (RESULT) rawFuture.get(timeout, unit);
			}
		}, state);
		
		synchronized(jobs) {
			var l = jobs.get(jobCategory);
			if(l == null) {
				l = new ArrayList<>();
				jobs.put(jobCategory, l);
				ioContributors.put(jobCategory, io.contributor("jobs." + jobCategory, (sndr) -> {
					ioSenders.put(jobCategory, sndr);
					return ioJobStatus(handle, sndr); 
				}));
			}
			l.add(handle);
		}
		jobsByUuid.put(state.uuid(), handle);
		
		return handle;
	}
	
	private IoChannel ioJobStatus(Handle<?> jobHandle, Sender sndr) {
		return IoChannel.of(sndr, (incoming) -> {
			// nothing incoming yet
		});
	}
	public TemplateModel fragJob(Class<?> jobCategory) {
		return fragJob(jobCategory.getName());
	}

	public TemplateModel fragJob(String jobCategory) {
		var categoryJobs = jobs(jobCategory);
		var template = web.template(Jobs.class, "job-progress.frag.html").
			variable("jobs.category", jobCategory).
			list("jobs", (content) -> categoryJobs.stream().map(hndl -> {
				return web.template(content).
					variable("job.uuid", hndl.state()::uuid).
					variable("job.text", hndl.state()::text).
					variable("job.title", hndl.state()::title).
					variable("job.category", hndl.state()::category).
					variable("job.max", hndl.state()::max).
					variable("job.val", hndl.state()::val).
					variable("job.percent", hndl.state()::percent).
					variable("job.cancelled", hndl.state()::cancelled).
					bundle(Jobs.class);
			}).toList());
		
		web.require(template, modulesRef);
		
		return template;
	}

	private void actionCancel(Transaction tx) {
		var job = jobsByUuid.get(UUID.fromString(tx.parameter("uuid").asString()));
		if(job == null)
			throw new IllegalArgumentException("No such job.");
		var future = job.result();
		future.cancel(true);
		try {
			future.get(10, TimeUnit.SECONDS);
		} catch (CancellationException | InterruptedException | ExecutionException | TimeoutException e) {
//			throw new IllegalStateException(e);
		}
		var returnTo = tx.parameterOr("returnTo").map(n -> n.asString()).orElse("");
		if(returnTo.equals("")) {
			tx.redirect(Status.MOVED_TEMPORARILY, tx.contextPath());
		}
		else {
			tx.redirect(Status.MOVED_TEMPORARILY, returnTo);
		}
	}

}
