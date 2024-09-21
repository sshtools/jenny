package com.sshtools.jenny.alertcentre;

import java.security.Principal;
import java.util.function.Consumer;

import com.sshtools.bootlace.api.UncheckedCloseable;

public interface AlertCentreContext {
	
	boolean isAdministrator(Principal principal);
	
	boolean isSystem(Principal principal);
	
	void onLogin(Consumer<Principal> principal);

	UncheckedCloseable systemContext();

	UncheckedCloseable administratorContext();

	UncheckedCloseable userContext();
}
