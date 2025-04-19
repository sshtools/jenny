package com.sshtools.jenny.gravatar;


import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.UUID;

public record GravatarProfile(String name, String contentType, Instant expire, Instant lastModified) {
	
	public GravatarProfile(String name) {
		this(name, Instant.now());
	}
	
	GravatarProfile(String name, Instant lastModified) {
		this(name, null, null, lastModified);
	}

	public GravatarProfile expire(Instant expire) {
		return new GravatarProfile(name, contentType, expire, Instant.now());
	}

	public GravatarProfile exists(String contentType) {
		return new GravatarProfile(name, contentType, expire, Instant.now());
	}
	
	public String uuid() {
		try {
			return UUID.nameUUIDFromBytes(name.toLowerCase().getBytes("UTF-8")).toString();
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
	}

	public boolean exists() {
		return contentType != null;
	}	
}
