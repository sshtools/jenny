package com.sshtools.jenny.mail;

import java.util.Properties;

import org.simplejavamail.api.email.Recipient;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import com.sshtools.jenny.messaging.MediaType;
import com.sshtools.jenny.messaging.MessageBuilder;
import com.sshtools.jenny.messaging.MessageDeliveryException;
import com.sshtools.jenny.messaging.MessageDeliveryResult;
import com.sshtools.jenny.messaging.MessageTextTemplate;
import com.sshtools.jenny.messaging.RecipientHolder;
import com.sshtools.jenny.messaging.SenderRealm;
import com.sshtools.jenny.messaging.StandardMediaType;
import com.sshtools.jini.config.INISet;
import com.sshtools.tinytemplate.Templates.TemplateProcessor;

import jakarta.mail.Message.RecipientType;

public class SimpleMailDeliveryProvider implements EmailNotificationService {

	private final INISet mailConfig;
	private Mailer mailer;
	private Object lock = new Object();
	private final TemplateProcessor tp;

	public SimpleMailDeliveryProvider(INISet mailConfig) {
		this.mailConfig = mailConfig;
		tp = new TemplateProcessor.Builder().build();
	}

	@Override
	public MediaType supportedMedia() {
		return StandardMediaType.EMAIL;
	}

	@Override
	public EmailMessageBuilder newBuilder(SenderRealm realm) {
		var b = new EmailMessageBuilder() {
			@Override
			protected MessageDeliveryResult sendImpl() throws MessageDeliveryException {
				return sendMsg(this);
			}

			@Override
			public RecipientHolder parseRecipient(String recipientAddress) {
				return RecipientHolder.ofEmailAddressSpec(recipientAddress);
			}
		};
		b.realm(realm);
		return b;
	}

	@Override
	public int weight() {
		return Integer.MAX_VALUE;
	}
	
	private Mailer getMailer() {
		synchronized(lock) {
			if(mailer == null) {
				mailer = createMailer();
			}
			return mailer;
		}
	}

	private Mailer createMailer() {
		
		var smtp = mailConfig.document().obtainSection("smtp");
		smtp.onValueUpdate(evt -> {
			synchronized(lock) {
				mailer = null;
			}
		});
		
		var bldr = MailerBuilder.withSMTPServerHost(smtp.get("hostname", "localhost"));
		smtp.getIntOr("port").ifPresent(bldr::withSMTPServerPort);
		smtp.getOr("username").ifPresent(bldr::withSMTPServerUsername);
		smtp.getOr("password").ifPresent(bldr::withSMTPServerPassword);
		smtp.getIntOr("session-timeout").ifPresent(bldr::withSessionTimeout);
		smtp.getBooleanOr("trust-all").ifPresent(bldr::trustingAllHosts);
		smtp.getAllOr("trust").ifPresent(bldr::trustingSSLHosts);
		smtp.getEnumOr(TransportStrategy.class, "transport").ifPresent(bldr::withTransportStrategy);
		
		smtp.sectionOr("properties").ifPresent(props -> {
			var p = new Properties();
			props.keys().forEach(k -> p.setProperty(k, props.get(k)));
			bldr.withProperties(p);
		});
		
		return bldr.buildMailer();
	}

	private MessageDeliveryResult sendMsg(EmailMessageBuilder builder) throws MessageDeliveryException {

		var results = new MessageDeliveryResult(builder.recipients());
		var mailer = getMailer();
		var bldr = EmailBuilder.startingBlank();
		
		bldr.to(builder.recipients().stream().map(this::toRecipient).toList());
		builder.subject().map(this::process).ifPresent(bldr::withSubject);
		builder.templateFor(MessageBuilder.CONTENT_TYPE_TEXT).map(this::process).ifPresent(bldr::withPlainText);
		builder.templateFor(MessageBuilder.CONTENT_TYPE_HTML).map(this::process).ifPresent(bldr::withHTMLText);
		
		mailer.sendMail(bldr.buildEmail());

		return results;
	}
	
	private String process(MessageTextTemplate template) {
		if(template.content().isPresent())
			return template.content().get().get();
		else if(template.model().isPresent())
			return tp.process(template.model().get());
		else
			return "";
	}

	private Recipient toRecipient(RecipientHolder recipient) {
		return new Recipient(recipient.getName(),  recipient.getAddress(), RecipientType.TO);
	}

}
