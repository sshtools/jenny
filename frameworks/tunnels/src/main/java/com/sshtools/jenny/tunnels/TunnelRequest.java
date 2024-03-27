package com.sshtools.jenny.tunnels;

import java.net.InetSocketAddress;

public final class TunnelRequest {

	public final static class Builder {

		private InetSocketAddress local = new InetSocketAddress("127.0.0.1", 0);
		private InetSocketAddress remote;

		public Builder withLocal(int port) {
			return withLocal("127.0.0.1", port);
		}

		public Builder withLocal(String host, int port) {
			return withLocal(new InetSocketAddress(host, port));
		}

		public Builder withLocal(InetSocketAddress local) {
			this.local = local;
			return this;
		}

		public Builder withRemote(InetSocketAddress remote) {
			this.remote = remote;
			return this;
		}

		public Builder withRemote(String host, int port) {
			return withRemote(new InetSocketAddress(host, port));
		}

		public Builder withRemote(int port) {
			return withRemote("127.0.0.1", port);
		}

		public TunnelRequest build() {
			if(remote == null)
				throw new IllegalStateException("Remote must be set.");
			return new TunnelRequest(this);
		}
	}

	private final InetSocketAddress local;
	private final InetSocketAddress remote;

	private TunnelRequest(Builder bldr) {
		this.local = bldr.local;
		this.remote = bldr.remote;
	}

	public InetSocketAddress local() {
		return local;
	}

	public InetSocketAddress remote() {
		return remote;
	}

}
