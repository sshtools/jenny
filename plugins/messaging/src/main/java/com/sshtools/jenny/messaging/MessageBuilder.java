package com.sshtools.jenny.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class MessageBuilder {
	
	public final static String CONTENT_TYPE_TEXT = "text/plain";
	public final static String CONTENT_TYPE_HTML= "text/html";

	private final List<RecipientHolder> recipients = new ArrayList<>();
	private List<MessageTextTemplate> templates = new ArrayList<MessageTextTemplate>();;
	private int delay;
	private Optional<String> context = Optional.empty();
	private Optional<SenderRealm> realm = Optional.empty();
	private boolean partialDeliveryIsException;
	private boolean archive;
	private boolean track;

	protected MessageBuilder() {
	}

	public boolean partialDeliveryIsException() {
		return partialDeliveryIsException;
	}

	public MessageBuilder partialDeliveryIsException(boolean partialDeliveryIsException) {
		this.partialDeliveryIsException = partialDeliveryIsException;
		return this;
	}

	public List<RecipientHolder> recipients() {
		return recipients;
	}

	public MessageBuilder recipient(RecipientHolder recipient) {
		this.recipients.clear();
		addRecipients(Arrays.asList(recipient));
		return this;
	}

	public MessageBuilder recipients(RecipientHolder[] recipients) {
		this.recipients.clear();
		if (recipients != null)
			this.recipients.addAll(Arrays.asList(recipients));
		return this;
	}

	public MessageBuilder recipients(List<RecipientHolder> recipients) {
		this.recipients.clear();
		this.recipients.addAll(recipients);
		return this;
	}

	public MessageBuilder addRecipients(RecipientHolder... recipients) {
		return addRecipients(Arrays.asList(recipients));
	}

	public MessageBuilder addRecipients(List<RecipientHolder> recipients) {
		this.recipients.addAll(recipients);
		return this;
	}

	public MessageBuilder addRecipientAddresses(String... recipients) {
		return addRecipientAddresses(Arrays.asList(recipients));
	}

	public MessageBuilder addRecipientAddresses(List<String> recipientAddresses) {
		for(var recipientAddress : recipientAddresses) {
			this.recipients.add(parseRecipient(recipientAddress));	
		}
		return this;
	}
	
	public boolean validate(String... addressSpecs) {
		for(var addressSpec : addressSpecs) {
			try {
				parseRecipient(addressSpec);
			}
			catch(IllegalArgumentException ve) {
				return false;
			}
		}
		return true;
	}
	
	protected RecipientHolder parseRecipient(String addressSpec) {
		return RecipientHolder.ofGeneric(addressSpec);
	}

	public List<MessageTextTemplate> templates() {
		return Collections.unmodifiableList(templates);
	}
	
	public MessageBuilder template(MessageTextTemplate template) {
		this.templates.clear();
		return addTemplate(template);
	}
	
	public MessageBuilder addTemplate(MessageTextTemplate template) {
		this.templates.add(template);
		return this;
	}

	public MessageBuilder templates(MessageTextTemplate... templates) {
		return templates(Arrays.asList(templates));
	}
	
	public MessageBuilder addTemplates(MessageTextTemplate... templates) {
		return addTemplates(Arrays.asList(templates));
	}
	
	public MessageBuilder templates(Collection<MessageTextTemplate> template) {
		this.templates.clear();
		return addTemplates(templates);
	}
	
	public MessageBuilder addTemplates(Collection<MessageTextTemplate> template) {
		this.templates.addAll(template);
		return this;
	}

	public MessageBuilder text(String text) {
		return addTemplate(MessageTextTemplate.ofText(text));
	}

	public int delay() {
		return delay;
	}

	public MessageBuilder delay(int delay) {
		this.delay = delay;
		return this;
	}

	public Optional<String> context() {
		return context;
	}

	public MessageBuilder context(String context) {
		this.context = Optional.of(context);
		return this;
	}

	public Optional<SenderRealm> realm() {
		return realm;
	}

	public MessageBuilder realm(SenderRealm realm) {
		this.realm = Optional.ofNullable(realm);
		return this;
	}
	
	public Optional<MessageTextTemplate> templateFor(String contentType) {
		return templates().stream().filter(t -> contentType.equalsIgnoreCase(t.contentType())).findFirst();
	}

	public boolean archive() {
		return archive;
	}

	public MessageBuilder archive(boolean archive) {
		this.archive = archive;
		return this;
	}

	public boolean track() {
		return track;
	}

	public MessageBuilder track(boolean track) {
		this.track = track;
		return this;
	}

	public final MessageDeliveryResult send() throws MessageDeliveryException {
		var res = sendImpl();
		if(res.isEmpty()) {
			throw new MessageDeliveryException("Nothing was sent.");
		}
		else if(res.isPartialFailure()) {
			if(partialDeliveryIsException)
				throw new MessageDeliveryException(res);
		}
		
		if(res.isSingleResult())
			res = res.getDetails().get(0);

		if(res.isFailure())
			throw new MessageDeliveryException(res);
		
		return res;
	}

	protected abstract MessageDeliveryResult sendImpl() throws MessageDeliveryException;

}
