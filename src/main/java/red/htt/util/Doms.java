package red.htt.util;

import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.Element;
import red.htt.R;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yui
 */
public class Doms {

    /**
     * 如果两本书没有共用同一 HTML 文件
     *
     * @param navPoint1 navPoint
     * @param navPoint2 navPoint
     */
    public static boolean nonSameHtml(Element navPoint1, Element navPoint2) {
        // 去掉锚点
        List<String> srcList1 = getAllSrc(navPoint1)
                .stream()
                .map(Strings::removeAnchor)
                .collect(Collectors.toList());
        List<String> srcList2 = getAllSrc(navPoint2)
                .stream()
                .map(Strings::removeAnchor)
                .collect(Collectors.toList());

        srcList1.retainAll(srcList2);
        return srcList1.isEmpty();
    }

    /**
     * 如果两本书有共用同一 HTML 文件
     *
     * @param navPoint1 navPoint
     * @param navPoint2 navPoint
     */
    public static boolean hasSameHtml(Element navPoint1, Element navPoint2) {
        return !nonSameHtml(navPoint1, navPoint2);
    }

    /**
     * left: text, right: src.
     */
    public static Pair<String, String> getTextAndSrc(Element pointEle) {
        String text = pointEle.element(R.NAV_LABEL).elementTextTrim(R.TEXT);
        String src = pointEle.element(R.CONTENT).attributeValue(R.SRC);
        return Pair.of(text, src);
    }

    /**
     * 获取 point 所指向文件的实际位置
     */
    public static File getRealFile(File opf, Element point) {
        String href = getTextAndSrc(point).getRight();
        return new File(opf.getParentFile(), Strings.removeAnchor(href));
    }

    private static List<String> getAllSrc(Element pointEle) {
        List<String> res = new ArrayList<>();
        LinkedList<Element> que = new LinkedList<>();
        que.add(pointEle);
        while (!que.isEmpty()) {
            Element pop = que.pop();
            res.add(getTextAndSrc(pop).getRight());
            que.addAll(pop.elements(R.NAV_POINT));
        }
        return res;
    }
}
