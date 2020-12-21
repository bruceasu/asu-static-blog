package me.asu.blog;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import lombok.Data;

/**
 * @author suk
 */
@Data
public class TagGenerator
{

	Charset srcEncoding = StandardCharsets.UTF_8;
	Charset outEncoding = StandardCharsets.UTF_8;

	public TagGenerator()
	{
	}

	public TagGenerator(Charset srcEncoding, Charset outEncoding)
	{
		if (srcEncoding != null) {
			this.srcEncoding = srcEncoding;
		}
		if (outEncoding != null) {
			this.outEncoding = outEncoding;
		}
	}

	public void generate(BlogContext ctx) throws Exception
	{
		Path tag = ctx.getTag();
		Path postSrc = ctx.getPostSrc();
		Path wikiDir = ctx.getWikiSrc();
		if (!Files.isDirectory(tag.getParent())) {
			Files.createDirectories(tag.getParent());
		}

        List<SrcFileInfo> fileInfoList1 = null;
        if (postSrc != null) {
            GetSrcFileInfoVisitor visitor1 = new GetSrcFileInfoVisitor(srcEncoding, "post");
            Files.walkFileTree(postSrc, visitor1);
            fileInfoList1 = visitor1.getFileInfoList();
		}

        List<SrcFileInfo> fileInfoList2 = null;
		if (wikiDir != null) {
            GetSrcFileInfoVisitor visitor2 = new GetSrcFileInfoVisitor(srcEncoding, "wiki");
            Files.walkFileTree(wikiDir, visitor2);
            fileInfoList2 = visitor2.getFileInfoList();
		}

        List<SrcFileInfo> all = new ArrayList<>();
		if (fileInfoList1 != null && !fileInfoList1.isEmpty()) {
            fileInfoList1 = generateUrl(postSrc, ctx.getPostContextPath(), fileInfoList1);
            all.addAll(fileInfoList1);
        }
        if (fileInfoList2 != null && !fileInfoList2.isEmpty()) {
            fileInfoList2 = generateUrl(wikiDir, ctx.getWikiContextPath(), fileInfoList2);
            all.addAll(fileInfoList2);
        }

        // group by tags
        SortedMap<String, List<SrcFileInfo>> tags = new TreeMap<>();
        Set<Map<String, Object>> tagSet = new HashSet<>();
        all.forEach(i->{
            String[] fileTags = i.getFileTags();
            if (fileTags == null || fileTags.length == 0) {
                return;
            }
            for (String t : fileTags) {
                if (t == null ||t.trim().isEmpty()) {
                    continue;
                }
                String tagLowercase = t.trim().toLowerCase();
                if (!tagSet.contains(tagLowercase)) {
                    Map m = new HashMap();
                    m.put("tag", t.trim());
                    m.put("tag-lowercase", tagLowercase);
                    tagSet.add(m);
                }
                List<SrcFileInfo> list = tags.get(tagLowercase);
                if (list == null) {
                    list = new ArrayList<>();
                    tags.put(tagLowercase, list);
                }
                list.add(i);
            }
        });
        tagSet.forEach(m->{
            String t = (String) m.get("tag-lowercase");
            List<SrcFileInfo> srcFileInfos = tags.get(t);
            srcFileInfos.sort((a, b) ->{
                Date d1 = a.getArticleDate();
                if(d1 == null) {
                    d1 = new Date(a.getLastModified());
                }
                Date d2 = b.getArticleDate();
                if (d2 == null) {
                    d2 = new Date(b.getLastModified());
                }
                return d2.compareTo(d1);
            });
//            List<Map> maps = new ArrayList<>();
//            srcFileInfos.forEach(s-> {
//	            try {
//		            maps.add(BeanUtils.convertBean(s));
//	            } catch (Exception e) {
//		            e.printStackTrace();
//	            }
//            });
            m.put("articles", srcFileInfos);
        });


		Map<Object, Object> value = new HashMap<>();
		value.put("global_public_url", ctx.getBaseUrl());
		value.put("tags",tagSet);

		String content = TemplateHelper.parse("tmpl.tags", value);
		Files.write(tag, content.getBytes(outEncoding));
	}


	private List<SrcFileInfo> generateUrl(Path srcDir, String contextPath, List<SrcFileInfo> fileInfoList)
	{
        if (!fileInfoList.isEmpty()) {
			for (SrcFileInfo info : fileInfoList) {
                Path relativize = srcDir.relativize(info.getPath());
                String s = relativize.toString();
                int i = s.lastIndexOf(".");
                s = s.substring(0, i) + ".html";
                Path destPath = Paths.get(contextPath, s);
                String href = String.format("%s", destPath.toString().replace(File.separator, "/"));
                info.setDestUrl(href);
            }
        }

        return fileInfoList;

	}

}
