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
package com.sshtools.jenny.api;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Resources {
	
	public static String formatKeyOrDefault(ResourceBundle bundle, String key, String defaultValue, Object... args) {
		return MessageFormat.format(stringOrDefault(bundle, key, defaultValue), args);
	}

	public static ResourceBundle of(Class<?> bundle, Locale l) {
		return ResourceBundle.getBundle(bundle.getName(), l, bundle.getClassLoader());
	}
	
	public static String stringOrEmpty(ResourceBundle bundle, String key) {
		return stringOrDefault(bundle, key, "");
	}
	
	public static String stringOrDefault(ResourceBundle bundle, String key, String defaultValue) {
		try {
			return bundle.getString(key);
		}
		catch(MissingResourceException mre) {
			return defaultValue;
		}
	}
}
