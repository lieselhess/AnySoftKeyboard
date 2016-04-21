package com.radicalninja.logger;

class LoggerNotCreatedException extends RuntimeException {

    LoggerNotCreatedException() {
        super("LogManager not initialized. Call LogManager.init(Context) first.");
    }

}
