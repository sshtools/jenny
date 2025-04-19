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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;

public class XPoints {
	
	private final static Log LOG = Logs.of(ApiLog.XPOINTS);
	

	@FunctionalInterface
	public interface XPointRef<INPUT, OUTPUT> extends Function<INPUT, OUTPUT> {
		/* TODO having this extend Function was a terrible idea */
	}
	
	public final static class XPointGroup implements Closeable {
		private Map<Class<Object>, List<XPointRef<?, ?>>> extensions = new ConcurrentHashMap<>();
		
		private final Consumer<XPointGroup> onClose;
		
		XPointGroup(Consumer<XPointGroup> onClose) {
			this.onClose = onClose;
		}

		@SuppressWarnings("unchecked")
		public <INPUT, OUTPUT> XPointGroup point(Class<OUTPUT> clazz, XPointRef<INPUT, OUTPUT> point) {
			List<XPointRef<?, ?>> l;
			synchronized (extensions) {
				l = extensions.get(clazz);
				if (l == null) {
					l = new ArrayList<>();
					extensions.put((Class<Object>) clazz, l);
				}
			}
			l.add(point);
			LOG.info("Registered extension point `{0}` of type `{1}` (now {2} registered)", point.getClass().getName(), clazz.getName(), l.size());
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public <P extends XPointRef<? extends INPUT,OUTPUT>, INPUT, OUTPUT> List<P> points(Class<OUTPUT> clazz) {
			var l = (List<P>) extensions.get(clazz);
			if(l == null) {
				return Collections.emptyList();
			}
			else {
				return l;
			}
		}

		@Override
		public void close() {
			onClose.accept(this);
		}

		@Override
		public String toString() {
			return "XPointGroup [extensions=" + extensions + "]";
		}
	}
	
	private List<XPointGroup> groups = new CopyOnWriteArrayList<>();
	

	public <P extends XPointRef<INPUT, OUTPUT>, INPUT, OUTPUT> List<P> points(Class<OUTPUT> clazz) {
		var l = new ArrayList<P>();
		groups.forEach(grp -> l.addAll(grp.points(clazz)));
		Collections.sort(l, (o1, o2) -> {
			var v1 = 0;
			if(o1.apply(null) instanceof WeightedXPoint wp) {
				v1 = wp.weight();
			}
			var v2 = 0;
			if(o2.apply(null) instanceof WeightedXPoint wp) {
				v2 = wp.weight();
			}
			return Integer.compare(v1, v2);
		});
		return l;
	}
	
	public XPointGroup group() {
		var group = new XPointGroup(g -> groups.remove(g));
		groups.add(group);
		return group;
	}
}

