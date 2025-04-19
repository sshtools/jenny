package com.sshtools.jenny.messaging;

public abstract class AbstractMessageDeliveryProvider<B extends MessageBuilder>
		implements MessageDeliveryProvider<B> {

	protected MessageDeliveryService messageDeliveryService;
	
	private MessageDeliveryController controller;
	private final MediaType supportedMedia;

	public AbstractMessageDeliveryProvider(MediaType supportedMedia) {
		this.supportedMedia = supportedMedia;
	}

	protected MessageDeliveryService getMessageDeliveryService() {
		return messageDeliveryService;
	}

	@Override
	public final B newBuilder(SenderRealm realm) {
		if (realm == null)
			throw new IllegalArgumentException("Realm must be provided.");
		var b = createBuilder();
		b.realm(realm);
		return b;
	}

	protected abstract B createBuilder();

	@Override
	public final MediaType supportedMedia() {
		return supportedMedia;
	}

	protected abstract MessageDeliveryResult doSend(B builder) throws MessageDeliveryException;

	public final MessageDeliveryController getController() {
		return controller;
	}

	public final void setController(MessageDeliveryController controller) {
		this.controller = controller;
	}
}
