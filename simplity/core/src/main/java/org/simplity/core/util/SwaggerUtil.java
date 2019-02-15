/*
 * Copyright (c) 2019 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class SwaggerUtil {
	private static final Logger logger = LoggerFactory.getLogger(SwaggerUtil.class);

	/**
	 * tool to be used by a programmer at design time to get all services in a
	 * swagger open api document
	 *
	 * @param swaggerJson
	 * @return json string for services
	 */
	public static String swaggerToServices(String swaggerJson) {
		JSONObject swagger = new JSONObject(swaggerJson);
		JSONWriter services = new JSONWriter();
		services.object();
		services.key("basePath").value("basePathPrefix");
		services.key("serviceNamePrefix").value("");

		services.key("paths").object();
		JSONObject paths = swagger.getJSONObject("paths");
		for (String path : paths.keySet()) {
			services.key(path).object();

			JSONObject ops = paths.getJSONObject(path);
			for (String op : ops.keySet()) {
				services.key(op).value("service");
				services.key(op + "-responseCodes").object();
				JSONObject responses = ops.getJSONObject(op).getJSONObject("responses");
				for (String resp : responses.keySet()) {
					services.key(resp).value(responses.getJSONObject(resp).get("description"));
				}
				services.endObject();
			}
			services.endObject();
		}
		services.endObject();
		services.endObject();
		return services.toString();
	}

	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<record xmlns=\"http://www.simplity.org/schema/\"\n";

	/**
	 * Create a record.xml for each structure definition in swagger
	 *
	 * @param swaggerJson
	 * @param folder
	 *            where files are to be saved
	 */
	public static void swaggerToRecs(String swaggerJson, File folder) {
		JSONObject defs = new JSONObject(swaggerJson).getJSONObject("definitions");
		for (String rec : defs.keySet()) {
			try {
				File file = new File(folder, rec + ".xml");
				if (file.exists() == false) {
					file.createNewFile();
				}
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
				writer.write(XML_HEADER);
				writer.write(" name = \"" + rec + '"');
				writer.write(" moduleName=\"\" recordType=\"structure\" >\n <fields>\n");
				Set<String> reqSet = new HashSet<>();
				JSONObject def = defs.getJSONObject(rec);
				JSONArray r = def.optJSONArray("required");
				if (r != null) {
					for (int i = r.length() - 1; i >= 0; i--) {
						reqSet.add(r.getString(i));
					}
				}

				JSONObject fields = def.optJSONObject("properties");
				for (String fieldName : fields.keySet()) {
					JSONObject field = fields.getJSONObject(fieldName);
					boolean isRequired = reqSet.contains(fieldName);
					/*
					 * is it a child record
					 */
					String ref = field.optString("$ref", null);
					if (ref != null) {
						writer.write("\n<childRecord name=\"");
						writer.write(fieldName);
						writer.write("\" referredRecord=\"");
						writer.write(ref.substring(ref.lastIndexOf('/') + 1));
						if (isRequired) {
							writer.write("\" isRequied=\"true");
						}
						writer.write("\" />");
						continue;
					}
					/*
					 * is it an array
					 */
					String type = field.optString("type", null);
					if (type != null && type.equals("array")) {
						JSONObject items = field.getJSONObject("items");
						/*
						 * is it an array of objects/recs
						 */
						ref = items.optString("$ref", null);
						if (ref != null) {
							writer.write("\n<recordArray name=\"");
							writer.write(fieldName);
							writer.write("\" referredRecord=\"");
							writer.write(ref.substring(ref.lastIndexOf('/' + 1)));
							if (isRequired) {
								writer.write("\" isRequied=\"true");
							}
							writer.write("\" />");
							continue;
						}
						/*
						 * it is an array of values
						 */

						writer.write("\n<valueArray name=\"");
						writer.write(fieldName);
						writer.write("\" dataType=\"");
						writer.write(items.optString("type"));
						if (isRequired) {
							writer.write("\" isRequied=\"true");
						}
						writer.write("\" />");
						continue;
					}
					/*
					 * it is a value field
					 */
					writer.write("\n<field name=\"");
					writer.write(fieldName);
					writer.write("\" dataType=\"");
					writer.write(type);
					if (isRequired) {
						writer.write("\" isRequied=\"true");
					}
					writer.write("\" />");
				}
				writer.write("\n</fields></record>\n");
				writer.close();
			} catch (IOException e) {
				logger.error("Unable to create file {} under folder {}. Error : {}", rec, folder.getAbsolutePath(),
						e.getMessage());
				continue;
			}
		}
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		String folderPath = "d:/rgb/swagger/";
		String swaggerName = "swagger.json";
		String outputName = "services.json";
		String swagger = IoUtil.readResource(folderPath + swaggerName);
		String services = swaggerToServices(swagger);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + outputName))) {
			writer.write(services);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		File folder = new File(folderPath);
		swaggerToRecs(swagger, folder);
	}
}
