package me.asu.blog;

import static me.asu.blog.DateUtils.parseDate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.asu.shell.ExitCodeException;
import me.asu.shell.Shell;
import me.asu.shell.ShellCommandExecutor;

public class ArticleGenerator
{
    private Path pandoc = Paths.get("D:\\blog\\_scripts", "pandoc.exe");

    public int generate(Path input, Path output, String globalUrl) throws Exception
    {
        if (!Files.isRegularFile(input)) {
            return 1;
        }
        Path parentPath = output.getParent();
        if (!Files.isDirectory(parentPath)) {
            Files.createDirectories(parentPath);
        }
        String[] runScriptCommand = Shell.getRunScriptCommand(pandoc.toFile());
        Path fileName = input.getFileName();
        boolean isOrgFile = fileName.toString().endsWith(".org");
        //System.out.println("fileName = " + fileName);
        //System.out.println("isOrgFile = " + isOrgFile);
        String[] args = {
                "-f", isOrgFile ? "org" : "markdown",
                "-t", "html",
                "-o", output.toAbsolutePath() + ".tmp",
                input.toAbsolutePath().toString()
                };
        String[] cmds = new String[runScriptCommand.length + args.length];
        System.arraycopy(runScriptCommand, 0, cmds, 0, runScriptCommand.length);
        System.arraycopy(args, 0, cmds, runScriptCommand.length, args.length);
        System.out.printf("准备生成文件， %s => %s%n", input, output);
        System.out.printf("执行命令： %s%n", Arrays.toString(cmds));
        ShellCommandExecutor exec = new ShellCommandExecutor( null, null, 0, cmds);
        try {
            exec.execute();
            Path tmpPath = Paths.get(output.toAbsolutePath() + ".tmp");
            String content = new String(Files.readAllBytes(tmpPath), StandardCharsets.UTF_8);

            SrcFileInfo info = isOrgFile ? getSrcFileInfoForOrg(input)
                    : getSrcFileInfoForMd(input);
            // 模板处理
            Map<String, Object> value=  new HashMap<>();
            value.put("content", content);
            String title = info.getTitle();
            if (title == null || title.isEmpty()) {
                title = fileName.toString();
                int i = title.lastIndexOf('.');
                title=title.substring(0,i);
            }
            value.put("title", title);
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
                value.put("tags", tags);
            }
            Date articleDate = info.getArticleDate();
            if (articleDate == null) {
                articleDate = new Date(info.getLastModified());
            }
            value.put("date", articleDate);
            value.put("global_public_url", globalUrl);

            content = TemplateHelper.parse("tmpl.post", value);
            Files.write(output, content.getBytes(StandardCharsets.UTF_8));
            Files.deleteIfExists(tmpPath);
            return 0;
        } catch (ExitCodeException e) {
            int exitCode =  e.getExitCode();
            System.out.println("exitCode = " + exitCode);
            System.out.println(exec.getOutput());
            System.out.println(e.getMessage());
            return exitCode;
        }
    }

    private SrcFileInfo getSrcFileInfoForOrg(Path file) throws IOException
    {
        SrcFileInfo s = new SrcFileInfo();
        s.setPath(file);
        s.setLastModified(file.toFile().lastModified());
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
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
        return s;
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

    private SrcFileInfo getSrcFileInfoForMd(Path file) throws IOException
    {
        SrcFileInfo s = new SrcFileInfo();
        s.setPath(file);
        s.setLastModified(file.toFile().lastModified());

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

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
        return s;
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
