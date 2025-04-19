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
import com.sshtools.jenny.config.Config;

module com.sshtools.jenny.config {
	exports com.sshtools.jenny.config;
	opens com.sshtools.jenny.config;
	
	requires transitive com.sshtools.jenny.api;
	requires transitive com.sshtools.jini;
	requires transitive com.sshtools.jini.schema;
	requires transitive com.sshtools.jini.config;
	
	provides Plugin with Config; 
}
