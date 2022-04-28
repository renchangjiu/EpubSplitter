package red.htt;

import org.apache.commons.cli.*;
import org.dom4j.DocumentException;
import red.htt.util.Commons;

import java.io.File;
import java.io.IOException;

/**
 * @author yui
 */
public class App {
    private static final String APP_NAME = "splitter";


    public static void main(String[] args) throws DocumentException, IOException, ParseException {

        Options opts = new Options();

        opts.addOption(Option.builder("f")
                .longOpt("file")
                .numberOfArgs(1)
                .desc("epub file path, required.")
                .build());

        opts.addOption(Option.builder("o")
                .longOpt("outputDir")
                .numberOfArgs(1)
                .desc("specify the output directory, default: directory of original epub")
                .build());

        opts.addOption(Option.builder("c")
                .longOpt("contains1stLevelCatalog")
                .numberOfArgs(0)
                .desc("the new catalog(toc.ncx) contain the first level catalog, default: not contain.")
                .build());

        opts.addOption(Option.builder("lv")
                .longOpt("catalogLevel")
                .numberOfArgs(1)
                .desc("specify which level of catalog to split, like: 1,2,3...etc, default: 1")
                .build());

        opts.addOption(Option.builder("h")
                .longOpt("help")
                .desc("usage")
                .build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(opts, args);
        if (cli.hasOption("h") || !cli.hasOption("f")) {
            String cmdLineSyntax = "java -jar " + APP_NAME + ".jar";
            String header = "";
            String footer = "\nPlease report issues at https://github.com/renchangjiu/EpubSplitter";
            new HelpFormatter().printHelp(cmdLineSyntax, header, opts, footer, true);
            return;
        }
        String path = cli.getOptionValue("f");
        File file = new File(path);
        if (Commons.isRelativePath(path)) {
            file = new File(Commons.getCurrentDir(), path);
        }

        File outputDir = new File(file.getParentFile().getAbsolutePath());

        if (cli.hasOption("o")) {
            String out = cli.getOptionValue("o");
            if (Commons.isRelativePath(out)) {
                outputDir = new File(Commons.getCurrentDir(), out);
            } else {
                outputDir = new File(out);
            }
        }

        boolean contains1stLevelCatalog = cli.hasOption("c");
        int catalogLevel = Integer.parseInt(cli.getOptionValue("lv", "1"));

        Shana app = new Shana(file, outputDir, contains1stLevelCatalog, catalogLevel);
        try {
            app.doo();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }


}
