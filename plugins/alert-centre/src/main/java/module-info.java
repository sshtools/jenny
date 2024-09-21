import com.sshtools.bootlace.api.Plugin;
import com.sshtools.jenny.alertcentre.AlertCentre;

module com.sshtools.jenny.alertscentre {
	exports com.sshtools.jenny.alertcentre ;
	opens com.sshtools.jenny.alertcentre;
	requires java.json;
	requires transitive com.sshtools.jenny.api;
	requires transitive com.sshtools.jenny.i18n;
	provides Plugin with AlertCentre;
}