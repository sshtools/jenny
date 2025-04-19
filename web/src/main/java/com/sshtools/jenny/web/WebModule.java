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

import java.io.Closeable;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.sshtools.bootlace.api.DependencyGraph.Dependency;
import com.sshtools.bootlace.api.NodeModel;
import com.sshtools.uhttpd.UHTTPD;
import com.sshtools.uhttpd.UHTTPD.Handler;
import com.sshtools.uhttpd.UHTTPD.Transaction;

/**
 * TODO: Detect .min resources and automatically use them (when not specified as 'main' or 'style' or 'module'),
 * also the reverse.
 */
public final class WebModule implements NodeModel<WebModule> {
	
	public enum Mount {
		FILE, DIRECTORY, CONTENT, URL
	}

	public enum Type {
		IMPORT_MAP, CSS, JS, MODULE, IMPORTED, ANCILIARY
	}
	
	public enum Placement {
		HEAD, BODYHEAD, BODYTAIL
	}
	
	public interface WebModulesRef extends Closeable {
		WebModule[] modules();
	}
	
	public static WebModule css(String uri, Handler handler, WebModule... requires) {
		return of(uri, handler, Type.CSS, requires);
	}
	
	public static WebModule js(String uri, Handler handler, WebModule... requires) {
		return of(uri, handler, Type.JS, requires);
	}
	
	public static WebModule jsModule(String uri, Handler handler, WebModule... requires) {
		return of(uri, handler, Type.MODULE, requires);
	}
	
	public static WebModule of(String uri, Handler handler, Type type, WebModule... requires) {
		return new Builder().
				withUri(uri).
				withResources(WebModuleResource.of(uri, handler, type)).
				withRequires(requires).
				build();
	}
	
	public static WebModule jsModule(String uri, Class<?> base, String path, WebModule... requires) {
		return of(uri, base, path, Type.MODULE, requires);
	}
	
	public static WebModule js(String uri, Class<?> base, String path, WebModule... requires) {
		return of(uri, base, path, Type.JS, requires);
	}
	
	public static WebModule css(String uri, Class<?> base, String path, WebModule... requires) {
		return of(uri, base, path, Type.CSS, requires);
	}
	
	public static WebModule of(String uri, Class<?> base, String path, WebModule... requires) {
		return new Builder().
				withUri(uri).
				withResources(WebModuleResource.of(base, path)).
				withRequires(requires).
				build();
	}
	
	public static WebModule of(String uri, Class<?> base, String path, Type type, WebModule... requires) {
		return new Builder().
				withUri(uri).
				withResources(WebModuleResource.of(base, type, path)).
				withRequires(requires).
				build();
	}
	
	public final static class WebModuleResource {
	
		public final static class Builder {
			private Optional<Type> type = Optional.empty();
			private Optional<Placement> placement = Optional.empty();
			private ResourceRef ref;
			private Optional<Handler> handler = Optional.empty();
			private Optional<String> content = Optional.empty();
			
			public Builder withHandler(Handler handler) {
				this.handler = Optional.of(handler);
				return this;
			}
			
			public Builder withType(Type type) {
				this.type = Optional.of(type);
				return this;
			}
			
			public Builder withPlacement(Placement placement) {
				this.placement = Optional.of(placement);
				return this;
			}
			
			public Builder withContent(String content) {
				this.content = Optional.of(content);
				return this;
			}
			
			public Builder withResource(Class<?> resourceParent, String resource) {
				return withResource(new ResourceRef(resourceParent, resource));
			}
			
			public Builder withResource(ClassLoader loader, String resource) {
				return withResource(new ResourceRef(loader, resource));
			}
			
			public Builder withResource(String resource) {
				return withResource(new ResourceRef(resource));
			}
			
			public Builder withResource(ResourceRef ref) {
				this.ref = ref;
				return this;
			}
			
			public WebModuleResource build() {
				return new WebModuleResource(this); 
			}
		}
		
		public static WebModuleResource of(String resource) {
			return new Builder().withResource(resource).build();
		}
		
		public static WebModuleResource css(String resource) {
			return new Builder().withResource(resource).withType(Type.CSS).build();
		}
		
		public static WebModuleResource js(String resource) {
			return new Builder().withResource(resource).withType(Type.JS).build();
		}
		
		public static WebModuleResource jsModule(String resource) {
			return new Builder().withResource(resource).withType(Type.MODULE).build();
		}
		
