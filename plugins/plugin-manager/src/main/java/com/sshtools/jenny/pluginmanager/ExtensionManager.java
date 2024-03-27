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
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonValue;

import com.sshtools.bootlace.api.ExtensionLayer;
import com.sshtools.bootlace.api.FilesAndFolders;
import com.sshtools.bootlace.api.Http;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.bootlace.api.PluginLayer;
import com.sshtools.jenny.bootstrap5.Bootstrap5;
import com.sshtools.jenny.i18n.I18N;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebModule.WebModulesRef;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.FormData;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class ExtensionManager implements Plugin {
	
	private Web web;
	private I18N i18n;
	private ExtensionLayer pluginsLayer;
	private WebModulesRef maangerModulesRef;
	private WebModulesRef installModulesRef;
	
	@Override
	public void afterOpen(PluginContext context) {
		
		web = context.plugin(Web.class);
		i18n = context.plugin(I18N.class);
		
		var managerI18nModule = WebModule.js("/extension-manager.i18n.js", i18n.i18NScript(ExtensionManager.class), i18n.webModule());
		
		context.autoClose(
				
			maangerModulesRef = web.modules(
				managerI18nModule,
				WebModule.of(
						"/extension-manager.js", 
						ExtensionManager.class, 
						"extension-manager.frag.js", 
						Bootstrap5.MODULE_JQUERY, Bootstrap5.MODULE_BOOTSTRAP_TABLE, Bootstrap5.MODULE_BOOTBOX, i18n.webModule(), managerI18nModule
				)
			),
			
			installModulesRef = web.modules(
				WebModule.of(
						"/extension-installer.js", 
						ExtensionManager.class, 
						"extension-installer.frag.js", 
						Bootstrap5.MODULE_JQUERY, Bootstrap5.MODULE_BOOTSTRAP5_AUTOCOMPLETE, i18n.webModule(), managerI18nModule
				)
			),
				
			web.router().route().
				handle("/extension-manager-api", this::extensionManagerApi).
				build(),
				
			web.router().route().
					handle("/extension-search", this::actionSearch).
				build());
		
		pluginsLayer = (ExtensionLayer)context.layer("extensions").orElseThrow(() -> new IllegalStateException("Could not find extensions layer"));
	}

	public TemplateModel fragExtensions() {
		var plugins = new ArrayList<String>();
		var path = pluginsLayer.path();
		try(var str = Files.newDirectoryStream(path, f -> Files.isDirectory(f))) {
			for(var dir : str) {
				plugins.add(dir.getFileName().toString());
			}
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
		
		return web.require(
			web.template(ExtensionManager.class, "extension-manager.frag.html").
				bundle(ExtensionManager.class).
				list("extensions", (content) -> plugins.stream().map(plugin -> 
					TemplateModel.ofContent(content)
						.variable("id", plugin)
				).toList()),
			maangerModulesRef);
	}

	public TemplateModel fragInstallExtension() {
		return web.require(
			web.template(ExtensionManager.class, "extension-installer.frag.html").
				bundle(ExtensionManager.class), 
			installModulesRef
		);
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

	public void extensionManagerApi(Transaction tx) {
		var arrBldr = Json.createArrayBuilder();
		
		pluginsLayer.extensions().stream().forEach(a -> {
			var player =  (PluginLayer)a;
			var primary = player.artifacts().iterator().next();
			arrBldr.add(Json.createObjectBuilder().
					add("id", a.id()).
					add("name", a.name().orElse("")).
					add("icon", player.icon().orElse("")).
					add("description", player.description().orElse(a.id())).
					add("version", primary.gav().version()));
		});
		
		tx.response("text/json", arrBldr.build().toString());
	}
	
	public void handleManagerPOST(Transaction tx) throws IOException {
		var req = tx.request();
			
		var op = req.ofNamed("op").get().asString();
		
		// Delete
		if(op.equals("remove")) {
			var extids = req.ofNamed("ids").get().asString().split("/");
			for(var extid : extids) {
				// Check exists to prevent shenanigans
				pluginsLayer.extension(extid).get();
				
				var path = pluginsLayer.path().resolve(extid);
				FilesAndFolders.recursiveDelete(path);
			}
		}
		else
			throw new IllegalStateException("No known op.");
	}

	public void handleInstallPOST(Transaction tx) throws IOException {
		Path extensionFile = null, extensionTempFile = null;
		boolean install = false;
		for(var part : tx.request().asParts()) {
			if(part instanceof FormData fd) {
				switch(part.name()) {
				case "file":
					extensionTempFile = pluginsLayer.path().resolve(fd.filename().map(fn -> fn + ".tmp").orElse("extension.zip.tmp")) ;
					extensionFile = pluginsLayer.path().resolve(fd.filename().map(fn -> fn).orElse("extension.zip")) ;
					try(var in = part.asStream()) {
						try(var out = Files.newOutputStream(extensionTempFile)) {
							in.transferTo(out);
						}
					}
					break;
				case "upload":
					install = true;
				default:
//					System.out.println("  pt: " + part.asString());
//					part.asStream().transferTo(OutputStream.nullOutputStream());
//					part.close();
					break;
				}
			}
		}
		
		if(install && extensionFile != null) {
			Files.deleteIfExists(extensionFile);
			Files.move(extensionTempFile, extensionFile);
		}
		else
			throw new UnsupportedOperationException();
		
	}

}
