package me.asu.shell;

import java.io.BufferedReader;
import java.io.IOException;

public interface BufferedReaderHandler {

    /**
     * 处理输入信息。
     *
     * @param reader {@link BufferedReader}
     * @throws IOException 读取错误。
     */
    default void handle(BufferedReader reader) throws IOException {
        String line;
        while (null != (line = reader.readLine())) {
            System.out.println(line);
        }
    }
}