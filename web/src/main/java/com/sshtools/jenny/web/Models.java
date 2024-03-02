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

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.sshtools.tinytemplate.Templates.TemplateModel;

public class Models {
	
	public static <E extends Enum<E>> List<TemplateModel> modelOf(Class<E> enumClass, String content) {
		return modelOf(enumClass, content, () -> null);
	}
	 
	public static <E extends Enum<E>> List<TemplateModel> modelOf(Class<E> enumClass, String content, Supplier<E> selected) {
		return Arrays.asList(enumClass.getEnumConstants()).stream().map(en -> 
			TemplateModel.ofContent(content).
					variable("name", en::name).
					variable("selected", () ->  {
						var sel = selected.get();
						return sel == null ? false : en.name().equals(sel.name()); 
					}).
					variable("ordinal", en::ordinal).
					variable("text", en::toString)
		).toList();
	}

}
