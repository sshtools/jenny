package com.sshtools.jenny.pages;

public final class Div extends El {

	protected Div(DivBuilder bldr) {
		super(bldr);
	}

	public final static class DivBuilder extends ElBuilder<DivBuilder, Div> {

		@Override
		public Div build() {
			return new Div(this);
		}
		
	}
}
