/*
 * Copyright (c) 2018 simplity.org
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

package org.simplity.core.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * tree representation of all valid pats for which service names are mapped
 *
 * @author simplity.org
 *
 */
public class Paths {
	static final Logger logger = LoggerFactory.getLogger(Paths.class);
	static final String PATH_SEP_STR = "/";
	/*
	 * root node. PathNode has the tree structure built into that.
	 */
	private Node rootNode;

	/**
	 * create an empty tree to which paths can be added later
	 */
	public Paths() {
		this.rootNode = new Node(null, null, false);
	}

	/**
	 * add a path-service mapping
	 *
	 * @param path
	 *            example a/{invoice}/{operation}
	 * @param services
	 *            each element is a method whose value is service. In case there
	 *            is a single service irrespective of method, then a single
	 *            member with "" is to be used example {"post":"updateInvoice",
	 *            "get":"invoiceDetails",,,,}
	 * @param servicePrefix
	 *            if non-null, every service name is prefixed with this to get
	 *            its fully-qualified name
	 * @return true if the mapping got added. False in case this path is a
	 *         duplicate, and hence ignored
	 */
	public boolean addPath(String path, JSONObject services, String servicePrefix) {
		String[] parts = path.split(PATH_SEP_STR);
		Node node = this.rootNode;
		for (String part : parts) {
			part = part.trim();
			if (part.isEmpty()) {
				logger.info("empty part ignored");
				continue;
			}
			if (part.charAt(0) == '{') {
				/*
				 * we assume that there are no syntax errors. so this token is
				 * {fieldName}
				 */
				String fieldName = part.substring(1, part.length() - 1);
				node = node.setFieldChild(fieldName);
			} else {
				node = node.setPathChild(part);
			}
			if (node == null) {
				logger.error("child node was not created for path part {}. Path {} not added to paths collection", part,
						path);
				return false;
			}
		}
		/*
		 * attach services for this node
		 */
		return node.setServices(services, servicePrefix);
	}

	/**
	 * add all paths from a json object. Each element of this object is a
	 * path-services mapping. e.g. {"/a/{b}/c":
	 * {"post":"addInvoice","get":"invoiceDetails",,,}, "/a/{b}/d" : {},}
	 *
	 * @param json
	 *            josn object as read from the standard json file for paths.
	 *            non-null.
	 */
	public void addPaths(JSONObject json) {
		String pathPrefix = json.optString(HttpConventions.TagNames.BASE_PATH, null);
		String servicePrefix = json.optString(HttpConventions.TagNames.SERVICE_NAME_PREFIX, null);
		JSONObject paths = json.optJSONObject(HttpConventions.TagNames.PATHS);
		if (paths == null) {
			if (pathPrefix == null && servicePrefix == null) {
				/*
				 * possible that base is not used, and file contains only paths
				 */
				paths = json;
			} else {
				logger.warn("No paths found in json");
				return;
			}
		}

		int nbr = 0;
		int nok = 0;
		for (String key : paths.keySet()) {
			String aPath = key;
			if (pathPrefix != null) {
				aPath = pathPrefix + aPath;
			}
			boolean ok = this.addPath(aPath, paths.optJSONObject(key), servicePrefix);
			if (ok) {
				nbr++;
			} else {
				nok++;
			}
		}
		if (nok > 0) {
			logger.error("{} paths not added because of errors.", nok);
		}
		if (nbr > 0) {
			logger.info("{} paths added", nbr);
		} else {
			logger.error("No paths added to the paths collection.");
		}
	}

	/**
	 * parse a path received from client and return the corresponding service.
	 * Also, extract any path-fields into the collection
	 *
	 * @param path
	 *            requested pat from client e.g. /a/123/add/
	 * @param method
	 *            http method
	 * @param fields
	 *            to which path-fields are to be extracted to. null if we need
	 *            not do that.
	 * @return service to which this path is mapped to. null if no service is
	 *         mapped
	 */
	public String parse(String path, String method, Map<String, Object> fields) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		Node node = this.findNodeForPath(path, fields);
		if (node == null) {
			logger.info("{} is an invalid path", path);
			return null;
		}
		/*
		 * get the service at this node. In case we do not have one at this
		 * node, then we keep going up
		 *
		 */
		while (node != null) {
			if (node.isValidEndNode()) {
				return node.getService(method);
			}
			/*
			 * it is possible that this part is a field and is optional..
			 */
			if (node.isFieldChild()) {
				node = node.getParent();
			} else {
				break;
			}
		}
		/*
		 * So, the path was partial part of a valid path
		 */
		logger.info("{} is an incomplete path", path);
		return null;
	}

	/**
	 * find the node corresponding to the path
	 *
	 * @param path
	 *            non-null, non-empty
	 * @param fields
	 * @return
	 */
	private Node findNodeForPath(String path, Map<String, Object> fields) {
		Node node = this.rootNode;
		if (node.isLeaf()) {
			logger.info("We have an empty list of paths!!");
			return null;
		}
		/*
		 * go down the path as much as we can.
		 */
		String[] parts = path.split(PATH_SEP_STR);
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			Node child = node.getChild(part);
			if (child == null) {
				/*
				 * this is not a valid path
				 */
				logger.warn("Path {} is invalid starting at token {}", path, part);
				return null;
			}
			/*
			 * is this a field to be picked-up
			 */
			if (child.isFieldChild() && fields != null) {
				fields.put(child.getName(), part);
			}
			node = child;
		}
		return node;
	}
}

