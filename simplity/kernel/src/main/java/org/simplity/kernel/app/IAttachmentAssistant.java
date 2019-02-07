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
package org.simplity.kernel.app;

import java.io.InputStream;

import org.simplity.kernel.ApplicationError;

/**
 * An assistant to AttachmentManager who can store/retrieve file/media in a permanent way. We
 * provide a simple assistant who uses a designated folder on the file system for this.
 *
 * @author simplity.org
 */
public interface IAttachmentAssistant {

  /**
   * store a file into corporate storage area. IMP : we DO NOT close stream. We believe the creator
   * has to destroy it.
   *
   * @param inStream stream to read he media from. Kept open on return. Caller need to close it.
   * @return key/token that can be used to retrieve this attachment any time.
   */
  public String store(InputStream inStream);

  /**
   * store a file into corporate storage area from its temp-area
   *
   * @param tempKey key to temp-storage area
   * @return key/token that can be used to retrieve this attachment any time.
   */
  public String store(String tempKey);

  /**
   * retrieve a media file from central storage
   *
   * @param storageKey
   * @return file-token that is to be used to access the retrieved content for sending to client or
   *     re-storage
   * @throws ApplicationError if storageKey is not valid
   */
  public String retrieve(String storageKey);

  /**
   * remove a media file from central storage
   *
   * @param storageKey
   */
  public void remove(String storageKey);
}
