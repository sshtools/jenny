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
package com.sshtools.jenny.bootswatch;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.sshtools.bootlace.api.ArtifactRef;
import com.sshtools.bootlace.api.GAV;
import com.sshtools.bootlace.api.LayerContext;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.bootlace.api.Zip;
import com.sshtools.jenny.web.GlobalTemplateDecorator;
import com.sshtools.jenny.web.NpmWebModule;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Cookie;
import com.sshtools.uhttpd.UHTTPD.CookieBuilder;
import com.sshtools.uhttpd.UHTTPD.Status;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class Bootswatch implements Plugin {
	
	final static String COOKIE_NAME = "jenny_bootswatch_theme";
	
	private final Web web 				= PluginContext.$().plugin(Web.class);
	
	public final static WebModule MODULE_BOOTSWATCH =
		NpmWebModule.of(
			Bootswatch.class, 
			GAV.ofSpec("npm:bootswatch")
		);

	private BootswatchThemeProvider decorator;
	
	public interface BootswatchThemeProvider extends Function<Transaction, GlobalTemplateDecorator> {
		Set<String> themes();
	}
	
	public final static class NpmGlobalTemplateDecoratorFactory implements BootswatchThemeProvider {

		private final Optional<ArtifactRef> bootswatchArtifact;
		private final Optional<ArtifactRef> bootstrapArtifact;
		
		private final Set<String> themes 	= new LinkedHashSet<>(Arrays.asList("default"));
		
		private NpmGlobalTemplateDecoratorFactory() {
			
			var lyrCtx = LayerContext.get(Bootswatch.class);
			
			bootswatchArtifact =  lyrCtx.findArtifact(GAV.ofSpec("npm:bootswatch"));
			bootstrapArtifact = lyrCtx.findArtifact(GAV.ofSpec("npm:bootstrap"));
			
			bootswatchArtifact.ifPresent(art -> art.path().ifPresent(path -> {
			var pattern = Pattern.compile("npm2mvn/npm/bootswatch/[^/]*/([^/]*)/.*");
			try {
				Zip.list(path).forEach(zipEntry -> {
					var matcher = pattern.matcher(zipEntry.getName());
					if(matcher.matches()) {
						themes.add(matcher.group(1));
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}));
		}

		@Override
		public GlobalTemplateDecorator apply(Transaction tx) {
			return new GlobalTemplateDecorator() {
				
				@Override
				public void decorate(TemplateModel model) {
					
					var selected = themeFromCookie(tx);
					if(selected.equals("default")) {
						bootstrapArtifact.ifPresent(art -> {
							model.variable("bootswatch.css", 
								String.format("/npm2mvn/%s/%s/%s/dist/css",
										art.gav().groupId(),
										art.gav().artifactId(),
										art.gav().version()));
						});
					}
					else {
						bootswatchArtifact.ifPresent(art -> {
							model.variable("bootswatch.css", 
									String.format("/npm2mvn/%s/%s/%s/dist/%s",
											art.gav().groupId(),
											art.gav().artifactId(),
											art.gav().version(),
											selected));
						});
					}
				}
			};
		}

		@Override
		public Set<String> themes() {
			return themes;
		}
		
	}
	
	public Bootswatch() {
		this(new NpmGlobalTemplateDecoratorFactory());
	}
	
	public Bootswatch(BootswatchThemeProvider decorator) {
		this.decorator = decorator;
	}
	
	@Override
	public void afterOpen(PluginContext context) {
		context.autoClose(
			web.router().route().
				post("/bootswatch-theme", this::switchTheme).
				build(),
			web.extensions().group().
				point(GlobalTemplateDecorator.class, (Transaction tx) -> decorator.apply(tx)));
	}
	
	/**
	 * Public API for getting a model of a fragment for a re-usable theme switcher
	 * component.
	 * 
	 * @param tx transaction
	 * @return model
	 */
	public TemplateModel fragThemeSwitcher(Transaction tx) {
		
		var selected = themeFromCookie(tx);
		
		return web.template(Bootswatch.class, "theme-switcher.frag.html").
				list("themes", (content) -> 
					decorator.themes().stream().map(name -> TemplateModel.ofContent(content).
						variable("id", name).
						condition("selected", name.equals(selected)).
						variable("name", name.substring(0, 1).toUpperCase() + name.substring(1))
					).
					toList()
				).
				bundle(Bootswatch.class);
	}

	public static String themeFromCookie(Transaction tx) {
		return tx.cookieOr(COOKIE_NAME).map(Cookie::value).orElse("default");
	}
	
	private void switchTheme(Transaction tx) {
		var content = tx.request();
		tx.cookie(new CookieBuilder().
				withName(COOKIE_NAME).
				withMaxExpiry().
				withValue(content.asFormData("theme").asString()).
				build());
		
		tx.redirect(Status.MOVED_TEMPORARILY, content.asFormData("returnTo").asString());
	}

}
