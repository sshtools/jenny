package com.sshtools.jenny.avatars;

import static java.util.Optional.of;

import java.util.Optional;

public class DefaultAvatarProvider implements AvatarProvider {

	@Override
	public Optional<Avatar> find(AvatarRequest request) {
		return of(new DefaultAvatar(request));
	}

	@Override
	public int weight() {
		return Integer.MAX_VALUE;
	}
	
	public final static class DefaultAvatar extends ImageAvatar {
		public DefaultAvatar(AvatarRequest request) {
			super(request, "/avatars/default/default-avatar.png", "user-avatar", "default-avatar");
		}
	}

}
