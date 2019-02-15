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
import java.util.Iterator;

/**
 * an iterator that gives you next file from a folder, recursively going thru sub-folders. we can
 * filter files based on extension, like xml. we always exclude folders starting with .
 *
 * @author simplity.org
 */
public class FileIterator implements Iterator<File> {
  private static final char DOT = '.';
  /** files inside the current folder being iterated */
  private File[] files;
  /** pointer to the above array. points to the next file to be used. Obviously starts with 0. */
  private int nextIdx;
  /**
   * in case our current file in files[] array is a folder, this is the iterator for that folder.
   * Null if we got a file form this folder
   */
  private FileIterator currentIterator;
  /** as received at the time of construction To be used to create child-iterators */
  private String extension;
  /**
   * we seek (not someone's blessings, but do the hard work) and keep the next answer ready. (Why
   * wait for some one to call, keep it ready. Nay way it is question of time before they call you
   * for next one!!) Real reason is that hasNext() does lot of hard work, which we do not want to
   * repeat in next()
   */
  private File nextFile;

  /**
   * get an iterator that returns files inside this folder, including all sub-folders (excluding
   * folders/files that start with .) to be invoked by FileCollection only
   *
   * @param folder root folder for this iterator
   * @param extension returns file with this extension (like xml. do not include .) null if you want
   *     all files.
   */
  public FileIterator(File folder, String extension) {
    if (extension != null) {
      this.extension = extension;
    }
    if (folder == null || folder.isDirectory() == false) {
      this.files = new File[0];
    } else {
      this.files = folder.listFiles();
    }
    this.setNextFile();
  }

  /** real worker. searches for the next one and sets this.nextFile. */
  private void setNextFile() {
    /*
     * do we have an active sub-iterator
     */
    if (this.currentIterator != null) {
      this.nextFile = this.currentIterator.next();
      if (this.nextFile != null) {
        return;
      }
    }
    /*
     * current iterator, if non-null, has ended.
     */
    this.currentIterator = null;
    this.nextFile = null;
    for (int i = this.nextIdx; i < this.files.length; i++) {
      File file = this.files[i];
      String fileName = file.getName();
      if (fileName.charAt(0) == FileIterator.DOT) {
        continue;
      }

      if (file.isDirectory()) {
        FileIterator iter = new FileIterator(file, this.extension);
        this.nextFile = iter.next();
        if (this.nextFile == null) {
          continue;
        }
        /*
         * we got a productive child !!
         */
        this.nextIdx = i + 1;
        this.currentIterator = iter;
        return;
      }

      if (file.isFile()) {
        if (this.extension == null || fileName.endsWith(this.extension)) {
          this.nextIdx = i + 1;
          this.nextFile = file;
          return;
        }
      }
    }
    /*
     * we are done. there is no file to set. it will remain as null,
     * indicating end of collection
     */
  }

  @Override
  public boolean hasNext() {
    return this.nextFile != null;
  }

  @Override
  public File next() {
    File fileToReturn = this.nextFile;
    if (fileToReturn != null) {
      /*
       * our main algorithm is to be ready with the next file
       */
      this.setNextFile();
    }
    return fileToReturn;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove is not implemented");
  }
}
