package com.sshtools.jenny.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.sshtools.jenny.messaging.MessageBuilder;
import com.sshtools.jenny.messaging.MessageTextTemplate;
import com.sshtools.jenny.messaging.RecipientHolder;

public abstract class EmailMessageBuilder extends MessageBuilder {
	private Optional<MessageTextTemplate> subject = Optional.empty();
	private List<EmailAttachment> attachments = new ArrayList<>();
	private Optional<RecipientHolder> replyTo;

	public EmailMessageBuilder() {
	}

	public EmailMessageBuilder replyTo(RecipientHolder replyTo) {
		this.replyTo = Optional.of(replyTo);
		return this;
	}

	public EmailMessageBuilder replyTo(String replyToName, String replyToEmail) {
		return replyTo(RecipientHolder.ofNameAndAddress(replyToName, replyToEmail));
	}

	public EmailMessageBuilder replyToName(String replyToName) {
		return replyTo(RecipientHolder.ofName(replyToName));
	}

	public Optional<RecipientHolder> replyTo() {
		return replyTo;
	}

	public EmailMessageBuilder replyToEmail(String replyToEmail) {
		return replyTo(RecipientHolder.ofEmailAddressSpec(replyToEmail));
	}

	public Optional<MessageTextTemplate> subject() {
		return subject;
	}

	public EmailMessageBuilder subject(String subject) {
		return subject(MessageTextTemplate.ofText(subject));
	}

	public EmailMessageBuilder subject(MessageTextTemplate subject) {
		this.subject = Optional.of(subject);
		return this;
	}

	public EmailMessageBuilder html(String html) {
		addTemplate(MessageTextTemplate.ofHtml(html));
		return this;
	}

	public List<EmailAttachment> attachments() {
		return attachments;
	}

	public EmailMessageBuilder attachment(EmailAttachment attachment) {
		this.attachments.clear();
		this.attachments.add(attachment);
		return this;
	}

	public EmailMessageBuilder addAttachments(List<EmailAttachment> attachments) {
		this.attachments.addAll(attachments);
		return this;
	}

	public EmailMessageBuilder addAttachments(EmailAttachment... attachments) {
		addAttachments(Arrays.asList(attachments));
		return this;
	}

	public EmailMessageBuilder attachments(List<EmailAttachment> attachments) {
		this.attachments.clear();
		this.attachments.addAll(attachments);
		return this;
	}
}
