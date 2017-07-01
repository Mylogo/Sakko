package me.mylogo.sakko;

/**
 * Created by Dennis Heckmann on 01.07.17
 * Copyright (c) 2017 Dennis Heckmann
 */
@FunctionalInterface
public interface SakkoDirectMessageHandler {

    void handleInput(IReactToIt reacter, String message);

}
