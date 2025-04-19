package com.sshtools.jenny.avatars;

import java.security.Principal;
import java.util.Optional;

import com.sshtools.jenny.api.ExtendedUserPrincipal;

public final class AvatarRequest {

	public final static class Builder {
		private Optional<Principal> user = Optional.empty();
		private Optional<String> username = Optional.empty();
		private Optional<String> name = Optional.empty();
		private Optional<String> mobilePhone = Optional.empty();
		private Optional<String> email = Optional.empty();
		private Optional<String> id = Optional.empty();

		public Builder forUser(Principal user) {

			this.user = Optional.of(user);

			if (user instanceof ExtendedUserPrincipal xprinc) {
				if (xprinc.getEmail() != null && xprinc.getEmail().length() > 0)
					withEmail(xprinc.getEmail());
				if (user.getName() != null && user.getName().length() > 0)
					withName(user.getName());
				if (xprinc.getMobilePhone() != null && xprinc.getMobilePhone().length() > 0)
					withMobilePhone(xprinc.getMobilePhone());
				return withUsername(user.getName()).withId(xprinc.getUuid());
			} else {
				return withUsername(user.getName()).withId(user.getName());
			}
		}

		public Builder withUsername(String username) {
			this.username = Optional.of(username);
			return this;
		}

		public Builder withName(String name) {
			this.name = Optional.of(name);
			return this;
		}

		public Builder withEmail(String email) {
			this.email = Optional.of(email);
			return this;
		}

		public Builder withMobilePhone(String mobilePhone) {
			this.mobilePhone = Optional.of(mobilePhone);
			return this;
		}

		public Builder withId(String id) {
			this.id = Optional.ofNullable(id);
			return this;
		}

		public AvatarRequest build() {
			return new AvatarRequest(this);
		}
	}

	private final Optional<Principal> user;
	private final Optional<String> username;
	private final Optional<String> email;
	private final Optional<String> mobilePhone;
	private final Optional<String> name;
	private final Optional<String> id;

	private AvatarRequest(Builder builder) {
		this.user = builder.user;
		this.username = builder.username;
		this.email = builder.email;
		this.mobilePhone = builder.mobilePhone;
		this.name = builder.name;
		this.id = builder.id;
	}

	public Optional<Principal> user() {
		return user;
	}

	public Optional<String> username() {
		return username;
	}

	public Optional<String> email() {
		return email;
	}

	public Optional<String> mobilePhone() {
		return mobilePhone;
	}

	public Optional<String> name() {
		return name;
	}

	public Optional<String> id() {
		return id;
	}
}
