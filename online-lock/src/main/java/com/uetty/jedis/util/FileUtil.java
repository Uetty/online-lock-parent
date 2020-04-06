package com.uetty.jedis.util;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings({"ResultOfMethodCallIgnored", "WeakerAccess", "unused"})
public class FileUtil {
	
	/**
	 * 输入流的数据输出到输出流
	 */
	public static void writeFromInputStream(OutputStream os, InputStream is) throws IOException {
		int len;
		byte[] buffer = new byte[1024];
		try {
			while ((len = is.read(buffer)) != -1) {
				os.write(buffer, 0, len);
			}
			os.flush();
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}
	
	public static void writeToFile(File file, String string, boolean append) throws IOException {
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		try (FileOutputStream fis = new FileOutputStream(file, append)) {
			fis.write(string.getBytes());
		}
	}
	
	public static boolean isAbsolutePath (String path) {
		if (path.startsWith("/")) return true;
		if (isWinOS()) {// windows
			return path.contains(":") || path.startsWith("\\");
		} else {// not windows, just unix compatible
			return path.startsWith("~");
		}
	}

	public static String readToString(File file) throws IOException {
		return readToString(file, StandardCharsets.UTF_8.name());
	}

	public static String readToString(File file, String charset) throws IOException {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			return readToString(inputStream, charset);
		}
	}

	public static String readToString(InputStream inputStream) throws IOException {
		return readToString(inputStream, StandardCharsets.UTF_8.name());
	}

