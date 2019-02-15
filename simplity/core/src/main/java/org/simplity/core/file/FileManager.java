/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.core.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.AppConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File has a simple connotation to most programmers : it is available on the
 * disk. if you write to it, you should be able to read it later, even across
 * layers. file manager will try to provide that view
 *
 * @author simplity.org
 */
public class FileManager {
	private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	private static final int MAX_TRY = 1000;
	private static final String MSG = "error while writing to a temp file";
	private static final int BUFFER_SIZE = 8 * 1024;
	private static final String WEB_TEMP = "javax.servlet.context.tempdir";
	private static final String TEMP = "java.io.tmpdir";
	/**
	 * to be set by boot-strapper as the root folder. All references to "file"
	 * in this application are assumed to be relative to this folder.
	 */
	private static final char FOLDER_CHAR = '/';

	private static final String FOLDER_STR = "/";
	private static final long MAX_SIZE = 10 * 1024 * 1024;

	private static ServletContext myContext = null;

	private static File tempFolder = new File(System.getProperty(TEMP));

	/**
	 * @param ctx
	 *            servlet context
	 */
	public static void setContext(ServletContext ctx) {
		myContext = ctx;
		tempFolder = (File) ctx.getAttribute(WEB_TEMP);
	}

	/**
	 * get a collection suitable for for-in loop.
	 *
	 * <p>
	 * e.g (for File file : FileManager.getCollection(folder, extension){}
	 *
	 * @param parentFolder
	 * @return collection of file names prefixed with the parent folder. never
	 *         null. will be an empty collection in case of any error. (like
	 *         non-existing folder)
	 */
	public static String[] getResources(String parentFolder) {
		String[] empty = new String[0];
		List<String> resources = new ArrayList<String>();
		if (new File(parentFolder).exists()) {
			addAllResourcesWithFs(parentFolder, resources);
		}
		return resources.toArray(empty);
	}

	@SuppressWarnings("unused")
	private static void addAllResourcesWithCtx(String parentFolder, List<String> resources) {
		Set<String> paths = myContext.getResourcePaths(FOLDER_CHAR + parentFolder);
		if (paths == null) {
			return;
		}
		for (String path : paths) {
			if (path.endsWith(FOLDER_STR)) {
				addAllResourcesWithCtx(path, resources);
			} else {
				resources.add(path);
			}
		}
	}

	private static void addAllResourcesWithFs(String folderName, List<String> resources) {
		String parentFolder = folderName;
		if (parentFolder.endsWith("/") == false) {
			parentFolder += '/';
		}
		File file = new File(parentFolder);
		if (file.exists() == false) {

			logger.info("Unusual that " + parentFolder + " is not a valid path.");

			return;
		}
		String[] files = file.list();
		if (files == null) {
			return;
		}
		/*
		 * files has only the simple name. Caller wants path relative to the
		 * parent
		 */
		for (String path : files) {
			path = parentFolder + path;
			File childFile = new File(path);
			if (childFile.isDirectory()) {
				addAllResourcesWithFs(path + FOLDER_CHAR, resources);
			} else {
				resources.add(path);
			}
		}
	}

	/**
	 * get input stream for a resource. Typically used by XML util to create a
	 * document
	 *
	 * @param fileName
	 *            file name relative to application root to be read
	 * @return stream, or null if resource could not be located
	 * @throws Exception
	 *             in case of any issue while reading this file
	 */
	public static InputStream getResourceStream(String fileName) throws Exception {
		File file = new File(fileName);
		if (file.exists()) {
			return new FileInputStream(file);
		}

		logger.info("file " + fileName + " does not exist.");

		return null;
	}

	/**
	 * create an empty temp file
	 *
	 * @return file, never null.
	 */
	public static File createTempFile() {
		File file = getNewFile();
		try {
			file.createNewFile();

			logger.info("Creating and returning an empty file");

			return file;
		} catch (IOException e) {
			throw new ApplicationError(e, MSG);
		}
	}

