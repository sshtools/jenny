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
package com.sshtools.jenny.toast;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.bootstrap5.Alerts.Alert;
import com.sshtools.jenny.bootstrap5.Bootstrap5;
import com.sshtools.jenny.io.Io;
import com.sshtools.jenny.io.Io.IoChannel;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebLog;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebModule.WebModulesRef;
import com.sshtools.tinytemplate.Templates.TemplateModel;

public class Toast implements Plugin {
	final static Log LOG = Logs.of(WebLog.JOBS);
	
	private Web web;
	private Io io;

	private WebModule webModule;
	private WebModulesRef modules;
	private final List<Alert> queuedAlerts = new ArrayList<>();
	
	@Override
	public void afterOpen(PluginContext context) {
		
		web = context.plugin(Web.class);
		io = context.plugin(Io.class);
		
		context.autoClose(
				
			modules = web.modules(webModule = WebModule.of(
				"/toast.js", 
				Toast.class, 
				"toast.frag.js",
				Bootstrap5.MODULE_BOOTSTRAP5,
				io.webModule()
			)),
			
			io.contributor("toast", IoChannel::of)
		);
		
		while(!queuedAlerts.isEmpty())
			toast(queuedAlerts.remove(0));
		
	}
	
	public WebModule webModule() {
		return webModule;
	}

	public TemplateModel fragToast() {
		return web.require(
				web.template(Toast.class, "toast.frag.html"), 
				modules
		);
	}
	
	public void toast(Alert alert) {
		toast(alert, Duration.ofSeconds(2));
	}

	public void toast(Alert alert, Duration delay) {
		if(web == null) {
			queuedAlerts.add(alert);			
		}
		else {
			web.globalUiQueue().schedule(() -> 
				io.broadcast("toast", Json.createObjectBuilder().
							add("type", "toast").
							add("subtitle", "").
							add("icon", alert.icon().orElse("")).
							add("title", alert.title().get()).
							add("style", alert.style().name().toLowerCase()).
							add("description", alert.description().get().orElse("")).
							add("actions", actions(alert)).
							build())
			, delay.toMillis(), TimeUnit.MILLISECONDS);
		}
		
	}
	
	private JsonArray actions(Alert alert) {
		var arr = Json.createArrayBuilder();
		for(var action : alert.actions()) {
			arr.add(Json.createObjectBuilder().
				add("style", action.style().name().toLowerCase()).
				add("text", action.text()).
				add("path", action.path()).
				add("icon", action.icon().orElse("")).build());
		}
		return arr.build();
	}
}
