package com.sshtools.jenny.messaging;

import java.util.Optional;
import java.util.function.Supplier;

import com.sshtools.tinytemplate.Templates.TemplateModel;

public final class MessageTextTemplate {

	private final String contentType;
	private final Optional<Supplier<String>> content;
	private final Optional<TemplateModel> model;
	
	private MessageTextTemplate(Builder builder) {
		this.contentType = builder.contentType;
		this.content = builder.content;
		this.model = builder.model;
	}

	public final static class Builder {
		private String contentType = MessageBuilder.CONTENT_TYPE_TEXT;
		private Optional<Supplier<String>> content = Optional.empty();
		private Optional<TemplateModel> model = Optional.empty();
		
		public Builder model(TemplateModel model) {
			this.model = Optional.of(model);
			return this;
		}
		
		public Builder contentType(String contentType) {
			this.contentType = contentType;
			return this;
		}

		public Builder content(String content) {
			if(content == null)
				throw new IllegalArgumentException("Content may not be null.");
			return content(() -> content);
		}

		public Builder content(Supplier<String> content) {
			this.content = Optional.of(content);
			return this;
		}
		
		public MessageTextTemplate build() {
			return new MessageTextTemplate(this);
		}
	}

	public static MessageTextTemplate ofText(String text) {
		return new Builder().content(text).build();
	}

	public static MessageTextTemplate ofHtml(String html) {
		return new Builder().contentType(MessageBuilder.CONTENT_TYPE_HTML).content(html).build();
	}
	
	public String contentType() {
		return contentType;
	}
	
	public Optional<Supplier<String>> content() {
		return content;
	}
	
	public Optional<TemplateModel> model() {
		return model;
	}
}