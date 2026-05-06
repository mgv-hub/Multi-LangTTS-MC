package com.mgv.client.tts.util;

public class TtsLogger {

    private static final String PREFIX_DEBUG = "[TTS-DEBUG] ";
    private static final String PREFIX_INFO = "[TTS-INFO] ";
    private static final String PREFIX_WARN = "[TTS-WARN] ";
    private static final String PREFIX_ERROR = "[TTS-ERROR] ";
    
    // debug logs go to stdout - filtered via launcher args in prod if needed
    
    public void debug(String message) {
        System.out.println(PREFIX_DEBUG + message);
    }
    
    public void info(String message) {
        System.out.println(PREFIX_INFO + message);
    }
    
    public void warn(String message) {
        System.out.println(PREFIX_WARN + message);
    }
    
    public void warn(String message, Throwable t) {
        System.out.println(PREFIX_WARN + message);
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }
    
    public void error(String message) {
        System.out.println(PREFIX_ERROR + message);
    }
    
    public void error(String message, Throwable t) {
        System.out.println(PREFIX_ERROR + message);
        if (t != null) {
            t.printStackTrace(System.out);
        }
    }
    
    /*
     * Considered switching to SLF4J/Log4j for log level control.
     * Kept simple for now - avoids extra dependencies and works out-of-box with MC launcher.
     */
}