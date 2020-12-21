package me.asu.blog;

import static me.asu.blog.DateUtils.parseDate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class GetSrcFileInfoVisitor extends SimpleFileVisitor<Path>
    {

        private final Charset srcEncoding;
        private final String project;
        private final List<SrcFileInfo> fileInfoList = new ArrayList<>();

        public GetSrcFileInfoVisitor(Charset srcEncoding, String project)
        {
            this.srcEncoding = srcEncoding;
            this.project = project;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
        {
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

        private void addMarkdownFile(Path file) throws IOException
        {
            SrcFileInfo s = new SrcFileInfo();
            s.setPath(file);
            s.setLastModified(file.toFile().lastModified());

            List<String> lines = Files.readAllLines(file, srcEncoding);

            String titleRegex = "^\\s*TITLE:\\s*(.+?)\\s*$";
            s.setTitle(getMdMeta(titleRegex, lines));

            String dateRegex = "^\\s*DATE:\\s*\\<(.+?)\\>\\s*$";
            String date = getMdMeta(dateRegex, lines);
            Date parse = parseDate(date);
            s.setArticleDate(parse);

            String filetagsRegex = "^\\s*filetags:\\s*(.+?)\\s*$";
            String filetags = getMdMeta(filetagsRegex, lines);
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

        private void addOrgFile(Path file) throws IOException
        {
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
            String date = getOrgMeta(dateRegex, lines);
            Date parse = parseDate(date);
            s.setArticleDate(parse);

            String filetagsRegex = "^#\\+filetags:\\s*(.+?)\\s*$";
            String filetags = getOrgMeta(filetagsRegex, lines);
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

        private String getOrgMeta(String regex, List<String> lines)
        {
            String value = "";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                Matcher m = pattern.matcher(line);
                if (m.matches()) {
                    value = m.group(1);

                    break;
                }
            }
            return value;
        }

        private String getMdMeta(String regex, List<String> lines)
        {
                /* \\[
                [comment]: <> (This is a comment, it will not be included)
                [comment]: <> (in  the output file unless you use it in)
                [comment]: <> (a reference style link.)
                [//]: <> (This is also a comment.)
                [//]: # (This may be the most platform independent comment)
                [//]: # A
                 */

            String commentRegex = "^\\[(?:comment|//)\\]:\\s*(?:<>|#)\\s*\\(?(.+?)\\)?\\s*$";
            Pattern commentPattern = Pattern.compile(commentRegex, Pattern.CASE_INSENSITIVE);

            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            for (String str : lines) {
                Matcher m = commentPattern.matcher(str);
                if (m.matches()) {
                    String comment = m.group(1);
                    Matcher m2 = pattern.matcher(comment);
                    if (m2.matches()) {
                        return m2.group(1);
                    }
                }
            }

            return "";
        }

    }