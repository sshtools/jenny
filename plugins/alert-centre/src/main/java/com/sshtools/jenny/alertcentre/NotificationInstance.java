package com.sshtools.jenny.alertcentre;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.UUID;

import com.sshtools.jenny.alertcentre.Monitor.Scope;


public record NotificationInstance(Notification alert, Principal user, Scope scope) {

	public String toUUID() {
		try {
			return UUID.nameUUIDFromBytes((alert().key() + "-" + user().getName()).getBytes("UTF-8")).toString();
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}
} 
