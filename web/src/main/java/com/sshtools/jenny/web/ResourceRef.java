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

import static com.sshtools.uhttpd.UHTTPD.classpathResource;

import com.sshtools.uhttpd.UHTTPD.Handler;

public record ResourceRef(Class<?> base, ClassLoader loader, String path) {

	public ResourceRef(Class<?> base) {
		this(base, null);
	}
	
	public ResourceRef(Class<?> base, String path) {
		this(base, base.getClassLoader(), path);
	}
	
	ResourceRef(String path) {
		this(null, null, path);
	}
	
	ResourceRef(ClassLoader loader, String path) {
		this(null, loader, path);
	}
	
	public Handler handler() {
		if (base() == null) {
			if(loader == null)
				throw new IllegalStateException("Must have a base() or a loader().");
			return classpathResource(loader(), path());
		} else {
			return classpathResource(base(), path());
		}
	}
	
	public ResourceRef translate(String uri) {
		return new ResourceRef(base, loader, uri + "/" + path);
	}

	public String fullpath() {
		var p = new StringBuilder();
		if(base != null) {
			p.append(base.getPackage().getName().replace('.', '/'));
		}
		if(path != null) {
			if(p.length() > 0)
				p.append('/');
			p.append(path);
		}
		return p.toString();
	}
}
