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
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jini.INIReader;
import com.sshtools.jini.INIReader.DuplicateAction;
import com.sshtools.jini.INIReader.MultiValueMode;
import com.sshtools.jini.INIWriter;
import com.sshtools.jini.config.INISet;
import com.sshtools.jini.config.INISet.Builder;
import com.sshtools.jini.config.Monitor;
import com.sshtools.jini.schema.INISchema;

public class Config implements Plugin {

	private boolean developerMode = "true".equals(System.getProperty("jenny.config.developer", String.valueOf(Files.exists(Paths.get("pom.xml")))));
	private Monitor monitor;
	private PluginContext context;

	public Config() {
	}

	@Override
	public void afterOpen(PluginContext context) throws Exception {
		this.context = context;
		monitor = new Monitor();  
	}

	@Override
	public void beforeClose(PluginContext context) throws Exception {
		monitor.close();
	}
	
	public INISet.Builder defaultConfig() {
		return createBuilder(context.root().app().info().app());
	}
	
	public INISet.Builder configBuilder(String name) {
		return createBuilder(name).
				withApp(context.root().app().info().app());
	}
	
	public INISet.Builder configBuilder(String name, Class<?> schemaBase, String schemaResource) {
		try(var rdr = new InputStreamReader(schemaBase.getResourceAsStream(schemaResource), "UTF-8")) {
			return createBuilder(name).
					withSchema(new INISchema.Builder().fromDocument(reader().build().read(rdr)).build()).
					withApp(context.root().app().info().app());
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}

	public Monitor monitor() {
		return monitor;
	}

	private Builder createBuilder(String name) {
		var bldr = new INISet.Builder(name).
                withMonitor(monitor).
                withScopes(INISet.Scope.GLOBAL).
                withWriteScope(INISet.Scope.GLOBAL);
		
		readerAndWriter(bldr);

		if(developerMode) {
            bldr.withPath(INISet.Scope.GLOBAL, Paths.get("conf"));
		}
		
		return bldr;
	}

	private void readerAndWriter(Builder bldr) {
		bldr.
			withWriterFactory(() -> 
		        new INIWriter.Builder().
		        	withMultiValueMode(MultiValueMode.REPEATED_KEY).
		        	withSectionPathSeparator('/')
		    ).
		    withReaderFactory(() -> 
		        reader()
		    );
		
	}

	private INIReader.Builder reader() {
		return new INIReader.Builder().
			withMultiValueMode(MultiValueMode.REPEATED_KEY).
			withDuplicateSectionAction(DuplicateAction.APPEND).
			withSectionPathSeparator('/');
	}
}
