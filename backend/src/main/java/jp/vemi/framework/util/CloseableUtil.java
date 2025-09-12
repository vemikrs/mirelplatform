package jp.vemi.framework.util;

import java.io.Closeable;
import java.io.IOException;

import jp.vemi.framework.exeption.MirelSystemException;

public class CloseableUtil {

    /**
     * close.<br/>
     * @param closeable {@link Closeable}の具象
     */
    public static void close(Closeable closeable) {

        if (null == closeable)
            return;

        try {
            closeable.close();
        } catch (IOException e) {
            throw new MirelSystemException(e);
        }

    }
}