		public static WebModuleResource of(Class<?> resourceParent, String resource) {
			return new Builder().withResource(resourceParent, resource).build();
		}
		
		public static WebModuleResource of(Class<?> resourceParent, Type type, String resource) {
			return new Builder().withResource(resourceParent, resource).withType(type).build();
		}
		
		public static WebModuleResource css(Class<?> resourceParent, String resource) {
			return new Builder().withResource(resourceParent, resource).withType(Type.CSS).build();
		}
		
		public static WebModuleResource js(Class<?> resourceParent, String resource) {
			return new Builder().withResource(resourceParent, resource).withType(Type.JS).build();
		}
		
		public static WebModuleResource jsModule(Class<?> resourceParent, String resource) {
			return new Builder().withResource(resourceParent, resource).withType(Type.MODULE).build();
		}
		
		public static WebModuleResource of(ResourceRef ref) {
			return new Builder().withResource(ref).build();
		}
		
		public static WebModuleResource css(ResourceRef ref) {
			return new Builder().withResource(ref).withType(Type.CSS).build();
		}
		
		public static WebModuleResource js(ResourceRef ref) {
			return new Builder().withResource(ref).withType(Type.JS).build();
		}
		
		public static WebModuleResource jsModule(ResourceRef ref) {
			return new Builder().withResource(ref).withType(Type.JS).build();
		}
		
		public static WebModuleResource js(String path, Handler handler) {
			return of(path, handler, Type.JS);
		}
		
		public static WebModuleResource css(String path, Handler handler) {
			return of(path, handler, Type.CSS);
		}
		
		public static WebModuleResource jsModule(String path, Handler handler) {
			return of(path, handler, Type.MODULE);
		}
		
		public static WebModuleResource of(String path, Handler handler, Type type) {
			return new Builder().
					withHandler(handler).
					withResource(new ResourceRef(path)).
					withType(type).
					build();
		}
		
		private final Optional<Placement> placement;
		private final Optional<Type> type;
		private final ResourceRef ref;		
		private final Optional<Handler> handler;
		private final Optional<String> content;
		private WebModule module;

		private WebModuleResource(WebModuleResource res, String uri) {
			this.placement = res.placement;
			this.type = res.type;
			this.handler = res.handler;
			this.content = res.content;
			this.module = res.module;
			this.ref = res.ref.translate(uri);
		}
		
		private WebModuleResource(Builder builder) {
			this.ref = builder.ref;
			if(ref == null && builder.handler.isEmpty() && builder.content.isEmpty()) {
				throw new IllegalStateException("Must either have a resource reference, content or a handler.");
			}
			this.placement = builder.placement;
			this.type = builder.type;
			this.handler = builder.handler;
			this.content = builder.content;
		}
		
		public ResourceRef ref() {
			return ref;
		}
		
		public String content() {
			return content.get();
		}
		
		public Optional<String> contentOr() {
			return content;
		}

		public Placement placement() {
			return placement.orElseGet(()-> type().equals(Type.CSS) ? Placement.HEAD : Placement.BODYTAIL);
		}

		public String uri() {
			if(module ==null)
				throw new IllegalStateException("Not attached to module.");
			return module.uri(this);
		}

		public Type type() {
			return type.orElseGet(() -> {
				if (ref == null) {
					throw new IllegalStateException(MessageFormat.format(
							"A web module resource backed by a `{0}` must have a specific `{1}`.",
							Handler.class.getName(), Type.class.getName()));
				} else {
					if(ref.path().toLowerCase().endsWith(".js")) {
						return Type.JS;
					}
					else if(ref.path().toLowerCase().endsWith(".css")) {
						return Type.CSS;
					}
					else  {
						return Type.ANCILIARY;
					}
				}
			});
		}

		public String scriptType() {
			var type = type();
			if(type.equals(Type.JS)) {
				return "text/javascript";
			}
			else if(type.equals(Type.MODULE)) {
				return "module";
			}
			else if(type.equals(Type.IMPORT_MAP)) {
				return "importmap";
			}
			else
				throw new IllegalStateException("Not a script type.");
		}

		public WebModuleResource translate(String uri) {
			return new WebModuleResource(this, uri);
		}
	}
		
