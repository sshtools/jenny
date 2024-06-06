import com.sshtools.bootlace.api.Plugin;
import com.sshtools.jenny.files.FileManagementBase;

module com.sshtools.jenny.files {
	exports com.sshtools.jenny.files ;
	opens com.sshtools.jenny.files;
	requires com.sshtools.jenny.vfs;
	requires java.json;
	requires transitive com.sshtools.jenny.api;
	requires transitive com.sshtools.jenny.bootstrap5;
	requires transitive com.sshtools.jenny.i18n;
	provides Plugin with FileManagementBase;
}