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
package com.sshtools.jenny.config;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.jenny.api.ApiLog;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jini.INI;
import com.sshtools.jini.INIReader;
import com.sshtools.jini.INIReader.MultiValueMode;
import com.sshtools.jini.INIWriter;

public class Config implements Plugin {
	
	private final static Log LOG = Logs.of(ApiLog.CONFIG);

	public enum Scope {
		USER, VENDOR
	}

	public interface Handle {

		INI ini();

		void store();
	}

	private record Key(Plugin plugin, Scope scope) {
	}

	private final Map<Key, Handle> config = new ConcurrentHashMap<>();
	private URLClassLoader bundleLoader;
	private final ResourceBundle emptyBundle;
	
	public Config() {
		try {
			emptyBundle = new PropertyResourceBundle(new StringReader(""));
		} catch (IOException e) {
			throw new IllegalStateException("Impossible.");
		}
	}
	
	@Override
	public void afterOpen(PluginContext context) throws Exception {
		bundleLoader = new URLClassLoader(new URL[] { resolveVendorDir().toUri().toURL() });
		Plugin.super.afterOpen(context);
	}

	public ResourceBundle bundle(Plugin plugin, Locale locale) {
		try {
			var bndl = ResourceBundle.getBundle(plugin.getClass().getName(), locale, bundleLoader);
			LOG.info("Found vendor bundle for ''{0}'' in locale ''{1}''", plugin.getClass().getName(), locale);
			return bndl;
		}
		catch(MissingResourceException mre) {
			LOG.info("No vendor bundle for ''{0}'' in locale ''{1}''", plugin.getClass().getName(), locale);
			return emptyBundle;
		}
	}

	public Handle config(Plugin plugin, Scope scope) {

		synchronized (config) {
			var key = new Key(plugin, scope);
			var cfg = config.get(key);
			if (cfg == null) {

				var iniFile = resolveIni(plugin, scope);
				var ini = loadIni(iniFile);
				
				LOG.info("Configuration for ''{0}''[{1}] is @ ''{2}''", plugin.getClass().getName(), scope, iniFile);

				cfg = new Handle() {

					@Override
					public void store() {
						var wtr = new  INIWriter.Builder().build();
						try {
							wtr.write(ini, iniFile);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}

					@Override
					public INI ini() {
						return ini;
					}
				};
				config.put(key, cfg);
			}
			return cfg;
		}
	}
	private Path resolveIni(Plugin plugin, Scope scope) {
		switch(scope) {
		case VENDOR:
			return resolveVendorPluginFile(plugin);
		default:
			return resolvePluginFile(plugin);			
		}
	}

	private INI loadIni(Path path) {
		if(Files.exists(path)) {
			try {
				return new INIReader.Builder().
						withMultiValueMode(MultiValueMode.SEPARATED).
						build().read(path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (ParseException e) {
				throw new IllegalArgumentException(MessageFormat.format("Failed to parse {0}", path), e);
			}
		}
		else {
			return INI.create();
		}
	}

	private Path resolvePluginFile(Plugin plugin) {
		return resolvePluginDir().resolve(resolveFile(plugin));
	}

	private Path resolveVendorPluginFile(Plugin plugin) {
		return resolveVendorDir().resolve(resolveFile(plugin));
	}

	private Path resolveFile(Plugin plugin) {
		return Paths.get(plugin.getClass().getName() + ".ini");
	}

	private Path resolvePluginDir() {
		return checkDir(resolveConfDir().resolve("plugins"));
	}

	private Path resolveVendorDir() {
		return checkDir(resolveConfDir().resolve("vendor"));
	}

	private Path resolveConfDir() {
		return checkDir(Paths.get(System.getProperty("jenny.config", "config")));
	}

	private Path checkDir(Path path) {
		try {
			if (!Files.exists(path))
				Files.createDirectories(path);
			return path;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
