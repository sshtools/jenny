package com.sshtools.jenny.tunnels;

import java.io.Closeable;

public interface TunnelInstance extends Closeable {

	int port();
	
}
