package me.asu.blog;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.Data;

/**
 * @author suk
 */
@Data
public class WikiIndexGenerator
{

    Charset srcEncoding   = StandardCharsets.UTF_8;
    Charset indexEncoding = StandardCharsets.UTF_8;

    public WikiIndexGenerator()
    {
    }

    public WikiIndexGenerator(Charset srcEncoding, Charset indexEncoding)
    {
        if (srcEncoding != null) {
            this.srcEncoding = srcEncoding;
        }
        if (indexEncoding != null) {
            this.indexEncoding = indexEncoding;
        }
    }

    public void generate(BlogContext ctx) throws Exception
    {
        Path index = ctx.getWikiIndex();
        Path srcDir = ctx.getWikiSrc();
        if (!Files.isDirectory(index.getParent())) {
            Files.createDirectories(index.getParent());
        }

        GetSrcFileInfoVisitor visitor = new GetSrcFileInfoVisitor(srcEncoding, "wiki");
        Files.walkFileTree(srcDir, visitor);

        List<SrcFileInfo> fileInfoList = visitor.getFileInfoList();
        fileInfoList.sort((a, b) -> {
            Date d1 = a.getArticleDate();
            Date d2 = b.getArticleDate();
            if (d1 == null) {
                d1 = new Date();
                d1.setTime(a.getLastModified());
            }
            if (d2 == null) {
                d2 = new Date();
                d2.setTime(b.getLastModified());
            }
            return d1.compareTo(d2) * (-1);
        });

        String content = generateContent(ctx, fileInfoList);

        Files.write(index, content.getBytes(indexEncoding));
    }


    private String generateContent(BlogContext ctx, List<SrcFileInfo> fileInfoList) throws Exception
    {
        Path srcDir = ctx.getWikiSrc();
        String destContextPath = ctx.getWikiContextPath();

        List<Map<String, Object>> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if (!fileInfoList.isEmpty()) {
            for (SrcFileInfo info : fileInfoList) {
                Path relativize = srcDir.relativize(info.getPath());
                String s = relativize.toString();
                int i = s.lastIndexOf(".");
                s = s.substring(0, i) + ".html";
                Path destPath = Paths.get(destContextPath, s);
                String href = String.format("%s", destPath.toString().replace(File.separator, "/"));
                Map<String, Object> m = new HashMap<>();
                m.put("url", href);
                m.put("title", info.getTitle());
                m.put("summary", info.getDescription());
                Date date = info.getArticleDate();
                if (date == null) {
                    date = new Date();
                    date.setTime(info.getLastModified());
                }
                m.put("date", sdf.format(date));

                String[] fileTags = info.getFileTags();
                if (fileTags != null && fileTags.length > 0) {
                    List<Map<String, Object>> tags = new ArrayList<>();
                    for (String fileTag : fileTags) {
                        if (fileTag.isEmpty()) {
                            continue;
                        }
                        Map<String, Object> t = new HashMap<>();
                        t.put("tag", fileTag);
                        t.put("tag-lowercase", fileTag.toLowerCase());
                        tags.add(t);
                    }
                    m.put("tags", tags);
                }
                list.add(m);
            }
        }
        Map value = new HashMap();
        value.put("global_public_url", ctx.getBaseUrl());
        value.put("list", list);

        return TemplateHelper.parse("tmpl.index", value);
    }

}
