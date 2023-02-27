package red.htt;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import red.htt.util.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author yui
 */
public class Shana {

    private File epubFile;
    private File outputDir;

    private boolean contains1stLevelCatalog;

    private int catalogLevel;


    private File rootDir;
    /**
     * content.opf
     */
    private File opf;

    /**
     * toc.ncx
     */
    private File ncx;
    private Document opfDoc;
    private Document ncxDoc;
    /**
     * 在 opf 文件中, itemId 到 href 的映射关系
     */
    private Map<String, String> id2href;
    private List<Element> bookPoints;
    private String currBookName;

    /**
     * content.opf
     */
    private File currOpf;

    /**
     * toc.ncx
     */
    private File currNcx;
    private File currRootDir;
    @Nullable
    private File currCover;

    private Document currOpfDoc;
    private Document currNcxDoc;
    private Element currBookPoint;

    /**
     * @param epubFile                epub file full path
     * @param outputDir               指定输出目录
     * @param contains1stLevelCatalog 新的目录是否包含第一级目录
     * @param catalogLevel            指定从第几级目录分割(1,2,3...)
     */
    public Shana(File epubFile, File outputDir, boolean contains1stLevelCatalog, int catalogLevel) {
        this.epubFile = epubFile;
        this.outputDir = outputDir;
        this.contains1stLevelCatalog = contains1stLevelCatalog;
        this.catalogLevel = catalogLevel;
    }

    public void doo() throws IOException, DocumentException {
        if (!this.epubFile.exists()) {
            throw new RuntimeException("epub file not found: " + this.epubFile.getAbsolutePath());
        }
        if (!this.outputDir.exists()) {
            this.outputDir.mkdirs();
            System.out.println("the output directory does not exist, it will be created");
        }
        this.rootDir = extractEpub(this.epubFile, this.outputDir);
        this.opf = findOpf(this.rootDir);
        this.ncx = findNcx(this.opf);
        this.opfDoc = new SAXReader().read(this.opf);
        this.ncxDoc = new SAXReader().read(this.ncx);
        if (!this.opf.getParentFile().equals(this.ncx.getParentFile())) {
            throw new RuntimeException("opf文件与ncx文件不在同一目录下, 需要修改, 但没做.");
        }
        this.id2href = getIdHrefMap(this.opfDoc);
        this.bookPoints = this.getNewBookPoints();
        int i = 0;
        for (Element point : this.bookPoints) {
            this.doOneBook(point);
            System.out.printf("progress:  %d/%d%n%n", ++i, this.bookPoints.size());
        }
        Files.deleteDirectory(this.rootDir);
    }