	public static String readToString(InputStream inputStream, String charset) throws IOException {
		try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
			StringBuilder writer = new StringBuilder();
			char[] chars = new char[1024];
			int c;
			while ((c = reader.read(chars, 0, chars.length)) != -1) {
				writer.append(chars, 0, c);
			}
			return writer.toString();
		}
	}

	public static List<String> readLines(File file) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return readLines(fis, StandardCharsets.UTF_8.name());
		}
	}

	public static List<String> readLines(File file, String charset) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return readLines(fis, charset);
		}
	}

	public static List<String> readLines(InputStream inputStream) throws IOException {
		return readLines(inputStream, StandardCharsets.UTF_8.name());
	}

	public static List<String> readLines(InputStream inputStream, String charset) throws IOException {
		List<String> list = new ArrayList<>();
		try (InputStreamReader reader = new InputStreamReader(inputStream, charset);
			 BufferedReader br = new BufferedReader(reader)) {

			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
		}
		return list;
	}

	public static void readLineByLine(File file, Consumer<String> consumer) throws IOException {
		readLineByLine(file, StandardCharsets.UTF_8.name(), consumer);
	}

	public static void readLineByLine(File file, String charset, Consumer<String> consumer) throws IOException {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			readLineByLine(inputStream, charset, consumer);
		}
	}

	public static void readLineByLine(InputStream inputStream, Consumer<String> consumer) throws IOException {
		readLineByLine(inputStream, StandardCharsets.UTF_8.name(), consumer);
	}

	public static void readLineByLine(InputStream inputStream, String charset, Consumer<String> consumer) throws IOException {
		try (InputStreamReader reader = new InputStreamReader(inputStream, charset);
			 BufferedReader br = new BufferedReader(reader)) {
			String line;
			while ((line = br.readLine()) != null) {
				consumer.accept(line);
			}
		}
	}

	public static void readCharByChar(File file, int maxLength, Consumer<char[]> consumer) throws IOException {
		readCharByChar(file, StandardCharsets.UTF_8.name(), maxLength, consumer);
	}

	public static void readCharByChar(File file, String charset, int maxLength, Consumer<char[]> consumer) throws IOException {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			readCharByChar(inputStream, charset, maxLength, consumer);
		}
	}

	public static void readCharByChar(InputStream inputStream, int maxLength, Consumer<char[]> consumer) throws IOException {
		readCharByChar(inputStream, StandardCharsets.UTF_8.name(), maxLength, consumer);
	}

	public static void readCharByChar(InputStream inputStream, String charset, int maxLength, Consumer<char[]> consumer) throws IOException {
		if (maxLength <= 0) {
			maxLength = 1024;
		}
		try (InputStreamReader reader = new InputStreamReader(inputStream, charset)) {
			char[] chars = new char[maxLength];
			int c;
			while ((c = reader.read(chars, 0, chars.length)) != -1) {
				char[] cb = new char[c];
				System.arraycopy(chars, 0, cb, 0, c);
				consumer.accept(cb);
			}
		}
	}

	public static void readByteByByte(File file, int maxLength, Consumer<byte[]> consumer) throws IOException {
		try (FileInputStream inputStream = new FileInputStream(file)) {
			readByteByByte(inputStream, maxLength, consumer);
		}
	}

	public static void readByteByByte(InputStream inputStream, int maxLength, Consumer<byte[]> consumer) throws IOException {
		if (maxLength <= 0) {
			maxLength = 1024;
		}
		byte[] collect = new byte[maxLength];
		int c;
		while ((c = inputStream.read(collect, 0, collect.length)) != -1) {
			byte[] bytes = new byte[c];
			System.arraycopy(collect, 0, bytes, 0, c);
			consumer.accept(bytes);
		}
	}

	/**
	 * 是否windows系统
	 */
	public static boolean isWinOS() {
		boolean isWinOS = false;
		try {
			String osName = System.getProperty("os.name").toLowerCase();
			String sharpOsName = osName.replaceAll("windows", "{windows}")
					.replaceAll("^win([^a-z])", "{windows}$1").replaceAll("([^a-z])win([^a-z])", "$1{windows}$2");
			isWinOS = sharpOsName.contains("{windows}");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isWinOS;
	}
	
	public static InputStream openFileInJar(String jarPath, String filePath) throws IOException {
		if (!filePath.startsWith(File.separator)) {
			filePath = File.separator + filePath;
		}
		String urlPath = "jar:file:" + jarPath + "!" + filePath;
		URL url = new URL(urlPath);
		return url.openStream();
	}
	
	public static String getFileNamePrefix(String fileName) {
        if (fileName == null || !fileName.contains(".")) return fileName;
        int i = (fileName = fileName.trim()).lastIndexOf(".");
        if (i <= 0) return fileName;
        return fileName.substring(0, i);
    }

    public static String getFileNameSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) return null;
        int i = (fileName = fileName.trim()).lastIndexOf(".");
        if (i <= 0 && i + 1 >= fileName.length()) return null;
        return fileName.substring(i + 1).toLowerCase();
    }

	public static boolean fileEquals(File file1, File file2) {
		if (file1 == file2) return true;
		if (file1 == null || file2 == null) return false;
		boolean eq = false;
		try {
			eq = file1.getCanonicalFile().equals(file2.getCanonicalFile());
		} catch (Exception ignore) {}
		return eq;
	}

    public static void deleteFiles(File file) {
        deleteFiles0(file, null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
	private static void deleteFiles0(File file, File ignore) {
		if (file == null || !file.exists()) {
			return;
		}
		if (fileEquals(file, ignore)) {
			return;
		}
		try {
			file = file.getCanonicalFile();
		} catch (IOException ignored) {
		}
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
				for (File child : children) {
					deleteFiles0(child, ignore);
				}
			}
        }
        file.delete();
    }

    public static List<File> findFileByName(File root, String fileName) {
		List<File> files = new ArrayList<>();
		if (root == null || !root.exists()) {
			return files;
		}
		if (root.isDirectory()) {
			File[] children = root.listFiles();
			if (children != null) {
				for (File child : children) {
					files.addAll(findFileByName(child, fileName));
				}
			}
		} else {
			if (Objects.equals(fileName, root.getName())) {
				files.add(root);
			}
		}
		return files;
	}

    public static void copyFiles(File sourceFile, File targetFile, boolean override) throws IOException {
        copyFiles0(sourceFile, targetFile, override, targetFile);
    }

    private static void copyFiles0 (File sourceFile, File targetFile, boolean override, File startTargetFile) throws IOException {
		sourceFile = Objects.requireNonNull(sourceFile).getCanonicalFile();
		targetFile = Objects.requireNonNull(targetFile).getCanonicalFile();

		// 忽略源文件里的目标文件目录
		if (fileEquals(sourceFile, startTargetFile)) {
			return;
		}

        if (!sourceFile.isDirectory())  { // 是文件（不是文件夹），直接拷贝
            FileInputStream fis = new FileInputStream(sourceFile);
            copySingleFile(fis, targetFile, override);
            return;
        }

        // 是文件夹
        if (targetFile.getAbsolutePath().startsWith(sourceFile.getAbsolutePath())) {
            // 该种拷贝方式会引起无限循环
            if (sourceFile.getParentFile() == null) {
                // 直接拷贝根目录到同一个盘，这种方式拷贝是明确要禁止的
                throw new IllegalStateException("cannot copy root directory to the same disk");
            }
            // 通过拷贝时，忽略源文件里的目标文件目录，可以避免无限循环的方式
			// noinspection ConstantConditions
			if (targetFile.exists() && targetFile.listFiles() != null && targetFile.listFiles().length > 0) {
                throw new IllegalStateException("source directory cannot contain target directory");
            }
        }

        if (targetFile.exists()) {
            if (targetFile.isFile() && override) { // 已存在的文件不是文件夹，如果是覆盖逻辑，则删除原来的文件
                deleteFiles(targetFile);
				// noinspection ResultOfMethodCallIgnored
				targetFile.mkdirs();
            }
        } else {
			// noinspection ResultOfMethodCallIgnored
			targetFile.mkdirs();
        }

        File[] files = sourceFile.listFiles();
        if (files == null) return;
        for (File child : files) {
            if (fileEquals(child, startTargetFile)) continue;
            String childName = child.getName();
            File targetChild = new File(targetFile, childName);
            copyFiles0(child, targetChild, override, startTargetFile);
        }
    }

    public static void copySingleFile(InputStream sourceInput, File targetFile, boolean override) throws IOException {
        Objects.requireNonNull(sourceInput);
        Objects.requireNonNull(targetFile);

        if (targetFile.exists()) {
            if (override) deleteFiles(targetFile);
            else return;
        } else {
            File parentFile = targetFile.getParentFile();
            if (parentFile != null && !parentFile.exists())
                parentFile.mkdirs();
        }
        targetFile.createNewFile();
        try (InputStream fis = sourceInput;
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = fis.read(bytes)) != -1) {
                fos.write(bytes, 0, len);
            }
        }
    }

    public static void moveFiles(File sourceFile, File targetFile, boolean override) throws IOException {
        copyFiles(sourceFile, targetFile, override);
        deleteFiles0(sourceFile, targetFile);
    }

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		File file = new File("E:\\IdeaProjects");
		List<File> findFiles = findFileByName(file, ".gitignore");
		System.out.println(findFiles);

		System.out.println((System.currentTimeMillis() - start));
	}
}
