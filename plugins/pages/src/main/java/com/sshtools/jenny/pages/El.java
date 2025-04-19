package com.sshtools.jenny.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class El {

	protected static abstract class ElBuilder<BLDR extends ElBuilder<BLDR, EL>, EL extends El> {
		
		private Optional<String> id = Optional.empty();
		private Set<Div> children = new LinkedHashSet<>();
		
		@SuppressWarnings("unchecked")
		public BLDR id(String id) {
			this.id = Optional.of(id);
			return (BLDR)this;
		}
		
		public BLDR children(Div... children) {
			return children(Arrays.asList(children));
		}
		
		public BLDR children(Collection<Div> children) {
			this.children.clear();
			return addChildren(children);
		}
		
		public BLDR addChildren(Div... children) {
			return addChildren(Arrays.asList(children));
		}
		
		@SuppressWarnings("unchecked")
		public BLDR addChildren(Collection<Div> children) {
			this.children.addAll(children);
			return (BLDR)this;
		}

		public abstract EL build(); 
	}

	protected final Optional<String> id;
	protected final List<Div> children;
	
	protected El(ElBuilder<?, ? extends El> bldr) {
		this.id = bldr.id;
		this.children = Collections.unmodifiableList(new ArrayList<>(bldr.children));
	}
	
	public List<Div> children() {
		return children;
	}
	
	public Optional<String> id() {
		return id;
	}
}
