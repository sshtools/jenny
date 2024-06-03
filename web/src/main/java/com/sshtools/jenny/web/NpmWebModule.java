package com.sshtools.jenny.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.json.Json;

import com.sshtools.bootlace.api.GAV;
import com.sshtools.jenny.web.WebModule.Type;
import com.sshtools.jenny.web.WebModule.WebModuleResource;

public final class NpmWebModule {
	
	public enum Compression {
		AUTO, NONE, MINIFY
	}
	
	public static String toNpm(GAV gav) {
		if(gav.groupId().equals("npm")) {
			return gav.artifactId();
		}
		else {
			return "@" + gav.groupId().substring(4) + "/" + gav.artifactId();
		}
	}
	
	public final static class Builder {
		private Optional<String> main = Optional.empty();
		private Optional<String> module = Optional.empty();
		private Optional<String> style = Optional.empty();
		private Optional<Type> type = Optional.empty();
		private Optional<String> sass = Optional.empty();
		private Optional<ClassLoader> loader = Optional.empty();
		private Optional<GAV> gav = Optional.empty();
		private List<WebModuleResource> resources = new ArrayList<WebModule.WebModuleResource>();
		private final Set<WebModule> requires = new LinkedHashSet<>();
		private Compression compression = Compression.AUTO;
		
		public  Builder withCompression(Compression compression) {
			this.compression = compression;
			return this;
		}
		
		public Builder withRequires(WebModule... requires) {
			return withRequires(Arrays.asList(requires));
		}
		
		public Builder withRequires(Collection<WebModule> requires) {
			this.requires.addAll(requires);
			return this;
		}
		
		public Builder withGAV(String... parts) {
			return withGAV(GAV.ofParts(parts));
		}
		
		public Builder withGAV(GAV gav) {
			this.gav = Optional.of(gav);
			return this;
		}
		
		public Builder withPackage(String name) {
			if(name.startsWith("@")) {
				var idx = name.indexOf('/');
				return withPackage(name.substring(1, idx), name.substring(idx + 1));
			}
			else {
				return withGAV(GAV.ofParts("npm", name));
			}
		}
		
		public Builder withPackage(String scope, String name) {
			return withGAV(GAV.ofParts("npm." + scope, name));
		}
		
		public Builder withClass(Class<?> clazz) {
			return withLoader(clazz.getClassLoader());
		}
		
		public Builder withLoader(ClassLoader loader) {
			this.loader = Optional.of(loader);
			return this;
		}
		
		public Builder withMain(String main) {
			this.main = Optional.of(main);
			return this;
		}
		
		public Builder withModule(String module) {
			this.module = Optional.of(module);
			return this;
		}
		
		public Builder withStyle(String style) {
			this.style = Optional.of(style);
			return this;
		}
		
		public Builder withType(Type type) {
			this.type = Optional.of(type);
			return this;
		}
		
		public Builder withSass(String sass) {
			this.sass = Optional.of(sass);
			return this;
		}
		
		public Builder addResources(WebModuleResource... resources) {
			return addResources(Arrays.asList(resources));
		}
		
		public Builder addResources(Collection<WebModuleResource> resources) {
			this.resources.addAll(resources);
			return this;
		}
		
		public Builder withResources(WebModuleResource... resources) {
			return withResources(Arrays.asList(resources));
		}
		
		public Builder withResources(Collection<WebModuleResource> resources) {
			this.resources.clear();
			return addResources(resources);
		}
		
