package com.sshtools.jenny.alertcentre;

import java.util.Optional;

public interface Monitor  {
	
	public enum Scope {
		USER, ADMINISTRATOR, SYSTEM
	}
	
	default Scope scope() {
		return Scope.ADMINISTRATOR;
	}

	Optional<Notification> query();
}
