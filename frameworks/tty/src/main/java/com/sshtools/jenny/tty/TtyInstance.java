package com.sshtools.jenny.tty;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

public interface TtyInstance extends Closeable {
	
	void resize(int width, int height);

	InputStream in();
	
	InputStream err();
	
	OutputStream out();
	
	String name();
	
	int width();
	
	int height();
}
