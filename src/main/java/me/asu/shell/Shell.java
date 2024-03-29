package me.asu.shell;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A base class for running a Unix command.
 *
 * <code>Shell</code> can be used to run unix commands like <code>du</code> or
 * <code>df</code>. It also offers facilities to gate commands by
 * time-intervals.
 */
abstract public class Shell {


    /**
     * Maximum command line length in Windows KB830473 documents this as 8191
     */
    public static final int WINDOWS_MAX_SHELL_LENGTH = 8191;
    /**
     * a Unix command to get the current user's name: {@value}.
     */
    public static final String USER_NAME_COMMAND = "whoami";
    /**
     * Windows CreateProcess synchronization object
     */
    public static final Object WindowsProcessLaunchLock = new Object();
    public static final OSType osType = getOSType();
    public static final boolean WINDOWS = (osType == OSType.OS_TYPE_WIN);

    // OSType detection
    public static final boolean SOLARIS = (osType == OSType.OS_TYPE_SOLARIS);
    public static final boolean MAC     = (osType == OSType.OS_TYPE_MAC);
    public static final boolean FREEBSD = (osType == OSType.OS_TYPE_FREEBSD);

    // Helper static vars for each platform
    public static final boolean LINUX   = (osType == OSType.OS_TYPE_LINUX);
    public static final boolean OTHER   = (osType == OSType.OS_TYPE_OTHER);
    public static final boolean PPC_64 = System.getProperties()
                                               .getProperty("os.arch")
                                               .contains("ppc64");
    /**
     * Token separator regex used to parse Shell tool outputs
     */
    public static final String TOKEN_SEPARATOR_REGEX = WINDOWS ? "[|\n\r]"
            : "[ \t\n\r\f]";
    /**
     * merge stdout and stderr
     */
    private final boolean redirectErrorStream;
    /**
     * Time after which the executing script would be timedout
     */
    protected long timeOutInterval = 0L;
    /**
     * Indicates if the parent env vars should be inherited or not
     */
    protected boolean inheritParentEnv = true;
    /**
     * If or not script timed out
     */
    private AtomicBoolean timedOut;
    /**
     * refresh interval in millis seconds
     */
    private long interval;
    /**
     * last time the command was performed
     */
    private long lastTime;
    /**
     * env for the command execution
     */
    private Map<String, String> environment;
    private File dir;
    /**
     * sub process used to execute the command
     */
    private Process process;
    private int exitCode;
    /**
     * If or not script finished executing
     */
    private volatile AtomicBoolean completed;


    public Shell() {
        this(0L);
    }

    public Shell(long interval) {
        this(interval, false);
    }

    /**
     * @param interval the minimum duration to wait before re-executing the command.
     */
    public Shell(long interval, boolean redirectErrorStream) {
        this.interval            = interval;
        this.lastTime            = (interval < 0) ? 0 : -interval;
        this.redirectErrorStream = redirectErrorStream;
    }

    /**
     * Checks if a given command (String[]) fits in the Windows maximum command line length Note
     * that the input is expected to already include space delimiters, no extra count will be added
     * for delimiters.
     *
     * @param commands command parts, including any space delimiters
     */
    public static void checkWindowsCommandLineLength(String... commands)
    throws IOException {
        int len = 0;
        for (String s : commands) {
            len += s.length();
        }
        if (len > WINDOWS_MAX_SHELL_LENGTH) {
            throw new IOException(String.format(
                    "The command line has a length of %d exceeds maximum allowed length of %d. "
                            + "Command starts with: %s", len, WINDOWS_MAX_SHELL_LENGTH, String
                            .join("", commands)
                            .substring(0, 100)));
        }
    }

