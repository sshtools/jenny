package com.sshtools.jenny.messaging;

import java.util.List;
import java.util.stream.Collectors;

public interface MessageDeliveryService  {

	MessageDeliveryController getController();

	void setController(MessageDeliveryController controller);
	
	String getDefaultForMediaType(MediaType mediaType);
}
