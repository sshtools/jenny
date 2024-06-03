/**
 * Copyright © 2023 JAdaptive Limited (support@jadaptive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.jenny.io;

import java.io.Closeable;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.sshtools.bootlace.api.Logs;
import com.sshtools.bootlace.api.Logs.Category;
import com.sshtools.bootlace.api.Logs.Log;
import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.web.Web;
import com.sshtools.jenny.web.WebModule;
import com.sshtools.uhttpd.UHTTPD.WebSocket;
import com.sshtools.uhttpd.UHTTPD.WebSocketBuilder;
import com.sshtools.uhttpd.UHTTPD.WebSocketHandler;

public class Io implements Plugin {
	final static Log LOG = Logs.of(Category.ofName(Io.class));
	
	public final static WebModule MODULE_IO = WebModule.of("/io/io.js", Io.class, "io.js");

	private record SocketChannelKey(WebSocket socket, String channel) {
	}

	public interface Contributor extends Closeable {
		Function<Sender, IoChannel> channelFactory();
		
		@Override
		void close();
	}

	public interface Sender {
		void send(JsonValue data);
	}
	
	private final static class SenderImpl implements Sender {
		private final WebSocket socket;
		private final String channel;
		
		SenderImpl(WebSocket socket, String channel) {
			this.socket =socket;
			this.channel = channel;
		}

		@Override
		public void send(JsonValue data) {
			socket.send(Json.createObjectBuilder().
					add("type", "message").
					add("channel", channel).
					add("data", data).
					build().toString());
		}
		
	}

	private final static class IoChannelImpl implements IoChannel {

		private Sender sender;
		private Consumer<JsonValue> receiver;
		private Optional<Consumer<IoChannel>> onUnsubscribe;

		private IoChannelImpl(Sender sender, Consumer<JsonValue> receiver, Consumer<IoChannel> onUnsubscribe) {
			super();
			this.sender = sender;
			this.receiver = receiver;
			this.onUnsubscribe = Optional.ofNullable(onUnsubscribe);
		}

		@Override
		public void send(JsonValue data) {
			sender.send(data);
		}
	}

	public interface IoChannel extends Sender {
		
		public static IoChannel of(Sender sender) {
			return of(sender, (e) -> {});
		}
		
		public static IoChannel of(Sender sender, Consumer<JsonValue> receiver) {
			return new IoChannelImpl(sender, receiver, null);
		}

		public static IoChannel of(Sender sender, Consumer<JsonValue> receiver, Consumer<IoChannel> onUnsubscribe) {
			return new IoChannelImpl(sender, receiver, onUnsubscribe);
		}
	}

	private final List<WebSocket> webSockets;
	private final Map<String, Contributor> contributors = new ConcurrentHashMap<>();
	private final WebSocketHandler io;
	private Web web;
	private final Map<SocketChannelKey, IoChannel> channels = new ConcurrentHashMap<>();

	public Io() {
		webSockets = new CopyOnWriteArrayList<>();
		io = new WebSocketBuilder().onText(this::receive).onData((data, last, ws) -> {
		}).onClose((code, text, ws) -> {
			removeWebsocket(ws);
		}).onOpen((ws) -> {
			webSockets.add(ws);
		}).build();
	}

	private void removeWebsocket(WebSocket ws) {
		channels.keySet().stream().filter(en -> en.socket.equals(ws)).collect(Collectors.toSet()).forEach(r -> channels.remove(r));
		webSockets.remove(ws);
	}

	private void receive(String text, WebSocket websocket) {

		var jr = Json.createReader(new StringReader(text));
		var msg = jr.readObject();
		var type = msg.getString("type");

		if (type.equals("subscribe")) {
			var channel = msg.getString("channel");
			var contributor = contributors.get(channel);
			if (contributor == null)
				LOG.warning("No I/O contributor {0}", channel);
			else
				channels.put(new SocketChannelKey(websocket, channel), contributor.channelFactory().apply(new SenderImpl(websocket, channel)));
		} else if (type.equals("unsubscribe")) {
			var ch = channels.remove(new SocketChannelKey(websocket, msg.getString("channel")));
			((IoChannelImpl)ch).onUnsubscribe.ifPresent(c -> c.accept(ch));
		} else if (type.equals("message")) {
			((IoChannelImpl)channels.get(new SocketChannelKey(websocket, msg.getString("channel")))).receiver.accept(msg.get("data"));
		}
	}

	public WebSocketHandler io() {
		return io;
	}

	@Override
	public void open(PluginContext context) {
		web = context.plugin(Web.class);
		context.autoClose(
			web.modules(MODULE_IO),
			web.router().route().
				webSocket("/io/io", io).build()
		);
	}

	public WebModule webModule() {
		return MODULE_IO;
	}

	/**
	 * Send notification message.
	 * 
	 * @param title    title
	 * @param subtitle title
	 * @param icon     icon
	 * @param body     body
	 * @param style    class
	 * @return
	 */
	public void broadcast(String channel, JsonObject msg) {
		channels.keySet().stream().
			filter(k -> k.channel().equals(channel)).
			forEach(k -> {
				var jmsg = Json.createObjectBuilder().
					add("type", "message").
					add("channel", channel).
					add("data", msg).
					build().toString();
				
				if(LOG.debug())
					LOG.debug("Broadcast: {0}", jmsg);
				
				k.socket().send(jmsg);
			});
	}

	/**
	 * Generic refresh
	 */
	public void refresh() {
		webSockets.forEach(ws -> ws.send(Json.createObjectBuilder().
				add("type", "refresh").
				build().toString()));
	}

	/**
	 * Generic reload
	 */
	public void reload() {
		webSockets.forEach(ws -> ws.send(Json.createObjectBuilder().
				add("type", "reload").
				build().toString()));
	}

	public Contributor contributor(String channel, Function<Sender, IoChannel> channelFactory) {
		if (contributors.containsKey(channel))
			throw new IllegalArgumentException(
					MessageFormat.format("Contribute for channel {0} already known.", channel));
		return new Contributor() {
			{
				contributors.put(channel, this);
			}

			@Override
			public void close() {
				contributors.remove(channel);
			}

			@Override
			public Function<Sender, IoChannel> channelFactory() {
				return channelFactory;
			}
		};
	}
}