    static private OSType getOSType() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            return OSType.OS_TYPE_WIN;
        } else if (osName.contains("SunOS") || osName.contains("Solaris")) {
            return OSType.OS_TYPE_SOLARIS;
        } else if (osName.contains("Mac")) {
            return OSType.OS_TYPE_MAC;
        } else if (osName.contains("FreeBSD")) {
            return OSType.OS_TYPE_FREEBSD;
        } else if (osName.startsWith("Linux")) {
            return OSType.OS_TYPE_LINUX;
        } else {
            // Some other form of Unix
            return OSType.OS_TYPE_OTHER;
        }
    }

    /**
     * Return a regular expression string that match environment variables
     */
    public static String getEnvironmentVariableRegex() {
        return (WINDOWS) ? "%([A-Za-z_][A-Za-z0-9_]*?)%"
                : "\\$([A-Za-z_][A-Za-z0-9_]*)";
    }

    /**
     * Returns a File referencing a script with the given basename, inside the given parent
     * directory.  The file extension is inferred by platform: ".cmd" on Windows, or ".sh"
     * otherwise.
     *
     * @param parent   File parent directory
     * @param basename String script file basename
     * @return File referencing the script in the directory
     */
    public static File appendScriptExtension(File parent, String basename) {
        return new File(parent, appendScriptExtension(basename));
    }

    /**
     * Returns a script file name with the given basename.  The file extension is inferred by
     * platform: ".cmd" on Windows, or ".sh" otherwise.
     *
     * @param basename String script file basename
     * @return String script file name
     */
    public static String appendScriptExtension(String basename) {
        return basename + (WINDOWS ? ".cmd" : ".sh");
    }

    /**
     * Returns a command to run the given script.  The script interpreter is inferred by platform:
     * cmd on Windows or bash otherwise.
     *
     * @param script File script to run
     * @return String[] command to run the script
     */
    public static String[] getRunScriptCommand(File script) {
        String absolutePath = script.getAbsolutePath();
        return WINDOWS ? new String[]{"cmd", "/c", absolutePath}
                : new String[]{"/bin/bash", bashQuote(absolutePath)};
    }

    /**
     * Quote the given arg so that bash will interpret it as a single value. Note that this quotes
     * it for one level of bash, if you are passing it into a badly written shell script, you need
     * to fix your shell script.
     *
     * @param arg the argument to quote
     * @return the quoted string
     */
    static String bashQuote(String arg) {
        StringBuilder buffer = new StringBuilder(arg.length() + 2);
        buffer.append('\'');
        buffer.append(arg.replace("'", "'\\''"));
        buffer.append('\'');
        return buffer.toString();
    }

    /**
     * Static method to execute a shell command. Covers most of the simple cases without requiring
     * the user to implement the <code>Shell</code> interface.
     *
     * @param cmd shell command to execute.
     * @return the output of the executed command.
     */
    public static String execCommand(String... cmd) throws IOException {
        return execCommand(null, cmd);
    }

    /**
     * Static method to execute a shell command. Covers most of the simple cases without requiring
     * the user to implement the <code>Shell</code> interface.
     *
     * @param env the map of environment key=value
     * @param cmd shell command to execute.
     * @return the output of the executed command.
     */
    public static String execCommand(Map<String, String> env, String... cmd)
    throws IOException {
        return execCommand(env, cmd, 0L);
    }

    /**
     * Static method to execute a shell command. Covers most of the simple cases without requiring
     * the user to implement the <code>Shell</code> interface.
     *
     * @param env     the map of environment key=value
     * @param cmd     shell command to execute.
     * @param timeout time in milliseconds after which script should be marked timeout
     * @return the output of the executed command.o
     */
    public static String execCommand(Map<String, String> env,
            String[] cmd,
            long timeout) throws IOException {
        ShellCommandExecutor exec = new ShellCommandExecutor(null, env, timeout, cmd);
        exec.execute();
        return exec.getOutput();
    }

    /**
     * set the environment for the command
     *
     * @param env Mapping of environment variables
     */
    protected void setEnvironment(Map<String, String> env) {
        this.environment = env;
    }

    /**
     * set the working directory
     *
     * @param dir The directory where the command would be executed
     */
    protected void setWorkingDirectory(File dir) {
        this.dir = dir;
    }

    /**
     * check to see if a command needs to be executed and execute if needed
     */
    protected void run() throws IOException {
        if (lastTime + interval > System.currentTimeMillis()) {
            return;
        }
        exitCode = 0; // reset for next run
        runCommand();
    }

    /**
     * Run a command
     */
    private void runCommand() throws IOException, OutOfMemoryError {
        ProcessBuilder        builder          = new ProcessBuilder(getExecString());
        Timer                 timeOutTimer     = null;
        ShellTimeoutTimerTask timeoutTimerTask = null;
        timedOut  = new AtomicBoolean(false);
        completed = new AtomicBoolean(false);

        if (environment != null) {
            builder.environment().putAll(this.environment);
        }

        // Remove all env vars from the Builder to prevent leaking of env vars from
        // the parent process.
        if (!inheritParentEnv) {

        }

        if (dir != null) {
            builder.directory(this.dir);
        }

        builder.redirectErrorStream(redirectErrorStream);

        if (Shell.WINDOWS) {
            synchronized (WindowsProcessLaunchLock) {
                // To workaround the race condition issue with child processes
                // inheriting unintended handles during process launch that can
                // lead to hangs on reading output and error streams, we
                // serialize process creation. More info available at:
                // http://support.microsoft.com/kb/315939
                process = builder.start();
            }
        } else {
            process = builder.start();
        }

        if (timeOutInterval > 0) {
            timeOutTimer     = new Timer("Shell command timeout");
            timeoutTimerTask = new ShellTimeoutTimerTask(this);
            //One time scheduling.
            timeOutTimer.schedule(timeoutTimerTask, timeOutInterval);
        }

        //    CodepageDetectorProxy cpDetector = CodepageDetectorProxy.getInstance();
        //    BufferedInputStream inInputStream = new BufferedInputStream(process.getInputStream());
        //    BufferedInputStream errInputStream = new BufferedInputStream(process.getErrorStream());

        //    Charset inCharset = cpDetector.detectCodepage(inInputStream, 1000);
        //    Charset errCharset = cpDetector.detectCodepage(errInputStream, 1000);

        /*
          sun.jnu.encoding 除了影响读取类名，还会影响传入参数的编码。
          file.encoding    默认编码， 影响读取内容的编码
         */
        String charset = System.getProperty("sun.jnu.encoding", Charset.defaultCharset()
                                                                       .name());
        final BufferedReader errReader = new BufferedReader(new InputStreamReader(process
                .getErrorStream(), charset));
        BufferedReader inReader = new BufferedReader(new InputStreamReader(process
                .getInputStream(), charset));
        final StringBuffer errMsg = new StringBuffer();
        // read error and input streams as this would free up the buffers
        // free the error stream buffer
        Thread errThread = new Thread() {
            @Override
            public void run() {
                try {
                    String line = errReader.readLine();
                    while ((line != null) && !isInterrupted()) {
                        errMsg.append(line);
                        errMsg.append(System.getProperty("line.separator"));
                        if (errMsg.length() > 1000000) {
                            // too large
                            System.err.println("Too large error message, truncate it.");
                            System.err.println(errMsg.toString());
                            errMsg.setLength(0);
                        }
                        line = errReader.readLine();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        };

        try {
            errThread.start();
        } catch (IllegalStateException ignored) {
        }

        try {
            // parse the output
            Thread outThread = new Thread() {
                @Override
                public void run() {

                    try {
                        parseExecResult(inReader);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            outThread.start();

            // wait for the process to finish and check the exit code
            exitCode = process.waitFor();
            // make sure that the error thread exits
            joinThread(errThread);
            joinThread(outThread);
            // clear the input stream buffer
            //String line = inReader.readLine();
            //while (line != null) {
            //    line = inReader.readLine();
            //}
            completed.set(true);
            //the timeout thread handling
            //taken care in finally block
            if (exitCode != 0) {
                throw new ExitCodeException(exitCode, errMsg.toString());
            }
        } catch (InterruptedException ie) {
            throw new IOException(ie.toString());
        } finally {
            if (timeOutTimer != null) {
                timeOutTimer.cancel();
            }
            // close the input stream
            try {
                // JDK 7 tries to automatically drain the input streams for us
                // when the process exits, but since close is not synchronized,
                // it creates a race if we close the stream first and the same
                // fd is recycled.  the stream draining thread will attempt to
                // drain that fd!!  it may block, OOM, or cause bizarre behavior
                // see: https://bugs.openjdk.java.net/browse/JDK-8024521
                //      issue is fixed in build 7u60
                InputStream stdout = process.getInputStream();
                synchronized (stdout) {
                    inReader.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            if (!completed.get()) {
                errThread.interrupt();
                joinThread(errThread);
            }
            try {
                InputStream stderr = process.getErrorStream();
                synchronized (stderr) {
                    errReader.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            process.destroy();
            lastTime = System.currentTimeMillis();
        }
    }

    /**
     * return an array containing the command name & its parameters
     */
    protected abstract String[] getExecString();

    /**
     * Parse the execution result
     */
    protected abstract void parseExecResult(BufferedReader lines)
    throws IOException;

    private static void joinThread(Thread t) {
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                t.interrupt(); // propagate interrupt
            }
        }
    }

    /**
     * Get the environment variable
     */
    public String getEnvironment(String env) {
        return environment.get(env);
    }

    /**
     * get the current sub-process executing the given command
     *
     * @return process executing the command
     */
    public Process getProcess() {
        return process;
    }

    /**
     * get the exit code
     *
     * @return the exit code of the process
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * To check if the passed script to shell command executor timed out or not.
     *
     * @return if the script timed out.
     */
    public boolean isTimedOut() {
        return timedOut.get();
    }

    /**
     * Set if the command has timed out.
     */
    private void setTimedOut() {
        this.timedOut.set(true);
    }

    public enum OSType {
        OS_TYPE_LINUX, OS_TYPE_WIN, OS_TYPE_SOLARIS, OS_TYPE_MAC, OS_TYPE_FREEBSD, OS_TYPE_OTHER
    }

    /**
     * Timer which is used to timeout scripts spawned off by shell.
     */
    private static class ShellTimeoutTimerTask extends TimerTask {

        private Shell shell;

        public ShellTimeoutTimerTask(Shell shell) {
            this.shell = shell;
        }

        @Override
        public void run() {
            Process p = shell.getProcess();
            try {
                p.exitValue();
            } catch (Exception e) {
                //Process has not terminated.
                //So check if it has completed
                //if not just destroy it.
                if (p != null && !shell.completed.get()) {
                    shell.setTimedOut();
                    p.destroy();
                }
            }
        }
    }


}
