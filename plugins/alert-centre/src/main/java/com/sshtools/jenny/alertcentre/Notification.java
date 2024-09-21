package com.sshtools.jenny.alertcentre;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import com.sshtools.jenny.alertcentre.Notification.Builder.NotificationAction;
import com.sshtools.jenny.api.Resources;
import com.sshtools.tinytemplate.Templates.TemplateModel;


public final class Notification {
	
	/**
	 * How long the alert can be dismissed for
	 */
	public enum Dismission {
		/**
		 * Cannot be dismissed 
		 */
		NEVER,
		/**
		 * Can be dismissed forever (by this user) 
		 */
		FOREVER,
		/**
		 * Can be dismissed till the next upgrade 
		 */
		UPGRADE,
		/**
		 * Can be dismissed till the next login (by this user) 
		 */
		LOGIN,
		/**
		 * Can be dismissed till the next server restart 
		 */
		RESTART,
		/**
		 * Can be dismissed until later (some amount of time, by this user) 
		 */
		DURATION
	}

	/**
	 * Interface defining callback for the when an action is performed on an alert
	 * i.e. the notification popup is clicked.
	 */
	@FunctionalInterface
	public interface AlertActionListener {
		/**
		 * Called when action is invoked.
		 */
		void action() throws Exception;
	}

	/**
	 * Builds a new alert.
	 */
	public final static class Builder {

		/**
		 * Represents an action that may be invoked from an alert. The
		 *
		 */
		public static class NotificationAction {
			private final String resourceKey;
			private Optional<String> icon = Optional.empty();
			private Optional<String> text = Optional.empty();
			private Optional<String> iconVariant = Optional.empty();
			private Optional<Supplier<ResourceBundle>> bundle = Optional.empty();
			private Optional<Supplier<Class<?>>> bundleClass = Optional.empty();
			private AlertActionListener listener;
			private List<Object> args = new ArrayList<>();
			private List<String> classes = new ArrayList<>();

			NotificationAction(String resourceKey, AlertActionListener listener, String... classes) {
				super();
				this.resourceKey = resourceKey;
				this.listener = listener;
				this.classes.addAll(Arrays.asList(classes));
			}
			
			/**
			 * Text
			 * 
			 * @param text text
			 * @return this for chaining
			 */
			public NotificationAction text(String text) {
				this.text = Optional.of(text);
				return this;
			}
			
			/**
			 * Bundle, if different from the alert the action is in
			 * 
			 * @param bundle bundle
			 * @return this for chaining
			 */
			public NotificationAction bundle(ResourceBundle bundle) {
				return bundle(() -> bundle);
			}
			
			/**
			 * Bundle, if different from the alert the action is in
			 * 
			 * @param bundle bundle
			 * @return this for chaining
			 */
			public NotificationAction bundle(Supplier<ResourceBundle> bundle) {
				this.bundle = Optional.of(bundle);
				return this;
			}
			
			/**
			 * Bundle, if different from the alert the action is in
			 * 
			 * @param bundle bundle
			 * @return this for chaining
			 */
			public NotificationAction bundleClass(Class<?> bundleClass) {
				return bundleClass(() -> bundleClass);
			}
			
			/**
			 * Bundle, if different from the alert the action is in
			 * 
			 * @param bundleClass bundle
			 * @return this for chaining
			 */
			public NotificationAction bundleClass(Supplier<Class<?>> bundleClass) {
				this.bundleClass = Optional.of(bundleClass);
				return this;
			}
			
			/**
			 * CSS classes for the action button
			 * 
			 * @param classes classes
			 * @return this for chaining
			 */
			public NotificationAction classes(String... arguments) {
				return classes(Arrays.asList(arguments));
			}
			
			/**
			 * CSS classes for the action button
			 * 
			 * @param classes classes
			 * @return this for chaining
			 */
			public NotificationAction classes(Collection<String> classes) {
				this.classes.clear();
				this.classes.addAll(classes);
				return this;
			}
			
			/**
			 * Arguments for i18n resource (when in use)
			 * 
			 * @param titleArgs arguments
			 * @return this for chaining
			 */
			public NotificationAction arguments(Object... arguments) {
				return argumentsList(Arrays.asList(arguments));
			}
			
			/**
			 * Arguments for i18n resource (when in use)
			 * 
			 * @param titleArgs arguments
			 * @return this for chaining
			 */
			public NotificationAction argumentsList(Collection<Object> arguments) {
				this.args.clear();
				this.args.addAll(arguments);
				return this;
			}

			/**
			 * Get the icon for this action. 
			 * 
			 * @return icon
			 */
			Optional<String> getIcon() {
				return icon;
			}

			/**
			 * Set the icon for this action. 
			 * 
			 * @param icon icon
			 * @return this for chaining
			 */
			public NotificationAction icon(String icon) {
				this.icon = Optional.of(icon);
				return this;
			}

			/**
			 * Set the icon variant for this action. 
			 * 
			 * @param iconVariant icon
			 * @return this for chaining
			 */
			public NotificationAction iconVariant(String iconVariant) {
				this.iconVariant = Optional.of(iconVariant);
				return this;
			}

