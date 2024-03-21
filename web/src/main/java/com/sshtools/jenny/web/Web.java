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

import static com.sshtools.tinytemplate.Templates.TemplateModel.ofContent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.sshtools.bootlace.api.ConfigResolver.Scope;
import com.sshtools.bootlace.api.DependencyGraph;
import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.api.XPoints;
import com.sshtools.jenny.config.Config;
import com.sshtools.jenny.config.Config.Handle;
import com.sshtools.jenny.web.Router.RouterBuilder;
import com.sshtools.jenny.web.WebModule.Placement;
import com.sshtools.jenny.web.WebModule.Type;
import com.sshtools.jenny.web.WebModule.WebModulesRef;
import com.sshtools.tinytemplate.Templates.Logger;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.Templates.TemplateProcessor;
import com.sshtools.uhttpd.UHTTPD;
import com.sshtools.uhttpd.UHTTPD.AllSelector;
import com.sshtools.uhttpd.UHTTPD.NCSALoggerBuilder;
import com.sshtools.uhttpd.UHTTPD.RootContext;
import com.sshtools.uhttpd.UHTTPD.RootContextBuilder;
import com.sshtools.uhttpd.UHTTPD.Status;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public final class Web implements Plugin {
	
	final static Log LOG = Logs.of(WebLog.WEB);

	public static final String TX_BASE_HREF = "base.href";
	public static final String TX_BASE_TARGET = "base.target";
	
	public static void setBase(Transaction tx) {
		tx.attr(Web.TX_BASE_HREF, (  tx.secure() ? "https://" : "http://" ) + tx.host() + tx.contextPath());
	}
	
	private final TemplateProcessor tp;
	private RootContext httpd;
	private final XPoints extensions;
	private final Router router;
	private final ScheduledExecutorService queue;
	private final Map<String, WebModule> modules = new ConcurrentHashMap<>();

	private com.sshtools.bootlace.api.RootContext rootContext;
	
	public Web() {
		this(new NpmPackageGlobalTemplateDecorator());
	}
	
	public Web(GlobalTemplateDecorator npmDecorator) {
		queue = new LoggedExecutorService(Executors.newScheduledThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "GlobalUIQueue");
			}
		}));
		
		/* Template processing */
		tp = new TemplateProcessor.Builder().
			withLogger(new Logger() {
				final static Log LOG = Logs.of(WebLog.TEMPLATES);
				
				@Override
				public void debug(String message, Object... args) {
					if(LOG.debug())
						LOG.warning(message, args);
				}
				
				@Override
				public void warning(String message, Object... args) {
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
	
	@Override
	public void afterOpen(PluginContext context) {
		this.rootContext = context.root();
		
		
		/* Main server loop */
		try {
			var sessions = UHTTPD.sessionCookies().build();
			
			var bldr = UHTTPD.server().
				/* Client side Javascript comms */
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
				get("/npm2mvn/(.*)", this::npmResource).
				withClasspathResources("/npm2mvn/(.*)", getClass().getClassLoader(), "npm2mvn/").
				withClasspathResources("/(.*)", getClass().getClassLoader(), "web/");
			
			configureServer(bldr);
				
			httpd = bldr.build();

			httpd.start();
			
			var webConfig = getWebConfig();
			webConfig.ini().getOr("port-info").ifPresent(pi -> {
				try(var out = new PrintWriter(Files.newBufferedWriter(Paths.get(pi)), true)) {
					httpd.httpPort().ifPresent(p -> out.println("http.port=" + p));
					httpd.httpsPort().ifPresent(p -> out.println("https.port=" + p));
				}
				catch(IOException ioe) {
					throw new UncheckedIOException(ioe);
				}
			});
			httpd.httpPort().ifPresent(p -> LOG.info("Listening on {0} for HTTP", p));
			httpd.httpsPort().ifPresent(p -> LOG.info("Listening on {0} for HTTPS", p));
		}
		catch(IOException e) {
			throw new UncheckedIOException(e);
		}
		
	}

	@Override
	public void close() {
		httpd.close();
	}
	
	public XPoints extensions() {
		return extensions;
	}

	public ScheduledExecutorService globalUiQueue() {
		return queue;
	}
	
	public WebModulesRef modules(WebModule... modules) {
		var l = new ArrayList<Route>();
		var m = new HashSet<String>();
		addModules(l, m, modules);
		return new WebModulesRef() {
			@Override
			public void close() throws IOException {
				l.forEach(Route::close);
				m.forEach(k -> Web.this.modules.remove(k));
			}

			@Override
			public WebModule[] modules() {
				return modules;
			}
		};
	}
	
	public TemplateProcessor processor() {
		return tp;
	}

	public TemplateModel require(TemplateModel template, WebModulesRef modules) {
		for(var mod : modules.modules())
			Router.requires(mod);
		return template;
		
	}
	
	public Router router() {
		return router;
	}
	
	public TemplateModel template(Class<?> parent, String relPath) {
		if(LOG.debug()) {
			LOG.debug("Loading template ''{0}'' with ''{1}'' as the base. The classloader is {2}", relPath, parent.getName(), parent.getClassLoader());
		}
		return decorateTemplate(Transaction.get(), TemplateModel.ofResource(parent, relPath));
	}
	
	public TemplateModel template(String content) {
		return decorateTemplate(Transaction.get(), TemplateModel.ofContent(content));
	}
	
	public TemplateModel template(String path, ClassLoader loader) {
		if(LOG.debug()) {
			LOG.debug("Loading template ''{0}'' with classloader is {1}", path, loader);
		}
		
		return decorateTemplate(Transaction.get(), TemplateModel.ofResource(path, loader));
	}

	public TemplateProcessor templateProcessor() {
		return tp;
	}

	private void addModules(ArrayList<Route> l, HashSet<String> m, WebModule... modules) {
		for(var module : modules) {
			if(!this.modules.containsKey(module.name())) {
				var hndl = router().route().
					handle(module.uri().replace(".", "\\."), module.handler()).
					build();
				l.add(hndl);
				this.modules.put(module.name(), module);
				m.add(module.name());
				addModules(l, m, module.requires().toArray(new WebModule[0]));
			}
		}
	}
	

	private void configureServer(RootContextBuilder bldr) {
		var webConfig = getWebConfig();
		
		var http = webConfig.ini().sectionOr("http");
		http.ifPresent(cfg -> {
			bldr.withHttp(cfg.getIntOr("port", 8080));
			bldr.withHttpAddress(cfg.getOr("address", "::"));
		});
		
		var https = webConfig.ini().sectionOr("https");
		https.ifPresent(cfg -> {
			bldr.withHttps(cfg.getIntOr("port", 8443));
			bldr.withHttpsAddress(cfg.getOr("address", "::"));
			cfg.getOr("key-password").ifPresent(kp -> bldr.withKeyPassword(kp.toCharArray()));
			cfg.getOr("keystore-file").ifPresent(ks -> bldr.withKeyStoreFile(Paths.get(ks)));
			cfg.getOr("keystore-password").ifPresent(kp -> bldr.withKeyPassword(kp.toCharArray()));
			cfg.getOr("keystore-type").ifPresent(kp -> bldr.withKeyStoreType(kp));
		});
		
		webConfig.ini().sectionOr("tuning").ifPresent(cfg -> {
			if(!cfg.getBooleanOr("compression", true)) {
				bldr.withoutCompression();
			}
		});
		
		webConfig.ini().sectionOr("ncsa").ifPresent(cfg -> {
			bldr.withLogger(new NCSALoggerBuilder().
					withAppend(cfg.getBooleanOr("append", true)).
					withDirectory(Paths.get(cfg.getOr("directory", System.getProperty("user.dir") + File.separator + "logs"))).
					withExtended(cfg.getBooleanOr("extended", true)).
					withServerName(cfg.getBooleanOr("server-name", false)).
					withFilenamePattern(cfg.getOr("pattern", "access_log_%d.log")).
					withFilenameDateFormat(cfg.getOr("date-format", "ddMM")).
					build());
		});
	}

	private Handle getWebConfig() {
		var config = PluginContext.$().plugin(Config.class);
		var webConfig = config.config(this, Scope.SYSTEM);
		return webConfig;
	}
	
	private List<TemplateModel> cssModules(Transaction tx, String content, Placement placement) {
		
		var l = sortModules(Type.CSS, placement);
		return l.stream().map(mod -> TemplateModel.ofContent(content).
					variable("href", mod.uri())
		).toList();
	}
	
	private TemplateModel decorate(Transaction tx, TemplateModel template) {
		template.variable("tx.path", tx::path);
		template.variable("tx.contextPath", () -> { 
			var str = tx.contextPath().toString();
			return str.equals("/") ? "" : str;
		});
		template.variable("tx.fullContextPath", tx::fullContextPath);
		template.variable("tx.fullPath", tx::fullPath);
		
		var points = extensions.points(GlobalTemplateDecorator.class);
		points.forEach(dec -> {
			dec.apply(tx).decorate(template);
		});
		return template;
	}
	
	private TemplateModel decorateTemplate(Transaction tx, TemplateModel template) {
		WebState.get(false).map(session -> session.locale()).ifPresent(template::locale);
		template.include("web.head", () -> fragHead(tx));
		template.include("web.bodyhead", () -> fragBodyHead(tx));
		template.include("web.bodytail", () -> fragBodyTail(tx));
		decorate(tx, template);
		return template;
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

	private TemplateModel fragHead(Transaction tx) {
		var templ = TemplateModel.ofResource(Web.class, "head.frag.html").
				list("css", (content) -> 
					cssModules(tx, content, Placement.HEAD)).
				list("javascript", (content) -> 
					javascriptModules(tx, content, Placement.HEAD));
		
		Optional<String> baseHref = tx.attrOr(TX_BASE_HREF);
		Optional<String> baseTarget = tx.attrOr(TX_BASE_TARGET);
		if(baseHref.isPresent() || baseTarget.isPresent()) {
			templ.object("base", (content) -> 
				ofContent(content)
					.variable("href", baseHref.orElse(""))
					.variable("target", baseTarget.orElse(""))
			);
		}
		
		return decorate(tx, templ);
	}

	private List<TemplateModel> javascriptModules(Transaction tx, String content, Placement placement) {
		
		var l = sortModules(Type.JAVASCRIPT, placement);
		
		return l.stream().
			map(mod -> TemplateModel.ofContent(content).
				variable("src", mod.uri())
		).toList();
	}
	
	private void npmResource(Transaction tx) {
		rootContext.globalResource(tx.match(0)).ifPresent(res -> {
			try {
				UHTTPD.urlResource(res).get(tx);
			} catch (Exception e) {
				LOG.error("Failed to serve static npm resource.", e);
			}
		});
	}

	private Collection<WebModule> sortModules(Type css, Placement placement) {
		/* The complete list */
		List<WebModule> l = new ArrayList<>();
		l.addAll(Router.requires());
		l.addAll(l.stream().flatMap(r -> r.requires().stream()).toList());
		
		/* Topological DAG sort */
		l =  new DependencyGraph<>(l).getTopologicallySorted();
		
		return new LinkedHashSet<>(l.stream().filter(m -> m.placement().equals(placement) && 
				   m.type().equals(css)).toList().reversed());
	}
}
