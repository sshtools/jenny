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
package com.sshtools.jenny.avatars;

import java.io.Closeable;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.api.ExtendedUserPrincipal;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebLog;
import com.sshtools.jenny.web.WebState;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Status;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class Avatars implements Plugin {
	final static Log LOG = Logs.of(WebLog.WEB);

	private Web web;
	private Optional<Function<String, Optional<Principal>>> userProvider = Optional.empty();

	@Override
	public void afterOpen(PluginContext context) {
		web = context.plugin(Web.class);
		
		context.autoClose(
				web.extensions().group().point(AvatarProvider.class,(a) -> new DefaultAvatarProvider()),
				web.extensions().group().point(AvatarRenderer.class,(a) -> new HTMLAvatarRenderer()),
				web.router().route().
					withClasspathResources("/avatars/default/(.*)", Avatars.class.getClassLoader(), "avatars/default").
					build());
	}

	public void handle(Transaction tx) {
		var spec = tx.match(0).split("/");
		var uuid = spec[0];
		var size = spec.length < 2 ? Optional.empty() : Optional.of(Integer.parseInt(spec[1]));

		var usr = userProvider.map(up ->
			up.apply(uuid)
		).orElseGet(() -> {
			var current = WebState.get().user();
			if(current.isPresent()) {
				var currentUser = current.get();
				if(currentUser instanceof ExtendedUserPrincipal exp && uuid.equals(exp.getUuid())) {
					return Optional.of(currentUser);
				}
				else if(currentUser.getName().equals(uuid)) {
					return Optional.of(currentUser);
				}
				return Optional.empty();
			}
			else
				throw new IllegalStateException("""
					Avatars only work for either the current user, or 
					another user if a user provider is installed. You are not
					currently logged in, so your avatar could not be determined.
					Either install a user provider, or don't show avatars when not
					logged in. 
						""");
		});
		
		usr.ifPresentOrElse(u -> {
			
		}, () -> tx.responseCode(Status.NOT_FOUND));
	}

	public Closeable userProvider(Function<String,Optional<Principal>> userProvider) {
		if (this.userProvider.isEmpty()) {
			this.userProvider = Optional.of(userProvider);
			return () -> this.userProvider = Optional.empty();
		} else {
			throw new IllegalStateException("Avatars already has a user provider.");
		}
	}

	public List<Avatar> avatars(AvatarRequest request) {
		return  web.extensions().points(AvatarProvider.class).stream().
				map(x -> x.apply(request)).
				map(a -> a.find(request)).
				filter(Optional::isPresent).
				map(Optional::get).
				toList();
	}

	public Avatar avatar(AvatarRequest request) {
		 return web.extensions().points(AvatarProvider.class).stream().
			map(x -> x.apply(request)).
			map(a -> a.find(request)).
			filter(Optional::isPresent).map(Optional::get).
			findFirst().orElseThrow(() -> new IllegalArgumentException("No avatar, not even the default."));
	}

	public TemplateModel render(AvatarRequest request) {
			var av = avatar(request);
		return web.extensions().points(AvatarRenderer.class).stream().
				map(x -> x.apply(av)).
				map(a -> a.render(av)).
				filter(Optional::isPresent).
				map(Optional::get).
				findFirst().
				orElseThrow(() -> new IllegalArgumentException("No renderer, not even the default."));
	}


//	@Autowired
//	private AvatarService avatarService;
//
//	@Autowired
//	private UserService userService;
//	
//	@RequestMapping(value= { "/app/api/userLogo/fetch/{uuid}", "/app/api/userLogo/fetch/{uuid}/{size}" }, method = { RequestMethod.GET })
//	@ResponseStatus(value = HttpStatus.OK)
//	@AuthenticatedContext(system = true)
//	public void legacyGravatar(WebRequest webRequest, HttpServletRequest request, HttpServletResponse response, 
//			@PathVariable String uuid, @PathVariable Optional<Integer> size) throws Exception {
//		
//		try {
//			var usr = userService.getObjectByUUID(uuid);
//			var bldr = new AvatarRequest.Builder();
//			bldr.withUsername(usr.getUsername());
//			if(StringUtils.isNotBlank(usr.getEmail()))
//				bldr.withEmail(usr.getEmail());
//			if(StringUtils.isNotBlank(usr.getName()))
//				bldr.withName(usr.getName());
//			
//			var av = avatarService.avatar(bldr.build());
//			
//			/* TODO: Bit of a hack. We should be able to 'render' to things other than an Element */
//			var el = av.render();
//			var img = el.selectFirst("img");
//			if(img != null) {
//				var href = img.attr("src");
//				if(href != null && href.startsWith("/")) {
//					request.getRequestDispatcher(href).forward(request, response);
//					return;
//				}
//			}
//		} catch(ObjectNotFoundException e) {
//		
//		}
//		
//		response.sendError(HttpServletResponse.SC_NOT_FOUND);
//		return;
//	}
}
