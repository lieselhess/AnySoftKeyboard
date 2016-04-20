package com.radicalninja.logger;

public class LoggerNotCreatedException extends RuntimeException {

    public LoggerNotCreatedException() {
        super("LogManager not initialized. Call LogManager.init(Context) first.");
    }

}