			/**
			 * Set the listener for this action.
			 * 
			 * @param listener listener
			 * @return this for chaining
			 */
			public NotificationAction listener(AlertActionListener listener) {
				this.listener = listener;
				return this;
			}

			String getResourceKey() {
				return resourceKey;
			}

			AlertActionListener getListener() {
				return listener;
			}
			
			List<String> getClasses() {
				return classes;
			}

			Optional<String> getIconVariant() {
				return iconVariant;
			}

			
			String resolveText(Notification parent, Locale locale ) {
				return text.orElseGet(() -> MessageFormat.format(resolveBundle(parent, locale).
							getString(resourceKey + ".text"), args.toArray(new Object[0])));
			}
			
			
			private ResourceBundle resolveBundle(Notification parent, Locale locale) {
				return bundle.map(b -> b.get()).orElseGet(() ->
					bundleClass.map(bc -> Resources.of(bc.get(), locale)).orElseGet(() -> parent.resolveBundle(locale)));
			}
		}

		private NotificationType type = NotificationType.INFO;
		private Dismission dismission = Dismission.DURATION;
		private List<Object> titleArgs = new ArrayList<>();
		private List<Object> contentArgs = new ArrayList<>();
		private Optional<Supplier<Class<?>>> bundleClass = Optional.empty();
		private Optional<Supplier<ResourceBundle>> bundle = Optional.empty();
		private Optional<String> key = Optional.empty();
		private Optional<String> title = Optional.empty();
		private Optional<String> content = Optional.empty();
		private Optional<String> icon = Optional.empty();
		private Optional<String> iconVariant = Optional.empty();
		private final List<NotificationAction> actions = new ArrayList<>();
		private Optional<Duration> dimissDuration = Optional.empty();
		private Optional<TemplateModel> accessory = Optional.empty();
		
		/**
		 * An accessory is any JSoup {@link Element} that will be added  
		 * between the content and the action of the alert.
		 * 
		 * @param accessory accessory
		 * @return this for chaining
		 */
		public Builder accessory(TemplateModel accessory) {
			this.accessory = Optional.of(accessory);
			return this;
		}
		
		/**
		 * Arguments for i18n title resource (when in use)
		 * 
		 * @param titleArgs title arguments
		 */
		public Builder titleArguments(Object... arguments) {
			return titleArgumentsList(Arrays.asList(arguments));
		}
		
		/**
		 * Arguments for i18n title resource (when in use)
		 * 
		 * @param titleArgs title arguments
		 */
		public Builder titleArgumentsList(Collection<Object> arguments) {
			this.titleArgs.clear();
			this.titleArgs.addAll(arguments);
			return this;
		}
		
		/**
		 * Arguments for i18n content resource (when in use)
		 * 
		 * @param contentArgs content arguments
		 */
		public Builder contentArguments(Object... arguments) {
			return contentArgumentsList(Arrays.asList(arguments));
		}
		
		/**
		 * Arguments for i18n content resource (when in use)
		 * 
		 * @param contentArgs content arguments
		 */
		public Builder contentArgumentsList(Collection<Object> arguments) {
			this.contentArgs.clear();
			this.contentArgs.addAll(arguments);
			return this;
		}

		/**
		 * Set the dimission policy for this message. 
		 * 
		 * @param dimission dimission policy
		 * @return this for chaining
		 */
		public Builder dismission(Dismission dismission) {
			this.dismission = dismission;
			return this;
		}
		
		/**
		 * Set how long to dismiss for, only applicable when {@link Dismission} is {@link Dismission#DURATION}. 
		 * 
		 * @param dimissDuration dismiss duration
		 * @return this for chaining
		 */
		public Builder dimissDuration(Duration dimissDuration) {
			this.dimissDuration = Optional.of(dimissDuration);
			return this;
		}

		/**
		 * Set the type of alert.
		 * 
		 * @param type type
		 * @return this
		 */
		public Builder type(NotificationType type) {
			this.type = type;
			return this;
		}

		/**
		 * Set the icon for this alert. 
		 * 
		 * @param icon icon name
		 * @return this for chaining
		 */
		public Builder icon(String icon) {
			this.icon = Optional.of(icon);
			return this;
		}

		/**
		 * Set the icon variant for this action. 
		 * 
		 * @param iconVariant icon
		 * @return this for chaining
		 */
		public Builder iconVariant(String iconVariant) {
			this.iconVariant = Optional.of(iconVariant);
			return this;
		}

		/**
		 * Set this key for this alert.
		 * 
		 * @param key key
		 * @return this for chaining
		 */
		public Builder key(String resourceKey) {
			this.key = Optional.of(resourceKey);
			return this;
		}

		/**
		 * The resource bundle for internationalised text.
		 * 
		 * @param clazz bundle
		 * @return bundle
		 */
		public Builder bundleClass(Class<?> clazz) {
			return bundleClass(() -> clazz);
		}


