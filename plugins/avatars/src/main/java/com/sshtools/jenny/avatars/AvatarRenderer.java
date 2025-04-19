package com.sshtools.jenny.avatars;

import java.util.Optional;

import com.sshtools.jenny.api.WeightedXPoint;
import com.sshtools.tinytemplate.Templates.TemplateModel;

public interface AvatarRenderer extends WeightedXPoint {
	
	Optional<TemplateModel> render(Avatar avatar);
}
