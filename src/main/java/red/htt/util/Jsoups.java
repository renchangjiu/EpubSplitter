package red.htt.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import red.htt.R;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author yui
 */
public class Jsoups {
    /**
     * 尝试从HTML中获取封面.
     * 规则: 若在第一个图片标签之前, 没有出现文本, 则认为该图片是封面
     */
    public static Optional<String> getCoverHref(File html) {
        try {
            Document doc = Jsoup.parse(html, StandardCharsets.UTF_8.name());
            Elements elements = doc.body().getAllElements();
            elements.remove(0);
            int imgTagIdx = -1;
            for (int i = 0; i < elements.size(); i++) {
                Element ele = elements.get(i);
                if (Strings.equalsAny(ele.tagName(), R.IMG, R.IMAGE)) {
                    imgTagIdx = i;
                    break;
                }
            }
            if (imgTagIdx == -1) {
                return Optional.empty();
            }
            for (int i = 0; i < imgTagIdx; i++) {
                Element ele = elements.get(i);
                if (Strings.isNotBlank(ele.text())) {
                    return Optional.empty();
                }
            }
            Element ele = elements.get(imgTagIdx);
            if (ele.tagName().equals(R.IMG)) {
                Optional<String> opt = getImageSrc(ele);
                if (opt.isPresent()) {
                    return opt;
                }
            }
            if (ele.tagName().equals(R.IMAGE)) {
                Optional<String> opt = getImageSrc(ele);
                if (opt.isPresent()) {
                    return opt;
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> getImageSrc(Element ele) {
        String src1 = ele.attr(R.SRC);
        if (Strings.isNotBlank(src1)) {
            return Optional.of(src1);
        }
        String src2 = ele.attr(R.XLINK_HREF);
        if (Strings.isNotBlank(src2)) {
            return Optional.of(src2);
        }
        return Optional.empty();
    }

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