	public final static class Builder {
		private String uri;
		private Optional<String> name = Optional.empty();
		private final Set<WebModule> requires = new LinkedHashSet<>();
		private List<WebModuleResource> resources = new ArrayList<>();
		private Optional<Mount> mount = Optional.empty();
		private Optional<ClassLoader> loader = Optional.empty();
		private Optional<ResourceRef> prefix = Optional.empty();
		
		public  Builder withName(String name) {
			this.name = Optional.of(name);
			return this;
		}
		
		public Builder withResources(WebModuleResource... resources) {
			this.resources.clear();
			return addResources(resources);
		}
		
		public Builder withResources(Collection<WebModuleResource> resources) {
			this.resources.clear();
			return addResources(resources);
		}
		
		public Builder addResources(WebModuleResource... resources) {
			return addResources(Arrays.asList(resources));
		}
		
		public Builder addResources(Collection<WebModuleResource> resources) {
			this.resources.addAll(resources);
			return this;
		}
		
		public Builder withRequires(WebModule... requires) {
			return withRequires(Arrays.asList(requires));
		}
		
		public Builder withRequires(Collection<WebModule> requires) {
			this.requires.addAll(requires);
			return this;
		}
		
		public Builder withUri(String uri) {
			if(uri.endsWith("/"))
				asDirectory();
			this.uri = uri;
			return this;
		}

