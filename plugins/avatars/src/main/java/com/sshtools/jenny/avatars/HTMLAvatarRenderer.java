package com.sshtools.jenny.avatars;

import java.util.Optional;

import com.sshtools.tinytemplate.Templates.TemplateModel;

public class HTMLAvatarRenderer implements AvatarRenderer {

	@Override
	public int weight() {
		return 100;
	}

	@Override
	public Optional<TemplateModel> render(Avatar avatar) {
		if(avatar instanceof ImageAvatar ia) {
			return Optional.of(TemplateModel.ofContent("""
					<img class="[classes]" src="[uri]"/>
					""".replace("[classes]", String.join(" ", ia.classes()).
						replace("[uri]", String.join(" ", ia.uri())))));
		}
		else {
			return Optional.empty();
		}
	}

}
