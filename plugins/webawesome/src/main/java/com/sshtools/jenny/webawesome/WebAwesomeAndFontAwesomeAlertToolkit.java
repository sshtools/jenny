package com.sshtools.jenny.webawesome;

import com.sshtools.jenny.alertcentre.AlertCentreToolkit;
import com.sshtools.jenny.alertcentre.NotificationType;
import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class WebAwesomeAndFontAwesomeAlertToolkit implements AlertCentreToolkit {

	@Override
	public String icon(NotificationType type) {
		switch(type) {
		case DANGER:
			return "circle-exclamation";
		case WARNING:
			return "triangle-exclamation";
		case DOWNLOAD:
			return "download";
		default:
			return "circle-info";
		}
	}

	@Override
	public String textStyle(NotificationType type) {
		switch(type) {
		case DANGER:
			return "danger";
		case WARNING:
			return "warning";
		case DOWNLOAD:
			return "success";
		default:
			return "brand";
		}
	}

	@Override
	public String bgStyle(NotificationType type) {
		switch(type) {
		case DANGER:
			return "danger";
		case WARNING:
			return "warning";
		case DOWNLOAD:
			return "";
		default:
			return "brand";
		}
	}

	@Override
	public String titleBgStyle(NotificationType type) {
		switch(type) {
		case DANGER:
			return "danger";
		case WARNING:
			return "warning";
		case DOWNLOAD:
			return "success";
		default:
			return "brand";
		}
	}

	@Override
	public TemplateModel template(Transaction tx) {
		return TemplateModel.ofResource(WebAwesomeAndFontAwesomeAlertToolkit.class, "wafa.alertcentre.frag.html");
	}
}
