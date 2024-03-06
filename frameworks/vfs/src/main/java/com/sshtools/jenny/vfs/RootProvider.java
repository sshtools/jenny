package com.sshtools.jenny.vfs;

import java.nio.file.Path;

public interface RootProvider {
	Iterable<Path> roots();
}
