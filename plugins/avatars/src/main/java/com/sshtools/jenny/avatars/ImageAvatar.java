package com.sshtools.jenny.avatars;

public class ImageAvatar extends AbstractAvatar {

	private final String uri;
	private final String[] classes;

	public ImageAvatar(AvatarRequest request, String uri, String... classes) {
		super(request);
		this.uri = uri;
		this.classes = classes;
	}
	
	public String[] classes() {
		return classes;
	}
	
	public String uri() {
		return uri;
	}
}
