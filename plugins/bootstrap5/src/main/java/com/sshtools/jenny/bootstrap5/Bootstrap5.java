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
package com.sshtools.jenny.bootstrap5;

import static com.sshtools.bootlace.api.GAV.ofSpec;
import static com.sshtools.jenny.web.NpmWebModule.of;

import com.sshtools.bootlace.api.GAV;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.jenny.web.NpmWebModule;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebModule.Type;
import com.sshtools.jenny.web.WebModule.WebModuleResource;
import com.sshtools.tinytemplate.bootstrap.forms.Form;

public class Bootstrap5 implements Plugin {
	
	public final static WebModule MODULE_JQUERY =
		of(
			Bootstrap5.class, 
			ofSpec("npm:jquery")
		);
	
	public final static WebModule MODULE_JQUERY_TREEGRID = new NpmWebModule.Builder().
			 	withClass(Bootstrap5.class).
			 	withMain("js/jquery.treegrid.min.js").
			 	withGAV(GAV.ofSpec("npm:jquery-treegrid")).
			 	withRequires(MODULE_JQUERY).
			 	addResources(WebModuleResource.css("css/jquery.treegrid.css")).
			 	build();
	
	public final static WebModule MODULE_BOOTSTRAP5 = new NpmWebModule.Builder().
		withGAV(ofSpec("npm:bootstrap")).
		withClass(Bootstrap5.class).
		withType(Type.JS).
		withMain("dist/js/bootstrap.bundle.min.js").
		withRequires(MODULE_JQUERY).
		build();
	
	public final static WebModule MODULE_BOOTSTRAP_ICONS = 
		of(
			Bootstrap5.class, 
			ofSpec("npm:bootstrap-icons"), 
			MODULE_BOOTSTRAP5
		);
	
	public final static WebModule MODULE_BOOTSTRAP_TABLE = new NpmWebModule.Builder().
		withMain("dist/bootstrap-table.min.js").
		withType(Type.JS).
		withGAV(ofSpec("npm:bootstrap-table")).
		withClass(Bootstrap5.class).
		withRequires(MODULE_BOOTSTRAP5).
		build();
	
	public final static WebModule MODULE_BOOTSTRAP_TABLE_TREEGRID = new NpmWebModule.Builder().
		withMain("dist/extensions/treegrid/bootstrap-table-treegrid.min.js").
		withType(Type.JS).
		withGAV(ofSpec("npm:bootstrap-table")).
		withClass(Bootstrap5.class).
		withRequires(MODULE_JQUERY_TREEGRID, MODULE_BOOTSTRAP_TABLE).
		build();
	
	public final static WebModule MODULE_BOOTSTRAP5_AUTOCOMPLETE = 
		of(
			Bootstrap5.class, 
			ofSpec("npm:bootstrap5-autocomplete"), 
			MODULE_BOOTSTRAP5
		);
	
	public final static WebModule MODULE_BOOTBOX = 
		of(
			Bootstrap5.class, 
			ofSpec("npm:bootbox"), 
			MODULE_BOOTSTRAP5
		);

	public final static WebModule MODULE_TTBS = WebModule.of(
			"/ttbs.js", 
			Form.class, 
			"ttbs.js",
			Bootstrap5.MODULE_BOOTSTRAP5
		);
}
