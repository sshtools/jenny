package com.sshtools.jenny.standalone.starter;

import java.lang.System.Logger.Level;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.sshtools.bootlace.api.Access;
import com.sshtools.bootlace.api.ArtifactVersion;
import com.sshtools.bootlace.api.BootContext;
import com.sshtools.bootlace.api.Layer;
import com.sshtools.bootlace.api.LayerContext;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.bootlace.api.ResolutionMonitor;
import com.sshtools.bootlace.api.RootContext;
import com.sshtools.jenny.api.Api;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

/**
 * AdiciiD. A web based front-end for Adicii.
 */
@Command(name = "jenny-starter", description = "Default Jenny Starter", mixinStandardHelpOptions = true)
public abstract class Starter implements Callable<Integer>, PluginContext, RootContext, BootContext, LayerContext {
	private final static long STARTED_STARTUP = System.currentTimeMillis();
	
	public interface PluginLayerInit {
		void accept(Plugin... plugins);
	}
	
	private static class ExceptionHandler implements IExecutionExceptionHandler {

		private final Starter cmd;
		private final Terminal terminal;

		private ExceptionHandler(Starter cmd, Terminal terminal) {
			this.cmd = cmd;
			this.terminal = terminal;
		}

		@Override
		public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult)
				throws Exception {
			var report = new AttributedStringBuilder();
			report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
			var msg = ex.getMessage() == null ? "An unknown error occured." : ex.getMessage();
			if (ex instanceof UnknownHostException) {
				msg = MessageFormat.format("Could not resolve hostname {0}: Name or service not known.",
						ex.getMessage());
			}
			report.append(msg);
			report.style(AttributedStyle.DEFAULT.foregroundDefault());
			report.append(System.lineSeparator());
			if (cmd.isVerboseExceptions()) {
				Throwable nex = ex;
				int indent = 0;
				while (nex != null) {
					if (indent > 0) {
						report.append(String.format("%" + (8 + ((indent - 1) * 2)) + "s", ""));
						report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
						report.append(nex.getMessage() == null ? "No message." : nex.getMessage());
						report.style(AttributedStyle.DEFAULT.foregroundDefault());
						report.append(System.lineSeparator());
					}

					for (var el : nex.getStackTrace()) {
						report.append(String.format("%" + (8 + (indent * 2)) + "s", ""));
						report.append("at ");
						if (el.getModuleName() != null) {
							report.append(el.getModuleName());
							report.append('/');
						}
						report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
						report.append(el.getClassName());
						report.append('.');
						report.append(el.getMethodName());
						report.style(AttributedStyle.DEFAULT.foregroundDefault());
						if (el.getFileName() != null) {
							report.append('(');
							report.append(el.getFileName());
							if (el.getLineNumber() > -1) {
								report.append(':');
								report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
								report.append(String.valueOf(el.getLineNumber()));
								report.style(AttributedStyle.DEFAULT.foregroundDefault());
								report.append(')');
							}
						}
						report.append(System.lineSeparator());
					}
					indent++;
					nex = nex.getCause();
				}
			}
			report.println(terminal);
			terminal.flush();
			return 0;
		}

	}
	
	public final static class PluginContextProviderImpl implements PluginContext.Provider {

		@Override
		public PluginContext get() {
			return pluginContext;
		}
		
	}

	public final static class LayerContextProviderImpl implements LayerContext.Provider {

		@Override
		public LayerContext get(ModuleLayer layer) {
			return layerContext;
		}
		
	}
	
	private static final AttributedStyle S = AttributedStyle.DEFAULT;
	
	protected final static void launch(String[] args, Function<Terminal, ? extends Starter> appSupplier) throws Exception {
		try (var terminal = TerminalBuilder.builder().build()) {
			var cmd = appSupplier.apply(terminal);
			System.exit(new CommandLine(cmd).setExecutionExceptionHandler(new ExceptionHandler(cmd, terminal))
					.execute(args));
		}
	}

	public static void main(String[] args) throws Exception {
		launch(args, terminal -> new Starter(terminal) {
			@Override
			protected void appPlugins(PluginLayerInit layers) {
			}
		});
	}

	static String systemLoggerToJULLevel(Level lvl) {
		switch(lvl) {
		case ALL:
			return "ALL";
		case DEBUG:
			return "FINE";
		case TRACE:
			return "FINEST";
		case INFO:
			return "INFO";
		case WARNING:
			return "WARNING";
		case ERROR:
			return "SEVERE";
		case OFF:
			return "OFF";
		default:
			throw new IllegalArgumentException();
		}
	}

	@Option(names = { "-X", "--verbose-exception" }, description = "Show full stack trace on error.")
	private boolean verboseException;

	@Option(names = { "-L", "--log-level" }, description = "Log level.")
	private Optional<Level> logLevel;

	@Option(names = { "-q", "--quiet" }, description = "No banner or boot monitor output.")
	private boolean quiet;

	@Option(names = { "-a", "--administrator" }, description = "Run as administrator.")
	private boolean administrator;
	
	@Option(names = {"-D", "--sysprop"}, mapFallbackValue = "")
	private Map<String, String> properties = new HashMap<>();

	private final Terminal terminal;
	private final Map<Class<? extends Plugin>, Plugin> plugins = new LinkedHashMap<>();
	private final Layer layer;
	private final List<Plugin> loaded = new ArrayList<>();
	private final AtomicBoolean run = new AtomicBoolean(true);
	private final Thread thread = Thread.currentThread();

	private PluginHostInfo info;

	private static PluginContext pluginContext;
	private static LayerContext layerContext;

	public Starter(Terminal terminal) {
		this(terminal, new PluginHostInfo("jenny-standalone-starter", ArtifactVersion.getVersion("com.sshtools", "jenny-standalone-starter"), "Jenny"));
	}
	
	public Starter(Terminal terminal, PluginHostInfo info) {
		this.terminal = terminal;
		this.info = info;
		pluginContext = this;
		layerContext = this;
		
		layer = new Layer() {
			
			@Override
			public Set<String> remoteRepositories() {
				return Collections.emptySet();
			}
			
			@Override
			public Optional<String> name() {
				return Optional.empty();
			}
			
			@Override
			public Optional<ResolutionMonitor> monitor() {
				return Optional.empty();
			}
			
			@Override
			public Set<String> localRepositories() {
				return Collections.emptySet();
			}
			
			@Override
			public String id() {
				return "monolithic";
			}
			
			@Override
			public Set<String> appRepositories() {
				return Collections.emptySet();
			}

			@Override
			public Access access() {
				return Access.PUBLIC;
			}
		};
	}
	
	@Override
	public void addListener(Listener listener) {
	}
	
	@Override
	public final BootContext app() {
		return this;
	}

	@Override
	public void autoClose(AutoCloseable... closeables) {
	}
	
	protected Consumer<Boolean> shutdownHook() {
		return shutdown -> {
			if(shutdown) {
				run.set(false);
			}
			thread.interrupt();
		};
	}
	
	protected abstract void appPlugins(PluginLayerInit layers);

	@Override
	public final Integer call() throws Exception {
		properties.forEach(System::setProperty);
		
		initLogging();
		
		if(!quiet) {
			printBanner();
		}
		
		while(run.get()) {
			initLayer(
					new Api());
			
			appPlugins(l -> initLayer(l));
			
			if(!quiet) {
				new AttributedStringBuilder().
						append("\n" + info().displayName() + " is now ").
						style(S.bold()).
						append("READY!").
						style(S.boldOff()).
						println(terminal);
				new AttributedStringBuilder().
						append("\nTook ").
						style(new AttributedStyle().faint()).
						append(String.valueOf(System.currentTimeMillis() - STARTED_STARTUP)).
						style(new AttributedStyle().faintOff()).
						append("ms").
						println(terminal);
				terminal.flush();
			}
			
			try {
				while(run.get()) { Thread.sleep(100000); }
			}
			catch(InterruptedException ie) {
			}
			
			Collections.reverse(loaded);
			loaded.forEach(l -> {
				try {
					l.beforeClose(Starter.this);
				} catch(RuntimeException re) {
					throw re;
				} catch (Exception e) {
					throw new IllegalStateException(e);
				} 
			});
			loaded.forEach(l -> {
				try {
					l.close();
				} catch(RuntimeException re) {
					throw re;
				} catch (Exception e) {
					throw new IllegalStateException(e);
				} 
			});
			loaded.clear();
		}
		
		return 0;
	}

	protected void printBanner() {
		System.out.println("""
			     .-./`)     .-''-.  ,---.   .--.,---.   .--.   ____     __  
			     \\ '_ .') .'_ _   \\ |    \\  |  ||    \\  |  |   \\   \\   /  / 
			    (_ (_) _)/ ( ` )   '|  ,  \\ |  ||  ,  \\ |  |    \\  _. /  '  
			      / .  \\. (_ o _)  ||  |\\_ \\|  ||  |\\_ \\|  |     _( )_ .'   
			 ___  |-'`| |  (_,_)___||  _( )_\\  ||  _( )_\\  | ___(_ o _)'    
			|   | |   ' '  \\   .---.| (_ o _)  || (_ o _)  ||   |(_,_)'     
			|   `-'  /   \\  `-'    /|  (_,_)\\  ||  (_,_)\\  ||   `-'  /      
			 \\      /     \\       / |  |    |  ||  |    |  | \\      /       
			  `-..-'       `'-..-'  '--'    '--''--'    '--'  `-..-'       
							""");
	}
	
	private void initLayer(Plugin... plugins) {
		
		Stream.of(plugins).forEach(this::initPlugin);
		Stream.of(plugins).forEach((p) -> {
			try {
				p.afterOpen(this);
				loaded.add(p);
			} catch(RuntimeException re) {
				throw re;
			} catch (Exception e) {
				throw new IllegalStateException(e);
			} 
		});
	}

	@Override
	public boolean canRestart() {
		return false;
	}

	@Override
	public boolean canShutdown() {
		return false;
	}

	@Override
	public void close() {
	}

	@Override
	public PluginHostInfo info() {
		return info;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final <P extends Plugin> Optional<P> pluginOr(Class<P> plugin) {
		return (Optional<P>) Optional.ofNullable(plugins.get(plugin));
	}

	@Override
	public final boolean hasPlugin(String className) {
		return plugins.keySet().stream().map(Class::getName).toList().contains(className);
	}

	@Override
	public void removeListener(Listener listener) {
	}

	@Override
	public void restart() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final RootContext root() {
		return this;
	}

	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}

	private void initLogging() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
