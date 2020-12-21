package me.asu.shell;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * output to stdout
 */
public class ShellCommandExecutor2 extends ShellCommandExecutor
{

    private BufferedReaderHandler handler = new BufferedReaderHandler() {};

    public ShellCommandExecutor2(String... execString)
    {
        super(execString);
    }

    public ShellCommandExecutor2(File dir, String... execString)
    {
        super(dir, execString);
    }

    public ShellCommandExecutor2(File dir, Map<String, String> env, String... execString)
    {
        super(dir, env, execString);
    }

    public ShellCommandExecutor2(File dir,
                                 Map<String, String> env,
                                 long timeout,
                                 String... execString)
    {
        super(dir, env, timeout, execString);
    }

    public ShellCommandExecutor2(File dir,
                                 Map<String, String> env,
                                 long timeout,
                                 boolean inheritParentEnv,
                                 String... execString)
    {
        super(dir, env, timeout, inheritParentEnv, execString);
    }

    @Override
    protected void parseExecResult(BufferedReader lines) throws IOException
    {
        if (handler != null) {
            handler.handle(lines);
        } else {
            super.parseExecResult(lines);
        }
    }


    public BufferedReaderHandler getHandler()
    {
        return handler;
    }

    public void setHandler(BufferedReaderHandler handler)
    {
        this.handler = handler;
    }
}