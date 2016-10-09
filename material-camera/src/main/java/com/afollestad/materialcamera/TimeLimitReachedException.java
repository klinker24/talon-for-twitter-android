package com.afollestad.materialcamera;

/**
 * @author Aidan Follestad (afollestad)
 */
public class TimeLimitReachedException extends Exception {

    public TimeLimitReachedException() {
        super("You've reached the time limit without starting a recording.");
    }
}