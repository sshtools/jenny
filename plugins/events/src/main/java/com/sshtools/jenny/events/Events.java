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
package com.sshtools.jenny.events;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.sshtools.bootlace.api.Plugin;

public final class Events implements Plugin {
	
	public Map<Class<?>, List<Consumer<?>>> events = new ConcurrentHashMap<>();
	private List<Consumer<?>> allEvents = new ArrayList<>(); 
	private final static ThreadLocal<Boolean> vetoed = new ThreadLocal<Boolean>();
	
	public static void veto() {
		vetoed.set(true);
	}
	
	public <T> Closeable on(Class<T> type, Consumer<T> handler) {
		var l = getEventHandlerList(type, true);
		l.add(handler);
		return new Closeable() {
			@Override
			public void close() throws IOException {
					l.remove(handler);
					if(l.isEmpty()) {
						events.remove(type);
					}
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	public <T> void fire(T event) {
		fire((Class<T>)event.getClass(), event);
	}
	
	@SuppressWarnings("unchecked")
	public <T> void fire(Class<T> type, T event) {
		var l = getEventHandlerList(type, false);
		for(int i = allEvents.size() - 1 ; i >= 0 ; i--) 
			((Consumer<T>)allEvents.get(i)).accept(event);
		for(int i = l .size() - 1 ; i >= 0 ; i--) 
			((Consumer<T>)l.get(i)).accept(event);
	}
	
	@SuppressWarnings("unchecked")
	public <T> boolean fireVetoable(T event) {
		return fireVetoable((Class<T>)event.getClass(), event);
	}
	
	@SuppressWarnings("unchecked")
	public <T> boolean fireVetoable(Class<T> type, T event) {
		var l = getEventHandlerList(type, false);
		try {
			for(int i = allEvents.size() - 1 ; !isVetoed() && i >= 0 ; i--) 
				((Consumer<T>)allEvents.get(i)).accept(event);
			for(int i = l .size() - 1 ; !isVetoed() && i >= 0 ; i--) 
				((Consumer<T>)l.get(i)).accept(event);
			return !isVetoed();
		}
		finally {
			vetoed.remove();
		}
	}

	private boolean isVetoed() {
		return Boolean.TRUE.equals(vetoed.get());
	}
	
	public <T> Closeable any(Consumer<T> handler) {
		allEvents.add(handler);
		return new Closeable() {
			@Override
			public void close() throws IOException {
				allEvents.remove(handler);
			}
		};
	}

	private List<Consumer<?>> getEventHandlerList(Class<?> type, boolean create) {
		var l = events.get(type);
		if(l == null && create) {
			l = new ArrayList<Consumer<?>>();
			events.put(type, l);
		}
		else if(l == null && !create) {
			return Collections.emptyList();
		}
		return l;
	}

}
