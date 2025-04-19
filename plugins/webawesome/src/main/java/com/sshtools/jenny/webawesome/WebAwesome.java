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
package com.sshtools.jenny.webawesome;

import static com.sshtools.bootlace.api.PluginContext.$;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Supplier;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.alertcentre.AlertCentre;
import com.sshtools.jenny.avatars.AvatarRenderer;
import com.sshtools.jenny.avatars.AvatarRequest;
import com.sshtools.jenny.avatars.Avatars;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebLog;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebModule.WebModuleResource;
import com.sshtools.jenny.web.WebState;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.bootstrap.forms.Field;
import com.sshtools.tinytemplate.bootstrap.forms.Form;
import com.sshtools.tinytemplate.bootstrap.forms.Form.Icon;
import com.sshtools.uhttpd.UHTTPD.Transaction;
import com.sshtools.tinytemplate.bootstrap.forms.Framework;
import com.sshtools.tinytemplate.bootstrap.forms.InputType;
import com.sshtools.tinytemplate.bootstrap.forms.Template;
import com.sshtools.tinytemplate.bootstrap.forms.TemplateResource;

public class WebAwesome implements Plugin {

	private final Web web 					= $().plugin(Web.class);
	private final AlertCentre alertCentre   = $().plugin(AlertCentre.class);
	private final Avatars avatars   		= $().plugin(Avatars.class);
	
	private static Map<Icon, Set<String>> DEFAULT_ICONS = Map.of(Icon.TRASH, Set.of("fa-solid", "fa-trash"));

	private static Map<Template, TemplateResource> DEFAULT_TEMPLATES = Map.of(
			Template.FORM, new TemplateResource(WebAwesome.class, "webawesome.form.template.html"), 
			Template.ROW, new TemplateResource(Form.class, "row.template.html"), 
			Template.GROUP, new TemplateResource(Form.class, "group.template.html"), 
			Template.INPUT, new TemplateResource(WebAwesome.class, "webawesome.input.template.html"), 
			Template.FIELD, new TemplateResource(WebAwesome.class, "webawesome.field.template.html"), 
			Template.COLUMN, new TemplateResource(Form.class, "column.template.html"), 
			Template.TEMPLATE, new TemplateResource(Form.class, "template.template.html")
	);

	public final static class WebAwesomeFramework implements Framework {

		@Override
		public Map<Icon, Set<String>> defaultIcons() {
			return DEFAULT_ICONS;
		}

		@Override
		public Map<Template, TemplateResource> defaultTemplates() {
			return DEFAULT_TEMPLATES;
		}

