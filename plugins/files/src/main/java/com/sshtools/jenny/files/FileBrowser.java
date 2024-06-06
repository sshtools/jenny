
package com.sshtools.jenny.files;

import static com.sshtools.bootlace.api.PluginContext.$;

import java.util.Optional;
import java.util.function.Supplier;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.bootstrap5.Bootstrap5;
import com.sshtools.jenny.i18n.I18N;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebModule.WebModuleResource;
import com.sshtools.jenny.web.WebModule.WebModulesRef;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Handler;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class FileBrowser implements Plugin {
	
	private final Web web 						= $().plugin(Web.class);
	private final FileManagementBase fileMgr 	= $().plugin(FileManagementBase.class);
	
	private WebModulesRef managerModules;
	private Optional<Handler> authenticator = Optional.empty();
	private Optional<Supplier<TemplateModel>> pageTemplateFactory = Optional.empty();	
	
	public final static WebModule FILE_BROWSER_DIALOG_MODULE = new WebModule.Builder().
			withUri("/file-browser/").
			withRequires(Bootstrap5.MODULE_BOOTBOX, WebModule.js(
					"/file-management.i18n.js", 
					I18N.script(FileManagementBase.class), 
					I18N.I18N_MODULE
			)).
			withResources(
				WebModuleResource.css(FileManagementBase.class, "file-browser-dialog.frag.css"),
				WebModuleResource.js(FileManagementBase.class, "file-browser-dialog.frag.js")
			).
			build();
	
	public FileBrowser() {
	}

	@Override
	public void open(PluginContext context) {


		context.autoClose(
		
			managerModules = web.modules(
				WebModule.of(
						"/file-browser.js", 
						FileBrowser.class, 
						"file-browser.page.js", 
						FileManagementBase.FILEMANAGER_MODULE
				)
			),
				
			web.router().route().
					handle("/browse-files-api|/browse-files-api/.*", this::checkAuthenticated, fileMgr::apiFiles).
					handle("/browse-files|/browse-files/.*", this::checkAuthenticated, this::pageFiles).
				build()
		);
	}
	
	public void pageTemplateFactory(Supplier<TemplateModel> pageTemplateFactory) {
		this.pageTemplateFactory = Optional.of(pageTemplateFactory);
	}
	
	public void authenticator(Handler authenticator) {
		this.authenticator = Optional.of(authenticator);
	}
	
	private void checkAuthenticated(Transaction tx) throws Exception {
		if(authenticator.isPresent())
			authenticator.get().get(tx);
	}

	private void pageFiles(Transaction tx) {
		
		
		fileMgr.fragFiles(tx, "/browse-files-api", "/browse-files").ifPresent(templ -> {
			tx.response(web.processor()
				.process(web.require(pageTemplateFactory.map(ptf -> ptf.get()).orElseGet(() -> web.template(FileBrowser.class, "file-browser.page.html"))
					.include("content", templ), managerModules)
				)
			);
		});
	}

}
