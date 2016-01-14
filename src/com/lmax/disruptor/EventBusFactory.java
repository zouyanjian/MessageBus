package com.lmax.disruptor;

import dorkbox.util.messagebus.MessageHolder;

/**
 * @author dorkbox, llc
 *         Date: 2/2/15
 */
public class EventBusFactory implements EventFactory<MessageHolder> {

    public EventBusFactory() {
    }

    @Override
    public
    MessageHolder newInstance() {
        return new MessageHolder();
    }
}