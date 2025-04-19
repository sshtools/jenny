package com.sshtools.jenny.avatars;

public abstract class AbstractAvatar implements Avatar {
	private final AvatarRequest request;
	
	protected AbstractAvatar(AvatarRequest request) {
		this.request = request;
	}

	@Override
	public final AvatarRequest request() {
		return request;
	}
}
