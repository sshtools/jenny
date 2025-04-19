package com.sshtools.jenny.messaging;

import java.security.Principal;
import java.util.Optional;

public interface MessagingPrincipal extends Principal {
	Optional<String> email();
	
	Optional<String> mobileNumber();
	
	Optional<String> description();

	default String getEmail() {
		return email().get();
	}
	
	default String getDescription() {
		return description().get();
	}
	
	default String getMobileNumber() {
		return mobileNumber().get();
	}
}
