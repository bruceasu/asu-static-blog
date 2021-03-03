package me.asu.blog;


import java.nio.file.Path;
import java.nio.file.Paths;

class Main
{
	static BlogContext ctx = new BlogContext();
	static Config config = new Config();
	static {
		String templateDir = config.getProperty("templates.dir");
		TemplateHelper.init(templateDir);

		ctx.setPandocPath(config.getProperty("pandoc.path", "pandoc"));
		String globalUrl=  config.getProperty("baseUrl", "https://bruceasu.github.io/");
		ctx.setBaseUrl(globalUrl);

		String blogHome = config.getProperty("home", "blog");
		ctx.setHome(blogHome);
		String baseOutputPath = config.getProperty("baseOutputPath", blogHome);
		ctx.setBaseOutputPath(baseOutputPath);
		ctx.setSrc(Paths.get(config.getProperty("src")));
		ctx.setAssets(Paths.get(config.getProperty("assets")));

		String postCtxPath = config.getProperty("postContextPath");
		Path postSrc = Paths.get(config.getProperty("postSrc"));
		Path postTarget = Paths.get(config.getProperty("postTarget"));
		Path index = Paths.get(config.getProperty("postIndex"));
		ctx.setPostSrc(postSrc);
		ctx.setPostContextPath(postCtxPath);
		ctx.setPostTarget(postTarget);
		ctx.setIndex(index);

		String wikiCtxPath = config.getProperty("wikiContextPath");
		Path wikiSrc = Paths.get(config.getProperty("wikiSrc"));
		Path wikiTarget = Paths.get(config.getProperty("wikiTarget"));
		Path wikiIndex = Paths.get(config.getProperty("wikiIndex"));
		ctx.setWikiSrc(wikiSrc);
		ctx.setWikiTarget(wikiTarget);
		ctx.setWikiContextPath(wikiCtxPath);
		ctx.setWikiIndex(wikiIndex);

		Path archive = Paths.get(config.getProperty("archive"));
		ctx.setArchive(archive);

		Path tag = Paths.get(config.getProperty("tag"));
		ctx.setTag(tag);

		Path bookSrc = Paths.get(config.getProperty("bookSrc"));
		Path bookTarget = Paths.get(config.getProperty("bookTarget"));
		ctx.setBookSrc(bookSrc);
		ctx.setBookTarget(bookTarget);
	}

	public static void main(String[] args)
	{
		try {
			ArticleGenerator ag = new ArticleGenerator(ctx.getPandocPath());
			generatePosts(ag);
			generateIndex();
			generateWiki(ag);
			generateWikiIndex();
			generateTags();
			generateArchive();
			generateBooks(ag);
			copyRes();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void copyRes() throws Exception
	{
		ResourcesCopier copier = new ResourcesCopier();
		copier.copy(ctx.getSrc(), Paths.get(ctx.getBaseOutputPath()), "css|pdf|png|jpg|jpeg|gif|htm|html|webp|bmp|ico");
		copier.copy(ctx.getAssets(), Paths.get(ctx.getBaseOutputPath()), "css|pdf|png|jpg|jpeg|gif|js|json|ttf|htm|html|webp|bmp|ico");
	}

	public static void generatePosts(ArticleGenerator ag) throws Exception
	{
		Path input = ctx.getPostSrc();
		Path output = ctx.getPostTarget();
		DirGenerator generator = new DirGenerator(ag);
		generator.generate(input, output, ctx.getBaseUrl());

	}

	public static void generateBooks(ArticleGenerator ag) throws Exception
	{
		DirGenerator generator = new DirGenerator(ag);
		generator.generate(ctx.getBookSrc(), ctx.getBookTarget(), ctx.getBaseUrl());
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

	public static void generateWiki(ArticleGenerator ag) throws Exception
	{
		DirGenerator generator = new DirGenerator(ag);
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