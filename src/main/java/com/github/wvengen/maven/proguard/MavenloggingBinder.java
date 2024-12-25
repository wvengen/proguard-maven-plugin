package com.github.wvengen.maven.proguard;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

import java.io.PrintStream;

public class MavenloggingBinder extends DefaultLogger {

    private final Log log;

    public MavenloggingBinder(Log log) {
        this.log = log;
    }

    @Override
    public void setOutputPrintStream(PrintStream output) {

    }

    @Override
    public void setErrorPrintStream(PrintStream err) {

    }

    @Override
    protected void printMessage(String message, PrintStream stream, int priority) {
        switch (priority) {
            case Project.MSG_ERR: {
                log.error(message);
                break;
            }
            case Project.MSG_VERBOSE:
            case Project.MSG_DEBUG: {
                log.debug(message);
                break;
            }
            case Project.MSG_WARN: {
                log.warn(message);
                break;
            }
            default: {
                log.info(message);
                break;
            }
        }
    }
}
