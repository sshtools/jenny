package com.sshtools.jenny.tty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TtyRequest {

	public final static class Builder {
		private Optional<String> term = Optional.empty();
		private Map<String, String> environment = new HashMap<>();
		private Optional<Path> path = Optional.empty();
		private List<String> command = new ArrayList<>();
		private int width = 80;
		private int height = 24;

		public Builder withSize(int width, int height) {
			return withWidth(width).withHeight(height);
		}

		public Builder withHeight(int height) {
			this.height = height;
			return this;
		}

		public Builder withWidth(int width) {
			this.width = width;
			return this;
		}

		public Builder withTerm(String term) {
			this.term = Optional.of(term);
			return this;
		}

		public Builder withEnvironment(Map<String, String> environment) {
			this.environment.clear();
			return addEnvironment(environment);
		}

		public Builder addEnvironment(Map<String, String> environment) {
			this.environment.putAll(environment);
			return this;
		}

		public Builder withPath(Path path) {
			this.path = Optional.of(path);
			return this;
		}

		public Builder withCommand(String... args) {
			return withCommand(Arrays.asList(args));
		}

		public Builder withCommand(List<String> args) {
			command.clear();
			return addArguments(args);
		}

		public Builder addArguments(String... args) {
			return addArguments(Arrays.asList(args));
		}

		public Builder addArguments(List<String> args) {
			this.command.addAll(args);
			return this;
		}
		
		public TtyRequest build() {
			return new TtyRequest(this);
		}
	}

	private final Optional<String> term;
	private final Map<String, String> environment;
	private final Optional<Path> path;
	private final List<String> command;
	private final int width, height;

	private TtyRequest(Builder bldr) {
		term = bldr.term;
		environment = Collections.unmodifiableMap(bldr.environment);
		path = bldr.path;
		command = Collections.unmodifiableList(bldr.command);
		width = bldr.width;
		height = bldr.height;
	}
	
	public int height() {
		return height;
	}
	
	public int width() {
		return width;
	}

	public Optional<String> term() {
		return term;
	}

	public Map<String, String> environment() {
		return environment;
	}

	public Optional<Path> path() {
		return path;
	}

	public List<String> command() {
		return command;
	}

}
