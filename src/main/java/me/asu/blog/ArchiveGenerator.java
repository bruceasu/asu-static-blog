package me.asu.blog;

import static me.asu.blog.DateUtils.parseDate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.Getter;

/**
 * @author suk
 */
@Data
public class ArchiveGenerator {

    Charset srcEncoding    = StandardCharsets.UTF_8;
    Charset outputEncoding = StandardCharsets.UTF_8;

    public ArchiveGenerator() {
    }

    public ArchiveGenerator(Charset srcEncoding, Charset outputEncoding) {
        if (srcEncoding != null) {
            this.srcEncoding = srcEncoding;
        }
        if (outputEncoding != null) {
            this.outputEncoding = outputEncoding;
        }
    }

    public static void main(String[] args) {
        testOrgRegex();
    }

    private static void testOrgRegex() {
        String dateRegex = "^#\\+DATE:\\s*\\<(.+?)\\>\\s*$";

        List<String> lines = Arrays.asList("#+DATE: <2020-01-01>", "#+DATE: <2020-01-01 05:23>", "#+DATE: <2020-01-01 05:23:44>", "#+DATE: <2020-01-01 05:23:44.123>");
        Pattern pattern = Pattern.compile(dateRegex, Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < lines.size(); i++) {
            String  line = lines.get(i);
            Matcher m    = pattern.matcher(line);
            if (m.matches()) {
                String date  = m.group(1);
                Date   parse = parseDate(date);
                System.out.printf("%s => %s%n", date, parse);
            }
        }


    }

