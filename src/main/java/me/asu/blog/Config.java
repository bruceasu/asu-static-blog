package me.asu.blog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {

    private static final Logger log = Logger.getLogger(Config.class.toString());
    private static Pattern VAR_PATTERN = Pattern.compile("\\$\\{\\w+?}");
    Properties config = new Properties();
    String     name   = "blog.properties";

    public Config() {
        Properties tmp = new Properties();
        // 1. load in classpath / package
        loadFromClasspath(tmp);
        // 2. load files in the same directory
        loadFromAppDir(tmp);
        // 3. load from the java properties
        loadFromJavaProperties(tmp);
        // 替换变量
        replaceVariables();
    }

    private void loadFromClasspath(Properties tmp) {
        try (InputStream in = getClass().getClassLoader()
                                        .getResourceAsStream(name)) {
            tmp.load(in);
            config.putAll(tmp);
            tmp.clear();
        } catch (IOException e) {
            log.warning(e.getMessage());
        }
    }

    private void loadFromAppDir(Properties tmp) {
        String mainPath = OsUtils.getMainPath(getClass());
        Path   p        = Paths.get(mainPath, name);
        if (Files.isRegularFile(p)) {
            try (Reader in = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                tmp.load(in);
                config.putAll(tmp);
                tmp.clear();
            } catch (IOException e) {
                log.warning(e.getMessage());
            }
        }
    }

    private void loadFromJavaProperties(Properties tmp) {
        String property = System.getProperty("config.file");
        if (property != null && !property.trim().isEmpty()) {
            Path p = Paths.get(property);
            if (Files.isRegularFile(p)) {
                try (Reader in = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    tmp.load(in);
                    config.putAll(tmp);
                    tmp.clear();
                } catch (IOException e) {
                    log.warning(e.getMessage());
                }
            }
        }
    }

    private void replaceVariables() {
        LinkedList<String> stack = new LinkedList<>();
        Set<String>        keys  = config.stringPropertyNames();

        keys.forEach(k -> {
            String  v = config.getProperty(k);
            Matcher m = VAR_PATTERN.matcher(v);

            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String group    = m.group();
                String var      = group.substring(2, group.length() - 1);
                String varValue = config.getProperty(var);
                if (varValue == null || varValue.isEmpty()) {
                    m.appendReplacement(sb, "");
                } else {
                    Matcher matcher = VAR_PATTERN.matcher(varValue);
                    if (matcher.find()) {
                        // 依赖的变量也还有变量依赖
                        stack.push(k);
                        continue;
                    } else {
                        m.appendReplacement(sb, varValue);
                    }
                }
            }
            m.appendTail(sb);
            String newValue = sb.toString();
            config.put(k, newValue);
        });
        // FIXME: 如果有环依赖，这里会死循环，无法跳出。
        while (!stack.isEmpty()) {
            String       k  = stack.pop();
            String       v  = config.getProperty(k);
            Matcher      m  = VAR_PATTERN.matcher(v);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String group = m.group();
                String var   = group.substring(2, group.length() - 1);
                if (stack.contains(var)) {
                    // 有环。
                    log.severe("配置文件中变量有依赖环，无法处理， 请检查文件。 " + k + ", " + var);
                    System.exit(1);
                }
                String varValue = config.getProperty(var);
                if (varValue == null || varValue.isEmpty()) {
                    m.appendReplacement(sb, "");
                } else {
                    Matcher matcher = VAR_PATTERN.matcher(varValue);
                    if (matcher.find()) {
                        // 依赖的变量也还有变量依赖
                        stack.add(k);
                        continue;
                    } else {
                        m.appendReplacement(sb, varValue);
                    }
                }
            }
            m.appendTail(sb);
            String newValue = sb.toString();
            config.put(k, newValue);
        }
    }

    public Object setProperty(String key,
            String value) {return config.setProperty(key, value);}

    public void load(Reader reader) throws IOException {config.load(reader);}

    public void load(InputStream inStream)
    throws IOException {config.load(inStream);}

    @Deprecated
    public void save(OutputStream out,
            String comments) {config.save(out, comments);}

    public void store(Writer writer, String comments) throws IOException {
        config.store(writer, comments);
    }

    public void store(OutputStream out, String comments) throws IOException {
        config.store(out, comments);
    }

    public void loadFromXML(InputStream in) throws IOException,
                                                   InvalidPropertiesFormatException {
        config.loadFromXML(in);
    }

    public void storeToXML(OutputStream os, String comment) throws IOException {
        config.storeToXML(os, comment);
    }

    public void storeToXML(OutputStream os, String comment, String encoding)
    throws IOException {config.storeToXML(os, comment, encoding);}

    public String getProperty(String key) {return config.getProperty(key);}

    public String getProperty(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    public Enumeration<?> propertyNames() {return config.propertyNames();}

    public Set<String> stringPropertyNames() {return config.stringPropertyNames();}

    public void list(PrintStream out) {config.list(out);}

    public void list(PrintWriter out) {config.list(out);}

    public int size() {return config.size();}

    public boolean isEmpty() {return config.isEmpty();}

    public Enumeration<Object> keys() {return config.keys();}

    public Enumeration<Object> elements() {return config.elements();}

    public boolean contains(Object value) {return config.contains(value);}

    public boolean containsValue(Object value) {return config.containsValue(value);}

    public boolean containsKey(Object key) {return config.containsKey(key);}

    public Object get(Object key) {return config.get(key);}

    public Object put(Object key, Object value) {return config.put(key, value);}

    public Object remove(Object key) {return config.remove(key);}

    public void putAll(Map<?, ?> t) {config.putAll(t);}

    public void clear() {config.clear();}

    public Set<Object> keySet() {return config.keySet();}

    public Set<Entry<Object, Object>> entrySet() {return config.entrySet();}

    public Collection<Object> values() {return config.values();}

    public Object getOrDefault(Object key, Object defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public void forEach(BiConsumer<? super Object, ? super Object> action) {
        config.forEach(action);
    }

    public void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
        config.replaceAll(function);
    }

    public Object putIfAbsent(Object key,
            Object value) {return config.putIfAbsent(key, value);}

    public boolean remove(Object key,
            Object value) {return config.remove(key, value);}

    public boolean replace(Object key, Object oldValue, Object newValue) {
        return config.replace(key, oldValue, newValue);
    }

    public Object replace(Object key,
            Object value) {return config.replace(key, value);}

    public Object computeIfAbsent(Object key,
            Function<? super Object, ?> mappingFunction) {return config.computeIfAbsent(key, mappingFunction);}

    public Object computeIfPresent(Object key,
            BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return config.computeIfPresent(key, remappingFunction);
    }

    public Object compute(Object key,
            BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return config.compute(key, remappingFunction);
    }

    public Object merge(Object key,
            Object value,
            BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return config.merge(key, value, remappingFunction);
    }
}
