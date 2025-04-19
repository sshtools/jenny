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
package com.sshtools.jenny.messaging;

import static com.sshtools.bootlace.api.PluginContext.$;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sshtools.bootlace.api.Plugin;
import com.sshtools.bootlace.api.PluginContext;
import com.sshtools.jenny.api.Api;
import com.sshtools.jenny.config.Config;
import com.sshtools.jini.INI.Section;
import com.sshtools.jini.config.INISet;

public class Messaging implements Plugin {
	private final Config config					= $().plugin(Config.class);
	private final Api api 						= $().plugin(Api.class);
	
	private INISet messagingConfig;
	private Section providersMessagingConfig;
	
	@Override
	public void afterOpen(PluginContext context) {
		
		messagingConfig = config.configBuilder("messaging").build();
		context.autoClose(
			messagingConfig
		);
		
		providersMessagingConfig = messagingConfig.document().obtainSection("providers");
	}

	@SuppressWarnings("unchecked")
	public <B extends MessageBuilder> MessageDeliveryProvider<B> bestProvider(MediaType media, Class<B> builder) {
		for(var prov : enabledProviders()) {
			if(prov.supportedMedia().equals(media)) {
				return (MessageDeliveryProvider<B>)prov;
			}
		}
		throw new IllegalArgumentException(String.format("No %s providers were available to deliver a message.", media));
	}

	@SuppressWarnings("unchecked")
	public <P extends MessageDeliveryProvider<?>> Optional<P> provider(String className) {
		return (Optional<P>) providers().stream().filter(p->p.getClass().getName().equals(className)).findFirst();
	}

	@SuppressWarnings("unchecked")
	public <P extends MessageDeliveryProvider<?>> List<P> providers() {
		return api.extensions().points(MessageDeliveryProvider.class).stream().map(p->(P)p.apply(this)).toList();
	}
	
	@SuppressWarnings("unchecked")
	public <P extends MessageDeliveryProvider<?>> List<P> enabledProviders() {
		return (List<P>) providers().stream().filter(this::isEnabled).toList();
	}
	
	private <P extends MessageDeliveryProvider<?>> boolean isEnabled(P provider) {
		if(!provider.enabled())
			return false;
		
		var enabledProviders = asList(providersMessagingConfig.getAllElse("enabled"));
		var disabledProviders = asList(providersMessagingConfig.getAllElse("disabled"));
		var clz = provider.getClass().getName();
		
		return ( enabledProviders.contains(clz) ) ||
			   ( !disabledProviders.contains(clz) && enabledProviders.isEmpty() );
	}

	public Optional<String> defaultForMediaType(MediaType mediaType) {
		return providersMessagingConfig.obtainSection(mediaType.name().toLowerCase()).getOr("default");
	}
	
	@SuppressWarnings("unchecked")
	public <B extends MessageBuilder> MessageDeliveryProvider<B> getProviderOrBest(MediaType media, String provider, Class<B> builder) {
		if(provider == null || provider.length() == 0) {
			var def = defaultForMediaType(media);
			if(def.isPresent()) {
				var prov = provider(def.get());
				if(prov.isPresent() ) {
					var p = prov.get();
					if(!isEnabled(p)) {
						throw new IllegalStateException(String.format("The default %s provider (%s) is not enabled. This may be due to configuration or licensing.", media, def));
					}
					if(!p.isDefault()) {
						throw new IllegalStateException(String.format("The %s provider (%s) cannot be used the default. Please select a different default provider, or select a specific provider for this operation.", media, def));
					}
					return (MessageDeliveryProvider<B>)p;
				}
			}
			return bestProvider(media, builder);
		}
		else {
			/* If a provider is specified, it MUST be available and enabled */
			var p = provider(provider).orElseThrow(() -> new IllegalStateException(String.format("Required %s provider (%s) does not exist.", media, provider)));
			if(!p.enabled()) {
				throw new IllegalStateException(String.format("Required %s provider (%s) is not enabled. This may be due to configuration or licensing.", media, provider));
			}
			return (MessageDeliveryProvider<B>)p;
		}
	}

	public List<MessageDeliveryProvider<?>> getProviders(MediaType type) {
		return providers().stream().filter(p -> p.supportedMedia().equals(type)).collect(Collectors.toList());
	}
}
