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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


public class TypedItem<T> {
	
	public interface Scope {
	}

	private final String key;
	private final Class<?> type;
	private final T defaultValue;
	private final Set<T> values;
	private final Optional<Scope> scope;
	
	private static Map<String, TypedItem<?>> items;

	private TypedItem(String key, Class<T> type, T defaultValue, @SuppressWarnings("unchecked") T... values) {
		this(key, type, Optional.empty(), defaultValue, values);	
	}
	
	private TypedItem(String key, Class<T> type, Optional<Scope> scope, T defaultValue, @SuppressWarnings("unchecked") T... values) {
		this.key = key;
		this.scope = scope;
		this.type = type;
		this.defaultValue = defaultValue;
		this.values = new LinkedHashSet<>(Arrays.asList(values));
	}
	
	static void add(TypedItem<?> item) {
		if(items == null)
			items = new LinkedHashMap<>();
		items.put(item.key(), item);
	}
	
	public static <T> TypedItem<T> add(String key, Class<T> type, T defaultValue, @SuppressWarnings("unchecked") T... values) {
		return add(key, type, Optional.empty(), defaultValue, values);
	}
	
	public static <T> TypedItem<T> add(String key, Class<T> type, Optional<Scope> scope, T defaultValue, @SuppressWarnings("unchecked") T... values) {
		TypedItem<T> item = new TypedItem<>(key, type, scope, defaultValue, values);
		add(item);
		return item;
	}

	public static boolean has(String name) {
		return items.containsKey(name);
	}
	
	public static TypedItem<?> get(String name) {
		TypedItem<?> configurationItem = items.get(name);
		if(configurationItem == null)
			throw new IllegalArgumentException("No configuration item " + name);
		return configurationItem;
	}
	
	public static Collection<TypedItem<?>> items() {
		return items.values();
	}
	
	public static Collection<String> keys() {
		return items.keySet();
	}

	public Optional<Scope> scope() {
		return scope;
	}

	public String key() {
		return key;
	}

	public Class<?> type() {
		return type;
	}

	public T defaultValue() {
		return defaultValue;
	}

	public Set<T> values() {
		return values;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypedItem<?> other = (TypedItem<?>) obj;
		return Objects.equals(key, other.key);
	}

	@SuppressWarnings("unchecked")
	public T parse(String val) {
		if(val == null)
			return defaultValue;
		try {
			if(type == Integer.class)
				return (T)((Integer)Integer.parseInt(val));
			else if(type == Long.class)
				return (T)((Long)Long.parseLong(val));
			else if(type == Boolean.class)
				return (T)((Boolean)Boolean.parseBoolean(val));
			else if(type == String.class)
				return (T)val;
			else if(Enum.class.isAssignableFrom(type))
				return (T)type.getMethod("valueOf", String.class).invoke(null, val);
			else
				throw new IllegalArgumentException("Cannot parse type " + type);
		}
		catch(Exception e) {
			return defaultValue;
		}
	}
}
