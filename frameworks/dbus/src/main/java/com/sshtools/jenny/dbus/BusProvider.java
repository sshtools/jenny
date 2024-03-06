package com.sshtools.jenny.dbus;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;

public interface BusProvider {
	DBusConnection create(DBusBusType type) throws DBusException;
}
