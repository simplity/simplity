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

package org.simplity.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.simplity.core.app.IAttachmentAssistant;
import org.simplity.core.file.FileManager;

/**
 * Most corporate have central service that stores files. Attachment Manager is the interface
 * between such a service and this application.
 *
 * <p>Process to upload a file.
 *
 * <p>1. client uploads the file thru attachmentAgent who in-turn uses this class to save the file
 * into temp storage, and returns a key to client.
 *
 * <p>2. client uses this key as part of its data while submitting data to server as part of its
 * save transaction
 *
 * <p>3. HttpAgent copies all saved media in session to serviceData while requesting service from
 * app layer.
 *
 * <p>4. service has to know about the field in which this key us expected from client. This is
 * specified as part of InputData specification. This class is invoked by the service to save the
 * attachment into permanent storage. This key is stored as part of transaction data record in
 * RDBMS.
 *
 * <p>process to send a file to client.
 *
 * <p>1. Service retrieves all data from db, including the storage token for the file.
 *
 * <p>2. Field that represent such a key are specified as part of OutputData specification. Service
 * uses this class to retrieve the stored attachment into a temp area. Value of the field is changed
 * to this new temp-key.
 *
 * <p>3. client application has to have the knowledge about the field that has the key for file to
 * be downloaded. It calls back the AttachmentAgent to down load the file to client. This is a get
 * method that is easy to be used as url for image or url for a new window
 *
 * @author simplity.org
 */
public class AttachmentManager {
	private static final Logger logger = LoggerFactory.getLogger(AttachmentManager.class);

  private static final String MSG =
      "No assistant is assigned to AttachmentManager. Manager expressed her regret that she is unable to manage media.";
  /** store room assistant instance. */
  private static IAttachmentAssistant assistant = null;

  /**
   * Save media into a temp/buffer area and return a Media data structure for the same. This is
   * suitable for client-facing class that receive files from client to put it into temp area.
   *
   * @param inStream from which to read from
   * @return key to the saved file. Always non-null
   */
  public static String saveToTempArea(InputStream inStream) {
    return FileManager.createTempFile(inStream).getName();
  }

  /**
   * delete a file from temp storage.
   *
   * @param key key/name
   */
  public static void removeFromTempArea(String key) {
    FileManager.deleteTempFile(key);
  }

  /**
   * save this attachment to permanent storage
   *
   * @param inStream from which to read the contents
   * @return string that is the key to this stored attachment. This can be used to retrieve this
   *     attachment.
   */
  public static String saveToStorage(InputStream inStream) {
    checkAssistant();
    return assistant.store(inStream);
  }

  /**
   * move the media from temp area to permanent area
   *
   * @param key points to the temp-storage of attachment to be stored
   * @return key that can be used to retrieve this from permanent storage
   */
  public static String moveToStorage(String key) {
    checkAssistant();
    File file = FileManager.getTempFile(key);
    if (file == null) {
      return null;
    }
    InputStream in = null;
    String newKey = null;
    try {

      in = new FileInputStream(file);
      newKey = assistant.store(in);
    } catch (Exception e) {

      logger.error("Error while moving attachment " + key + " from temp area to storage", e);

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
    file.delete();
    return newKey;
  }

  /**
   * get it back from storage
   *
   * @param key that was returned while storing it
   * @return key to temp storage area. Null if the input key is not valid
   */
  public static String moveFromStorage(String key) {
    checkAssistant();
    return assistant.retrieve(key);
  }

  /**
   * get it back from storage
   *
   * @param key that was returned while storing it
   */
  public static void removeFromStorage(String key) {
    checkAssistant();
    assistant.remove(key);
  }

  /**
   * to be called before the manager is pressed into action. Part of application set-up action
   *
   * @param ast a thread-safe assistant who would be cached and used for all media storage
   *     operations
   */
  public static void setAssistant(IAttachmentAssistant ast) {
    assistant = ast;

    logger.info(
        "Attachment Manager is happy to announce that she got an assistant, and is ready to serve attachment files now.");
  }

  /** avoid confusing run-time error in case the assistant is not set */
  private static void checkAssistant() {
    if (assistant == null) {
      throw new ApplicationError(MSG);
    }
  }
}
