/**
 * Copyright © 2023 JAdaptive Limited (support@jadaptive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.jenny.gravatar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.avatars.AvatarProvider;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebLog;
import com.sshtools.jini.INI;
import com.sshtools.jini.INIWriter;
import com.sshtools.uhttpd.UHTTPD.Status;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class Gravatar implements Plugin {
	final static Log LOG = Logs.of(WebLog.WEB);

	private Web web; 
	private Path dir;

	@Override
	public void afterOpen(PluginContext context) throws IOException {
		web = context.plugin(Web.class);
		dir = Paths.get(System.getProperty("user.dir")).resolve("tmp").resolve("gravatar.com");
		Files.createDirectories(dir);

		context.autoClose(
			web.extensions().group().point(AvatarProvider.class,(a) -> new GravatarAvatarProvider()),
			web.router().route().
				handle("/gravatar/(.*)", this::gravatar).
				build()
		);
	}

	public void saveOrUpdate(GravatarProfile profile) {
		var f = dir.resolve(profile.uuid() + ".ini");
		var ini = INI.create();
		ini.put("name", profile.name());
		ini.put("uuid", profile.uuid());
		ini.put("last-modified", profile.lastModified().getEpochSecond());
		if(profile.contentType() != null)
			ini.put("content-type", profile.contentType());
		if(profile.expire() != null)
			ini.put("expire", profile.expire().getEpochSecond());
		
		try {
			new INIWriter.Builder().build().write(ini, f);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	Optional<GravatarProfile> getByUUID(String uuid) {
		var f = dir.resolve(uuid + ".ini");
		if(Files.exists(f)) {
			var ini = INI.fromFile(f);
			var prf = new GravatarProfile(ini.get("name"), Instant.ofEpochSecond(ini.getLong("last-modified")));
			prf = prf.exists(ini.get("content-type", null));
			prf = prf.expire(ini.getLongOr("expire").map(Instant::ofEpochSecond).orElse(null));
			return Optional.of(prf);
		}
		else {
			return Optional.empty();
		}
	}

	public Path directory() {
		return dir;
	}
	
	public void gravatar(Transaction tx) {
		var spec = tx.match(0).split("/");
		var uuid = spec[0];
		var size = spec.length < 2 ? Optional.empty() : Optional.of(Integer.parseInt(spec[1]));
		getByUUID(uuid).ifPresentOrElse(av -> {
			var path = directory().resolve(uuid);
			try {
				var lastMod = Files.getLastModifiedTime(path);
				if(tx.modified(lastMod.toMillis())) {
					tx.responseLength(Files.size(path));
					tx.responseType(av.contentType());
					try(var wtr = Channels.newOutputStream(tx.responseWriter())) {
						try(var rdr = Files.newInputStream(path)) {
							rdr.transferTo(wtr);
						}
					}
				}	
			}
			catch(IOException ioe) {
				throw new UncheckedIOException(ioe);
			}
		}, () -> tx.responseCode(Status.NOT_FOUND));
		
	}
	
//	@RequestMapping(value= { "/gravatar/{uuid}" }, method = { RequestMethod.GET })
//	@ResponseStatus(value = HttpStatus.OK)	
//	public void gravatar(WebRequest webRequest, HttpServletRequest request, HttpServletResponse response, 
//			@PathVariable String uuid, @PathVariable Optional<Integer> size) throws Exception {
//		
//		var av = gravatarService.getObjectByUUID(uuid);
//		var path = gravatarService.getDirectory().resolve(uuid);
//		var lastMod = Files.getLastModifiedTime(path).toMillis();
//		if(webRequest.checkNotModified(lastMod)) {
//			response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
//			return;
//		}
//		else {
//			response.setContentType(av.getContentType());
//			response.setContentLengthLong(Files.size(path));
//			response.setDateHeader("Last-Modified", lastMod);
//			try(var in = Files.newInputStream(path)) {
//				in.transferTo(response.getOutputStream());
//			}
//		}
//	}
	
}
