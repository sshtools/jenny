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
package com.sshtools.jenny.dbus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;

public class DBus implements Plugin {

	private final Map<DBusBusType, DBusConnection> connections = Collections.synchronizedMap(new HashMap<>());
	private BusProvider provider;
	private boolean obtained;

	@Override
	public void afterOpen(PluginContext context) throws Exception {
	}

	public void provider(BusProvider provider) {
		if (this.provider == null && provider != null) {
			if (obtained) {
				throw new IllegalStateException("Cannot set provider after bus has been obtained.");
			}
			this.provider = provider;
		} else if (this.provider != null && provider == null) {
			this.provider = null;
		} else {
			throw new IllegalStateException();
		}
	}

	public DBusConnection systemBus() {
		return get(DBusBusType.SYSTEM);
	}

	public DBusConnection sessionBus() {
		return get(DBusBusType.SESSION);
	}

	public DBusConnection bus() {
		/* TODO configurable default bus for the app */
		if(Boolean.getBoolean("jenny.dbus.preferSessionBus"))
			return sessionBus();
		else
			return systemBus();
	}

	public DBusConnection get(DBusBusType type) {
		synchronized (connections) {
			var c = connections.get(type);
			if (c == null) {
				try {
					if (provider != null) {
						c = provider.create(type);
					} else {
						switch (type) {
						case SESSION:
							c = DBusConnectionBuilder.forSessionBus().build();
							break;
						case SYSTEM:
							c = DBusConnectionBuilder.forSystemBus().build();
							break;
						default:
							throw new UnsupportedOperationException();
						}
					}
				} catch (DBusException dbe) {
					throw new IllegalStateException("Failed to connect to bus.", dbe);
				}
				connections.put(type, c);
				obtained = true;
			}
			return c;
		}
	}

}
