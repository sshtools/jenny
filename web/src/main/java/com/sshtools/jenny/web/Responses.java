package com.sshtools.jenny.web;

import java.text.MessageFormat;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.jenny.api.Resources;


public class Responses {
	
	@SuppressWarnings("serial")
	public final static class RedirectResponse extends RuntimeException {
		public RedirectResponse(String location) {
			super(location);
		}
		
		public String location() {
			return getMessage();
		}
		
		public String toString() {
			return Responses.redirect(location()).toString();
		}
	}

	private final static Log LOG = Logs.of(WebLog.WEB);
	
	public static JsonObject success() {
		return buildSuccess().
				build();
	}

	public static JsonObjectBuilder buildSuccess() {
		return Json.createObjectBuilder().
				add("success", true);
	}
	
	public static JsonObject success(JsonValue payload) {
		return buildSuccess().
				add("payload", payload).
				build();
	}
	
	public static JsonObject redirect(String location) {
		return Json.createObjectBuilder().
				add("redirect", true).
				add("location", location).
				build();
	}

	public static JsonObject error(Exception exception) {
		LOG.error("API failure.", exception);
		return error(exception.getClass().getName(), exception.getMessage() == null ? "No additional detail supplied." : exception.getMessage());
		
	}

	public static JsonObject error(Class<?> bundle,  String key, Object... args) {
		var rsrcs = Resources.of(bundle, WebState.get().locale());
		var message = args.length == 0 ? rsrcs.getString(key) : MessageFormat.format(rsrcs.getString(key), args);
		LOG.error("API failure. {}", message);
		return error(Exception.class.getName(), message);
	}

	public static JsonObject error(String type, String message) {
		return Json.createObjectBuilder().
				add("success", false).
				add("type", type).
				add("message", message).
				build();
	}
}
