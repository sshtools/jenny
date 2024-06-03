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
package com.sshtools.jenny.product;

import java.util.Optional;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;

public final class Product implements Plugin {
	
	private Info info;

	public record Info(String app, String vendor) {
	}
	
	public final static class Builder {

		private Optional<String> app = Optional.empty();
		private Optional<Class<?>> appClass = Optional.empty(); 
		private Optional<String> vendor = Optional.empty();
		
		public Builder withApp(Class<?> appClass) {
			this.appClass = Optional.of(appClass);
			return this;
		} 
		
		public Builder withApp(String app) {
			this.app = Optional.of(app);
			return this;
		}
		
		public Builder withVendor(String vendor) {
			this.vendor = Optional.of(vendor);
			return this;
		}	
		
		public Info build() {
			return new Info(
				app.or(() -> appClass.map(Class::getName)).orElseThrow(() -> new IllegalStateException("'App' must be set, either a class or short ID.")),
				vendor.orElse("Unknown")
			);
		}
	}
	
	public Product() {
	}
	
	public Product(Info info) {
		info(info);
	}

	@Override
	public void afterOpen(PluginContext context) throws Exception {
	}
	
	public void info(Info info) {
		if(this.info != null)
			throw new IllegalStateException("Product already registered.");
		this.info = info; 
	}

	public Info info() {
		return info;
	}

}
