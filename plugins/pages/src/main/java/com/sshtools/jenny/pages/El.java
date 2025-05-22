package com.sshtools.jenny.pages;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class El {

	protected static abstract class ElBuilder<BLDR extends ElBuilder<BLDR, EL>, EL extends El> {
		
		private Optional<String> id = Optional.empty();
		private Supplier<Collection<Div>> children = () -> Collections.emptyList();
		
		@SuppressWarnings("unchecked")
		public BLDR id(String id) {
			this.id = Optional.of(id);
			return (BLDR)this;
		}
		
		public BLDR children(Div... children) {
			return children(Arrays.asList(children));
		}
		
		public BLDR children(Collection<Div> children) {
			return children(() -> children);
		}

		@SuppressWarnings("unchecked")
		public BLDR children(Supplier<Collection<Div>> children) {
			this.children = children;
			return (BLDR)this;
		}

		public abstract EL build(); 
	}

	protected final Optional<String> id;
	protected final Supplier<Collection<Div>> children;
	
	protected El(ElBuilder<?, ? extends El> bldr) {
		this.id = bldr.id;
		this.children = bldr.children;
	}
	
	public Supplier<Collection<Div>> children() {
		return children;
	}
	
	public Optional<String> id() {
		return id;
	}
}
