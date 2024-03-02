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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.jenny.api.XPoints;
import com.sshtools.jenny.web.Router.RouterBuilder;
import com.sshtools.jenny.web.WebModule.Placement;
import com.sshtools.jenny.web.WebModule.Type;
import com.sshtools.jenny.web.WebModule.WebModuleHandle;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.tinytemplate.Templates.Logger;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.Templates.TemplateProcessor;
import com.sshtools.uhttpd.UHTTPD;
import com.sshtools.uhttpd.UHTTPD.AllSelector;
import com.sshtools.uhttpd.UHTTPD.RootContext;
import com.sshtools.uhttpd.UHTTPD.Status;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public final class Web implements Plugin {
	
	final static Log LOG = Logs.of(WebLog.WEB);
	
	private final TemplateProcessor tp;
	private RootContext httpd;
	private final XPoints extensions;
	private final Router router;
	private final ScheduledExecutorService queue;
	private final Map<WebModuleHandle, WebModule> modules = new ConcurrentHashMap<>();
	
	public Web() {
		this(new NpmPackageGlobalTemplateDecorator());
	}
	
	public Web(GlobalTemplateDecorator npmDecorator) {
		queue = Executors.newScheduledThreadPool(1);
		
		/* Template processing */
		tp = new TemplateProcessor.Builder().
			withLogger(new Logger() {
				final static Log LOG = Logs.of(WebLog.TEMPLATES);
				
				@Override
				public void warning(String message, Object... args) {
					LOG.warning(message, args);
				}
				
				@Override
				public void debug(String message, Object... args) {
					if(LOG.debug())
						LOG.warning(message, args);
				}
			}).
			withMissingAsNull().
			build();
		
		/* Extensions */
		extensions = new XPoints();
		extensions.group().
			point(GlobalTemplateDecorator.class, (tx) -> npmDecorator::decorate);
		
		/* Routes */
		router = new RouterBuilder().
				build();
	}
	
	public WebModuleHandle module(WebModule module) {
		var hndl = router().route().
			handle(module.uri().replace(".", "\\."), module.resource()).
			build();
		
		var wmHndl = new WebModuleHandle() {
			
			@Override
			public void close() throws IOException {
				modules.remove(this);
				hndl.close();
			}

			@Override
			public WebModule webModule() {
				return module;
			}
		};
		modules.put(wmHndl, module);
		
		return wmHndl;
	}
	
	public TemplateModel require(WebModuleHandle module, TemplateModel template) {
		Router.requires(module);
		return template;
		
	}

	public TemplateModel template(String content) {
		return decorateTemplate(Transaction.get(), TemplateModel.ofContent(content));
	}
	
	public TemplateModel template(Class<?> parent, String relPath) {
		return decorateTemplate(Transaction.get(), TemplateModel.ofResource(parent, relPath));
	}
	
	public TemplateModel template(String path, ClassLoader loader) {
		return decorateTemplate(Transaction.get(), TemplateModel.ofResource(path, loader));
	}

	public TemplateProcessor templateProcessor() {
		return tp;
	}
	
	public ScheduledExecutorService globalUiQueue() {
		return queue;
	}
	
	public XPoints extensions() {
		return extensions;
	}
	
	public TemplateProcessor processor() {
		return tp;
	}
	
	public Router router() {
		return router;
	}

	@Override
	public void afterOpen(PluginContext context) {
		
		
		/* Main server loop */
		try {
			var sessions = UHTTPD.sessionCookies().build();
			
			httpd = UHTTPD.server().
				/* Client side Javascript comms */
//				webSocket("/ws/monitor", io.io()).
//				context(UHTTPD.context("/app/(.*)").
					handle(new AllSelector(), sessions).
					
					/* Extension points */
					handle(new AllSelector(), router).
					
					/* Templated pages */
					handle("/index.html", tx -> {
						tx.response(tp.process(template(Web.class, "index.html")));
					}).
					
					/* Default handler */
					handle("/", (tx) -> {
						tx.redirect(Status.MOVED_PERMANENTLY, tx.contextPath().resolve("/index.html"));
					}).
					
					/* Templated pages */
					status(Status.NOT_FOUND, tx -> {
						tx.response("text/html", tp.process(template(Web.class, "404.html")));
					}).
					
					/* Default resource */
					withClasspathResources("/npm2mvn/(.*)", getClass().getClassLoader(), "npm2mvn/").
					withClasspathResources("/(.*)", getClass().getClassLoader(), "web/").
				
//					build()).
				
				/* Other server configuration */
				withHttpAddress("0.0.0.0").
				
				/* TODO: For some reason "View Source" in Firefox (13/10/23) fails if compression is on! */
				withoutCompression(). 
				
				build();

			httpd.start();
		}
		catch(IOException e) {
			throw new UncheckedIOException(e);
		}
		
	}
	

	@Override
	public void close() {
		httpd.close();
	}
	
	private TemplateModel fragHead(Transaction tx) {
		return decorate(tx, TemplateModel.ofResource(Web.class, "head.frag.html").
				list("css", (content) -> 
					cssModules(tx, content, Placement.HEAD)).
				list("javascript", (content) -> 
					javascriptModules(tx, content, Placement.HEAD)));
	}
	
	private TemplateModel fragBodyHead(Transaction tx) {
		return decorate(tx, TemplateModel.ofResource(Web.class, "bodyhead.frag.html").
				list("javascript", (content) -> 
					javascriptModules(tx, content, Placement.BODYHEAD)));
		}
	
	private TemplateModel fragBodyTail(Transaction tx) {
		return decorate(tx, TemplateModel.ofResource(Web.class, "bodytail.frag.html").
				list("javascript", (content) -> 
					javascriptModules(tx, content, Placement.BODYTAIL)));
	}

	private List<TemplateModel> javascriptModules(Transaction tx, String content, Placement placement) {
		
		var l = sortModules(Type.JAVASCRIPT, placement);
		
		return l.stream().
			map(mod -> TemplateModel.ofContent(content).
				variable("src", mod.webModule().uri())
		).toList();
	}

	private List<TemplateModel> cssModules(Transaction tx, String content, Placement placement) {
		
		var l = sortModules(Type.CSS, placement);
		return l.stream().map(mod -> TemplateModel.ofContent(content).
					variable("href", mod.webModule().uri())
		).toList();
	}

	private Collection<WebModuleHandle> sortModules(Type css, Placement placement) {
		// TODO need to recursively get, and all resolve module dependencies
		//
		List<WebModuleHandle> l = new ArrayList<>();
		l.addAll(Router.requires());
		l.addAll(l.stream().flatMap(r -> r.webModule().requires().stream()).toList());
		Collections.reverse(l);
		return new LinkedHashSet<>(l.stream().filter(m -> m.webModule().placement().equals(placement) && 
				   m.webModule().type().equals(css)).toList());
	}
	
	private TemplateModel decorateTemplate(Transaction tx, TemplateModel template) {
		State.get(false).map(session -> session.locale()).ifPresent(template::locale);
		template.include("web.head", () -> fragHead(tx));
		template.include("web.bodyhead", () -> fragBodyHead(tx));
		template.include("web.bodytail", () -> fragBodyTail(tx));
		decorate(tx, template);
		return template;
	}

	private TemplateModel decorate(Transaction tx, TemplateModel template) {
		template.variable("tx.path", tx::path);
		template.variable("tx.contextPath", () -> { 
			var str = tx.contextPath().toString();
			return str.equals("/") ? "" : str;
		});
		template.variable("tx.fullContextPath", tx::fullContextPath);
		template.variable("tx.fullPath", tx::fullPath);
		extensions.points(GlobalTemplateDecorator.class).forEach(dec -> {
			dec.apply(tx).decorate(template);
		});
		return template;
	}
}
