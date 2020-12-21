package me.asu.blog;

import java.nio.file.Path;
import lombok.Data;

@Data
public class BlogContext
{

	String baseUrl = "http://localhost:8080";

	String home;

	Path   src;
	Path   assets;

	Path   postSrc;
	Path   postTarget;
	String postContextPath;

	Path   index;
	Path   tag;
	Path   archive;

	Path   wikiSrc;
	Path   wikiTarget;
	Path   wikiIndex;
	String wikiContextPath;

	Path   bookSrc;
	Path   bookDest;

}
