package com.sshtools.jenny.tunnels;

public interface TunnelProvider {
	TunnelInstance acquire(TunnelRequest request);
}
