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
open module com.sshtools.jenny.boot {
	exports com.sshtools.jenny.boot;
	
//	requires layrry.core;
	requires com.sshtools.bootlace.api;
	requires com.sshtools.bootlace.platform;
	requires org.jline;
	requires progressbar;

	/* TODO: For now, app layer can't see these modules without them being here */
	requires java.net.http;
	requires java.scripting;
	requires java.logging;
//	requires java.sql; /* TODO I mean why gson! */
}