    /**
     * 根据 toc.ncx, 以及指定的目录层级, 获取新书的目录节点
     */
    private List<Element> getNewBookPoints() {
        System.out.printf("正在解析目录, 指定的目录提取层级为: %d%n", this.catalogLevel);
        List<Element> res = new ArrayList<>();
        List<String> names = new ArrayList<>();
        Element navMap = this.ncxDoc.getRootElement().element(R.NAV_MAP);

        LinkedList<Element> queue = new LinkedList<>(navMap.elements(R.NAV_POINT));
        int level = 0;
        while (!queue.isEmpty() && ++level <= this.catalogLevel) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                Element pop = queue.pop();
                List<Element> list = pop.elements(R.NAV_POINT);
                queue.addAll(list);
                if (list.isEmpty() || level == this.catalogLevel) {
                    res.add(pop);
                    names.add(Doms.getTextAndSrc(pop).getLeft());
                }
            }
        }

        String fmt = "共发现 %d 本: [%s]%n%n";
        System.out.printf(fmt, res.size(), Strings.join(names, ", "));
        return res;
    }


    /**
     * 开始操作
     */
    private void doOneBook(Element ncxPoint) throws IOException, DocumentException {
        this.currBookPoint = ncxPoint;
        // 创建新书的目录, 并复制必要的文件(opf, ncx...etc)
        this.currBookName = getNewBookName(ncxPoint);
        this.currRootDir = new File(this.rootDir.getParentFile(), this.currBookName);

        if (this.currRootDir.exists()) {
            Files.deleteDirectory(this.currRootDir);
        }
        this.currOpf = new File(this.currRootDir, this.opf.getAbsolutePath().replace(this.rootDir.getAbsolutePath(), ""));
        this.currNcx = new File(this.currRootDir, this.ncx.getAbsolutePath().replace(this.rootDir.getAbsolutePath(), ""));
        Files.copyFile(this.opf, this.currOpf);
        Files.copyFile(this.ncx, this.currNcx);

        this.currOpfDoc = new SAXReader().read(this.currOpf);
        this.currNcxDoc = new SAXReader().read(this.currNcx);

        this.currCover = getCoverFile(this.currOpfDoc, this.currOpf).orElse(null);
        String childPath = "META-INF/container.xml";
        File file = new File(this.rootDir, childPath);
        Files.copyFile(file, new File(this.currRootDir, childPath));
        Files.copyFile(new File(this.rootDir, "mimetype"), new File(this.currRootDir, "mimetype"));
        if (this.currCover != null) {
            File coverSrc = new File(this.currCover.getAbsolutePath().replace(this.currRootDir.getAbsolutePath(), this.rootDir.getAbsolutePath()));
            Files.copyFile(coverSrc, this.currCover);
        }

        Element end = this.findLastNavPoint(ncxPoint);
        Set<File> files = this.getUsedTextFiles(ncxPoint, end);
        this.copyFile(files);
        Set<File> files2 = this.getUsedResource();
        this.copyFile(files2);

        this.modifyNcx(ncxPoint);
        this.modifyBookName();
        this.cutExcessHtml();
        this.tryToSetCover(ncxPoint);
        Files.write(this.currOpf, this.currOpfDoc.asXML(), StandardCharsets.UTF_8);
        Files.write(this.currNcx, this.currNcxDoc.asXML(), StandardCharsets.UTF_8);
        this.compress();
        Files.deleteDirectory(this.currRootDir);
    }


    /**
     * 修改ncx文件, 重构目录树
     */
    private void modifyNcx(Element ncxPoint) {
        Element point = (Element) ncxPoint.clone();

        Element root = this.currNcxDoc.getRootElement();
        root.remove(root.element(R.NAV_MAP));
        Element navMap = root.addElement(R.NAV_MAP);

        List<Element> subPoints = point.elements(R.NAV_POINT);
        if (subPoints.isEmpty() || contains1stLevelCatalog) {
            navMap.add(point);
        } else {
            for (Element pt : subPoints) {
                navMap.add((Element) pt.clone());
            }
        }
    }

    /**
     * 压缩新书
     */
    private void compress() throws IOException {
        String filename = this.currBookName;
        // 若节点名有重复的
        for (Element point : bookPoints) {
            String text = Doms.getText(point);
            if (text.equals(Doms.getText(this.currBookPoint)) && !point.equals(this.currBookPoint)) {
                filename = text + String.format("[%d]", this.bookPoints.indexOf(currBookPoint) + 1);
                break;
            }
        }
        File file = new File(this.outputDir, filename + ".epub");
        if (file.exists()) {
            file.delete();
        }
        ZipFile zip = new ZipFile(file);
        ZipParameters param = new ZipParameters();
        param.setIncludeRootFolder(false);
        zip.addFolder(this.currRootDir, param);
        zip.close();
        System.out.println("completed: " + this.currBookName);
    }


    private String getNewBookName(Element elePoint) {
        return Doms.getTextAndSrc(elePoint).getLeft();
    }

    private Element findLastNavPoint(Element element) {
        while (true) {
            List<Element> points = element.elements(R.NAV_POINT);
            if (CollectionUtils.isEmpty(points)) {
                return element;
            } else {
                element = points.get(points.size() - 1);
            }
        }
    }


    /**
     * 获取用到的 html,xhtml 文件, 并修改 ncx 文件的 spine 标签
     */
    private Set<File> getUsedTextFiles(Element start, Element end) {
        Set<File> res = new HashSet<>();
        Element root = this.currOpfDoc.getRootElement();
        Element eleManifest = root.element(R.MANIFEST);

        Pair<String, String> pari1 = Doms.getTextAndSrc(start);
        Pair<String, String> pari2 = Doms.getTextAndSrc(end);
        String idStart = this.findIdByHref(pari1.getRight());
        String idEnd = this.findIdByHref(pari2.getRight());
        Element eleSpine = root.element(R.SPINE);

        // 删掉之前的
        List<Element> itemRefs = eleSpine.elements(R.ITEMREF);
        for (Element ref : itemRefs) {
            String id = ref.attributeValue(R.IDREF);
            if (id.equals(idStart)) {
                break;
            } else {
                eleSpine.remove(ref);
            }
        }
        // 删掉之后的
        itemRefs = eleSpine.elements(R.ITEMREF);
        for (int i = itemRefs.size() - 1; i >= 0; i--) {
            Element ref = itemRefs.get(i);
            String id = ref.attributeValue(R.IDREF);
            if (id.equals(idEnd)) {
                break;
            } else {
                eleSpine.remove(ref);
            }
        }
        itemRefs = eleSpine.elements(R.ITEMREF);
        for (Element ref : itemRefs) {
            String id = ref.attributeValue(R.IDREF);
            String href = this.id2href.get(id);
            res.add(new File(this.currOpf.getParentFile(), href));
        }
        return res;
    }

    /**
     * 修改书名
     */
    private void modifyBookName() {
        Element metadata = this.currOpfDoc.getRootElement().element(R.METADATA);
        Element eleTitle = metadata.element(R.TITLE);
        eleTitle.setText(this.currBookName);
        List<Element> meta = metadata.elements(R.META);
        for (Element ele : meta) {
            if (ele.attributeValue(R.NAME).equals(R.CALIBRE_TITLE_SORT)) {
                ele.addAttribute(R.CONTENT, this.currBookName);
            }
        }
    }



    private static final Pattern[] PTNS = new Pattern[]{
            Pattern.compile("<img.*?src=\"(.*?)\".*?/>"),
            Pattern.compile("<img.*?xlink:href=\"(.*?)\".*?/>"),
            Pattern.compile("<image.*?xlink:href=\"(.*?)\".*?/>"),
            Pattern.compile("<image.*?xlink:href=\"(.*?)\".*?></image>"),
            Pattern.compile("<link.*?href=\"(.*?)\".*?/>"),

    };

    /**
     * 获取用到的图片、样式等资源文件, 并修改 opf 的 manifest 标签
     */
    private Set<File> getUsedResource() throws IOException {
        Set<File> res = new HashSet<>();
        Set<String> fileNames = new HashSet<>();

        Collection<File> htmls = Files.listFiles(this.currRootDir, new String[]{"html", "xhtml"}, true);
        // 文本文件中引用的资源文件
        for (File html : htmls) {
            String con = Files.readFileToString(html, StandardCharsets.UTF_8);
            for (Pattern ptn : PTNS) {
                Matcher mat = ptn.matcher(con);
                while (mat.find()) {
                    File file = new File(html.getParentFile(), mat.group(1));
                    res.add(file);
                    fileNames.add(file.getName());
                }
            }
        }
        // 用到的文本文件不删
        Element root = this.currOpfDoc.getRootElement();
        Element eleSpine = root.element(R.SPINE);
        Set<String> idSet = eleSpine.elements(R.ITEMREF)
                .stream()
                .map(v -> v.attributeValue(R.IDREF))
                .collect(Collectors.toSet());
        idSet.add(eleSpine.attributeValue(R.TOC));

        // 封面不删
        Optional<String> coverId = getCoverId(this.currOpfDoc);
        coverId.ifPresent(idSet::add);

        // 修改xml
        Element eleManifest = root.element(R.MANIFEST);
        List<Element> items = eleManifest.elements(R.ITEM);
        for (Element item : items) {
            String href = item.attributeValue(R.HREF);
            String id = item.attributeValue(R.ID);
            if (fileNames.contains(FilenameUtils.getName(href)) || idSet.contains(id)) {
                continue;
            }
            eleManifest.remove(item);
        }
        return res;
    }


    private String findIdByHref(String href) {
        Set<Map.Entry<String, String>> entSet = this.id2href.entrySet();
        href = Strings.removeAnchor(href);
        for (Map.Entry<String, String> ent : entSet) {
            // TODO: 若opf于ncx不在同一目录下, 可能会有bug
            if (href.equals(ent.getValue())) {
                return ent.getKey();
            }
        }
        throw new RuntimeException("not found, href=" + href);
    }

    private static File extractEpub(File epubFile, File outputDir) throws IOException {
        System.out.println("extracting epub file...");
        ZipFile zip = new ZipFile(epubFile);
        File rootDir = new File(outputDir, epubFile.getName() + "-temp");
        if (rootDir.exists()) {
            Files.deleteDirectory(rootDir);
        }
        zip.extractAll(rootDir.getAbsolutePath());
        System.out.println("extract complete");
        return rootDir;
    }


    /**
     * 从 meta 标签及 item 标签中获取封面的 itemId
     */
    private static Optional<String> getCoverId(Document opfDoc) {
        return opfDoc.getRootElement()
                .element(R.METADATA)
                .elements(R.META)
                .stream()
                .filter(v -> v.attributeValue(R.NAME).equals(R.COVER))
                .map(v -> v.attributeValue(R.CONTENT))
                .findFirst();
    }

    /**
     * 同上
     */
    private static Optional<File> getCoverFile(Document opfDoc, File opf) {
        Optional<String> opt = getCoverId(opfDoc);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        String coverId = opt.get();

        Optional<String> opt2 = opfDoc.getRootElement()
                .element(R.MANIFEST)
                .elements(R.ITEM)
                .stream()
                .filter(v -> v.attributeValue(R.ID).equals(coverId))
                .map(v -> v.attributeValue(R.HREF))
                .findFirst();
        if (opt2.isEmpty()) {
            return Optional.empty();
        }
        String href = opt2.get();
        return Optional.of(new File(opf.getParentFile(), href));
    }

    /**
     * 从 rootDir 复制指定文件到 currRootDir
     */
    private void copyFile(Set<File> files) throws IOException {
        for (File file : files) {
            File src = new File(file.getAbsolutePath().replace(this.currRootDir.getAbsolutePath(), this.rootDir.getAbsolutePath()));
            Files.copyFile(src, file);
        }
    }

    /**
     * 尝试在顶级目录指向的页面中, 找到img标签, 则设置新的封面
     */
    private void tryToSetCover(Element pointEle) throws IOException {
        Pair<String, String> pair = Doms.getTextAndSrc(pointEle);
        File file = new File(this.currNcx.getParentFile(), Strings.removeAnchor(pair.getRight()));
        Optional<String> opt = Jsoups.getCoverHref(file);
        if (opt.isEmpty()) {
            return;
        }

        File image = new File(file.getParentFile(), opt.get());
        // 覆盖原封面
        Files.copyFile(image.getCanonicalFile(), this.currCover);
        System.out.println("reset cover");
    }

    /**
     * 若该书的 HTML 中有不属于该书的内容(即多个子书共用了一个 HTML 文件), 则删掉
     */
    private void cutExcessHtml() {
        int idx = this.bookPoints.indexOf(this.currBookPoint);
        Element curr = this.bookPoints.get(idx);
        Element prev = null;
        Element next = null;

        String anchor = Strings.getAnchor(Doms.getTextAndSrc(curr).getRight());
        File file = Doms.getRealFile(this.currOpf, curr);
        if (idx == 0) {
            // 第一本
            next = this.bookPoints.get(1);
            if (Doms.hasSameHtml(curr, next)) {
                String nextAnchor = Strings.getAnchor(Doms.getTextAndSrc(next).getRight());
                File nextFile = Doms.getRealFile(this.currOpf, next);
                Jsoups.cutNext(nextFile, nextAnchor);
            }
        } else if (idx == this.bookPoints.size() - 1) {
            // 最后一本
            prev = this.bookPoints.get(idx - 1);
            if (Doms.hasSameHtml(curr, prev)) {
                Jsoups.cutPrev(file, anchor);
            }
        } else {
            // 中间的
            prev = this.bookPoints.get(idx - 1);
            next = this.bookPoints.get(idx + 1);
            if (Doms.hasSameHtml(curr, prev)) {
                Jsoups.cutPrev(file, anchor);
            }
            if (Doms.hasSameHtml(curr, next)) {
                String nextAnchor = Strings.getAnchor(Doms.getTextAndSrc(next).getRight());
                File nextFile = Doms.getRealFile(this.currOpf, next);
                Jsoups.cutNext(nextFile, nextAnchor);
            }
        }
    }

    /**
     * 从 container.xml 文件中, 寻找 content.opf
     */
    private static File findOpf(File rootDir) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(new File(rootDir, "META-INF/container.xml"));
        Element root = doc.getRootElement();
        String opfPath = root.element("rootfiles").element("rootfile").attributeValue("full-path");
        File opf = new File(rootDir, opfPath);
        if (!opf.exists()) {
            throw new RuntimeException("file not found: content.opf");
        }
        return opf;
    }

    /**
     * 从 content.opf 中, 寻找 toc.ncx
     */
    private static File findNcx(File opf) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(opf);
        Element root = doc.getRootElement();
        String ncxVal = root.element(R.SPINE).attributeValue(R.TOC);
        Iterator<Element> iter = root.element(R.MANIFEST).elementIterator(R.ITEM);
        while (iter.hasNext()) {
            Element ele = iter.next();
            if (ele.attributeValue(R.ID).equals(ncxVal)) {
                String href = ele.attributeValue(R.HREF);
                File ncx = new File(opf.getParentFile(), href);
                if (ncx.exists()) {
                    return ncx;
                }
            }
        }
        throw new RuntimeException("未发现 toc.ncx 文件");
    }


    /**
     * 返回 id 到 href 的映射关系, href 是相对 opf 文件的相对路径
     */
    public static Map<String, String> getIdHrefMap(Document opfDoc) {
        Element root = opfDoc.getRootElement();
        Element eleManifest = root.element(R.MANIFEST);
        return eleManifest.elements(R.ITEM)
                .stream()
                .collect(Collectors.toMap(v -> v.attributeValue(R.ID), v -> v.attributeValue(R.HREF)));
    }


}
