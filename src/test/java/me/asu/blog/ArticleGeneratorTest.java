package me.asu.blog;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class ArticleGeneratorTest {

    static String           globalUrl = "https://bruceasu.github.io/";
    static BlogContext      ctx       = new BlogContext();
    static ArticleGenerator ag;

    static {
        TemplateHelper.init("src/main/templates");
        ctx.setBaseUrl(globalUrl);
        ctx.setPandocPath("D:\\blog\\_scripts\\pandoc.exe");
        ag = new ArticleGenerator(ctx.getPandocPath());
        String blogHome = "D:\\blog\\";
        ctx.setHome(blogHome);
        String src = "_src";
        String assets = "_assets";
        ctx.setSrc(Paths.get(blogHome, src));
        ctx.setAssets(Paths.get(blogHome, assets));

        // posts
        String postCtxPath = "posts";
        Path postSrc = Paths.get(blogHome, src, postCtxPath);
        Path postTarget = Paths.get(blogHome, postCtxPath);
        Path index = Paths.get(blogHome, "index.html");

        ctx.setPostSrc(postSrc);
        ctx.setPostContextPath(postCtxPath);
        ctx.setPostTarget(postTarget);
        ctx.setIndex(index);

        // wiki
        String wikiCtxPath = "wiki";
        Path wikiSrc = Paths.get(blogHome, src, wikiCtxPath);
        Path wikiTarget = Paths.get(blogHome, wikiCtxPath);
        Path wikiIndex = Paths.get(blogHome, wikiCtxPath, "index.html");
        ctx.setWikiSrc(wikiSrc);
        ctx.setWikiTarget(wikiTarget);
        ctx.setWikiContextPath(wikiCtxPath);
        ctx.setWikiIndex(wikiIndex);

        // reprint
        String reprintCtxPath = "reprint";
        Path reprintSrc = Paths.get(blogHome, src, reprintCtxPath);
        Path reprintTarget = Paths.get(blogHome, reprintCtxPath);
        Path reprintIndex = Paths.get(blogHome, reprintCtxPath, "index.html");
        ctx.setReprintSrc(reprintSrc);
        ctx.setReprintTarget(reprintTarget);
        ctx.setReprintContextPath(reprintCtxPath);
        ctx.setReprintIndex(reprintIndex);

        Path archive = Paths.get(blogHome, "archive.htm");
        ctx.setArchive(archive);

        Path tag = Paths.get(blogHome, "tags.html");
        ctx.setTag(tag);

        // books
        String bookCtxPath = "books";
        Path bookSrc = Paths.get(blogHome, src, bookCtxPath);
        Path bookTarget = Paths.get(blogHome, bookCtxPath);
        ctx.setBookSrc(bookSrc);
        ctx.setBookTarget(bookTarget);
    }

    public static void main(String[] args) {

        try {
            generatePosts();
            generateIndex();

            generateWiki();
            generateWikiIndex();

            generateReprint(ag);
            generateReprintIndex();

            generateTags();
            generateArchive();

            copyRes();

            generateBooks();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void copyRes() throws Exception {
        ResourcesCopier copier = new ResourcesCopier();
        copier.copy(ctx.getSrc(), Paths.get(ctx.getHome(), "tmp"), "css|pdf|png|jpg|jpeg|gif|htm|html|webp|bmp|ico");
        copier.copy(ctx.getAssets(), Paths.get(ctx.getHome(), "tmp"), "css|pdf|png|jpg|jpeg|gif|js|json|ttf|htm|html|webp|bmp|ico");
    }

    public static void generatePosts() throws Exception {
        Path input = ctx.getPostSrc();
        Path output = ctx.getPostTarget();
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }
        DirGenerator generator = new DirGenerator(ag);
        generator.generate(input, output, globalUrl);

    }

    public static void generateBooks() throws Exception {
        Path input = Paths.get(ctx.getHome(), "_src", "books");
        Path output = Paths.get(ctx.getHome(), "tmp", "books");
        DirGenerator generator = new DirGenerator(ag);
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }
        if (!Files.isDirectory(output)) {
            Files.createDirectories(output);
        }
        generator.generate(input, output, globalUrl);

    }

    public static void generateIndex() throws Exception {
        IndexGenerator generator = new IndexGenerator();

        generator.generate(ctx);
    }

    public static void generateArchive() throws Exception {
        ArchiveGenerator generator = new ArchiveGenerator();
        generator.generate(ctx);
    }

    public static void generateWiki() throws Exception {
        DirGenerator generator = new DirGenerator(ag);
        Path input = ctx.getWikiSrc();
        Path output = ctx.getWikiTarget();
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }
        generator.generate(input, output, ctx.getBaseUrl());
    }

    public static void generateWikiIndex() throws Exception {
        WikiIndexGenerator generator = new WikiIndexGenerator();
        generator.generate(ctx);
    }

    public static void generateTags() throws Exception {
        TagGenerator generator = new TagGenerator();

        generator.generate(ctx);
    }


    public static void generateReprint(ArticleGenerator ag) throws Exception {
        DirGenerator generator = new DirGenerator(ag);
        Path         input     = ctx.getReprintSrc();
        Path         output    = ctx.getReprintTarget();
        if (!Files.isDirectory(input)) {
            System.err.println(input + " is not a directory");
            return;
        }
        generator.generate(input, output, ctx.getBaseUrl());
    }

    public static void generateReprintIndex() throws Exception {
        ReprintIndexGenerator generator = new ReprintIndexGenerator();
        generator.generate(ctx);
    }
}