		@Override
		public String processValueRender(Field<?, ?> field, String raw) {
			if(field.resolveInputType() == InputType.SELECT && field.resolveMultiple()) {
				try {
					return URLEncoder.encode(raw, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new UncheckedIOException(e);
				}
			}
			else {
				return Framework.super.processValueRender(field, raw);
			}
		}

		@Override
		public String processValueSubmit(Field<?, ?> field, String raw) {
			if(field.resolveInputType() == InputType.SELECT && field.resolveMultiple()) {
				try {
					return URLDecoder.decode(raw, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new UncheckedIOException(e);
				}
			}
			else {
				return Framework.super.processValueSubmit(field, raw);
			}
		}
	}

	private final static WebModule FONTAWESOME_MODULE = new WebModule.Builder().withUri("fontawesome/")
			.asDirectory(WebAwesome.class, "fontawesome-pro-6.4.0-web")
			.withResources(WebModuleResource.of("css/all.css")).build();

	public final static WebModule MODULE = new WebModule.Builder()
			.withUrl("https://early.webawesome.com/webawesome@3.0.0-alpha.11/dist").withRequires(FONTAWESOME_MODULE)
			.withResources(
				WebModuleResource.jsModule("webawesome.loader.js"),
				WebModuleResource.of("styles/themes/default.css"),
				WebModuleResource.of("styles/webawesome.css")
			)
			.build();
	
	public final static WebModule JENNYAWESOME_MODULE = new WebModule.Builder().
			withUri("webawesome/").
			asDirectory(WebAwesome.class).
			withRequires(WebAwesome.MODULE).
			withResources(
				WebModuleResource.of("wafa.alertcentre.frag.js"),
				WebModuleResource.of("jennyawesome.js")
			).
			build();
	
	@SuppressWarnings("serial")
	public final static class Callouts extends ArrayList<Callouts.Callout> {
		final static Log LOG = Logs.of(WebLog.ALERTS);
		 
		public record Action(String path, String text, Variant variant, Optional<String> icon, Optional<String> iconVariant) {
			public  Action(String path, String text, Variant variant, Optional<String> icon) {
				this(path, text, variant, icon, Optional.empty());
			}
			public Action(String path, String text, Variant variant) {
				this(path, text, variant, Optional.empty());
			}
		}
		
		@FunctionalInterface
		public interface AlertTask {
			void call() throws Exception;
		}
		
		public static TemplateModel alertable(Class<?> bundle, TemplateModel model, AlertTask alertable) {
			return alertable(bundle, model, alertable, null);
		}
		
		public static TemplateModel alertable(Class<?> bundle, TemplateModel model, AlertTask alertable, AlertTask onSuccess) {
			try {
				alertable.call();
				if(onSuccess != null)
					onSuccess.call();
				return model;
			}
			catch(Exception e) {
				LOG.error("Alertable task failed.", e);
				model.include("alerts", () ->
					Callouts.of(new Builder().fromException(e).
							withBundle(bundle).
							withBundle(Callouts.class).
							build()).template()
				);
				return model;
			}
		}
		
		public final static class Builder {
			private Optional<Class<?>> bundleClass = Optional.empty();
			private Optional<ResourceBundle> bundle = Optional.empty();
			private Optional<Locale> locale = Optional.empty();
			private Optional<String> titleKey = Optional.empty();
			private Optional<String> icon = Optional.empty();
			private Optional<String> iconVariant = Optional.empty();
			private Optional<String> title = Optional.empty();
			private Optional<String> descriptionKey = Optional.empty();
			private Optional<String> description = Optional.empty();
			private Optional<Object[]> titleArgs = Optional.empty();
			private Optional<Object[]> descriptionArgs = Optional.empty();
			private Variant variant = Variant.NEUTRAL;
			private Set<Action> actions = new LinkedHashSet<>();
			private boolean iconForVariant = true;
			
			public Builder fromException(Throwable exception) {
				if(exception.getMessage() == null) {
					withTitleKey("exceptionNoMessage");
				}
				else if(exception instanceof RuntimeException) {
					withTitleKey("exception", exception.getMessage());
				}
				else {
					withTitleKey("error", exception.getMessage());
				}
				withVariant(Variant.DANGER);
				return this;
			}

			public Builder withBundle(Class<?> clazz) {
				this.bundleClass = Optional.of(clazz);
				return this;
			}

			public Builder withBundle(ResourceBundle bundle) {
				this.bundle = Optional.of(bundle);
				return this;
			}

			public Builder withLocale(Locale locale) {
				this.locale = Optional.of(locale);
				return this;
			}

			public Builder withVariant(Variant variant) {
				this.variant = variant;
				return this;
			}

			public Builder withTitleKey(String key, Object... args) {
				this.titleKey = Optional.of(key);
				this.titleArgs = Optional.of(args);
				this.title = Optional.empty();
				return this;
			}

			public Builder withIcon(String icon) {
				this.icon = Optional.of(icon);
				return this;
			}

			public Builder withIconVariant(String iconVariant) {
				this.iconVariant = Optional.of(iconVariant);
				return this;
			}

			public Builder withTitle(String text) {
				this.titleKey = Optional.empty();
				this.titleArgs = Optional.empty();
				this.title = Optional.of(text);
				return this;
			}

			public Builder withDescriptionKey(String key, Object... args) {
				this.descriptionKey = Optional.of(key);
				this.descriptionArgs = Optional.of(args);
				this.description = Optional.empty();
				return this;
			}

			public Builder withDescription(String text) {
				this.descriptionKey = Optional.empty();
				this.descriptionArgs = Optional.empty();
				this.description = Optional.of(text);
				return this;
			}
			
			public Builder withActions(Action... actions) {
				this.actions.addAll(Arrays.asList(actions));
				return this;
			}
			public Builder withoutIconForVariant() {
				return withIconForVariant(false);
			}

			public Builder withIconForVariant(boolean iconForVariant) {
				this.iconForVariant = iconForVariant;
				return this;
			}

			public Callout build() {
				return new Callout(variant, 
					this::resolveTitle,
					this::resolveDescription,
					icon,
					iconVariant,
					iconForVariant,
					actions.toArray(new Action[0]));
			}
			
			private String resolveTitle() {
				return title.orElseGet(() -> 
					MessageFormat.format(resolveBundleString(titleKey), resolveArgs(titleArgs))
				);
			}
			private String resolveBundleString(Optional<String> key) {
				return resolveBundleString(key.orElseThrow(() -> new IllegalStateException("Neither a message key or raw text was provided for alert.")));
			}
			
			private String resolveBundleString(String key) {
				return buildBundle().getString(key);
			}
			
			private Optional<String> resolveDescription() {
				return description.or(() -> 
					descriptionKey.map(k -> 
						MessageFormat.format(resolveBundleString(k), resolveArgs(descriptionArgs))
					)
				);
			}
			
			private Object[] resolveArgs(Optional<Object[]> args) {
				return args.orElse(TemplateModel.NO_ARGS);
			}

			private ResourceBundle buildBundle() {
				return bundle.orElseGet(() -> 
					ResourceBundle.getBundle(this.bundleClass.orElse(Callouts.class).getName(), 
								this.locale.orElse(Locale.getDefault()), 
								this.bundleClass.orElse(Web.class).getClassLoader()
					)
				);
			}
		}
		public final static Callouts of(Collection<Callout> alerts) {
			return new Callouts(alerts);
		}

		public final static Callouts of(Callout... alerts) {
			return of(Arrays.asList(alerts));
		}
		
		public final static Optional<TemplateModel> flashed() {
				var webState = WebState.get();
			try {
				return Optional.ofNullable((TemplateModel)webState.get(Callouts.class.getName()));
			}
			finally {
				webState.env().remove(Callouts.class.getName());
			}
		}
		
		public final static void flash(TemplateModel model) {
			WebState.get().set(Callouts.class.getName(), model);
		}

		public final static TemplateModel danger(Class<?> bundle, String key, Object... args) {
			return of(asDanger(bundle, key, args)).template();
		}

		public static Callout asDanger(Class<?> bundle, String key, Object... args) {
			return new Builder().withVariant(Variant.DANGER).withTitleKey(key, args).withBundle(bundle).build();
		}

		public final static TemplateModel danger(String message) {
			return of(asDanger(message)).template();
		}

		public static Callout asDanger(String message) {
			return new Builder().withVariant(Variant.DANGER).withTitle(message).build();
		}

		public final static TemplateModel neutral(String message) {
			return of(asNeutral(message)).template();
		}

		public static Callout asNeutral(String message) {
			return new Builder().withVariant(Variant.NEUTRAL).withTitle(message).build();
		}

		public final static TemplateModel neutral(Class<?> bundle, String key, Object... args) {
			return of(asNeutral(bundle, key, args)).template();
		}

		public static Callout asNeutral(Class<?> bundle, String key, Object... args) {
			return new Builder().withVariant(Variant.NEUTRAL).withTitleKey(key, args).withBundle(bundle).build();
		}

		public final static TemplateModel success(String message) {
			return of(asSuccess(message)).template();
		}

		public static Callout asSuccess(String message) {
			return new Builder().withVariant(Variant.SUCCESS).withTitle(message).build();
		}

		public final static TemplateModel success(Class<?> bundle, String key, Object... args) {
			return of(asSuccess(bundle, key, args)).template();
		}

		public static Callout asSuccess(Class<?> bundle, String key, Object... args) {
			return new Builder().withVariant(Variant.SUCCESS).withTitleKey(key, args).withBundle(bundle).build();
		}

		public final static TemplateModel warning(String message) {
			return of(new Builder().withVariant(Variant.WARNING).withTitle(message).build()).template();
		}

		public final static TemplateModel warning(Class<?> bundle, String key, Object... args) {
			return of(asWarning(bundle, key, args)).template();
		}

		public static Callout asWarning(Class<?> bundle, String key, Object... args) {
			return new Builder().withVariant(Variant.WARNING).withTitleKey(key, args).withBundle(bundle).build();
		}

		public final static TemplateModel brand(Class<?> bundle, String key, Object... args) {
			return of(asBrand(bundle, key, args)).template();
		}

		public static Callout asBrand(Class<?> bundle, String key, Object... args) {
			return new Builder().withVariant(Variant.BRAND).withTitleKey(key, args).withBundle(bundle).build();
		}

		public final static TemplateModel brand(String message) {
			return of(asBrand(message)).template();
		}

		public static Callout asBrand(String message) {
			return new Builder().withVariant(Variant.BRAND).withTitle(message).build();
		}

		public enum Variant {
			NEUTRAL, SUCCESS, DANGER, WARNING, BRAND;
			
			public String icon() {
				switch(this) {
				case SUCCESS:
					return "circle-check";
				case DANGER:
					return "circle-exclamation";
				case WARNING:
					return "triangle-exclamation";
				case BRAND:
					return "circle-info";
				default:
					return null;
				}
			}
		}

		public record Callout(Variant variant, Supplier<String> title, Supplier<Optional<String>> description, Optional<String> icon, Optional<String> iconVariant, boolean iconForVariant, Action... actions) {
			public TemplateModel ofContent(String content) {
				
				var templ = TemplateModel.ofContent(content).
						variable("variant", () -> variant.name().toLowerCase()).
						variable("iconVariant", () -> iconVariant.orElse("regular")).
						variable("icon", () -> {
							if(icon.isEmpty()) {
								return variant.icon();
							}
							else
								return icon.get();
						}).
						variable("title", title). 
						list("actions", (innerContent) -> Arrays.asList(actions).stream().map(action -> 
							TemplateModel.ofContent(content).
								variable("text", action::text).
								variable("path", action::path).
								variable("variant", () -> action.variant().name().toLowerCase()).
								variable("iconVariant", action::iconVariant).
								variable("icon", action::icon)
						).toList()
				);
				
				description.get().ifPresent(d -> templ.variable("description", () -> d));
				
				return templ;
			}
		}

		public Callouts(Collection<Callout> alerts) {
			super(alerts);
		}

		public TemplateModel template() {
			return TemplateModel.ofResource(Callouts.class, "callouts.frag.html").
					list("items", (content) -> stream().map(
						a -> a.ofContent(content)).toList()
					);
		}
	}
	
	public TemplateModel fragAvatar(Transaction tx) {
		return avatars.render(new AvatarRequest.Builder().
				forUser(WebState.get().user().get()).
				build());
	}

	@Override
	public void afterOpen(PluginContext context) {
		context.autoClose(
			alertCentre.toolkit(new WebAwesomeAndFontAwesomeAlertToolkit()),
			web.extensions().group().point(AvatarRenderer.class,(a) -> new WebAwesomeAvatarRenderer())
		);
	}
}
