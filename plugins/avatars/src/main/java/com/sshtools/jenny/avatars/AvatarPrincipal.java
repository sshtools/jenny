package com.sshtools.jenny.avatars;

import java.io.InputStream;

import com.sshtools.jenny.api.ExtendedUserPrincipal;

public interface AvatarPrincipal extends ExtendedUserPrincipal {

	void setAvatar(String contentType, InputStream data);
}
