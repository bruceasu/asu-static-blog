package me.asu.blog;


import java.nio.file.Path;
import java.nio.file.Paths;

class ArticleGeneratorTest
{
    static String globalUrl = "https://bruceasu.github.io/";
    static BlogContext ctx = new BlogContext();
    static {
        ctx.setBaseUrl(globalUrl);

        String blogHome = "D:\\blog\\";
        ctx.setHome(blogHome);
        String src = "_src";
        String assets = "_assets";
        ctx.setSrc(Paths.get(blogHome, src));
        ctx.setAssets(Paths.get(blogHome, assets));

        String postCtxPath = "posts";
        Path postSrc = Paths.get(blogHome,src, postCtxPath);
        Path postTarget = Paths.get(blogHome, postCtxPath);
        Path index = Paths.get(blogHome, "index.html");

        ctx.setPostSrc(postSrc);
        ctx.setPostContextPath(postCtxPath);
        ctx.setPostTarget(postTarget);
        ctx.setIndex(index);

        String wikiCtxPath = "wiki";
        Path wikiSrc = Paths.get(blogHome, src, wikiCtxPath);
        Path wikiTarget = Paths.get(blogHome, wikiCtxPath);
        Path wikiIndex = Paths.get(blogHome, wikiCtxPath, "index.html");
        ctx.setWikiSrc(wikiSrc);
        ctx.setWikiTarget(wikiTarget);
        ctx.setWikiContextPath(wikiCtxPath);
        ctx.setWikiIndex(wikiIndex);

        Path archive = Paths.get(blogHome, "archive.htm");
        ctx.setArchive(archive);

        Path tag = Paths.get(blogHome, "tags.html");
        ctx.setTag(tag);
    }

    public static void main(String[] args)
    {
        try {
            generatePosts();
            generateIndex();
            generateWiki();
            generateWikiIndex();
            generateTags();
            generateArchive();

            copyRes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generate() throws Exception
    {
        Path input = Paths.get("D:\\blog\\_scripts\\a.org");
        Path output = Paths.get("D:\\blog\\_scripts\\a.htm");
        ArticleGenerator generator = new ArticleGenerator();

        int generate = generator.generate(input, output, globalUrl);
        System.out.println("generate = " + generate);

    }

    public static void copyRes() throws Exception
    {
        ResourcesCopier copier = new ResourcesCopier();
        copier.copy(ctx.getSrc(), Paths.get(ctx.getHome(), "tmp"), "css|pdf|png|jpg|jpeg|gif|htm|html|webp|bmp|ico");
        copier.copy(ctx.getAssets(), Paths.get(ctx.getHome(), "tmp"), "css|pdf|png|jpg|jpeg|gif|js|json|ttf|htm|html|webp|bmp|ico");
    }
    public static void generatePosts() throws Exception
    {
        Path input = ctx.getPostSrc();
        Path output = ctx.getPostTarget();
        DirGenerator generator = new DirGenerator();
        generator.generate(input, output, globalUrl);

    }

    public static void generateIndex() throws Exception
    {
        IndexGenerator generator = new IndexGenerator();

        generator.generate(ctx);
    }
    public static void generateArchive() throws Exception
    {
        ArchiveGenerator generator = new ArchiveGenerator();
        generator.generate(ctx);
    }

    public static void generateWiki() throws Exception
    {
        DirGenerator generator = new DirGenerator();
        Path input = ctx.getWikiSrc();
        Path output = ctx.getWikiTarget();
        generator.generate(input, output, ctx.getBaseUrl());
    }
    public static void generateWikiIndex() throws Exception
    {
        WikiIndexGenerator generator = new WikiIndexGenerator();
        generator.generate(ctx);
    }
    public static void generateTags() throws Exception
    {
        TagGenerator generator = new TagGenerator();


        generator.generate(ctx);
    }
}