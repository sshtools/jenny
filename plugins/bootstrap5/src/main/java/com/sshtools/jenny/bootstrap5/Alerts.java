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
package com.sshtools.jenny.bootstrap5;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Supplier;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebLog;
import com.sshtools.jenny.web.WebState;
import com.sshtools.tinytemplate.Templates.TemplateModel;

@SuppressWarnings("serial")
public final class Alerts extends ArrayList<Alerts.Alert> {
	final static Log LOG = Logs.of(WebLog.ALERTS);
	 
	public record Action(String path, String text, Style style, Optional<String> icon) { }
	
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
				Alerts.of(new Builder().fromException(e).
						withBundle(bundle).
						withBundle(Alerts.class).
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
		private Optional<String> title = Optional.empty();
		private Optional<String> descriptionKey = Optional.empty();
		private Optional<String> description = Optional.empty();
		private Optional<Object[]> titleArgs = Optional.empty();
		private Optional<Object[]> descriptionArgs = Optional.empty();
		private Style style = Style.LIGHT;
		private Set<Action> actions = new LinkedHashSet<>();
		private boolean iconForStyle = true;
		
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
			withStyle(Style.DANGER);
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

		public Builder withStyle(Style style) {
			this.style = style;
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
		public Builder withoutIconForStyle() {
			return withIconForStyle(false);
		}

		public Builder withIconForStyle(boolean iconForStyle) {
			this.iconForStyle = iconForStyle;
			return this;
		}

		public Alert build() {
			return new Alert(style, 
				this::resolveTitle,
				this::resolveDescription,
				icon,
				iconForStyle,
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
				ResourceBundle.getBundle(this.bundleClass.orElse(Alerts.class).getName(), 
							this.locale.orElse(Locale.getDefault()), 
							this.bundleClass.orElse(Web.class).getClassLoader()
				)
			);
		}
	}
	public final static Alerts of(Collection<Alert> alerts) {
		return new Alerts(alerts);
	}

	public final static Alerts of(Alert... alerts) {
		return of(Arrays.asList(alerts));
	}
	
	public final static Optional<TemplateModel> flashed() {
			var webState = WebState.get();
		try {
			return Optional.ofNullable((TemplateModel)webState.get(Alerts.class.getName()));
		}
		finally {
			webState.env().remove(Alerts.class.getName());
		}
	}
	
	public final static void flash(TemplateModel model) {
		WebState.get().set(Alerts.class.getName(), model);
	}

	public final static TemplateModel danger(Class<?> bundle, String key, Object... args) {
		return of(new Builder().withStyle(Style.DANGER).withTitleKey(key, args).withBundle(bundle).build()).template();
	}

	public final static TemplateModel danger(String message) {
		return of(new Builder().withStyle(Style.DANGER).withTitle(message).build()).template();
	}

	public final static TemplateModel primary(String message) {
		return of(new Builder().withStyle(Style.PRIMARY).withTitle(message).build()).template();
	}

	public final static TemplateModel primary(Class<?> bundle, String key, Object... args) {
		return of(new Builder().withStyle(Style.PRIMARY).withTitleKey(key, args).withBundle(bundle).build()).template();
	}

	public final static TemplateModel success(String message) {
		return of(new Builder().withStyle(Style.SUCCESS).withTitle(message).build()).template();
	}

	public final static TemplateModel success(Class<?> bundle, String key, Object... args) {
		return of(new Builder().withStyle(Style.SUCCESS).withTitleKey(key, args).withBundle(bundle).build()).template();
	}

	public final static TemplateModel warning(String message) {
		return of(new Builder().withStyle(Style.WARNING).withTitle(message).build()).template();
	}

	public final static TemplateModel warning(Class<?> bundle, String key, Object... args) {
		return of(new Builder().withStyle(Style.WARNING).withTitleKey(key, args).withBundle(bundle).build()).template();
	}

	public final static TemplateModel info(Class<?> bundle, String key, Object... args) {
		return of(new Builder().withStyle(Style.INFO).withTitleKey(key, args).withBundle(bundle).build()).template();
	}

	public final static TemplateModel info(String message) {
		return of(new Builder().withStyle(Style.INFO).withTitle(message).build()).template();
	}

	public final static TemplateModel light(Class<?> bundle, String key, Object... args) {
		return of(new Builder().withStyle(Style.LIGHT).withTitleKey(key, args).withBundle(bundle).build()).template();
	}

	public final static TemplateModel light(String message) {
		return of(new Builder().withStyle(Style.LIGHT).withTitle(message).build()).template();
	}

	public final static TemplateModel dark(Class<?> bundle, String key, Object... args) {
		return of(new Builder().withStyle(Style.DARK).withTitleKey(key, args).withBundle(bundle).build()).template();
	}

	public final static TemplateModel dark(String message) {
		return of(new Builder().withStyle(Style.DARK).withTitle(message).build()).template();
	}

	public enum Style {
		PRIMARY, SECONDARY, SUCCESS, DANGER, WARNING, INFO, LIGHT, DARK;
		
		public String icon() {
			switch(this) {
			case SUCCESS:
				return "check-circle-fill";
			case DANGER:
				return "sign-stop-fill";
			case WARNING:
				return "exclamation-triangle-fill";
			case INFO:
				return "info-circle-fill";
			default:
				return null;
			}
		}
	}

	public record Alert(Style style, Supplier<String> title, Supplier<Optional<String>> description, Optional<String> icon, boolean iconForStyle, Action... actions) {
		public TemplateModel ofContent(String content) {
			
			var templ = TemplateModel.ofContent(content).
					variable("style", () -> style.name().toLowerCase()).
					variable("icon", () -> {
						if(icon.isEmpty()) {
							return style.icon();
						}
						else
							return icon.get();
					}).
					variable("title", title). 
					list("actions", (innerContent) -> Arrays.asList(actions).stream().map(action -> 
						TemplateModel.ofContent(content).
							variable("text", action::text).
							variable("path", action::path).
							variable("style", () -> action.style().name().toLowerCase()).
							variable("icon", action::icon)
					).toList()
			);
			
			description.get().ifPresent(d -> templ.variable("description", () -> d));
			
			return templ;
		}
	}

	public Alerts(Collection<Alert> alerts) {
		super(alerts);
	}

	public TemplateModel template() {
		return TemplateModel.ofResource(Alerts.class, "alerts.html").
				list("items", (content) -> stream().map(
					a -> a.ofContent(content)).toList()
				);
	}
}
