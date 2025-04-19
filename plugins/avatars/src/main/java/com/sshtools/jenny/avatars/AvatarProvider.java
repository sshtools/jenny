package com.sshtools.jenny.avatars;

import java.util.Optional;

import com.sshtools.jenny.api.WeightedXPoint;

public interface AvatarProvider extends WeightedXPoint {
	
	Optional<Avatar> find(AvatarRequest request);
}
