package com.sshtools.jenny.alertcentre;


import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.sshtools.jenny.alertcentre.Notification.Dismission;
import com.sshtools.jini.Data;
import com.sshtools.jini.INI.Section;

public final class Dismissal {
	
	public final static class Builder {
		private Optional<UUID> uuid = Optional.empty();
		private Optional<String> user = Optional.empty();
		private Dismission dismission = Dismission.RESTART;
		private Optional<String> version = Optional.empty();
		private String alert;
		private Optional<Instant> expire = Optional.empty();
		private Optional<Instant> serverStart = Optional.empty();
		
		public Builder fromData(Section data) {
			withUuid(data.key());
			user = data.getOr("user");
			dismission = data.getEnum(Dismission.class, "dismission", Dismission.RESTART);
			version = data.getOr("version");
			alert = data.get("alert");
			expire = data.getOr("expire").map(e -> Instant.parse(e));
			serverStart  = data.getOr("server-start").map(e -> Instant.parse(e));
			return this;
		}

		public Builder withUuid(String uuid) {
			return withUuid(UUID.fromString(uuid));
		}
		
		public Builder withUuid(UUID uuid) {
			this.uuid = Optional.of(uuid);
			return this;
		}
		
		public Builder withUser(Principal user) {
			return withUser(user.getName());
		}
		
		public Builder withUser(String user) {
			this.user = Optional.of(user);
			return this;
		}
		
		public Builder withDismission(Dismission dismission) {
			this.dismission = dismission;
			return this;
		}
		
		public Builder withVersion(String version) {
			this.version = Optional.of(version);
			return this;
		}
		
		public Builder withAlert(String alert) {
			this.alert = alert;
			return this;
		}
		
		public Builder withExpire(Instant expire) {
			this.expire = Optional.of(expire);
			return this;
		}
		
		public Builder withServerStart(Instant serverStart) {
			this.serverStart = Optional.of(serverStart);
			return this;
		}
		
		public Dismissal build() {
			return new Dismissal(this);
		}
	}
	
	
	private final UUID uuid;
	private final Optional<String> user;
	private final Dismission dismission;
	private final Optional<String> version;
	private final String alert;
	private Optional<Instant> expire = Optional.empty();
	private Optional<Instant> serverStart = Optional.empty();
	
	private Dismissal(Builder bldr) {
		this.uuid = bldr.uuid.orElseGet(UUID::randomUUID);
		this.user = bldr.user;
		this.dismission = bldr.dismission;
		this.version = bldr.version;
		this.alert = Optional.ofNullable(bldr.alert).orElseThrow(() -> new IllegalStateException("Notification key required."));
		this.expire = bldr.expire;
		this.serverStart = bldr.serverStart;
	}
	
	public UUID uuid() {
		return uuid;
	}

	public Optional<Instant> serverStart() {
		return serverStart;
	}

	public String alert() {
		return alert;
	}

	public Optional<String> user() {
		return user;
	}

	public Dismission dismission() {
		return dismission;
	}

	public Optional<String> version() {
		return version;
	}

	public Optional<Instant> expire() {
		return expire;
	}

	public boolean expired() {
		return expire.map(e -> System.currentTimeMillis() >= e.toEpochMilli()).orElse(false);
	}

	@Override
	public String toString() {
		return "Dismissal [user=" + user + ", dismission=" + dismission + ", version=" + version + ", alert=" + alert
				+ ", expire=" + expire + ", serverStart=" + serverStart + "]";
	}

	public void store(Data data) {		
		user.ifPresentOrElse(u -> data.put("user", u), () -> data.remove("user"));
		data.putEnum("dismission", dismission);		
		version.ifPresentOrElse(u -> data.put("version", u), () -> data.remove("version"));
		data.put("alert", alert);		
		expire.ifPresentOrElse(u -> data.put("expire", u.toString()), () -> data.remove("expire"));
		serverStart.ifPresentOrElse(u -> data.put("server-start", u.toString()), () -> data.remove("server-start"));
	}
	
}
