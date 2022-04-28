package red.htt.util;


import org.apache.commons.lang3.StringUtils;

/**
 * @author yui
 */
public final class Strings extends StringUtils {
    public static String removeAnchor(String href) {
        return href.replaceAll("(.*)#.*", "$1");
    }

    /**
     * 获取 href 中, #号以后的字符(anchor)
     */
    public static String getAnchor(String href) {
        return href.replaceAll(".*#(.*)", "$1");
    }
}
