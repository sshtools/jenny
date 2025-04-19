package com.sshtools.jenny.messaging;

public interface MessageDeliveryController {

	boolean canSend(MessageDeliveryProvider<?> provider);
}