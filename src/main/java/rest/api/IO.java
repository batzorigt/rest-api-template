package rest.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public interface IO {

    static void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Throwable ignoreMe) {
            // noop
        }
    }

    static String singleLine(String multiLines) {
        return multiLines.replaceAll("[\\t\\n\\r\\s]+", " ");
    }

    static String singleLineFromClassPath(String fileName) {
        return singleLine(multiLineFromClassPath(fileName));
    }

    static String multiLineFromClassPath(String fileName) {
        try (InputStream inputStream = IO.class.getClassLoader().getResourceAsStream(fileName);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int readBytes;
            byte[] buffer = new byte[1024];

            while ((readBytes = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
            }

            return out.toString();
        } catch (Throwable e) {
            return null;
        }
    }

    static String singleLineFromFile(String fileName) {
        return singleLine(multiLineFromFile(fileName));
    }

    static String multiLineFromFile(String fileName) {
        try {
            return Files.readString(new File(fileName).toPath());
        } catch (Throwable e) {
            return null;
        }
    }

}
