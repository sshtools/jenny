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
package com.sshtools.jenny.i18n;

import javax.json.Json;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Category;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.api.Resources;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebState;
import com.sshtools.uhttpd.UHTTPD.Handler;

public class I18N implements Plugin {
	final static Log LOG = Logs.of(Category.ofName(I18N.class));

	private Web web;
	private WebModule webModule;

	@Override
	public void afterOpen(PluginContext context) {
		web = context.plugin(Web.class);
		webModule = WebModule.of("/i18n/i18n.js", I18N.class, "i18n.js");
		context.autoClose(web.modules(webModule));
	}

	public Handler i18NScript(Class<?> bundle) {
		
		return tx -> {
			/* TODO make cacheable */
			var rbundle = Resources.of(bundle, WebState.get().locale());
			var arrBldr = Json.createObjectBuilder();
			rbundle.keySet().forEach(k -> {
				arrBldr.add(k, rbundle.getString(k));
			});
			
			var buf = new StringBuilder();
			buf.append("if(typeof i18n === 'undefined') { alert('I18N Javascript support not loaded.'); } else { i18n.bundles['");
			buf.append(bundle.getSimpleName());
			buf.append("'] = JSON.parse('");
			buf.append(arrBldr.build().toString().replace("'", "\\'"));
			buf.append("'); }");
			
			tx.response("text/javascript", buf);
		};
	}

	public WebModule webModule() {
		return webModule;
	}
}
