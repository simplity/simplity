/*
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
import java.io.InputStream;
import java.util.UUID;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.IAttachmentAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** we use a designated folder to save all attachments */
public class FileBasedAssistant implements IAttachmentAssistant {
	private static final Logger logger = LoggerFactory.getLogger(FileBasedAssistant.class);

	private final File storageRoot;

	/**
	 * set the root folder where files will be stored permanently.
	 *
	 * @param rootPath
	 *            must be a valid folder name under which we should be allowed
	 *            to create folders and files
	 */
	public FileBasedAssistant(String rootPath) {
		this.storageRoot = new File(rootPath);
		if (this.storageRoot.exists() == false) {
			throw new ApplicationError(
					rootPath + " is not a valid path. Attachment Manager will not work for you.");
		}
		if (this.storageRoot.isDirectory() == false) {
			throw new ApplicationError(
					rootPath + " is not a folder. Attachment Manager will not work for you.");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.MediaStorageAssistant#store(java.io.InputStream,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String store(InputStream inStream) {
		String key = UUID.randomUUID().toString();
		File file = new File(this.storageRoot, key);
		FileManager.streamToFile(file, inStream);
		return key;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.MediaStorageAssistant#store(java.lang.string,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public String store(String tempKey) {
		File file = FileManager.getTempFile(tempKey);
		if (file == null) {

			logger.info("No temp file found for key " + tempKey);

			return null;
		}
		String key = UUID.randomUUID().toString();
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(file);
			file = new File(this.storageRoot, key);
			FileManager.streamToFile(file, inStream);
			return key;
		} catch (Exception e) {

			logger.error("Error while storing temp file " + tempKey, e);

			return null;
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	@Override
	public String retrieve(String storageKey) {
		File file = new File(this.storageRoot, storageKey);
		if (file.exists() == false) {

			logger.info("Invalid storage key requested : " + storageKey);

			return null;
		}
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			return FileManager.createTempFile(in).getName();
		} catch (Exception e) {

			logger.error(
					"error while copying permanent storage with key " + storageKey + " to temp area", e);

			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/** @return path to folder where files are stored */
	public String getRootPath() {
		return this.storageRoot.getPath();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.media.MediaStorageAssistant#discard(java.lang.String)
	 */
	@Override
	public void remove(String storageKey) {
		File file = new File(this.storageRoot, storageKey);
		if (file.exists()) {
			file.delete();
		}
	}
}
