package com.sshtools.jenny.webawesome;

import static com.sshtools.tinytemplate.Templates.TemplateModel.ofContent;

import java.util.Optional;

import com.sshtools.jenny.avatars.Avatar;
import com.sshtools.jenny.avatars.AvatarRenderer;
import com.sshtools.jenny.avatars.DefaultAvatarProvider.DefaultAvatar;
import com.sshtools.jenny.avatars.ImageAvatar;
import com.sshtools.tinytemplate.Templates.TemplateModel;

public class WebAwesomeAvatarRenderer implements AvatarRenderer {

	@Override
	public Optional<TemplateModel> render(Avatar request) {
		if(request instanceof DefaultAvatar da) {
			return Optional.of(ofContent("""
				<wa-avatar class="[classes]" label="[label]"></wa-avatar>	
					""".replace("[uri]", da.uri())
					.replace("[label]", da.request().username().orElse(""))
					.replace("[classes]", String.join(" ", da.classes()))));
		}
		else if(request instanceof ImageAvatar ia) {
			return Optional.of(ofContent("""
				<wa-avatar class="[classes]" image="[uri]"  label="[label]"></wa-avatar>	
					""".replace("[uri]", ia.uri())
						.replace("[label]", ia.request().username().orElse(""))
						.replace("[classes]", String.join(" ", ia.classes()))));
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public int weight() {
		return 10;
	}

}
