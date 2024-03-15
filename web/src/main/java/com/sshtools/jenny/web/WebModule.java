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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.sshtools.bootlace.api.DependencyGraph.Dependency;
import com.sshtools.bootlace.api.NodeModel;
import com.sshtools.uhttpd.UHTTPD;
import com.sshtools.uhttpd.UHTTPD.Handler;

public final class WebModule implements NodeModel<WebModule> {

	public enum Type {
		CSS, JAVASCRIPT
	}
	
	public enum Placement {
		HEAD, BODYHEAD, BODYTAIL
	}
	
	public interface WebModulesRef extends Closeable {
		WebModule[] modules();
	}
	
	public static WebModule of(String uri, Handler handler, WebModule... requires) {
		return new Builder().
				withUri(uri).
				withHandler(handler).
				withRequires(requires).
				build();
	}
	
	public static WebModule of(String uri, Class<?> base, String path, WebModule... requires) {
		return new Builder().
				withUri(uri).
				withResource(base, path).
				withRequires(requires).
				build();
	}
	
	public final static class Builder {
		private String uri;
		private Optional<Type> type = Optional.empty();
		private Optional<Placement> placement = Optional.empty();
		private ResourceRef ref;
		private final Set<WebModule> requires = new LinkedHashSet<>();
		private Optional<Handler> handler = Optional.empty();
		
		public Builder withHandler(Handler handler) {
			this.handler = Optional.of(handler);
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
			this.uri = uri;
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
		
		public Builder withResource(Class<?> resourceParent, String resource) {
			return withResource(new ResourceRef(resourceParent, resource));
		}
		
		public Builder withResource(ResourceRef ref) {
			this.ref = ref;
			return this;
		}
		
		public WebModule build() {
			return new WebModule(this); 
		}
		
	}
	
	private final String uri;
	private final Handler handler;
	private final Placement placement;
	private final Type type;
	private final Set<WebModule> requires;
	
	private WebModule(Builder builder) {
		this.uri = Objects.requireNonNull(builder.uri);
		var ref = builder.ref;
		if(ref == null && builder.handler.isEmpty()) {
			throw new IllegalStateException("Must either have a resource reference or a handler.");
		}
		this.type = builder.type.orElseGet(()-> getType(ref));
		this.placement = builder.placement.orElseGet(()-> type.equals(Type.CSS) ? Placement.HEAD : Placement.BODYTAIL);
		this.requires = Collections.unmodifiableSet(new LinkedHashSet<>(builder.requires));
		this.handler = builder.handler.orElseGet(() -> UHTTPD.classpathResource(ref.parent(), ref.path()));
	}

	public Set<WebModule> requires() {
		return requires;
	}

	public Handler handler() {
		return handler;
	}

	public String uri() {
		return uri;
	}

	public Placement placement() {
		return placement;
	}

	public Type type() {
		return type;
	}

//	String uri();
//
//	Type type();
//
//	public static WebModule of(String uri, Type type) {
//		return new WebModule() {
//			@Override
//			public String uri() {
//				return uri;
//			}
//
//			@Override
//			public Type type() {
//				return type;
//			}
//		};
//	}

	private Type getType(ResourceRef ref) {
		if(ref == null) {
			return uri.toLowerCase().endsWith(".js") ? Type.JAVASCRIPT : Type.CSS;
		}
		else {
			return ref.path().toLowerCase().endsWith(".js") ? Type.JAVASCRIPT : Type.CSS;
		}
	}

	@Override
	public String toString() {
		return "WebModule [uri=" + uri + ", handler=" + handler + ", placement=" + placement + ", type=" + type
				+ ", requires=" + requires + "]";
	}
	
		
	@Override
	public String name() {
		return uri();
	}
		
	@Override
	public void dependencies(Consumer<Dependency<WebModule>> dep) {
		dep.accept(new Dependency<WebModule>(this, this));
		requires().forEach(mod -> {
			dep.accept(new Dependency<WebModule>(this, mod));
		});
	}
	
	
}