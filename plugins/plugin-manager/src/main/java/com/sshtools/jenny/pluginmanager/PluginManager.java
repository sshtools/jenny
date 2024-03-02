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
package com.sshtools.jenny.pluginmanager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.NoSuchFileException;

import javax.json.Json;
import javax.json.JsonValue;

import com.sshtools.bootlace.api.Http;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.web.Web;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class PluginManager implements Plugin {
	
	private Web web;
	
	@Override
	public void afterOpen(PluginContext context) {
		web = context.plugin(Web.class);
		
		context.autoClose(
			web.router().route().
				handle("/plugin-installer\\.frag\\.js", UHTTPD.classpathResource(PluginManager.class, "plugin-installer.frag.js")).
				build(),
				
			web.router().route().
					handle("/plugin-search", this::actionSearch).
				build());
	}

	public TemplateModel fragPlugins() {
		return web.template(PluginManager.class, "plugin-manager.frag.html").
				bundle(PluginManager.class);
	}

	public TemplateModel fragInstall() {
		return web.template(PluginManager.class, "plugin-installer.frag.html").
				bundle(PluginManager.class);
	}
	
	private void actionSearch(Transaction tx) {
		var httpClient = Http.createDefaultHttpClientBuilder().build();
		var uri = URI.create(String.format("https://search.maven.org/solrsearch/select?q=%s&rows=20&wt=json",
				Http.urlEncode(tx.parameter("query").asString())));
		var request = HttpRequest.newBuilder().GET().uri(uri).build();

		var handler = HttpResponse.BodyHandlers.ofInputStream();
		try {
			var response = httpClient.send(request, handler);
			switch (response.statusCode()) {
			case 200:
				try (var in = response.body()) {
					
					/* Input */
					var object = Json.createReader(in).readObject();
					var resp = object.get("response").asJsonObject();
					var docs = resp.get("docs").asJsonArray();
					
					/* Output */
					
					var arr = Json.createArrayBuilder();
					docs.stream().map(JsonValue::asJsonObject).forEach(jv -> {
						var obj = Json.createObjectBuilder();
						var gav = jv.getString("id") + ":" + jv.getString("latestVersion");
						var repo = jv.getString("repositoryId");
						obj.add("value", gav);
						obj.add("label", gav + " @ " + repo);
						arr.add(obj);
					});
					
					tx.response("text/json", arr.build().toString());
				}
				break;
			case 404:
				throw new NoSuchFileException(uri.toString());
			default:
				throw new IOException("Unexpected status " + response.statusCode());
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
