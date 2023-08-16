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

    static String toSingleLine(String multiLines) {
        return multiLines.replaceAll("[\\t\\n\\r\\s]+", " ");
    }

    static String readAsSingleLineFromClassPath(String fileName) {
        return toSingleLine(readFromClassPath(fileName));
    }

    static String readFromClassPath(String fileName) {
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

    static String readAsSingleLineFromFileSystem(String fileName) {
        return toSingleLine(readFromFileSystem(fileName));
    }

    static String readFromFileSystem(String fileName) {
        try {
            return Files.readString(new File(fileName).toPath());
        } catch (Throwable e) {
            return null;
        }
    }

}
