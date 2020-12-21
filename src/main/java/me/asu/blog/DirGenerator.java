package me.asu.blog;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author suk
 */
public class DirGenerator
{

    ArticleGenerator generator = new ArticleGenerator();

    public void generate(Path inputDir, Path outDir, String globalUrl) throws Exception
    {
        if (!Files.isDirectory(outDir)) {
            Files.createDirectories(outDir);
        }

        Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>()
        {


            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException
            {
                try {
                    if (file.toString().endsWith(".org")) {
                        String s = file.getFileName().toString();
                        s = s.substring(0, s.length() - 3);
                        Path dest = getDestPath(file, s, inputDir, outDir);
                        if (!checkModified(file, dest)) {
                            return FileVisitResult.CONTINUE;
                        }
                        generator.generate(file, dest, globalUrl);
                    } else if (file.toString().endsWith(".md")) {
                        String s = file.getFileName().toString();
                        s = s.substring(0, s.length() - 2);
                        Path dest = getDestPath(file, s, inputDir, outDir);
                        if (!checkModified(file, dest)) {
                            return FileVisitResult.CONTINUE;
                        }
                        generator.generate(file, dest, globalUrl);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return FileVisitResult.CONTINUE;
            }


        });
    }

    private boolean checkModified(Path file, Path dest)
    {
        if (Files.isRegularFile(dest)) {
            long dl = dest.toFile().lastModified();
            long sl = file.toFile().lastModified();
            if (dl > sl) {
                System.out.printf("源文件(%s)没有修改，不生成新文件。%n", file);
                return false;
            }
        }
        return true;
    }

    private Path getDestPath(Path file, String s, Path inputDir, Path outDir)
    {
        Path path = inputDir.relativize(file.getParent());
        return Paths.get(outDir.toString(), path.toString(), s + "html");
    }
}
