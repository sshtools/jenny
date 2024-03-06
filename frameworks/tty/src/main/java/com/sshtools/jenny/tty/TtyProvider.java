package com.sshtools.jenny.tty;

public interface TtyProvider {
	TtyInstance allocate(TtyRequest request);
}
