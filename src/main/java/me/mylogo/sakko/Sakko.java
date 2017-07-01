package me.mylogo.sakko;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Created by Dennis Heckmann on 30.06.17
 * Copyright (c) 2017 Dennis Heckmann
 */
public abstract class Sakko implements IReactToIt {

    static final String THE_SPLITTER = "*", SUBSCRIBE_CHANNEL = "SubCH";
    private Map<String, List<SakkoMessageHandler>> handlers;
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 28080;
    private int port;

    public Sakko() {
        this(DEFAULT_PORT);
    }

    public Sakko(int port) {
        this.port = port;
        handlers = new ConcurrentHashMap<>();
    }

    public int getPort() {
        return port;
    }

    public void addHandler(String channel, SakkoMessageHandler handler) {
        List<SakkoMessageHandler> handlerList = this.handlers.get(channel);
        if (handlerList == null) {
            handlerList = new ArrayList<>();
            handlers.put(channel, handlerList);
        }
        synchronized (handlerList) {
            handlerList.add(handler);
        }
    }

    public abstract void publish(String channel, String message);
    public abstract void subscribe(String channel);
    public abstract void close() throws SakkoException;

    public void subscribe(String channel, SakkoMessageHandler handler) {
        subscribe(channel);
        addHandler(channel, handler);
    }

    String makeMessage(String channel, String message) {
        return channel + THE_SPLITTER + message;
    }

    void handle(String input, BiConsumer<String, String> consumer) {
        int index = input.indexOf(THE_SPLITTER);
        if (index != -1) {
            String channelName = input.substring(0, index);
            String message = input.substring(index + 1, input.length());
            consumer.accept(channelName, message);
        }
    }

    @Override
    public void reactTo(String channel, String message) {
        List<SakkoMessageHandler> theHandlers = handlers.get(channel);
        if (theHandlers != null) {
            theHandlers.forEach(it -> it.handleInput(message));
        }
    }

}
