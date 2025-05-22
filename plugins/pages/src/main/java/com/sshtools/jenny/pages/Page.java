package com.sshtools.jenny.pages;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public final class Page extends El implements Closeable {

	public final static class Builder extends ElBuilder<Builder, Page> {
		private Optional<String> contentType = Optional.empty();
		private Optional<String> uri = Optional.empty();
		private Optional<Runnable> onClose = Optional.empty();
		private Optional<Consumer<Transaction>> onHandle = Optional.empty();
		private Optional<Consumer<Transaction>> onGet = Optional.empty();
		private Optional<Consumer<Transaction>> onPost = Optional.empty();
		private Optional<Supplier<Object>> content = Optional.empty();
		private Optional<Supplier<TemplateModel>> template = Optional.empty();
		private Optional<Class<?>> templateBase = Optional.empty();
		private Optional<String> templateResource = Optional.empty();

		public Builder withUri(String uri) {
			this.uri = Optional.of(uri);
			return this;
		}
		
		public Builder withContent(String content) {
			return withContent(() -> content);
		}
		
		public Builder withContent(Supplier<Object> content) {
			this.content = Optional.of(content);
			return this;
		}
		
		public Builder withContentType(String contentType) {
			this.contentType = Optional.of(contentType);
			return this;
		}
		
		public Builder withTemplate(Supplier<Object> content) {
			this.content = Optional.of(content);
			return this;
		}
		
		public Builder onClose(Runnable onClose) {
			this.onClose = Optional.of(onClose);
			return this;
		}

		@Override
		public Page build() {
			return new Page(this);
		}
	}
	
	private final Optional<String> contentType;
	private final Optional<Supplier<Object>> content;
	private final Optional<Runnable> onClose;
	private final String uri;
	
	private Page(Builder bldr) {
		super(bldr);
		this.contentType = bldr.contentType;
		this.content = bldr.content;
		this.onClose = bldr.onClose;
		this.uri = bldr.uri.orElseThrow(() -> new IllegalArgumentException("All pages require a URI."));
	}
	
	public void render(Transaction tx) {
		tx.response(contentType.orElse("text/plain"), content.map(Supplier::get).orElse(""));
	}

	@Override
	public void close() throws IOException {
		onClose.ifPresent(Runnable::run);
	}
}