		public Builder withUrl(String url) {
			try {
				return withUrl(URI.create(url).toURL());
			} catch (MalformedURLException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		public Builder withUrl(URL url) {
			asUrl();
			try {
				this.uri = url.toURI().toString();
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
			return this;
		}
		
		public Builder withLoader(ClassLoader loader) {
			this.loader = Optional.of(loader);
			return this;
		}
		
		public Builder withPrefix(String prefix) {
			return withPrefix(new ResourceRef(prefix));
		}
		
		public Builder withPrefix(ResourceRef prefix) {
			this.prefix = Optional.of(prefix);
			return this;
		}
		
		public Builder asFile() {
			return as(Mount.FILE);
		}
		
		public Builder asUrl() {
			return as(Mount.URL);
		}
		
		public Builder asDirectory() {
			return as(Mount.DIRECTORY);
		}
		
		public Builder asDirectory(Class<?> base) {
			return asDirectory(new ResourceRef(base));
		}
		
		public Builder asDirectory(Class<?> base, String prefix) {
			return asDirectory(new ResourceRef(base, prefix));
		}
		
		public Builder asDirectory(ResourceRef prefix) {
			return as(Mount.DIRECTORY).withPrefix(prefix).withLoader(prefix.loader());
		}
		
		public Builder as(Mount mount) {
			this.mount = Optional.of(mount);
			return this;
		}
		
		
		public WebModule build() {
			return new WebModule(this); 
		}
		
	}
	
	private final static char[] ESC_CHARS = { '\\', '*', '+', '?', '[', '{', '.', '(', ')', '^', '$', '|' };
	
	private static String escapeLiteral(String path) {
		for(var c : ESC_CHARS) {
			path = path.replace(String.valueOf(c), "\\" + c);
		}
		return path;
	}

	private final String pattern;
	private final List<WebModuleResource> resources;
	private final Set<WebModule> requires;
	private final String name;
	private final Optional<Handler> handler;
	private final Mount mount;
	private final String uri;
	private final Optional<ResourceRef> prefix;
	private Optional<ClassLoader> loader;
	
	private WebModule(Builder builder) {
		this.prefix = builder.prefix;
		this.loader = builder.loader;
		this.resources = Collections.unmodifiableList(new ArrayList<>(builder.resources).stream().peek(f-> {
			if( f.ref.loader() != null && loader.isPresent()) 
				throw new IllegalArgumentException("Loader is set on the " +WebModuleResource.class.getName() + " , so should not be set on any " + ResourceRef.class.getName() + ".");
		}).toList());
		this.resources.forEach(r -> r.module = this);
		
		this.mount = builder.mount.orElseGet(() -> builder.uri == null ? Mount.CONTENT : builder.uri.endsWith("/") ? Mount.DIRECTORY : Mount.FILE);
		
		if(mount == Mount.FILE && resources.size() != 1) {
			throw new IllegalStateException(MessageFormat.format("Mount `{0}` must specify exactly on resource to map to, there are {1}", Mount.FILE, resources.size()));
		}
		
		var uri = builder.uri;
		if(uri != null && !uri.startsWith("/") ) {
			if(mount == Mount.URL && !uri.startsWith("http://") && !uri.startsWith("https://"))  {
				uri = "https://" + uri;
			}
			else if(mount != Mount.URL) {
				uri = "/" + uri;
			}
		}
		
		if(mount == Mount.DIRECTORY || mount == Mount.URL) {
			if(!uri.endsWith("/")) {
				uri += "/";
			}
		}
		else {
			/* TODO map to .min.js/.min.css or .js/.css depending on compression setting */
			//	var firstRes = resources.get(0);
		}
		
		var pattern = uri == null ? null : escapeLiteral(Objects.requireNonNull(uri));
		if(mount == Mount.DIRECTORY) {
			pattern += "(.*)";
		}
		
		this.pattern = pattern;
		this.uri = uri;
		this.name = builder.name.orElse(this.pattern);
		this.requires = Collections.unmodifiableSet(new LinkedHashSet<>(builder.requires));
		
		if(mount == Mount.DIRECTORY) {
			/* TODO is this really the right condition? prefix.isPresent() maybe better */
			if(this.loader.isPresent()) {
				/**
				 * Web module groups any arbitrary resource on the classpath
				 * under the URI
				 */
				var loader = this.loader.get();
				handler = Optional.of(new Handler() {
					
					@Override
					public void get(Transaction req) throws Exception {
						var rel = req.match(0);
						String fullPath;
						if(prefix.isPresent()) {
							fullPath = prefix.get().fullpath() + "/" + rel;  
						}
						else {
							fullPath = WebModule.this.uri.substring(1) + "/" + rel;	
						}
						UHTTPD.classpathResource(loader, fullPath).get(req);
					}
				});
			}
			else {
				/* Web module groups a list a WebModuleResource under one
				 * URI and uses the path of each resource to identify the
				 * resource
				 */
				handler = Optional.of(new Handler() {
					@Override
					public void get(Transaction req) throws Exception {
						var rel = req.match(0);
						for(var resource : resources) {
							if(resource.ref != null) {
								
								if(rel.equals(resource.ref.path())) {
									if(resource.handler.isPresent()) {
										resource.handler.get().get(req);
									}
									else {
										resource.ref.handler().get(req);
									}
									return;
								}
							}
						}
					}
				});
			}
		}
		else if(mount == Mount.CONTENT) {
			this.handler = Optional.of((c) -> {});
		}
		else if(mount == Mount.URL) {
			this.handler = Optional.empty();
		}
		else {
			/**
			 * Web module is a single mapping from a uri to a WebModuleResource
			 */
			var res = resources.get(0);
			if(res.handler.isPresent()) {
				this.handler = Optional.of(res.handler.get());
			}
			else {
				this.handler = Optional.of(res.ref.handler());
			}
		}
	}
	
	private String uri(WebModuleResource webModuleResource) {
		if(mount == Mount.FILE) {
			return uri;
		}
		else {
			if(this.loader.isPresent()) {
				if(webModuleResource.ref.loader() == null && webModuleResource.ref.base() == null) {
					return uri + webModuleResource.ref.path();
				}
				else {
					return "/" + webModuleResource.ref.path();
				}
			}
			else {				
				return uri + webModuleResource.ref.path();
			}
		}
	}
	
	public Optional<ResourceRef> prefix() {
		return prefix;
	}

	public Set<WebModule> requires() {
		return requires;
	}
	
	public Handler handler() {
		return handler.orElseThrow(() -> new IllegalStateException("Module does not have a handler."));
	}
	
	public Mount mount() {
		return mount;
	}

	public List<WebModuleResource> resources() {
		return resources;
	}

	public String pattern() {
		return pattern;
	}
		
	@Override
	public String name() {
		return name;
	}
		
	@Override
	public void dependencies(Consumer<Dependency<WebModule>> dep) {
		doDependencies(dep, this);
	}

	@Override
	public String toString() {
		return "WebModule [pattern=" + pattern + ", resources=" + resources + ", requires=" + requires + ", name="
				+ name + ", handler=" + handler + ", mount=" + mount + ", uri=" + uri + ", loader=" + loader + "]";
	}
	
	private void doDependencies(Consumer<Dependency<WebModule>> dep, WebModule upstream) {
		dep.accept(new Dependency<WebModule>(upstream, this));
		requires().forEach(mod -> {
			mod.doDependencies(dep, this);
		});
	}

	public boolean hasHandler() {
		return handler.isPresent();
	}
	
}