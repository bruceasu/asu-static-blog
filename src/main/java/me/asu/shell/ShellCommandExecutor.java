package me.asu.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A simple shell command executor.
 *
 * <code>ShellCommandExecutor</code>should be used in cases where the output
 * of the command needs no explicit parsing and where the command, working directory and the
 * environment remains unchanged. The output of the command is stored as-is and is expected to be
 * small.
 */
public class ShellCommandExecutor extends Shell implements CommandExecutor {

    private String[]     command;
    private StringBuffer output;


    public ShellCommandExecutor(String... execString) {
        this(null, execString);
    }

    public ShellCommandExecutor(File dir, String... execString) {
        this(dir, null, execString);
    }

    public ShellCommandExecutor(File dir,
            Map<String, String> env,
            String... execString) {
        this(dir, env, 0L, execString);
    }

    public ShellCommandExecutor(File dir,
            Map<String, String> env,
            long timeout,
            String... execString) {
        this(dir, env, timeout, true, execString);
    }

    /**
     * Create a new instance of the ShellCommandExecutor to execute a command.
     *
     * @param execString       The command to execute with arguments
     * @param dir              If not-null, specifies the directory which should be set as the
     *                         current working directory for the command. If null, the current
     *                         working directory is not modified.
     * @param env              If not-null, environment of the command will include the key-value
     *                         pairs specified in the map. If null, the current environment is not
     *                         modified.
     * @param timeout          Specifies the time in milliseconds, after which the command will be
     *                         killed and the status marked as timedout. If 0, the command will not
     *                         be timed out.
     * @param inheritParentEnv Indicates if the process should inherit the env vars from the parent
     *                         process or not.
     */
    public ShellCommandExecutor(File dir,
            Map<String, String> env,
            long timeout,
            boolean inheritParentEnv,
            String... execString) {
        command = execString.clone();
        if (dir != null) {
            setWorkingDirectory(dir);
        }
        if (env != null) {
            setEnvironment(env);
        }
        timeOutInterval       = timeout;
        this.inheritParentEnv = inheritParentEnv;
    }


    /**
     * Execute the shell command.
     */
    @Override
    public void execute() throws IOException {
        for (String s : command) {
            if (s == null) {
                throw new IOException("(null) entry in command string: "
                        + String.join(" ", command));
            }
        }
        this.run();
    }

    /**
     * Get the output of the shell command.
     */
    @Override
    public String getOutput() {
        return (output == null) ? "" : output.toString();
    }

    @Override
    public void close() {
    }

    /**
     * Returns the commands of this instance. Arguments with spaces in are presented with quotes
     * round; other arguments are presented raw
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String[]      args    = getExecString();
        for (String s : args) {
            if (s.indexOf(' ') >= 0) {
                builder.append('"').append(s).append('"');
            } else {
                builder.append(s);
            }
            builder.append(' ');
        }
        return builder.toString();
    }

    @Override
    public String[] getExecString() {
        return command;
    }

    @Override
    protected void parseExecResult(BufferedReader lines) throws IOException {
        output = new StringBuffer();
        char[] buf = new char[512];
        int    nRead;
        while ((nRead = lines.read(buf, 0, buf.length)) > 0) {
            output.append(buf, 0, nRead);
        }
    }
}