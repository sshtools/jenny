package com.sshtools.jenny.bootstrap5;

import java.util.function.Consumer;

import com.sshtools.tinytemplate.Templates.TemplateModel;
import com.sshtools.tinytemplate.bootstrap.forms.Form.FormDataReceiver;
import com.sshtools.tinytemplate.bootstrap.forms.Form.FormFile;
import com.sshtools.tinytemplate.bootstrap.forms.Form.Results;
import com.sshtools.tinytemplate.bootstrap.forms.InputType;
import com.sshtools.tinytemplate.bootstrap.forms.Text;
import com.sshtools.tinytemplate.bootstrap.forms.Validation.ValidationException;
import com.sshtools.uhttpd.UHTTPD.FormData;
import com.sshtools.uhttpd.UHTTPD.Transaction;

public class TemplatedFormDataReceiver {
	
	public static <R> void alertForResults(Class<?> base, TemplateModel template, Results<R> res, Consumer<Results<R>> onOk, String errSuffix) {
		if(res.ok()) {
			template.variable("validated", true);
			Alerts.alertable(base, template, () -> {
				try {
					onOk.accept(res);
				}
				catch(ValidationException ve) {
					template.include("alerts", () -> Alerts.danger(base, "error." + errSuffix, 
							ve.text().map(Text::resolveString).orElse("")));
				}
			});
		}
		else {		
			var errs = res.results();
			if(errs.size() == 1) {
				template.include("alerts", () -> Alerts.danger(base, "error." + errSuffix, 
						errs.get(0).firstError().text().map(Text::resolveString).orElse("")));
			}
			else {
				template.include("alerts", () -> Alerts.danger(base, "errors." + errSuffix, errs.size()));
			}
		}
	}
	

	@SuppressWarnings("resource")
	public static Consumer<FormDataReceiver> txFormDataReceiver(Transaction tx) {
		return fdr -> {
			for(var part : tx.request().asBufferedParts()) {
				if(part instanceof FormData fd) {
					var field = fdr.field(part.name());
					if(field != null) {
						if(field.resolveInputType() == InputType.FILE)
							fdr.file(field,
								new FormFile(
									fd.filename().orElse("upload"), 
									fd.contentType().orElse("application/octet-stream"), 
									-1, 
									fd.asStream()
								)
							); 
						else
							fdr.field(field, part.asString());
					}
				}
				else
					throw new UnsupportedOperationException("todo");
			}
		};
	}
	
}