		public WebModule build() {
			var loader = this.loader.orElseThrow(() -> new IllegalStateException("Loader must be provided."));
			var gav = this.gav.orElseThrow(() -> new IllegalStateException("GAV must be provided."));
			try {
				var res = "META-INF/LOCATOR." + gav.groupId() + "." + gav.artifactId() + ".properties";
				var en = loader.getResources(res);
				var props = new Properties();
				if(en.hasMoreElements()) {
					var url = en.nextElement();
					try(var in = url.openStream()) {
						props.load(in);
					}
				}
				else
					throw new NoSuchFileException(res);
				
				
				var cat = "META-INF/CATALOGUE." + gav.groupId() + "." + gav.artifactId() + ".list";
				var items = new HashSet<String>();
				try(var rdr = new BufferedReader(new InputStreamReader(loader.getResourceAsStream(cat)))) {
					String line;
					while( ( line = rdr.readLine() ) != null) {
						items.add(line.trim());	
					}
				}
				
				var main = this.main.orElseGet(() -> locateBest(props, items, "main", "js"));
				var style = this.style.orElseGet(() -> locateBest(props, items, "style", "css"));
				var module = this.module.orElseGet(() -> locateBest(props, items, "module", "js"));
				var type = this.type.orElseGet(() -> {
					try {
						return Type.valueOf(props.getProperty("type").toUpperCase());
					}
					catch(Exception e) {
						return module != null ? Type.MODULE : Type.JS;
					}
				});
				var resource = props.getProperty("resource");
				var useModule = type == Type.MODULE || type == Type.IMPORTED;
				
				var bldr = new WebModule.Builder();
				bldr.withName(toNpm(gav));
				bldr.withLoader(loader);
				bldr.addResources(this.resources);
				bldr.withUri("/" + resource);
				bldr.withRequires(this.requires);
				bldr.asDirectory();
				
				var packages = props.getProperty("resource") + "/package.json";
				var pkgres = loader.getResource(packages);
				if(pkgres != null && useModule) {
					try(var json = Json.createReader(pkgres.openStream())) {
						var mfest = json.readObject();
						var deps = mfest.getJsonObject("dependencies");
						if(deps != null) {
							for(var dep : deps.keySet()) {
								String group;
								String art;
								var idx = dep.indexOf('/');
								if(idx == -1) {
									group = "npm";
									art = dep;
								}
								else {
									group = "npm." + dep.substring(1, idx);
									art = dep.substring(idx + 1);
								}
								
								bldr.withRequires(new NpmWebModule.Builder().
									withLoader(loader).
									withGAV(GAV.ofParts(group, art)).
									withType(type).
									build());
							}
						}
					}
				}
				
				if(type == Type.IMPORTED) {
					bldr.addResources(new WebModuleResource.Builder().
						withType(type).
						withResource(normalize(module)).
						build());
				}
				else if(module != null && type == Type.MODULE ) {
					bldr.addResources(new WebModuleResource.Builder().
						withType(type).
						withResource(normalize(module)).
						build());
				}
				else if(main != null && ( type == Type.JS || type == Type.MODULE ) ) {
					bldr.addResources(new WebModuleResource.Builder().
						withType(type).
						withResource(normalize(main)).
						build());
				}
				
				if(style != null) {
					bldr.addResources(new WebModuleResource.Builder().
						withType(Type.CSS).
						withResource(normalize(style)).
						build());
				}
				
				return bldr.build();
				
			} catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}
		
		private String locateBest(Properties props, Set<String> cat, String key, String ext) {
			var path = props.getProperty(key);
			if(path == null)
				return null;
			
			var base = stripExtensions(path, ext);
			String sel = null;
			
			switch(compression) {
			case AUTO:
				sel = base + ".min." + ext;
				if(cat.contains(sel)) {
					return sel;
				}
				sel = base + "." + ext;
				if(cat.contains(sel)) {
					return sel;
				}
				sel = base;
				if(cat.contains(sel)) {
					return sel;
				}
				break;
			case NONE:
				sel = base + "." + ext;
				if(cat.contains(sel)) {
					return sel;
				}
				sel = base;
				if(cat.contains(sel)) {
					return sel;
				}
				throw new IllegalArgumentException(MessageFormat.format("Resource with path ''{0}'' does not exist with non-compressed extension ''{1}''.", path, ext));
			case MINIFY:
				sel = base + ".min." + ext;
				if(cat.contains(sel)) {
					return sel;
				}
				throw new IllegalArgumentException(MessageFormat.format("Resource with path ''{0}'' does not exist with compressed extension ''.min.{1}''.", path, ext));
			default:
				throw new UnsupportedOperationException();
			}
			
			return path;
			
		}
		
		private String stripExtensions(String path, String ext) {
			var idx = path.indexOf("." + ext);
			if(idx != -1) {
				path = path.substring(0, idx);
			}
			idx = path.indexOf(".min");
			if(idx != -1) {
				path = path.substring(0, idx);
			}
			return path;
		}
		
	}
	
	public final static WebModule of(ClassLoader loader, GAV gav, WebModule... requires) {
		return new Builder().withLoader(loader).withGAV(gav).withRequires(requires).build();
	}

	public final static WebModule of(Class<?> base, GAV gav, WebModule... requires) {
		return of(base.getClassLoader(), gav, requires);
	}
	
	private static String normalize(String path) {
		return Paths.get(path).normalize().toString();
	}
}