    private static void testMdCommentRegex() {
        String[] s = {
                "[comment]: <> (This is a comment, it will not be included)  ",
                "[comment]: <> (in  the output file unless you use it in)",
                "[comment]: <> (a reference style link.)",
                "[//]: <> (This is also a comment.)",
                "[//]: # (This may be the most platform independent comment)",
                "[//]: # A  "};

        String  regex   = "^\\[(?:comment|//)\\]:\\s*(?:<>|#)\\s*\\(?(.+?)\\)?\\s*$";
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

    public void generate(BlogContext ctx) throws Exception {
        Path archive = ctx.getArchive();
        Path srcDir  = ctx.getPostSrc();
        if (!Files.isDirectory(archive.getParent())) {
            Files.createDirectories(archive.getParent());
        }

        GetSrcFileInfoVisitor visitor = new GetSrcFileInfoVisitor();
        Files.walkFileTree(srcDir, visitor);

        List<SrcFileInfo> fileInfoList = visitor.getFileInfoList();
        String            content      = generateContent(ctx, fileInfoList);

        Files.write(archive, content.getBytes(outputEncoding));
    }

    private String generateContent(BlogContext ctx,
            List<SrcFileInfo> fileInfoList) throws Exception {

        Path   srcDir      = ctx.getPostSrc();
        String contextPath = ctx.getPostContextPath();

        List<Map>        list = new ArrayList<>();
        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd");
        if (!fileInfoList.isEmpty()) {
            for (SrcFileInfo info : fileInfoList) {
                Path   relativize = srcDir.relativize(info.getPath());
                String s          = relativize.toString();
                int    i          = s.lastIndexOf(".");
                s = s.substring(0, i) + ".html";
                Path   destPath    = Paths.get(contextPath, s);
                String href        = destPath.toString()
                                             .replace(File.separator, "/");
                Date   articleDate = info.getArticleDate();
                String date        = null;
                if (articleDate != null) {
                    date = sdf.format(articleDate);
                }
                Map<String, Object> m = new HashMap<>();
                m.put("title", info.getTitle());
                m.put("date", date);
                m.put("url", href);
                m.put("lastModified", info.getLastModified());
                list.add(m);
            }
        }

        Collections.sort(list, (a, b) -> {
            String d1 = (String) a.get("date");
            String d2 = (String) b.get("date");
            if (d1 == null) {
                Date d = new Date((long) a.get("lastModified"));
                d1 = sdf.format(d);
            }
            if (d2 == null) {
                Date d = new Date((long) b.get("lastModified"));
                d2 = sdf.format(d);
            }

            return (-1) * d1.compareTo(d2);
        });

        Map<String, Object> wikiIndex = new HashMap<>();
        wikiIndex.put("title", "Wiki");
        wikiIndex.put("date", "");
        wikiIndex.put("url", "wiki/index.html");
        list.add(wikiIndex);

        Map<String, Object> bookIndex = new HashMap<>();
        bookIndex.put("title", "books");
        bookIndex.put("date", "");
        bookIndex.put("url", "books/index.html");
        list.add(bookIndex);

        Map value = new HashMap();
        value.put("global_public_url", ctx.getBaseUrl());
        value.put("list", list);
        value.put("title", "Archives");

        return TemplateHelper.parse("tmpl.archive", value);
    }

    class GetSrcFileInfoVisitor extends SimpleFileVisitor<Path> {

        @Getter
        List<SrcFileInfo> fileInfoList = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException {
                /*
                Path     path;
                long     lastModified;
                String[] fileTags;
                String   title;
                Date     articleDate;
                String   description;

                String author; // 转发
                String authorWebsite; // 转发来源
                */
            if (file.toString().endsWith(".org")) {

                addOrgFile(file);
            } else if (file.toString().endsWith(".md")) {
                addMarkdownFile(file);
            }

            return FileVisitResult.CONTINUE;
        }

        private void addOrgFile(Path file) throws IOException {
            SrcFileInfo s = new SrcFileInfo();
            s.setPath(file);
            s.setLastModified(file.toFile().lastModified());
            List<String> lines = Files.readAllLines(file, srcEncoding);
                    /*
                    不区分大小写
                    #+TITLE:       TITLE
                    #+DATE:        <2016-05-10 20:00>
                    #+filetags:    linux reprint
                    #+DESCRIPTION: DESCRIPTION
                    #+AUTHOR:      NAME
                    #+AUTHORWEBSITE:  LINK
                     */

            String titleRegex = "^#\\+TITLE:\\s*(.+?)\\s*$";
            s.setTitle(getOrgMeta(titleRegex, lines));

            String dateRegex = "^#\\+DATE:\\s*\\<(.+?)\\>\\s*$";
            String date      = getOrgMeta(dateRegex, lines);
            Date   parse     = parseDate(date);
            s.setArticleDate(parse);

            String filetagsRegex = "^#\\+filetags:\\s*(.+?)\\s*$";
            String filetags      = getOrgMeta(filetagsRegex, lines);
            if (filetags != null) {
                String[] tags = filetags.split("\\s+");
                s.setFileTags(tags);
            }

            String descRegex = "^#\\+DESCRIPTION:\\s*(.+?)\\s*$";
            s.setDescription(getOrgMeta(descRegex, lines));

            String authorRegex = "^#\\+AUTHOR:\\s*(.+?)\\s*$";
            s.setAuthor(getOrgMeta(authorRegex, lines));

            String authorWebSiteRegex = "^#\\+AUTHORWEBSITE:\\s*(.+?)\\s*$";
            s.setAuthorWebsite(getOrgMeta(authorWebSiteRegex, lines));

            fileInfoList.add(s);
        }

        private void addMarkdownFile(Path file) throws IOException {
            SrcFileInfo s = new SrcFileInfo();
            s.setPath(file);
            s.setLastModified(file.toFile().lastModified());

            List<String> lines = Files.readAllLines(file, srcEncoding);

            String titleRegex = "^\\s*TITLE:\\s*(.+?)\\s*$";
            s.setTitle(getMdMeta(titleRegex, lines));

            String dateRegex = "^\\s*DATE:\\s*\\<(.+?)\\>\\s*$";
            String date      = getMdMeta(dateRegex, lines);
            Date   parse     = parseDate(date);
            s.setArticleDate(parse);

            String filetagsRegex = "^\\s*filetags:\\s*(.+?)\\s*$";
            String filetags      = getMdMeta(filetagsRegex, lines);
            if (filetags != null) {
                String[] tags = filetags.split("\\s+");
                s.setFileTags(tags);
            }

            String descRegex = "^\\s*DESCRIPTION:\\s*(.+?)\\s*$";
            s.setDescription(getMdMeta(descRegex, lines));

            String authorRegex = "^\\s*AUTHOR:\\s*(.+?)\\s*$";
            s.setAuthor(getMdMeta(authorRegex, lines));

            String authorWebSiteRegex = "^\\s*AUTHORWEBSITE:\\s*(.+?)\\s*$";
            s.setAuthorWebsite(getMdMeta(authorWebSiteRegex, lines));
            fileInfoList.add(s);
        }

        private String getOrgMeta(String regex, List<String> lines) {
            String  value   = "";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            for (int i = 0; i < lines.size(); i++) {
                String  line = lines.get(i);
                Matcher m    = pattern.matcher(line);
                if (m.matches()) {
                    value = m.group(1);

                    break;
                }
            }
            return value;
        }

        private String getMdMeta(String regex, List<String> lines) {
                /* \\[
                [comment]: <> (This is a comment, it will not be included)
                [comment]: <> (in  the output file unless you use it in)
                [comment]: <> (a reference style link.)
                [//]: <> (This is also a comment.)
                [//]: # (This may be the most platform independent comment)
                [//]: # A
                 */

            String  commentRegex   = "^\\[(?:comment|//)\\]:\\s*(?:<>|#)\\s*\\(?(.+?)\\)?\\s*$";
            Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);

            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            for (String str : lines) {
                Matcher m = commentPattern.matcher(str);
                if (m.matches()) {
                    String  comment = m.group(1);
                    Matcher m2      = pattern.matcher(comment);
                    if (m2.matches()) {
                        return m2.group(1);
                    }
                }
            }

            return "";
        }

    }

}