//		var properties = new Properties();
//		try (var stream = AdiciiD.class.getClassLoader().getResourceAsStream("logging.properties")) {
//			if(stream == null)
//				throw new IOException("Cannot find logging.properties in classpath.");
//			properties.load(stream);
//			
//			var levelStr = systemLoggerToJULLevel(logLevel.orElse(Level.WARNING));
//			properties.put(".level", levelStr);
//			properties.put("java.util.logging.ConsoleHandler.level", levelStr);
//			
//			var wrt = new StringWriter();
//			properties.store(wrt, "Logging Properties");
//			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(wrt.toString().getBytes()));
////          LOGGER= Logger.getLogger(MyClass.class.getName());
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}
	}

	private void initPlugin(Plugin plugin) {
		try {
			plugin.open(this);
		} catch(RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to start plugin.", e);
		}
		plugins.put(plugin.getClass(), plugin);
	}

	private boolean isVerboseExceptions() {
		return verboseException;
	}

	@Override
	public final Optional<URL> globalResource(String path) {
		return Optional.ofNullable(getClass().getClassLoader().getResource(path));
	}

	@Override
	public final Iterable<ModuleLayer> childLayers() {
		return Collections.emptyList();
	}

	@Override
	public final Layer layer() {
		return layer;
	}

	@Override
	public final Optional<Layer> layer(String id) {
		return Optional.empty();
	}

	@Override
	public final ClassLoader loader() {
		return getClass().getClassLoader();
	}

	@Override
	public final Set<Layer> parents() {
		return Collections.emptySet();
	}
}
