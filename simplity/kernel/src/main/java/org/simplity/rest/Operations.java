/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.rest;

import java.io.File;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;

/**
 * static class that gets swagger/openApi for a given url
 *
 * @author simplity.org
 *
 */
public class Operations {


	/**
	 * root node of the path-tree that holds service spec for paths
	 */
	private static PathNode rootNode = new PathNode(null, null, null);

	/**
	 * private constructor
	 */
	private Operations() {
		// enforce static
	}

	/**
	 *
	 * @param path
	 * @param method
	 * @param params
	 * @return spec or null if there is no spec for this url
	 */
	public static Operation getServiceSpec(String path, String method, JSONObject params) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		PathNode node = findNodeForPath(path, params);
		/*
		 * get the spec at this node. In case we do not have one at this node,
		 * then we keep going up
		 *
		 */
		while (node != null) {
			Tracer.trace("Looking for spec at " + node.getPathPrefix());
			if (node.isValidPath()) {
				Operation spec = node.getServiceSpec(method);
				if (spec == null) {
					Tracer.trace(method + " is not valid for path " + path);
					return null;
				}
				return spec;
			}

			node = node.getParent();
		}
		/*
		 * no spec found at any part of this path
		 */
		Tracer.trace(path + " is not a valid path");
		return null;
	}

	private static PathNode findNodeForPath(String path, JSONObject params) {
		PathNode node = rootNode;
		if (path == null) {
			return node;
		}
		String p = path.trim();
		if (p.isEmpty()) {
			return node;
		}
		/*
		 * go down the path as much as we can.
		 */
		String[] parts = p.split(Tags.PATH_SEP_STR);
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			Tracer.trace("looking at node=" + node.getPathPrefix() + " for part=" + part);
			PathNode child = node.getChild(part);
			if (child == null) {
				/*
				 * this is not path. Is it field?
				 */
				child = node.getFieldChild();
				if (child == null) {
					/*
					 * we reached a leaf. we ignore any remaining part
					 */
					break;
				}
				/*
				 * copy this part as field value
				 */
				params.put(node.getFieldName(), part);
			}
			node = child;
		}
		Tracer.trace("leaf node is " + node.getPathPrefix());
		return node;
	}

	/**
	 * load all api's from a resource folder
	 *
	 * @param apiFolder
	 */
	public static void loadAll(String apiFolder) {
		File folder = new File(apiFolder);
		if (folder.exists() == false || folder.isDirectory() == false) {
			Tracer.trace("Api spec folder " + apiFolder + " is not a folder.");
			return;
		}
		String[] files = FileManager.getResources(apiFolder);
		if (files.length == 0) {
			Tracer.trace("Api spec folder " + apiFolder + " has no files.");
			return;
		}
		for (String fileName : files) {
			if (fileName.endsWith(".json") == false) {
				Tracer.trace("Skipping non-joson file " + fileName);
				continue;
			}
			loadFromFile(fileName);
		}
		return;
	}

	/**
	 *
	 * @param fileName
	 */
	public static void loadFromFile(String fileName) {
		Tracer.trace("Going to load file " + fileName);
		try {
			String json = FileManager.readResource(fileName);
			loadFromJsonText(json);
		} catch (Exception e) {
			Tracer.trace(e, "Error while loading open-api spec " + fileName);
		}
	}

	/**
	 * @param jsonText
	 */
	public static void loadFromJsonText(String jsonText) {
		loadFromJson(new JSONObject(jsonText));
	}

	/**
	 * unload/reset all apis
	 */
	public static void unloadAll() {
		rootNode = new PathNode(null, null, null);
	}

	/**
	 * @param json
	 */
	public static void loadFromJson(JSONObject json) {
		JSONObject paths = json.optJSONObject(Tags.PATHS_ATTR);
		if (paths == null || paths.length() == 0) {
			Tracer.trace(" No paths in the API");
			return;
		}
		/*
		 * for run-time efficiency, we substitute refs with actual JSON
		 */
		JSONObject defs = json.optJSONObject(Tags.DEFS_ATTR);
		if (defs != null) {
			Tracer.trace("We found " + defs.length() + " definitions");
			/*
			 * defs may contain refs. substitute the first
			 */
			replaceRefs(defs, defs);
			/*
			 * replace refs in rest of the api
			 */
			replaceRefs(json, defs);
		} else {
			Tracer.trace("No definitions in this api");
		}

		String basePath = json.optString(Tags.BASE_PATH_ATTR, null);
		String moduleName = json.optString(Tags.MODULE_ATTR, null);
		loadAnApi(paths, basePath, moduleName);
	}

	/**
	 * @param paths
	 * @param basePath
	 */
	private static void loadAnApi(JSONObject paths, String basePath, String moduleName) {

		for (String key : paths.keySet()) {
			JSONObject methods = paths.optJSONObject(key);
			String fullPath = basePath == null ? key : basePath + key;

			/*
			 * build the branch to this path-node
			 */
			PathNode node = rootNode;
			if (fullPath != null && fullPath.isEmpty() == false) {

				String[] parts = trimPath(fullPath).split(Tags.PATH_SEP_STR);
				for (String part : parts) {
					if (part.isEmpty()) {
						Tracer.trace("Empty part found in path.Igonred");
						continue;
					}
					String fieldName = null;
					if (part.charAt(0) == '{') {
						/*
						 * we trust the spec is in order
						 */
						fieldName = part.substring(1, part.length() - 1);
						node = node.setFieldName(fieldName, moduleName);
					} else {
						node = node.setChild(part, moduleName);
					}
				}
			}
			/*
			 * add the methods at the leaf
			 */
			node.setPathSpec(methods);
		}
	}

	private static String trimPath(String path) {
		String result = path;

		if (result.charAt(0) == Tags.PATH_SEP_CHAR) {
			result = result.substring(1);
		}

		int idx = result.length() - 1;
		if (idx >= 0 && result.charAt(idx) == Tags.PATH_SEP_CHAR) {
			result = result.substring(0, idx);
		}
		return result;
	}

	/**
	 * find internal references and replace them with actual objects
	 *
	 * @param json
	 * @param definitions
	 *
	 */
	private static void replaceRefs(JSONObject json, JSONObject definitions) {
		if (json == null || json.length() == 0) {
			return;
		}

		for (String key : json.keySet()) {
			Object obj = json.get(key);
			if (obj instanceof JSONObject) {
				JSONObject jsonObj = (JSONObject) obj;
				String ref = getRef(jsonObj);
				if (ref == null) {
					/*
					 * normal JSON. Recurse to inspect it further
					 */
					replaceRefs(jsonObj, definitions);
				} else {
					/*
					 * needs replacement
					 */
					jsonObj = definitions.optJSONObject(ref);
					if (jsonObj == null) {
						Tracer.trace("defintion for " + ref + " not found. reference replaced with an empty object");
						jsonObj = new JSONObject();
					}
					Tracer.trace("Replacing " + ref);
					json.put(key, jsonObj);
				}
			} else if (obj instanceof JSONArray) {
				replaceRefs((JSONArray) obj, definitions);
			}
		}
	}

	/**
	 * find internal references and replace them with actual objects
	 *
	 * @param array
	 * @param definitions
	 *
	 */
	private static void replaceRefs(JSONArray array, JSONObject definitions) {
		if (array == null) {
			return;
		}
		int nbr = array.length();
		for (int i = 0; i < nbr; i++) {
			Object obj = array.get(i);
			if (obj instanceof JSONObject) {
				JSONObject jsonObj = (JSONObject) obj;
				String ref = getRef(jsonObj);
				if (ref == null) {
					/*
					 * it is a normal JSON object. recurse and replace
					 */
					replaceRefs(jsonObj, definitions);
				} else {
					/*
					 * it is a ref object. replace this with its actual josn
					 * object
					 */
					jsonObj = definitions.optJSONObject(ref);
					if (jsonObj == null) {
						Tracer.trace("defintion for " + ref + " not found. reference replaced with an empty object");
						jsonObj = new JSONObject();
					}
					Tracer.trace("Replacing " + ref + " at position " + i);
					array.put(i, jsonObj);
				}
			} else if (obj instanceof JSONArray) {
				replaceRefs((JSONArray) obj, definitions);
			}
		}
	}

	/**
	 * @param jsonObj
	 * @return attribute name to be referred to, if this is a ref-object. FOr
	 *         example if this object is {"$ref": "#/definitions/pets"} this
	 *         method returns "pets"
	 */
	private static String getRef(JSONObject jsonObj) {
		if (jsonObj == null || jsonObj.length() != 1) {
			return null;
		}
		String ref = jsonObj.optString(Tags.REF_ATTR, null);
		if (ref == null) {
			return null;
		}
		if (ref.indexOf(Tags.REF_START) != 0) {
			Tracer.trace("$ref is to be set to a value starting with " + Tags.REF_START);
			return null;
		}
		Tracer.trace("Found a ref entry for " + ref);
		return ref.substring(Tags.REF_START.length());
	}

	public static void main(String[] args) {
		String rootFolder = "C:/repos/simplity/test/WebContent/WEB-INF/api/";
		loadAll(rootFolder);
		JSONObject params = new JSONObject();
		Operation spec = getServiceSpec("/app/troubleTicket/1234", "get", params);
		if (spec == null) {
			Tracer.trace("That is not a valid request");
		} else {
			System.out.println("ServiceName is " + spec.getServiceName());
			System.out.println("params is " + params.toString(2));
		}
	}
	/*
	 * 1. parameters and responses at the root level to be used as reference
	 * 2. body is an object in swagger. In our convention, it contains other
	 * objects/primitive. How do we want to handle that?
	 *
	 */
}