	/**
	 * create a temp file using the bytes in the stream. We DO NOT close
	 * inStream, based on the tradition creator-should-close after reading
	 *
	 * @param inStream
	 *            from which to create the content of the file.
	 * @return file, in which this inStream is saved. file.getName() is unique,
	 *         and can be used for next readFile() calls
	 */
	public static File createTempFile(InputStream inStream) {
		File file = getNewFile();

		OutputStream outStream = null;
		try {
			outStream = new FileOutputStream(file);
			copyOut(inStream, outStream);
			return file;
		} catch (Exception e) {
			throw new ApplicationError(e, MSG);
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * create a temp file using the reader. reader is not closed, simply because
	 * we did not create it
	 *
	 * @param reader
	 *            from which to create the content of the file.
	 * @return file, in which this reader is saved. file.getName() is unique,
	 *         and can be used for next readFile() calls
	 */
	public static File createTempFile(Reader reader) {
		File file = getNewFile();

		Writer writer = null;
		try {
			writer = new FileWriter(file);
			for (int ch = reader.read(); ch != -1; ch = reader.read()) {
				writer.write(ch);
			}
			return file;
		} catch (Exception e) {
			throw new ApplicationError(e, MSG);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	private static File getNewFile() {
		long nano = System.nanoTime();
		String fileName = "" + nano;
		File file = new File(tempFolder, fileName);
		if (file.exists() == false) {
			return file;
		}

		logger.info("Rare condition of a file-name clash for temp file " + fileName + " going to try suffixing..");

		/*
		 * increase the digits and start incrementing. Only way this can fail is
		 * when MAX_TRY processes simultaneously use this same technique to
		 * create temp file at this same nano-second!!!
		 */
		nano *= MAX_TRY;
		for (int i = 0; i < MAX_TRY; i++) {
			fileName = "" + ++nano;
			file = new File(tempFolder, fileName);
			if (file.exists() == false) {
				return file;
			}
		}
		throw new ApplicationError("Unable to create a temp file even after " + MAX_TRY);
	}

	/**
	 * get an output stream for this temp file.
	 *
	 * @param fileName
	 *            file name that was returned from an earlier call to
	 *            createTempFile()
	 * @return stream, or null if file not found.
	 */
	public static File getTempFile(String fileName) {
		File file = new File(tempFolder, fileName);
		if (file.exists()) {
			return file;
		}

		logger.info("Non-existing temp file " + fileName + " requested. returning null");

		return null;
	}

	/**
	 * tray and delete a temp file with the given name. No error is raised even
	 * if the delete operation fails.
	 *
	 * @param fileName
	 *            to be deleted.
	 */
	public static void deleteTempFile(String fileName) {
		try {
			File file = new File(tempFolder, fileName);
			if (file.exists()) {
				file.delete();
			}
		} catch (Exception ignore) {
			//
		}
	}

	/**
	 * creates a file, (or re-writes its contents) from an input stream. Use nio
	 * package instead of this utility if you use java 7 or later.
	 *
	 * @param file
	 *            File is created if required. contents over-ridden if file
	 *            exists.
	 * @param inputStream
	 *            from which to read. Should not have been opened. we will open
	 *            and close it.
	 * @return true if all okay. False otherwise. we do do not tell you what
	 *         happened, but write that to trace.
	 */
	public static boolean streamToFile(File file, InputStream inputStream) {
		OutputStream out = null;
		try {
			out = new FileOutputStream(file);
			copyOut(inputStream, out);
			return true;
		} catch (Exception e) {

			logger.error("Error while writing to file " + file.getAbsolutePath(), e);

			return false;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * copy bytes one stream to the other. Note that we do not close the
	 * streams, because we follow the golden rule that open-close should happen
	 * in the same method.
	 *
	 * @param in
	 *            input stream to copy from
	 * @param out
	 *            output stream to copy to
	 * @throws IOException
	 *             in case of any io error during copy
	 */
	public static void copyOut(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
			out.write(buffer, 0, n);
		}
	}

	/**
	 * if the file is small, why the fuss. Just get it as string as UTF-8
	 *
	 * @param file
	 * @return file content as UTF-8 text
	 */
	public static String readFile(File file) {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			long len = file.length();
			if (len > MAX_SIZE) {
				throw new IOException("File " + file.getAbsolutePath() + " too large, was " + len + " bytes.");
			}
			byte[] bytes = new byte[(int) len];
			is.read(bytes);
			return new String(bytes, AppConventions.CHAR_ENCODING);
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while reading file " + file.getAbsolutePath());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * if the file is small, why the fuss. Just get it as string as UTF-8
	 *
	 * @param file
	 * @param text
	 *            file content to be written as UTF-8
	 */
	public static void writeFile(File file, String text) {
		OutputStream os = null;
		try {
			byte[] bytes = text.getBytes(AppConventions.CHAR_ENCODING);
			os = new FileOutputStream(file);
			os.write(bytes);
			os.flush();
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while reading file " + file.getAbsolutePath());
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}
}
