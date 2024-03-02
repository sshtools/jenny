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

import com.sshtools.uhttpd.UHTTPD;
import com.sshtools.uhttpd.UHTTPD.Handler;

public final class WebModule {

	public enum Type {
		CSS, JAVASCRIPT
	}
	
	public enum Placement {
		HEAD, BODYHEAD, BODYTAIL
	}
	
	public interface WebModuleHandle extends Closeable {
		WebModule webModule();
	}
	
	public final static class Builder {
		private String uri;
		private Optional<Type> type = Optional.empty();
		private Optional<Placement> placement = Optional.empty();
		private ResourceRef ref;
		private final Set<WebModuleHandle> requires = new LinkedHashSet<>();
		
		public Builder withRequires(WebModuleHandle... requires) {
			return withRequires(Arrays.asList(requires));
		}
		
		public Builder withRequires(Collection<WebModuleHandle> requires) {
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
	private final ResourceRef ref;
	private final Placement placement;
	private final Type type;
	private final Set<WebModuleHandle> requires;
	
	private WebModule(Builder builder) {
		this.uri = Objects.requireNonNull(builder.uri);
		this.ref = Objects.requireNonNull(builder.ref);
		this.type = builder.type.orElseGet(()-> ref.path().toLowerCase().endsWith(".js") ? Type.JAVASCRIPT : Type.CSS);
		this.placement = builder.placement.orElseGet(()-> type.equals(Type.CSS) ? Placement.HEAD : Placement.BODYTAIL);
		this.requires = Collections.unmodifiableSet(new LinkedHashSet<>(builder.requires));
	}
	
	public Set<WebModuleHandle> requires() {
		return requires;
	}

	public Handler resource() {
		return UHTTPD.classpathResource(ref.parent(), ref.path());
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
}