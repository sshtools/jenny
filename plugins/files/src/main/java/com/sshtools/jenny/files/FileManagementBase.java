
package com.sshtools.jenny.files;

import static com.sshtools.bootlace.api.PluginContext.$;
import static com.sshtools.jenny.bootstrap5.Alerts.alertable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.json.Json;

import com.sshtools.bootlace.api.FilesAndFolders;
import com.sshtools.bootlace.api.GAV;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.Zip;
import com.sshtools.jenny.api.Resources;
import com.sshtools.jenny.bootstrap5.Alerts;
import com.sshtools.jenny.bootstrap5.Bootstrap5;
import com.sshtools.jenny.files.FileManagerOptions.Mode;
import com.sshtools.jenny.files.FileManagerOptions.NavAction;
import com.sshtools.jenny.files.FileManagerOptions.SelectionMode;
import com.sshtools.jenny.i18n.I18N;
import com.sshtools.jenny.vfs.Vfs;
import com.sshtools.jenny.web.NpmWebModule;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.jenny.web.WebState;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD;
import com.sshtools.uhttpd.UHTTPD.FormData;
import com.sshtools.uhttpd.UHTTPD.Method;
import com.sshtools.uhttpd.UHTTPD.Status;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class FileManagementBase implements Plugin {
	
	enum ClipboardMode {
		COPY, CUT
	}
	
	record ClipboardItem(Path path, ClipboardMode mode) {}
	
	private final static WebModule MODULE_MOMENT = 
		NpmWebModule.of(
			Bootstrap5.class, 
			GAV.ofSpec("npm:moment")
		);

	private final Vfs vfs 				= $().plugin(Vfs.class);

	public final static WebModule FILEMANAGER_I18N_MODULE = I18N.module(FileManagementBase.class);
	
	public final static WebModule FILEMANAGER_MODULE = WebModule.of(
			"/file-management.js", 
			FileManagementBase.class, 
			"file-management.frag.js", 
			FILEMANAGER_I18N_MODULE, MODULE_MOMENT, Bootstrap5.MODULE_BOOTSTRAP_TABLE, Bootstrap5.MODULE_BOOTBOX
	);
	
	public final static WebModule FILEMANAGER_UPLOAD_MODULE = WebModule.of(
			"/upload.js", 
			FileManagementBase.class, 
			"upload.frag.js", 
			FILEMANAGER_I18N_MODULE, Bootstrap5.MODULE_BOOTSTRAP_TABLE
);

	public FileManagementBase() {
	}

	public void apiFiles(Transaction tx) {
		var arrBldr = Json.createArrayBuilder();
		var session = WebState.get();
		var hiddenFiles = (Boolean)session.env().getOrDefault("hiddenFiles", false);
		
		var fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		var par = filePathForRequestPath(tx);
		var options = getOptionsFromParameters(tx);
		
		var lst = new ArrayList<Path>();
		
		try(var str =  java.nio.file.Files.newDirectoryStream(par, (file)-> 
			isIncluded(hiddenFiles, options, file)
		)) {
			str.forEach(p -> lst.add(p));
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
		
		Collections.sort(lst, (p1, p2) -> {
			var t1 = Files.isDirectory(p1);
			var t2 = Files.isDirectory(p2);
			var ts = Boolean.valueOf(t1).compareTo(t2) * - 1;
			if(ts == 0) {
				/* TODO sort alpha option */
				return p1.getFileName().toString().toLowerCase().compareTo(p2.getFileName().toString().toLowerCase());
			}
			else {
				return ts;
			}
		});
		
		lst.forEach(p -> {
				
			var folder = Files.isDirectory(p);
			var link = Files.isSymbolicLink(p);
			
			long size;
			try {
				size = Files.size(p);
			} catch (IOException e) {
				size = 0;
			}
			
			String lastModStr;
			FileTime lastMod;
			try {
				lastMod = Files.getLastModifiedTime(p);
				lastModStr = fmt.format(new Date(lastMod.toMillis()));
			} catch (Exception e) {
				lastModStr = "Unknown";
				lastMod = null;
			}
			
			arrBldr.add(Json.createObjectBuilder().
					add("name", p.getFileName().toString()).
					add("path", p.toString()).
					add("folder", folder). 
					add("file", !folder).
					add("size", size).
					add("type", folder ? "folder": (link ? "link" : "file")).	
					add("lastModified", lastMod == null ? 0 : lastMod.toMillis()).	
					add("lastModifiedText", lastModStr).
					build());
		});
		
		tx.response("text/json", arrBldr.build().toString());
	}

	protected boolean isIncluded(boolean hiddenFiles, FileManagerOptions options, Path file) throws IOException {
		return ( hiddenFiles || !Files.isHidden(file) ) &&
			   ( options.mode() == Mode.FILES_AND_DIRECTORIES || 
			   ( Files.isDirectory(file) && options.mode() == Mode.DIRECTORIES )  || 
			   ( !Files.isDirectory(file) && options.mode() == Mode.FILES ) );
	}
	
	private List<TemplateModel> breadcrumbs(Transaction tx, String content, String uiPath, String queryOptions) {
			var l = new ArrayList<TemplateModel>();
		var path = filePathForRequestPath(tx);
		var bc = new ArrayList<String>();
		path.forEach(p -> { 
			bc.add(p.getFileName().toString());
			l.add(TemplateModel.ofContent(content).
				bundle(FileManagementBase.class).
				
				/* TODO these 2 should really not be required, TinyTemplate should be finding
				 * them in the parent template, but it is not. Cannot reproduce in a similar 
				 * test, so this is a pain!
				 */
				variable("ui-path", uiPath).
				variable("query-options", queryOptions).
				
				variable("name", p).
				variable("path", String.join("/", bc)));
		});
		return l;
	}
	
	private List<ClipboardItem> clipboard() {
		var state = WebState.get();
		List<ClipboardItem> clipboard = state.get(ClipboardItem.class.getName());
		if(clipboard == null) {
			clipboard = new ArrayList<>();
			state.set(ClipboardItem.class.getName(), clipboard);
		}
		return clipboard;
	}
	
	private Path filePathForRequestPath(Transaction tx) {
		var path = tx.path();
		path = resolvePath(path);
		return path;
	}

	private Path resolvePath(Path path) {
		var root = root();
		if(path.getNameCount() > 1)
			path = root.resolve(path.subpath(1, path.getNameCount()).toString());
		else
			path = root;
		return path;
	}
		

	private String formatFileTime(FileTime ft) {
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(ft.toMillis()));
	}

	public TemplateModel fragFile(Transaction tx) throws IOException {
		var par = filePathForRequestPath(tx);
		Web.setBase(tx);
			
		var attrsView = Files.getFileAttributeView(par, BasicFileAttributeView.class);
		var attrs = attrsView.readAttributes();
		var bundle = Resources.of(FileManagementBase.class, WebState.get().locale());
			
		var template = TemplateModel.ofResource(FileManagementBase.class, "file.frag.html")
					.bundle(FileManagementBase.class)
					.variable("path", () -> filePathForRequestPath(tx))
					.variable("parent", par::getParent)
					.variable("modified", () -> formatFileTime(attrs.lastModifiedTime()))
					.variable("created", () -> formatFileTime(attrs.creationTime()))
					.variable("accessed", () -> formatFileTime(attrs.creationTime()))
					.variable("title", () -> bundle.getString(typeKey(par) + ".title"))
					.variable("icon", () -> typeIcon(par))
					.variable("name", par.getFileName().toString());
					
		var posixAttrsView = Files.getFileAttributeView(par, PosixFileAttributeView.class);
		
		PosixFileAttributes posixAttrs = null;
		
		if(posixAttrsView != null) {
			posixAttrs = posixAttrsView.readAttributes();
		}
		
		if(tx.method() == Method.POST) {
			
			var req = tx.request();
			var newName = req.asFormData("name").asString();
			var currentName = par.getFileName().toString();
			if(!Objects.equals(newName, currentName)) {
				alertable(FileManagementBase.class, template, () -> {
					var newFile = par.getParent().resolve(sanitizeFilename(newName));
					Files.move(par, newFile);
					
				}, () -> {
					Alerts.flash(Alerts.info(FileManagementBase.class, "renamed", currentName, newName));
				});
			}
			
			if(posixAttrs != null) {
				var l = new ArrayList<PosixFilePermission>();
				for(var p : PosixFilePermission.values()) {
					req.ofFormData(p.name()).ifPresent(fd -> {
						if(fd.asBoolean()) {
							l.add(p);
						}
					});
				}
				if(!l.equals(new ArrayList<>(posixAttrs.permissions()))) {
					alertable(FileManagementBase.class, template, () -> {
						posixAttrsView.setPermissions(l.stream().collect(Collectors.toSet()));
					}, () -> {
						Alerts.flash(Alerts.info(FileManagementBase.class, "permissionsChanged"));
					});
				}
				posixAttrs = posixAttrsView.readAttributes();
			}
		}
		
		if(posixAttrsView != null) {
			var permissions = posixAttrs.permissions();
			template.list("posix", c -> Arrays.asList(PosixFilePermission.values()).stream().map(p -> 
				TemplateModel.ofContent(c).
						variable("name", p).
						variable("text", bundle.getString(p.name() + ".label")).
						condition("selected", permissions.contains(p))
			).toList());
		}
		
		return template;
	}

	public Optional<TemplateModel> fragFiles(Transaction tx, String apiPath, String uiPath) {
		var par = filePathForRequestPath(tx);
		
		if(Files.notExists(par) && tx.method().equals(Method.GET)) {
			var options = getOptionsFromParameters(tx);
			if(options.fallback()) {
				while(par != null && !Files.exists(par)) {
					par = par.getParent();
				}
				if(par == null) {
					par = Paths.get(System.getProperty("user.home"));
				}
			}
		}
		
		
		if(Files.isDirectory(par))
			return fragListing(tx, apiPath, uiPath, par);
		else {
			if(tx.method() == Method.GET) {
				try {
					UHTTPD.fileResource(par).get(tx);
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
			return Optional.empty();
		}
	}

	protected FileManagerOptions getOptionsFromParameters(Transaction tx) {
		return tx.parameterOr("options").map(fd -> FileManagerOptions.fromAttributeValue(fd.asString())).orElse(FileManagerOptions.defaultOptions());
	}
	
	private Optional<TemplateModel> fragListing(Transaction tx, String apiPath, String uiPath, Path par) {
		var hiddenFiles = WebState.get().env().getOrDefault("hiddenFiles", false);
		Web.setBase(tx);
		var session = WebState.get();		
		var clipboard = clipboard();
		
		var template = TemplateModel.ofResource(FileManagementBase.class, "file-management.frag.html")
					.bundle(FileManagementBase.class)
					.variable("path", () -> filePathForRequestPath(tx))
					.variable("api-path", apiPath)
					.variable("home-path", () -> System.getProperty("user.home"))
					.variable("ui-path", uiPath)
					.variable("parent", par::getParent)
					.variable("clipboard-items", clipboard::size)
					.condition("clipboard-empty", clipboard::isEmpty)
					.condition("root", () -> par.toString().equals(root().toString()))
					.condition("clipboard-occupied", () -> !clipboard.isEmpty());
		
		FileManagerOptions options;

		if(tx.method().equals(Method.POST)) {
			var req = tx.request();
			String op = "";
			String name = "";
			int uploads = 0;
			String filename = null;
			FileManagerOptions reqOpts = null;
			hiddenFiles = false;
			
			for(var part : req.asParts(FormData.class)) {
				if(part.name().equals("op")) {
					op = part.asString();
				}
				else if(part.name().equals("name")) {
					name = part.asString();
				}
				else if(part.name().equals("options")) {
					reqOpts = new FileManagerOptions.Builder().fromAttributeValue(part.asString()).build();
				}
				else if(part.name().equals("hiddenFiles")) {
					hiddenFiles = part.asBoolean();
				}
				else if(part.name().equals("upload")) {
					uploads++;
					filename = part.filename().get();
					if(!filename.equals("")) {
						var fullpath = par.resolve(sanitizeFilename(filename));
						part.asFile(fullpath);
					}
				}
			}
			
			options = reqOpts == null ? FileManagerOptions.defaultOptions() : reqOpts;
			
			// Upload
			if(op.equals("upload")) {
				if(uploads == 0) {
					template.include("alerts", () -> Alerts.danger(FileManagementBase.class, "noFilesUploaded"));					
				}
				else if(uploads == 1) {	
					var fFilename = filename;
					template.include("alerts", () -> Alerts.info(FileManagementBase.class, "uploadedFile", fFilename, par));
				}
				else  {
					var fUploads = uploads;
					template.include("alerts", () -> Alerts.info(FileManagementBase.class, "uploadedFiles", fUploads, par));
				}
					
			}
			// Mkdir
			else if(op.equals("mkdir")) {
				name = sanitizeFilename(name);
				var dir = par.resolve(name);
				
				alertable(FileManagementBase.class, template, () -> {
					Files.createDirectories(dir);
				}, () -> {
					template.include("alerts", () -> Alerts.info(FileManagementBase.class, "directoryCreated", dir));	
				});
			}
			
			// Cd
			else if(op.equals("cd")) {
				var dir = par.resolve(name);
				
				alertable(FileManagementBase.class, template, () -> {
					if(!Files.exists(dir)) {
						throw new NoSuchFileException(dir.toString());
					}
					String queryOptions = "?options=" + URLEncoder.encode(options.asAttributeValue(), "UTF-8");
					tx.redirect(Status.MOVED_TEMPORARILY,  uiPath + "/" + dir.getRoot().relativize(dir) + queryOptions);
				}, () -> {
					template.include("alerts", () -> Alerts.info(FileManagementBase.class, "noSuchDirectory", dir));	
				});
			}
			
			// Delete
			else if(op.equals("delete")) {
				var filenames = name.split("/");
				
				alertable(FileManagementBase.class, template, () -> {
					for(var fn : filenames) {
						fn = sanitizeFilename(fn);
						var fileOrDir = par.resolve(fn);
						FilesAndFolders.recursiveDelete(fileOrDir);
					}
				}, () -> {
					template.include("alerts", () -> Alerts.info(FileManagementBase.class, "filesDeleted", filenames.length));	
				});
			}
			
			// Download
			else if(op.equals("download")) {
				var filenames = name.split("/");
				try {
					if(filenames.length == 1) {
						var file = par.resolve(filenames[0]);
						if(Files.isDirectory(file)) {
							respondWithZip(tx, par, filenames);
						}
						else {
							UHTTPD.fileResource(par.resolve(file)).get(tx);
						}
					}
					else {
						respondWithZip(tx, par, filenames);
					}
				
					return Optional.empty();
				}
				catch(IOException ioe) {
					throw new UncheckedIOException(ioe);
				}
				catch(RuntimeException re) {
					throw re;
				}
				catch(Exception e) {
					throw new IllegalStateException(e);
				}
			}
			
			// Copy
			else if(op.equals("copy")) {
				var filenames = name.split("/");
				alertable(FileManagementBase.class, template, () -> {
					toClipboard(tx, par, filenames, ClipboardMode.COPY);
				}, () -> {
					template.include("alerts", () -> Alerts.info(FileManagementBase.class, "filesCopiedToClipboard", filenames.length));	
				});
			}
			
			// Cut
			else if(op.equals("cut")) {
				var filenames = name.split("/");
				alertable(FileManagementBase.class, template, () -> {
					toClipboard(tx, par, filenames, ClipboardMode.CUT);
				}, () -> {
					template.include("alerts", () -> Alerts.info(FileManagementBase.class, "filesCutToClipboard", filenames.length));	
				});
			}
			
			// Copy
			else if(op.equals("paste")) {
				var sz = clipboard.size();
				alertable(FileManagementBase.class, template, () -> {
				}, () -> {
					var it = clipboard.iterator();
					while(it.hasNext()) {
						var item = it.next();
						try {
							switch(item.mode()) {
							case CUT:
								Files.move(item.path(), par);
								break;
							case COPY:
								FilesAndFolders.copy(item.path(), par);
								break;
							default:
								throw new UnsupportedOperationException();
							}
						}
						finally {
							it.remove();
						}
					}
					template.include("alerts", () -> {
						return Alerts.info(FileManagementBase.class, "filesPastedFromClipboard", sz, par);
					});	
				});
				clipboard.clear();
			}
			else {
				throw new IllegalStateException("Unknown op.");
			}
			
			session.env().put("hiddenFiles", hiddenFiles);
		}
		else {
			options = getOptionsFromParameters(tx);
		}
		template.variable("hiddenFiles", hiddenFiles);
		template.variable("options", options.asAttributeValue());
		
		for(var action : FileManagerOptions.Action.values()) {
			template.condition("action." + action.name().toLowerCase().replace('_', '-'), options.actions().isEmpty() || options.actions().contains(action));
		}
		
		for(var navAction : FileManagerOptions.NavAction.values()) {
			template.condition("navAction." + navAction.name().toLowerCase().replace('_', '-'), options.navActions().isEmpty() ? Arrays.asList(NavAction.forBrowser()).contains(navAction) : options.navActions().contains(navAction));
		}
		
		for(var component : FileManagerOptions.Component.values()) {
			template.condition("component." + component.name().toLowerCase().replace('_', '-'), options.components().isEmpty() || options.components().contains(component));
		}
		
		template.variable("mode", options.mode().name().toLowerCase().replace('_', '-'));
		template.variable("selection-mode", options.selectionMode().name().toLowerCase().replace('_', '-'));
		template.condition("selectable", options.selectionMode() == SelectionMode.MULTIPLE);
		template.condition("row-actions", options.rowActions());
		
		try {
			var queryOptions = "?options=" + URLEncoder.encode(options.asAttributeValue(), "UTF-8");
			template.list("breadcrumbs", (content) -> breadcrumbs(tx, content, uiPath, queryOptions));
			template.variable("query-options", queryOptions);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Argh Java");
		}
		return Optional.of(template);
	}

	public Optional<TemplateModel> fragUpload(Transaction tx, String uiPath) {
		var par = filePathForRequestPath(tx);
		var template = TemplateModel.ofResource(FileManagementBase.class, "upload.frag.html")
					.bundle(FileManagementBase.class)
					.variable("path", () -> filePathForRequestPath(tx))
					.variable("parent", par::getParent);
		Web.setBase(tx);
		
		if(tx.method() == Method.POST) {
			
			var req = tx.request();
			var uploads = new AtomicInteger();
			var filename = new StringBuilder();
			
			Alerts.flash(alertable(FileManagementBase.class, template, () -> {
				for(var part : req.asParts(FormData.class)) {
					var fn = part.filename().get();
					if(fn.length() > 0) {
						filename.setLength(0);
						filename.append(fn);
						uploads.incrementAndGet();
						var fullpath = par.resolve(sanitizeFilename(filename.toString()));
						part.asFile(fullpath);
					}
				}
			}, () -> {
				if(uploads.get() == 0) {
					Alerts.flash(Alerts.danger(FileManagementBase.class, "noFilesUploaded"));					
				}
				else if(uploads.get() == 1) {	
					Alerts.flash(Alerts.info(FileManagementBase.class, "uploadedFile", filename.toString(), par));
				}
				else  {
					Alerts.flash(Alerts.info(FileManagementBase.class, "uploadedFiles", uploads.get(), par));
				}	
			}));
			
			tx.redirect(Status.MOVED_TEMPORARILY, uiPath + par.toString().replace('\\', '/'));
			return Optional.empty();
		}
		
		return Optional.of(template);
	}
	
	private void recursiveZip(Path rootPath, ZipOutputStream zos, Path fileOrDir) throws IOException {
		
			var rel = rootPath.relativize(fileOrDir);
		if(Files.isDirectory(fileOrDir)) {
			var zipEntry = new ZipEntry(rel.toString().replace('\\', '/') + "/");			
			zos.putNextEntry(zipEntry);
			zos.closeEntry();
			try(var str = Files.newDirectoryStream(fileOrDir)) { 
				for(var child : str) {
					if(child.getFileName().toString().equals(".") || child.getFileName().toString().equals("..")) /* TODO really? */
						continue;
					recursiveZip(rootPath, zos, fileOrDir.resolve(child));
				}
			}
		}
		else {			
			var zipEntry = new ZipEntry(rel.toString().replace('\\', '/'));
			zipEntry.setLastModifiedTime(Files.getLastModifiedTime(fileOrDir));
			zipEntry.setSize(Files.size(fileOrDir));
			Zip.putNextEntry(zos, zipEntry, fileOrDir);
			try(var in = Files.newInputStream(fileOrDir)) {
				in.transferTo(zos);
			}
			zos.closeEntry();
		}
		
	}

	private void respondWithZip(Transaction tx, Path par, String[] filenames) throws IOException {
		var zipFilename = filenames.length == 1 ? filenames[0] : par.getFileName().toString();
		tx.responseType("application/zip");
		tx.header("Content-Disposition", "attachment; filename=" + zipFilename+ ";");
					
		try(var zos = new ZipOutputStream(Channels.newOutputStream(tx.responseWriter()))) {
			for(var filename : filenames) {
				filename = sanitizeFilename(filename);
				var fileOrDir = par.resolve(filename);
				recursiveZip(par, zos, fileOrDir);
			}
		}
	}

	private Path root() {
		return vfs.root().iterator().next();
	}
	
	private String sanitizeFilename(String filename) {
		while(filename.startsWith("/") || filename.startsWith("\\")) {
			filename = filename.substring(1);
		}
		return filename;
	}
	

	private void toClipboard(Transaction tx, Path par, String[] filenames, ClipboardMode mode) {
		var clipboard = clipboard();
		clipboard.clear();
		for(var filename : filenames) {
			clipboard.add(new ClipboardItem(par.resolve(filename), mode));
		}
	}

	private String typeIcon(Path path) {
		if(Files.isDirectory(path)) {
			return "folder";
		}
		else if(Files.isSymbolicLink(path)) {
			return "link";
		}
		else {
			return "file";
		}
	}
	
	private String typeKey(Path path) {
		if(Files.isDirectory(path)) {
			return "directory";
		}
		else if(Files.isSymbolicLink(path)) {
			return "symlink";
		}
		else {
			return "file";
		}
	}

}
