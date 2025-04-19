package com.sshtools.jenny.mail;

import com.sshtools.jenny.messaging.MessageBuilder;
import com.sshtools.jenny.messaging.MessageDeliveryProvider;

public interface EmailMessageDeliveryProvider<B extends MessageBuilder> extends MessageDeliveryProvider<B> {

}
