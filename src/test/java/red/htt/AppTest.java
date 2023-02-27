package red.htt;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Test
    public void yui() throws Exception {
        String[] args = {
                // "-f", "D:/download/李清照集笺注 - 李清照 - 上海古籍出版社 - 中国古典文学丛书.epub",
                "-f", "D:/OneDrive/Books/1未整理/汗青堂系列精选集（套装共19册。） - 斯文·贝克特 & 丹尼尔·比尔 & 凯尔·哈珀 & 蒂姆·克莱顿 & 等 - 后浪出版咨询（北京）有限责任公司 - 汗青堂.epub",
                "-o", "D:/download/1"
        };
        App.main(args);
    }


}
