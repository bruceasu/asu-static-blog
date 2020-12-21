package me.asu.blog;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

/**
 * @author suk
 */
public class ResourcesCopier
{

	public void copy(Path inputDir, Path outDir, String baseExtension) throws Exception
	{
		if (!Files.isDirectory(outDir)) {
			Files.createDirectories(outDir);
		}
		if (baseExtension == null) {
			baseExtension = "";
		}
		String[] split = baseExtension.trim().toLowerCase().split("\\|");
		HashSet<String> extendsions = new HashSet<>();
		for (String s : split) {
			if (s != null && !s.trim().isEmpty()) {
				extendsions.add(s);
			}
		}

		Files.walkFileTree(inputDir, new SimpleFileVisitor<Path>()
		{


			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
			{
				try {

					String f = file.toString();
					int i = f.lastIndexOf('.');
					if (i == -1) {
						return FileVisitResult.CONTINUE;
					}
					String ext = f.substring(i + 1).toLowerCase();
					if (extendsions.contains(ext)) {
						Path relativize = inputDir.relativize(file);
						Path dest = Paths.get(outDir.toString(), relativize.toString());
						Path parent = dest.getParent();
						if(!Files.isDirectory(parent)) {
							Files.createDirectories(parent);
						}
						if (diff(file, dest)) {
							System.out.printf("复制文件： %s -> %s%n", file, dest);
							Files.copy(file, dest, REPLACE_EXISTING);
						} else {
							System.out.printf("%s 与 %s 相同，不复制。%n", file, dest);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return FileVisitResult.CONTINUE;
			}


		});
	}

	private boolean diff(Path file, Path dest) throws IOException, NoSuchAlgorithmException
	{
		if (!Files.isRegularFile(dest)) {
			return true;
		}

		if (Files.getLastModifiedTime(file).compareTo(Files.getLastModifiedTime(dest)) > 0) {
			return true;
		}

		if (file.toFile().length() != dest.toFile().length()) {
			return true;
		}

		if (checkDigest(file, dest)) { return true; }

		return false;
	}

	private boolean checkDigest(Path file, Path dest) throws NoSuchAlgorithmException, IOException
	{
		MessageDigest md5 = MessageDigest.getInstance("md5");
		byte[] d1 = md5.digest(Files.readAllBytes(file));
		md5.reset();
		byte[] d2 = md5.digest(Files.readAllBytes(dest));
		for (int i = 0; i < d1.length; i++) {
			if (d1[i] != d2[i]) {
				return true;
			}
		}
		return false;
	}


}
