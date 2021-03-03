package me.asu.blog;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.logging.Logger;
import sun.security.action.GetPropertyAction;

public class OsUtils {
    private static final Logger log   = Logger.getLogger(OsUtils.class.toString());
    private static final String LINUX = "Linux";
    private static final String WINDOWS = "Windows";

    public OsUtils() {
    }

    public static boolean isWindows() {
        PrivilegedAction pa = new GetPropertyAction("os.name");
        String osname = (String)AccessController.doPrivileged(pa);
        return osname.startsWith("Windows");
    }

    public static boolean isLinux() {
        PrivilegedAction pa = new GetPropertyAction("os.name");
        String osname = (String)AccessController.doPrivileged(pa);
        return "Linux".equals(osname);
    }

    public static void main(String[] args) {
        System.out.println(getMainPath(OsUtils.class));
        simpleLogTrace();
    }

    public static void simpleLogTrace() {
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stacks = (new Throwable()).getStackTrace();
        int stacksLen = stacks.length;
        StackTraceElement stack = stacks[1];
        String fileName = stack.getFileName();
        String className = stack.getClassName();
        String methodName = stack.getMethodName();
        int lineNumber = stack.getLineNumber();
        sb.append(className).append(".").append(methodName).append(" in ").append(fileName).append(":").append(lineNumber);
        String msg = sb.toString();
        log.info(msg);
    }

    public static void logCallerInfo(Object... parameters) {
        Throwable throwable = new Throwable();
        StackTraceElement el = throwable.getStackTrace()[1];
        String className = el.getClassName();
        String method = el.getMethodName();
        String fileName = el.getFileName();
        int lineNumber = el.getLineNumber();
        String msg = String.format("調用 [%s:%d]", fileName, lineNumber);
        log.info(msg);
        StringBuilder builder = new StringBuilder(String.format("%s.%s(", className, method));
        if (parameters != null && parameters.length > 0) {
            builder.append("\n");

            for(int i = 0; i < parameters.length; ++i) {
                Object parameter = parameters[i];
                builder.append(String.format("\t參數 %d： %s%n", i, parameter));
            }
        }

        builder.append(")");
        log.info(builder.toString());
    }

    public static String getMainPath(Class clz) {
        URL url = clz.getProtectionDomain().getCodeSource().getLocation();
        String filePath = null;
        if (url.toString().startsWith("jar:file:")) {
            try {
                filePath = URLDecoder.decode(url.getPath(), "utf-8");
                log.config("decoded filePath = " + filePath);
            } catch (Exception var5) {
                var5.printStackTrace();
            }

            filePath = filePath.substring(5, filePath.indexOf("!"));
            log.config("filePath = " + filePath);
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        } else {
            try {
                filePath = URLDecoder.decode(url.getPath(), "utf-8");
            } catch (Exception var4) {
                var4.printStackTrace();
            }
            log.config("filePath = " + filePath);
            if (filePath.endsWith(".jar")) {
                filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            }
        }

        log.config("filePath = " + filePath);
        File file = new File(filePath);
        filePath = file.getAbsolutePath();
        return filePath;
    }
}
