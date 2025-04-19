package com.sshtools.jenny.messaging;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecipientHolder {

	private final Optional<String> name;
	private final Optional<String> address;
	private final Optional<MessagingPrincipal> principal;
	
	static Set<String> salutations = new HashSet<String>(Arrays.asList("MR", "MS", "MRS", "DR", "PROF"));
	
	public final static String EMAIL_PATTERN = "(?:\"?([^\"]*)\"?\\s)?(?:<?(.+@[^>]+)>?)";
	public final static String GENERIC_PATTERN = "(?:\"?([^\"]*)\"?\\s)?(?:<?(.+)>?)";
	
	public static RecipientHolder ofEmailAddressSpec(String addressSpec) {
		return new RecipientHolder(addressSpec, EMAIL_PATTERN, null);
	}
	
	public static RecipientHolder ofGeneric(String addressSpec) {
		return new RecipientHolder(addressSpec, GENERIC_PATTERN, null);
	}
	
	public static RecipientHolder ofName(String name) {
		return new RecipientHolder(Optional.of(name), Optional.empty());
	}
	
	public static RecipientHolder ofAddress(String address) {
		return new RecipientHolder(Optional.empty(), Optional.of(address));
	}
	
	public static RecipientHolder ofNameAndAddress(String name, String address) {
		return new RecipientHolder(Optional.of(name), Optional.of(address));
	}
	
	public static RecipientHolder ofPrincipal(MessagingPrincipal principal) {
		return new RecipientHolder(principal);
	}
	
	public static RecipientHolder ofPrincipalAndCustomAddress(MessagingPrincipal principal, String address) {
		return new RecipientHolder(principal, address);
	}

	
	private RecipientHolder(String addressSpec, String pattern, MessagingPrincipal principal) {
		
		Pattern depArrHours = Pattern.compile(pattern);
		Matcher matcher = depArrHours.matcher(addressSpec);
		if(matcher.find()) {
			this.name = Optional.ofNullable(matcher.group(1));
			this.address = Optional.ofNullable(matcher.group(2));
		} else {
			this.address = Optional.of(addressSpec);
			this.name = Optional.empty();
		}	
		this.principal = Optional.ofNullable(principal);
	}

	private RecipientHolder(Optional<String> name, Optional<String> address) {
		this.name = name;
		this.address = address;
		this.principal = Optional.empty();
	}
	
	private RecipientHolder(MessagingPrincipal principal, String address) {
		this.name = principal.description();
		this.address = Optional.of(address);
		this.principal = Optional.empty();
	}
	
	private RecipientHolder(MessagingPrincipal principal) {
		this.name = principal.description();
		this.address = principal.email();
		this.principal = Optional.of(principal);
	}
	
	public Optional<String> address() {
		return address;
	}
	
	public String getAddress() {
		return address().get();
	}
	
	public Optional<String> name() {
		return name;
	}
	
	public String getName() {
		return name().get();
	}
	
	public Optional<String> firstName() {
		return name().map(n -> {
			int idx = n.indexOf(',');
			if(idx != -1) {
				return n.substring(idx + 1).trim();
			}
			else {
				idx = n.indexOf(' ');
				if(idx > 0) {
					String firstName = n.substring(0,  idx);
					int idx2 = n.indexOf(' ', idx+1);
					if(salutations.contains(firstName.toUpperCase()) && idx2 > 0) {
						firstName = n.substring(idx+1, idx2);
					}
					return firstName;
				}
			}
			return n;
		});
	}
	
	public Optional<MessagingPrincipal> principal() {
		return principal;
	}

	@Override
	public String toString() {
		return String.format("%s <%s>", name, address);
	}
}
