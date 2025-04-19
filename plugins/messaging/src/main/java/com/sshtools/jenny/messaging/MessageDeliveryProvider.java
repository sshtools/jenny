package com.sshtools.jenny.messaging;

import com.sshtools.jenny.api.WeightedXPoint;

public interface MessageDeliveryProvider<B extends MessageBuilder>  extends WeightedXPoint{

	MediaType supportedMedia();
	
	default boolean isDefault() {
		return true;
	}
	
	default B newBuilder() {
		return newBuilder(null);
	}
	
	B newBuilder(SenderRealm realm);

	default boolean enabled() {
		return true;
	}

}
