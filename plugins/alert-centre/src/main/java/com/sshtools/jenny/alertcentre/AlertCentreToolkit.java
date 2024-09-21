package com.sshtools.jenny.alertcentre;

import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public interface AlertCentreToolkit {
	
	String icon(NotificationType type);
	
	String textStyle(NotificationType type);
	
	String bgStyle(NotificationType type);
	
	String titleBgStyle(NotificationType type);

	TemplateModel template(Transaction tx);
}
