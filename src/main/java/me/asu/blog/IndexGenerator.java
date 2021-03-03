package me.asu.blog;

import static me.asu.blog.DateUtils.parseDate;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;

/**
 * @author suk
 */
@Data
public class IndexGenerator
{

    Charset srcEncoding   = StandardCharsets.UTF_8;
    Charset indexEncoding = StandardCharsets.UTF_8;

    public IndexGenerator()
    {
    }

    public IndexGenerator(Charset srcEncoding, Charset indexEncoding)
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
        Path index = ctx.getIndex();
        Path srcDir = ctx.getPostSrc();
        if (!Files.isDirectory(index.getParent())) {
            Files.createDirectories(index.getParent());
        }

        GetSrcFileInfoVisitor visitor = new GetSrcFileInfoVisitor(srcEncoding, "posts");
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
        if (fileInfoList.size() > 20) {
            fileInfoList = fileInfoList.subList(0, 20);
        }
        String content = generateContent(ctx, fileInfoList);

        Files.write(index, content.getBytes(indexEncoding));
    }


    private String generateContent(BlogContext ctx, List<SrcFileInfo> fileInfoList) throws Exception
    {
        Path srcDir = ctx.getPostSrc();
        String destContextPath = ctx.getPostContextPath();

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






    public static void main(String[] args)
    {
        testOrgRegex();
    }

    private static void testOrgRegex()
    {
        String dateRegex = "^#\\+DATE:\\s*\\<(.+?)\\>\\s*$";

        List<String> lines = Arrays.asList("#+DATE: <2020-01-01>", "#+DATE: <2020-01-01 05:23>",
                "#+DATE: <2020-01-01 05:23:44>", "#+DATE: <2020-01-01 05:23:44.123>");
        Pattern pattern = Pattern.compile(dateRegex, Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                String date = m.group(1);
                Date parse = parseDate(date);
                System.out.printf("%s => %s%n", date, parse);
            }
        }


    }

    private static void testMdCommentRegex()
    {
        String[] s = {"[comment]: <> (This is a comment, it will not be included)  ",
                      "[comment]: <> (in  the output file unless you use it in)",
                      "[comment]: <> (a reference style link.)",
                      "[//]: <> (This is also a comment.)",
                      "[//]: # (This may be the most platform independent comment)", "[//]: # A  "};

        String regex = "^\\[(?:comment|//)\\]:\\s*(?:<>|#)\\s*\\(?(.+?)\\)?\\s*$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        for (String str : s) {
            Matcher m = pattern.matcher(str);
            if (m.matches()) {
                System.out.printf("%s is matched. %n", str);
                String group = m.group(1);
                System.out.println("group = " + group);
            } else {
                System.out.printf("%s is not matched. %n", str);
            }
        }
    }

}
