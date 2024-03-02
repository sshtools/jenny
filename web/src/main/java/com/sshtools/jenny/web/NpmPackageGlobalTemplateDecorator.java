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
package com.sshtools.jenny.web;

import static java.lang.String.format;

import com.sshtools.bootlace.api.LayerContext;
import com.sshtools.tinytemplate.Templates.TemplateModel;

public class NpmPackageGlobalTemplateDecorator implements GlobalTemplateDecorator {

	@Override
	public void decorate(TemplateModel model) {
		decorateFromLayer(model, LayerContext.get(NpmPackageGlobalTemplateDecorator.class));
	}

	private void decorateFromLayer(TemplateModel model, LayerContext layer) {
		layer.layer().finalArtifacts().ifPresent(a -> {
			a.artifacts().stream().forEach(art -> {
				var gav = art.gav();
				gav.groupIdOr().ifPresent(g -> {
					if(g.equals("npm") || g.startsWith("npm.")) {
						model.variable(format("%s.%s", gav.groupId(), gav.artifactId()), format("/npm2mvn/%s/%s/%s", gav.groupId(), gav.artifactId(), gav.version()));
						model.variable(format("%s.%s.version", gav.groupId(), gav.artifactId()), gav.version());
					}
				});
			});
		});
		layer.childLayers().forEach(l -> decorateFromLayer(model, LayerContext.get(l)));
	}

}
