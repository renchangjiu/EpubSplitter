package red.htt.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author yui
 */
public class Jsoups {
    public static void cutPrev(File html, String tagId) {
        cut(html, tagId, true);
    }

    public static void cutNext(File html, String tagId) {
        cut(html, tagId, false);
    }

    /**
     * if prev == true:
     * 截掉 body 至 tagId 之间的所有内容
     * <p>
     * if prev == false:
     * 截掉 tagId 至 /body 之间的所有内容
     * <p>
     */
    private static void cut(File htmlFile, String tagId, boolean prev) {
        try {
            Document doc = Jsoup.parse(htmlFile, StandardCharsets.UTF_8.name());

            Element body = doc.body();
            Element target = doc.getElementById(tagId);
            if (target == null) {
                return;
            }
            Element temp = target;

            while (true) {
                temp = temp.parent();
                if (temp.equals(body)) {
                    break;
                }
                target = target.parent();
            }

            Elements elements = body.children();
            if (prev) {
                for (Element ele : elements) {
                    if (target.equals(ele)) {
                        break;
                    }
                    ele.remove();
                }
            } else {
                int size = elements.size();
                for (int i = size - 1; i >= 0; i--) {
                    Element ele = elements.get(i);
                    if (target.equals(ele)) {
                        ele.remove();
                        break;
                    }
                    ele.remove();
                }
            }
            Files.write(htmlFile, doc.html(), StandardCharsets.UTF_8);
            System.out.println("cut excess html file tag: " + htmlFile.getName());
        } catch (Exception e) {
            System.out.println("cutExcessHtml error:");
            System.out.println("\t" + e.getMessage());
        }
    }
}