		/**
		 * The resource bundle for internationalised text.
		 * 
		 * @param clazz bundle
		 * @return bundle
		 */
		public Builder bundle(ResourceBundle bundle) {
			return bundle(() -> bundle);
		}

		/**
		 * The resource bundle for internationalised text.
		 * 
		 * @param clazz bundle
		 * @return bundle
		 */
		public Builder bundleClass(Supplier<Class<?>> clazz) {
			this.bundleClass = Optional.of(clazz);
			return this;
		}


		/**
		 * The resource bundle for internationalised text.
		 * 
		 * @param clazz bundle
		 * @return bundle
		 */
		public Builder bundle(Supplier<ResourceBundle> bundle) {
			this.bundle = Optional.of(bundle);
			return this;
		}

		/**
		 * Set this title for this alert.
		 * 
		 * @param title title
		 * @return this for chaining
		 */
		public Builder title(String title) {
			this.title = Optional.of(title);
			return this;
		}

		/**
		 * Set this content for this toast.
		 *alert
		 * @param content content
		 * @return this for chaining
		 */
		public Builder content(String content) {
			this.content = Optional.of(content);
			return this;
		}

		/**
		 * Convenience method to add a new named action with a listener.
		 * 
		 * @param key     resource key
		 * @param listener listener
		 * @param classes button CSS classes
		 * @return this for chaining
		 */
		public Builder action(String resourceKey, AlertActionListener listener, String... classes) {
			actions.add(new NotificationAction(resourceKey, listener, classes));
			return this;
		}

		/**
		 * Convenience method to add a new named action with a listener.
		 * 
		 * @param key     resource key
		 * @param icon icon
		 * @param listener listener
		 * @param classes button CSS classes
		 * @return this for chaining
		 */
		public Builder action(String resourceKey, String icon, AlertActionListener listener, String... classes) {
			actions.add(new NotificationAction(resourceKey, listener, classes).icon(icon));
			return this;
		}

		/**
		 * Trigger a new notification message based on the configuration in this
		 * builder.
		 */
		public Notification build() {
			return new Notification(this);
		}
	}
	
	private final NotificationType type;
	private final Optional<String> title;
	private final Optional<String> content;
	private final Optional<String> icon;
	private final List<NotificationAction> actions;
	private final Optional<Duration> dimissDuration;
	private final String key;
	private final Optional<Supplier<ResourceBundle>> bundle;
	private final Optional<Supplier<Class<?>>> bundleClass;
	private final Dismission dismission;
	private final List<Object> titleArgs;
	private final List<Object> contentArgs;
	private final Optional<TemplateModel> accessory;
	private final Optional<String> iconVariant;

	private Notification(Builder builder) {
		accessory = builder.accessory;
		bundle = builder.bundle;
		bundleClass = builder.bundleClass;
		key = builder.key.orElseThrow(() -> new IllegalStateException("All alerts requires a key()"));
		type = builder.type;
		title = builder.title;
		content = builder.content;
		icon = builder.icon;
		actions = Collections.unmodifiableList(new ArrayList<>(builder.actions));
		titleArgs = Collections.unmodifiableList(new ArrayList<>(builder.titleArgs));
		contentArgs = Collections.unmodifiableList(new ArrayList<>(builder.contentArgs));
		dimissDuration = builder.dimissDuration;
		dismission = builder.dismission;
		iconVariant = builder.iconVariant;
	}
	
	public List<Object> titleArgs() {
		return titleArgs;
	}
	
	public List<Object> contentArgs() {
		return contentArgs;
	}

	public NotificationType type() {
		return type;
	}

	public Optional<TemplateModel> accessory() {
		return accessory;
	}

	public String key() {
		return key;
	}

	public Optional<String> title() {
		return title;
	}

	public Optional<String> content() {
		return content;
	}

	public Optional<String> icon() {
		return icon;
	}

	public Optional<String> iconVariant() {
		return iconVariant;
	}

	public List<NotificationAction> actions() {
		return actions;
	}

	public Optional<Duration> dimissDuration() {
		return dimissDuration;
	}
	
	public Dismission dismission() {
		return dismission;
	}

	public Optional<NotificationAction> action(String action) {
		return actions.stream().filter(a -> a.getResourceKey().equals(action)).findFirst();
	}
	
	String resolveTitle(Locale locale ) {
		return title.orElseGet(() -> MessageFormat.format(resolveBundle(locale).
					getString(key + ".title"), titleArgs.toArray(new Object[0])));
	}
	
	String resolveContent(Locale locale ) {
		return content.orElseGet(() ->  MessageFormat.format(resolveBundle(locale).
					getString(key + ".content"), contentArgs.toArray(new Object[0])));
	}
	
	private ResourceBundle resolveBundle(Locale locale) {
		return bundle.map(b -> b.get()).orElseGet(() ->
				Resources.of(bundleClass.map(bc -> bc.get()).orElseThrow(() -> new IllegalStateException("No bundle could be resolved.")), locale));
	}
}
