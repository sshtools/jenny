
package com.sshtools.jenny.alertcentre;

import static com.sshtools.bootlace.api.PluginContext.$;

import java.security.Principal;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import com.sshtools.bootlace.api.Http;
import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Category;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.bootlace.api.UncheckedCloseable;
import com.sshtools.jenny.alertcentre.Monitor.Scope;
import com.sshtools.jenny.alertcentre.Notification.Builder.NotificationAction;
import com.sshtools.jenny.alertcentre.Notification.Dismission;
import com.sshtools.jenny.api.Resources;
import com.sshtools.jenny.config.Config;
import com.sshtools.jenny.product.Product;
import com.sshtools.jenny.web.Responses;
import com.sshtools.jenny.web.Responses.RedirectResponse;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebState;
import com.sshtools.jini.INI.Section;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class AlertCentre implements Plugin {
	
	private final static class Defaults {
		private final static AlertCentreContext DEFAULT = new AlertCentreContext() {
			@Override
			public boolean isAdministrator(Principal principal) {
				return false;
			}

			@Override
			public boolean isSystem(Principal principal) {
				return false;
			}

			@Override
			public void onLogin(Consumer<Principal> principal) {
			}

			@Override
			public UncheckedCloseable systemContext() {
				return new UncheckedCloseable() {
					@Override
					public void close() {
					}
					
				};
			}

			@Override
			public UncheckedCloseable administratorContext() {
				return systemContext();
			}

			@Override
			public UncheckedCloseable userContext() {
				return systemContext();
			}
		};
	}
	
	public static AlertCentreContext defaultContext() {
		return Defaults.DEFAULT;
	}
	
	final static Instant SERVER_STARTED = Instant.now();
	
	private final Web web 						= $().plugin(Web.class);
	private final Config config					= $().plugin(Config.class);
	private final Product product				= $().plugin(Product.class);
	private final List<Monitor> monitors 		= new ArrayList<>();
	private AlertCentreContext context = new AlertCentreContext() {
		
		@Override
		public UncheckedCloseable userContext() {
			throw throwUE();
		}
		
		@Override
		public UncheckedCloseable systemContext() {
			throw throwUE();
		}
		
		@Override
		public UncheckedCloseable administratorContext() {
			throw throwUE();
		}

		private UnsupportedOperationException throwUE() {
			return new UnsupportedOperationException("Notification centre requires AlertCentre.context() is used to provide some integration points.");
		}

		@Override
		public void onLogin(Consumer<Principal> principal) {
		}

		@Override
		public boolean isAdministrator(Principal principal) {
			throw throwUE();
		}

		@Override
		public boolean isSystem(Principal principal) {
			throw throwUE();
		}
	};
	
	private AlertCentreToolkit toolkit = new AlertCentreToolkit() {
		
		@Override
		public String titleBgStyle(NotificationType type) {
			throw throwUE();
		}
		
		@Override
		public String textStyle(NotificationType type) {
			throw throwUE();
		}
		
		@Override
		public String icon(NotificationType type) {
			throw throwUE();
		}
		
		@Override
		public String bgStyle(NotificationType type) {
			throw throwUE();
		}

		@Override
		public TemplateModel template(Transaction tx) {
			throw throwUE();
		}

		private UnsupportedOperationException throwUE() {
			return new UnsupportedOperationException("Notification centre requires AlertCentre.toolkit() is used to provide some UI integration points.");
		}
	};
	
	private Section dismissalDatabase;

	private final static Log LOG = Logs.of(Category.ofName(AlertCentre.class));
	
	public AlertCentre() {
	}

	public UncheckedCloseable toolkit(AlertCentreToolkit toolkit) {
		if(toolkit == null)
			throw new NullPointerException();
		if(this.toolkit == null)
			throw new IllegalStateException("Only one alert centre toolkit is allowed.");
		
		this.toolkit = toolkit;
		
		return UncheckedCloseable.onClose(() -> AlertCentre.this.toolkit = null);
	}
	
	public UncheckedCloseable context(AlertCentreContext context) {
		if(context == null)
			throw new NullPointerException();
		if(this.context == null)
			throw new IllegalStateException("Only one alert centre context is allowed.");
		
		this.context = context;
		
		context.onLogin(user -> {
			dismissalDatabase.sections().values().stream().map(s -> s[0]).filter(s -> 
				s.getEnum(Dismission.class, "dismission", Dismission.RESTART).equals(Dismission.LOGIN) &&
				s.get("user", "").equals(user.getName())
			).forEach(s -> s.remove());
		});
		
		return UncheckedCloseable.onClose(() -> AlertCentre.this.context = null);
	}
	
	public UncheckedCloseable monitor(Monitor monitor) {
		monitors.add(monitor);
		return UncheckedCloseable.onClose(() -> monitors.remove(monitor));
	}
	
	public void remove(Monitor monitor) {
		monitors.remove(monitor);
	}

	@Override
	public void open(PluginContext context) {
		
		var cfgBldr = config.configBuilder("alerts");
		var alertConfig = cfgBldr.build();

		context.autoClose(
			alertConfig,
				
			web.router().route().
					get("/alert-action/(.*)/(.*)", this::apiAlertAction).
				build()
		);
		
		dismissalDatabase = alertConfig.document().obtainSection("dismissals");
		
		if(Boolean.getBoolean("jadaptive.alerts.reset")) {
			LOG.info("Resetting dismissals.");
			dismissalDatabase.clear();
			LOG.info("Reset dismissals.");
		}
		else {
		
			LOG.info("Clearing up stale dismissals.");
			
			var counter = new AtomicInteger();
			Consumer<Section> deleteAndCount = dis -> {
				dis.remove();
				counter.incrementAndGet();
			};
			
			/* Delete all dismissals that have dismission of UPGRADE and a version
			 * field that does not match the current version
			 */
			dismissalDatabase.sections().values().stream().map(s -> s[0]).filter(s -> 
				s.getEnum(Dismission.class, "dismission", Dismission.RESTART).equals(Dismission.UPGRADE) &&
				!s.get("version", "").equals(product.info().version())
			).forEach(deleteAndCount);
			
			/*
			 * Delete all dismissals that have dismission of RESTART or LOGIN
			 */
			dismissalDatabase.sections().values().stream().map(s -> s[0]).filter(s -> {
				var e = s.getEnum(Dismission.class, "dismission", Dismission.RESTART); 
				return e.equals(Dismission.RESTART) || e.equals(Dismission.LOGIN);
			}
			).forEach(deleteAndCount);
			
			/*
			 * Delete all dismissals that have expired
			 */
			dismissalDatabase.sections().values().stream().map(s -> s[0]).filter(s -> {
				var e = s.getEnum(Dismission.class, "dismission", Dismission.RESTART); 
				return e.equals(Dismission.RESTART) || e.equals(Dismission.LOGIN);
			}
			).forEach(deleteAndCount);
			

			dismissalDatabase.sections().values().stream().map(s -> s[0]).
				filter(s -> s.contains("expire")).
				forEach(s -> {
					if(Instant.parse(s.get("expire")).isBefore(SERVER_STARTED)) {
						deleteAndCount.accept(s);
					}
				});
	
			LOG.info("Cleared up {} stale dismissals.", counter.get());
		}
	}
	
	public TemplateModel fragAlertIcons(Transaction tx) {
		var templ = toolkit.template(tx);
		var state = WebState.get();
		var allNotifications = alerts(state.user().orElseThrow(() -> new IllegalStateException("Not authenticated.")), null);
		
		templ.list("types", (c) -> {
		
			var iconTemplates = new ArrayList<TemplateModel>();
			for(var type : NotificationType.values()) {
				var notifications = allNotifications.stream().filter(alt -> alt.alert().type() == type).map(NotificationInstance::alert).toList();
				if(notifications.isEmpty())
					continue;

				iconTemplates.add(TemplateModel.ofContent(c).
					list("alerts", cc ->
						notifications.stream().map(notification -> {
							var allActions = new ArrayList<NotificationAction>(notification.actions());
							if(notification.dismission() != Dismission.NEVER) {
								allActions.add(createDismissAction(state, notification));
							}
							
							return TemplateModel.ofContent(cc).
								variable("type", type.name()).
								variable("any-icon", () -> notification.icon().orElseGet(() -> toolkit.icon( type))).
								variable("icon", () -> notification.icon().orElse("")).
								variable("icon-variant", () -> notification.iconVariant().orElse("")).
								variable("title", () -> notification.resolveTitle(state.locale())).
								variable("content", () -> notification.resolveContent(state.locale())).
								variable("type-bg-style", toolkit.bgStyle( type)).
								variable("type-text-style", toolkit.textStyle(type)).
								variable("type-icon", toolkit.icon( type)).
								list("actions", (ac) -> allActions.stream().map(action -> 
									TemplateModel.ofContent(ac).
										variable("classes", String.join(" ", action.getClasses())).
										variable("icon", () -> action.getIcon().orElse("")).
										variable("uri", () -> "/alert-action/" + Http.urlEncode(notification.key()) + "/" + Http.urlEncode(action.getResourceKey())).
										variable("icon-variant", () -> action.getIconVariant().orElse("")).
										variable("text", () -> action.resolveText(notification, state.locale()))
								).toList());
							}
						).toList()
					).
					variable("type", type.name()).
					variable("bg-style", toolkit.bgStyle( type)).
					variable("text-style", toolkit.textStyle(type)).
					variable("icon", toolkit.icon( type)));
			}
			
			return iconTemplates;
		});
		
		return templ;
	}

	private NotificationAction createDismissAction(WebState state, Notification notification) {
		return new NotificationAction("dismiss", () -> {
			dismiss(WebState.get().user().orElseThrow(() -> new IllegalStateException("Not authenticated.")), notification.key());
		}).text(Resources.of(getClass(), state.locale()).getString("dismission." + notification.dismission().name()));
	}
	
	private void apiAlertAction(Transaction tx) throws Exception {
		try {
			action(WebState.get().user().orElseThrow(() -> new IllegalStateException("Not authenticated.")), tx.match(0), tx.match(1));
			tx.response(Responses.success());
		}
		catch(RedirectResponse redirect) {
			tx.response(redirect);
		}
		catch(Exception e) {
			LOG.error("Action failed.", e);
			tx.response(Responses.error(e)); 
		}
	}
	
	public void action(Principal user, String key, String action) throws Exception {
		var alert = alerts(user, null).stream().
				filter(alt -> alt.alert().key().equals(key)).
				findFirst().
				orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("No alert with key of {0}", key))); 
		alert.alert().action(action).orElseGet(() -> dismissAction(alert.alert(), action)).getListener().action();
	}

	public List<NotificationInstance> alerts(Principal user, NotificationType type) {
		var admin = context.isAdministrator(user);
		var sysTenant = context.isSystem(user);
		
		var dismissalsThreadLocal = new ThreadLocal<List<Dismissal>>();
		
		var cache =  new HashMap<Monitor, Optional<NotificationInstance>>();
		
		return monitors.stream().
			filter(monitor -> {
				var scope = monitor.scope();
				if(scope == Scope.USER) {
					return true;
				}
				else if(scope == Scope.ADMINISTRATOR && admin) {
					return admin;
				}
				else if(scope == Scope.SYSTEM && sysTenant && admin) {
					return admin;
				}
				return false;
			}).
			map(monitor -> {
				if(cache.containsKey(monitor)) {
					return cache.get(monitor);
				}
				else {
					var res = alertInstanceForMonitor(user, monitor);
					cache.put(monitor, res);
					return res;
				}
			}).
			filter(Optional::isPresent).
			map(Optional::get).
			filter(alt -> {
				var dismissals = dismissalsThreadLocal.get();
				if(dismissals == null) {
					/* Lazily load the dismissals for this user */
					dismissals = StreamSupport.stream(listDismissalsForUser(user).spliterator(), false).filter(dis -> !dis.expired()).toList();
					dismissalsThreadLocal.set(dismissals);
				}
				
				if(dismissals.stream().filter(dis -> dis.alert().equals(alt.alert().key())).findFirst().isPresent()) {
					return false;
				}
				
				return type == null || alt.alert().type() == type; 
			}).
			toList();
	}
	

	public Iterable<Dismissal> listDismissalsForUser(Principal user) {
		return dismissalDatabase.sections().values().stream().map(s -> s[0]).filter(s -> s.get("user", "").equals(user.getName())).map(sec -> new Dismissal.Builder().fromData(sec).build()).toList();
	}

	public Dismissal dismiss(Principal user, NotificationInstance alert) {
		var bldr = new Dismissal.Builder();
		bldr.withUuid(alert.toUUID());
		switch(alert.alert().dismission()) {
		case UPGRADE:
			bldr.withVersion(product.info().version());
			break;
		case RESTART:
			bldr.withServerStart(SERVER_STARTED);
			break;
		case DURATION:
			bldr.withExpire(Instant.now().plus(alert.alert().dimissDuration().orElseGet(() -> Duration.ofDays(1))));
			break;
		default:
			break;
		}
		bldr.withAlert(alert.alert().key());
		bldr.withUser(user);
		bldr.withDismission(alert.alert().dismission());
		
		var dismissal = bldr.build();
		dismissal.store(dismissalDatabase.obtainSection(dismissal.uuid().toString()));
		
		return dismissal;
	}

	private void dismiss(Principal user, String alert) {
		var alertObj = alerts(user, null).stream().
			filter(alt -> alt.alert().key().equals(alert)).
			findFirst().
			orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("No alert with key of {0}", alert)));
		
		dismiss(user, alertObj);
	}
	
	private NotificationAction dismissAction(Notification notification, String action) {
		if(action.equals("dismiss")) {
			return createDismissAction(WebState.get(), notification);
		}
		else {
			throw new IllegalArgumentException(MessageFormat.format("No action with key of {0}", action));			
		}
	}
	

	private Optional<NotificationInstance> alertInstanceForMonitor(Principal user, Monitor monitor) {
		switch(monitor.scope()) {
		case SYSTEM:
			try(var ctx = context.systemContext()) {
				return monitor.query().map(alt -> new NotificationInstance(alt, user, monitor.scope()));
			}
		case ADMINISTRATOR:
			try(var uctx = context.administratorContext()) {
				return monitor.query().map(alt -> new NotificationInstance(alt, user, monitor.scope()));
			}
		default:
			try(var uctx = context.userContext()) {
				return monitor.query().map(alt -> new NotificationInstance(alt, user, monitor.scope()));
			}
		}
	}
}
