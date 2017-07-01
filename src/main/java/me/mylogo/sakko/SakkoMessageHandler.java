package me.mylogo.sakko;

/**
 * Created by Dennis Heckmann on 30.06.17
 * Copyright (c) 2017 Dennis Heckmann
 */
@FunctionalInterface
public interface SakkoMessageHandler {

    void handleInput(String message);

}
