package red.htt.util;

import java.io.File;

/**
 * @author yui
 */
public class Commons {

    public static File getCurrentDir() {
        return new File(System.getProperty("user.dir"));
    }


    /**
     * 是否是绝对路径
     */
    public static boolean isAbsolutePath(String path) {
        return path.startsWith("/") || path.contains(":");
    }

    /**
     * 是否是相对路径
     */
    public static boolean isRelativePath(String path) {
        return !isAbsolutePath(path);
    }
}
