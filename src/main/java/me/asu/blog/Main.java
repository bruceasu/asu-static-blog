package me.asu.blog;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Main {

    static BlogContext ctx    = new BlogContext();
    static Config      config = new Config();

    static {
        String templateDir = config.getProperty("templates.dir");
        TemplateHelper.init(templateDir);

        ctx.setPandocPath(config.getProperty("pandoc.path", "pandoc"));
        String globalUrl = config.getProperty("baseUrl", "/");
        ctx.setBaseUrl(globalUrl);

        String blogHome = config.getProperty("home", "blog");
        ctx.setHome(blogHome);
        String baseOutputPath = config.getProperty("baseOutputPath", blogHome);
        ctx.setBaseOutputPath(baseOutputPath);
        ctx.setSrc(Paths.get(config.getProperty("src")));
        ctx.setAssets(Paths.get(config.getProperty("assets")));

        // post
        String postCtxPath = config.getProperty("postContextPath");
        Path postSrc = Paths.get(config.getProperty("postSrc"));
        Path postTarget = Paths.get(config.getProperty("postTarget"));
        Path index = Paths.get(config.getProperty("postIndex"));
        ctx.setPostSrc(postSrc);
        ctx.setPostContextPath(postCtxPath);
        ctx.setPostTarget(postTarget);
        ctx.setIndex(index);

        // wiki
        String wikiCtxPath = config.getProperty("wikiContextPath");
        Path wikiSrc = Paths.get(config.getProperty("wikiSrc"));
        Path wikiTarget = Paths.get(config.getProperty("wikiTarget"));
        Path wikiIndex = Paths.get(config.getProperty("wikiIndex"));
        ctx.setWikiSrc(wikiSrc);
        ctx.setWikiTarget(wikiTarget);
        ctx.setWikiContextPath(wikiCtxPath);
        ctx.setWikiIndex(wikiIndex);

        // reprint
        String reprintCtxPath = config.getProperty("reprintContextPath");
        Path reprintSrc = Paths.get(config.getProperty("reprintSrc"));
        Path reprintTarget = Paths.get(config.getProperty("reprintTarget"));
        Path reprintIndex = Paths.get(config.getProperty("reprintIndex"));
        ctx.setReprintSrc(reprintSrc);
        ctx.setReprintTarget(reprintTarget);
        ctx.setReprintContextPath(reprintCtxPath);
        ctx.setReprintIndex(reprintIndex);

        // archives
        Path archive = Paths.get(config.getProperty("archive"));
        ctx.setArchive(archive);

        Path tag = Paths.get(config.getProperty("tag"));
        ctx.setTag(tag);
        // books
        Path bookSrc = Paths.get(config.getProperty("bookSrc"));
        Path bookTarget = Paths.get(config.getProperty("bookTarget"));
        ctx.setBookSrc(bookSrc);
        ctx.setBookTarget(bookTarget);
    }

    public static void main(String[] args) {
        try {
            ArticleGenerator ag = new ArticleGenerator(ctx.getPandocPath());
            generatePosts(ag);
            generateIndex();

            generateWiki(ag);
            generateWikiIndex();

            generateReprint(ag);
            generateReprintIndex();

            generateTags();

            generateArchive();

            generateBooks(ag);

            copyRes();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generatePosts(ArticleGenerator ag) throws Exception {
        Path input = ctx.getPostSrc();
        Path output = ctx.getPostTarget();
        String baseUrl = ctx.getBaseUrl();
        DirGenerator generator = new DirGenerator(ag);
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }

        generator.generate(input, output, baseUrl);

    }

    public static void generateIndex() throws Exception {
        IndexGenerator generator = new IndexGenerator();
        generator.generate(ctx);
    }

    public static void generateWiki(ArticleGenerator ag) throws Exception {
        Path input = ctx.getWikiSrc();
        Path output = ctx.getWikiTarget();
        DirGenerator generator = new DirGenerator(ag);
        String baseUrl = ctx.getBaseUrl();
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }

        generator.generate(input, output, baseUrl);
    }

    public static void generateWikiIndex() throws Exception {
        WikiIndexGenerator generator = new WikiIndexGenerator();
        generator.generate(ctx);
    }

    public static void generateTags() throws Exception {
        TagGenerator generator = new TagGenerator();

        generator.generate(ctx);
    }

    public static void generateArchive() throws Exception {
        ArchiveGenerator generator = new ArchiveGenerator();
        generator.generate(ctx);
    }

    public static void generateBooks(ArticleGenerator ag) throws Exception {
        DirGenerator generator = new DirGenerator(ag);
        Path input = ctx.getBookSrc();
        Path output = ctx.getBookTarget();
        String baseUrl = ctx.getBaseUrl();
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }
        generator.generate(input, output, baseUrl);
    }

    public static void copyRes() throws Exception {
        ResourcesCopier copier = new ResourcesCopier();
        copier.copy(ctx.getSrc(), Paths.get(ctx.getBaseOutputPath()), "css|pdf|png|jpg|jpeg|gif|htm|html|webp|bmp|ico");
        copier.copy(ctx.getAssets(), Paths.get(ctx.getBaseOutputPath()), "css|pdf|png|jpg|jpeg|gif|js|json|ttf|htm|html|webp|bmp|ico");
    }

    public static void generateReprint(ArticleGenerator ag) throws Exception {
        DirGenerator generator = new DirGenerator(ag);
        Path input = ctx.getReprintSrc();
        Path output = ctx.getReprintTarget();
        String baseUrl = ctx.getBaseUrl();
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }
        generator.generate(input, output, baseUrl);
    }

    public static void generateReprintIndex() throws Exception {
        ReprintIndexGenerator generator = new ReprintIndexGenerator();
        generator.generate(ctx);
    }
}