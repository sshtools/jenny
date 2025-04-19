package com.sshtools.jenny.gravatar;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.avatars.Avatar;
import com.sshtools.jenny.avatars.AvatarProvider;
import com.sshtools.jenny.avatars.AvatarRequest;
import com.sshtools.jenny.avatars.ImageAvatar;
import com.sshtools.jenny.web.WebLog;

public class GravatarAvatarProvider implements AvatarProvider {
	
	final static Log LOG = Logs.of(WebLog.USERS);


	private Gravatar gravatarService = PluginContext.$().plugin(Gravatar.class);

	@Override
	public Optional<Avatar> find(AvatarRequest request) {
		if (request.email().isPresent()) {
			var email = request.email().get();
			var profile = findProfile(email);
			
			if (profile == null || !profile.exists())
				return Optional.empty();
			else {
				return Optional.of(new ImageAvatar(request, "/gravatar/[uuid]".replace("[uuid]", profile.uuid()), "user-avatar", "avatar-gravatar"));
			}

		}
		return Optional.empty();
	}

	private GravatarProfile findProfile(String email) {
		String uuid;
		try {
			uuid = UUID.nameUUIDFromBytes(email.toLowerCase().getBytes("UTF-8")).toString();
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
		
		GravatarProfile profile;
		
		var res = gravatarService.getByUUID(uuid);
		if(res.isPresent()) {
			profile = res.get();
			if((profile.exists() != Files.exists(pathForProfile(profile))) || Instant.now().isAfter(profile.expire())) {
				updateGravatar(profile);
			}
		} else {
			/* Not found, check with gravatar */
			profile = new GravatarProfile(email);
			updateGravatar(profile);
		}
		return profile;
	}

	@Override
	public int weight() {
		return 100;
	}

	private GravatarProfile updateGravatar(GravatarProfile profile)  {
		var email = profile.name();
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			var hashbytes = digest.digest(email.getBytes(StandardCharsets.UTF_8));
			
			try {
				var clientBldr = HttpClient.newBuilder();
				var client = clientBldr.build();
				var tokenRequest = HttpRequest.newBuilder()
						.uri(new URI("https://gravatar.com/avatar/" + HexFormat.of().formatHex(hashbytes) + "?d=404"))
					    .GET()
					    .build();
				var path = pathForProfile(profile);
				var tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofFile(path));
				
				if(tokenResponse.statusCode() == 200) {
					profile = profile.exists(tokenResponse.headers().firstValue("Content-Type").orElse("application/octet-stream"));
				}
				else {
					Files.delete(path);
				}
				profile = profile.expire(Instant.now().plus(1, ChronoUnit.DAYS));
				gravatarService.saveOrUpdate(profile);
			}
			catch(IOException | InterruptedException | URISyntaxException ioe) {
				
				LOG.warning("Failed to check for Gravatar avatar. {0}", ioe.getMessage());
				
				profile = profile.expire(Instant.now().plus(30, ChronoUnit.MINUTES));
				gravatarService.saveOrUpdate(profile);
			}
			return profile;
		}
		catch(NoSuchAlgorithmException nsae) {
			throw new IllegalStateException(nsae);
		}
	}

	private Path pathForProfile(GravatarProfile profile) {
		return gravatarService.directory().resolve(profile.uuid());
	}

}
