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
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.jenny.web.GlobalTemplateDecorator;
import com.sshtools.jenny.web.HandlerFactory;
import com.sshtools.jenny.web.Web;

module com.sshtools.jenny.web {
	exports com.sshtools.jenny.web;
	opens com.sshtools.jenny.web;
	
	requires transitive com.sshtools.bootlace.api;
	requires transitive com.sshtools.jenny.api;
	requires transitive com.sshtools.tinytemplate;
	requires transitive com.sshtools.uhttpd;
	requires static java.scripting;
	
	uses HandlerFactory;
	uses GlobalTemplateDecorator;
	
	provides Plugin with Web;
}