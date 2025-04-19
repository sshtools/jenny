import com.sshtools.bootlace.api.LayerContext;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.standalone.starter.Starter;


open module com.sshtools.jenny.standalone.starter {
	
	exports com.sshtools.jenny.standalone.starter;
	
	requires transitive org.jline.terminal;
	requires transitive org.jline.style;
	requires java.net.http;
	requires jdk.net;
	requires java.scripting;
	requires java.logging;
	requires info.picocli;
	requires transitive com.sshtools.jenny.api;
	requires jul.to.slf4j;
	
	
	provides PluginContext.Provider with Starter.PluginContextProviderImpl;	
	provides LayerContext.Provider with Starter.LayerContextProviderImpl;

}