/**
 * this class is exclusively used by PathTree, but is not an inner class. Hence
 * we have put this inside this compilation unit
 *
 * @author simplity.org
 *
 */
class Node {
	private static final String DUPLICATE = "Duplicate path-service mapping detected at path-part {} and ignored";
	/**
	 * path-part or field name.
	 */
	private final String name;
	/**
	 * non-null if the next part of the path isa field. null if the next part if
	 * non-field part, or if this is the lef node
	 */
	private Node fieldChild;
	/**
	 * child paths from here. one entry for each possible value of the path part
	 * from here. null if this is a field
	 */
	private Map<String, Node> children;
	/**
	 * default service that is mapped to this path. null if no default service
	 * is mapped at this level.
	 */
	private String defaultService;
	/**
	 * non-null if different methods map to different services.
	 */
	private Map<String, String> services;

	/**
	 * way to go up the path.
	 */
	private final Node parent;

	private final boolean isFieldChild;

	/**
	 * construct a Path node under the parent
	 *
	 * @param parent
	 */
	Node(Node parent, String nodeName, boolean isField) {
		this.name = nodeName;
		this.parent = parent;
		this.isFieldChild = isField;
	}

	/**
	 * @return true if this is a leaf node. False if this node has at least one
	 *         child node, either a field-child or path-child
	 */
	public boolean isLeaf() {
		return this.children == null && this.fieldChild == null;
	}

	/**
	 * @return true if this node is the field-child of its parent. false if it
	 *         is a path-child
	 */
	public boolean isFieldChild() {
		return this.isFieldChild;
	}

	/**
	 * @return true if this path is a valid complete path. It means that service
	 *         names are mapped to this path
	 */
	boolean isValidEndNode() {
		return this.defaultService != null || this.services != null;
	}

	/**
	 * @return the fieldName
	 */
	String getName() {
		return this.name;
	}

	/**
	 * @param fieldName
	 *            the fieldName to set
	 * @return child-node associated with this field
	 */
	Node setFieldChild(String field) {
		if (this.children != null) {
			Paths.logger.error("Node at {} already has path-children, and hence a field child is invalid.", this.name);
			return null;
		}
		if (this.fieldChild == null) {
			return this.fieldChild = new Node(this, field, true);
		}

		if (field.equals(this.fieldChild.name)) {
			return this.fieldChild;
		}

		Paths.logger.error(
				"Two paths have common path till part {} and then have different fieldnames : {} and {} as next part. This leads to ambiguity while determining the oath for a given url.",
				this.name, field, this.fieldChild.name);
		return null;
	}

	/**
	 * set a child path for this path-part
	 *
	 * @param pathPart
	 *            a part of the path
	 * @return child node for this path-part
	 */
	Node setPathChild(String pathPart) {
		if (this.fieldChild != null) {
			Paths.logger.error("Node at {} already has a field-child named {}. Path child naemd {} can not be added.",
					this.name, this.fieldChild.name, pathPart);
			return null;
		}
		Node child = null;

		if (this.children == null) {
			this.children = new HashMap<String, Node>();
		} else {
			child = this.children.get(pathPart);
			if (child != null) {
				return child;
			}
		}
		child = new Node(this, pathPart, false);
		this.children.put(pathPart, child);
		return child;
	}

	/**
	 * @param pathPart
	 *            as received from client. can be value of field in case this
	 *            node is for a field
	 * @return child node for this pathPart. null if no child node for this
	 *         part.
	 */
	Node getChild(String pathPart) {
		if (this.fieldChild != null) {
			return this.fieldChild;
		}
		if (this.children != null) {
			return this.children.get(pathPart);
		}
		return null;
	}

	/**
	 * set service associated with methods
	 *
	 * @param services
	 *
	 * @param servicePrefix
	 *            if non-null, this is prefixed for each service name
	 */
	boolean setServices(JSONObject services, String servicePrefix) {
		if (this.defaultService != null || this.services != null) {
			Paths.logger.error(DUPLICATE, this.name);
			return false;
		}
		Set<String> methods = services.keySet();
		String service = services.optString("", null);
		if (service != null) {
			if (servicePrefix != null) {
				service = servicePrefix + service;
			}
			this.defaultService = service;
			Paths.logger.info("Default service {} set for node {}", service, this.name);
			if (methods.size() == 1) {
				return true;
			}
		}

		this.services = new HashMap<String, String>();
		for (String method : methods) {
			if (method.isEmpty()) {
				continue;
			}
			service = services.optString(method);
			if (servicePrefix != null) {
				service = servicePrefix + service;
			}
			this.services.put(method.toLowerCase(), service);
		}
		return true;
	}

	/**
	 * @param method
	 * @return service associated with this method, or null if not service is
	 *         associated for this path.
	 */
	String getService(String method) {
		if (method == null || method.isEmpty()) {
			if (this.defaultService == null) {
				Paths.logger
						.info("No method specified and this path has no default service.");
			}
			return this.defaultService;
		}

		/*
		 * is there an entry for this method?
		 */
		if (this.services != null) {
			String serviceName = this.services.get(method.toLowerCase());
			if (serviceName != null) {
				return serviceName;
			}
		}
		/*
		 * go for default
		 */
		if (this.defaultService == null) {
			Paths.logger.info("No service attached to method {}, and there is no default service", method);
		}
		return this.defaultService;
	}

	/**
	 * @return the parent node. null for the root node
	 */
	Node getParent() {
		return this.parent;
	}
}
