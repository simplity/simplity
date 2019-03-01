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

package org.simplity.ide.comp;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.simplity.core.app.AppConventions;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.IComponent;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.comp.ValidationReference;
import org.simplity.core.util.XmlParseException;
import org.simplity.core.util.XmlUtil;
import org.simplity.eclipse.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Repository of all validated/parsed resources as well as all comps that are
 * parsed and referred
 *
 * @author simplity.org
 *
 */
public class Repository implements IRepository {
	protected static final Logger logger = LoggerFactory.getLogger(Repository.class);
	/*
	 * if a field is to be added, add it at the end, and add the corresponding
	 * comptype to the other array
	 */
	private static final String[] XREF_FIELD_NAMES = { "adapterName", "dataType", "procedureName", "sqlName",
			"jobsToRunOnStartup", "messageName", "failureMessageName", "successMessageName", "serviceName",
			"referredServiceForInput", "referredServiceForOutput", "recordName", "inputRecordName", "outputRecordName",
			"referredRecord", "defaultRefRecord", "filterRecordName" };
	private static final ComponentType[] XREF_COMPS = { ComponentType.ADAPTER, ComponentType.DT, ComponentType.SP,
			ComponentType.SQL, ComponentType.JOBS, ComponentType.MSG, ComponentType.MSG, ComponentType.MSG,
			ComponentType.SERVICE, ComponentType.SERVICE, ComponentType.SERVICE, ComponentType.REC, ComponentType.REC,
			ComponentType.REC, ComponentType.REC, ComponentType.REC, ComponentType.REC };
	private static final Map<String, ComponentType> FIELD_COMP_XREF = new HashMap<>(XREF_COMPS.length);

	private static final String EXTENSION_WITH_DOT = "." + Constants.Names.EXTENSION;

	/*
	 * put field-comp xrefs into map
	 */
	static {
		for (int i = XREF_FIELD_NAMES.length - 1; i >= 0; i--) {
			FIELD_COMP_XREF.put(XREF_FIELD_NAMES[i], XREF_COMPS[i]);
		}
	}
	/**
	 * this is just for documentation as of now. Will be the key/id in case we
	 * extend our design to have multiple repositories in a project
	 */
	String rootPath;
	/**
	 * resources that are mined to detect components
	 */
	private Map<String, Resource> resources = new HashMap<>();

	/**
	 * pseudo comp for application
	 */
	private Comp appComp;

	/**
	 * comps repository. one map per comp type. These are the comps that are
	 * mined (from a resource). others comps.
	 */
	private Map<String, Comp>[] allComps;

	/**
	 *
	 * @param path
	 *            root path of this repository relative to the project. This is
	 *            the id of the repository. e.g. main/resource/comps/
	 */
	@SuppressWarnings("unchecked")
	public Repository(String path) {
		this.rootPath = path;
		int n = ComponentType.values().length;
		this.allComps = new Map[n];
		for (int i = 0; i < n; i++) {
			this.allComps[i] = new HashMap<>();
		}
	}

	@Override
	public String getRootPath() {
		return this.rootPath;
	}

	@Override
	public void addResource(String path, InputStream stream) {
		/*
		 * we internally treat add and replace as same
		 */
		this.replaceResource(path, null, stream);
	}

	@Override
	public void modifyResource(String path, InputStream stream) {

		this.replaceResource(path, null, stream);
		return;
	}

	@Override
	public void replaceResource(String path, String oldPath, InputStream stream) {
		if (oldPath != null) {
			this.resourceRemoved(oldPath);
		}

		if (path != null) {
			this.resourceRemoved(path);
			CompType compType = getCompType(path);
			if (compType != null) {
				this.load(path, compType.ct, stream);
			}
		}
		this.logStats();
	}

	@Override
	public void removeResource(String path) {
		this.replaceResource(null, path, null);
	}

	@Override
	public void validateResource(String path, InputStream stream, IValidationListener listener) {
		CompType compType = getCompType(path);
		if (compType == null) {
			return;
		}

		ComponentType ct = compType.ct;
		try {
			if (ct == null) {
				this.validateApp(stream, listener);
			} else if (ct.isPreloaded()) {
				this.validateMulti(ct, stream, listener);
			} else {
				this.validateSingle(ct, stream, listener);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Unable to validate resource {}. Error:{}", path, e.getMessage());
		}
	}

	private void load(String path, ComponentType ct, InputStream stream) {
		Resource resource = new Resource(path, ct);
		this.resources.put(path, resource);
		if (ct == null) {
			this.appComp = new Comp(resource, null, null);
			resource.addComp(this.appComp);
			return;
		}
		if (ct.isPreloaded() == false) {
			/*
			 * we will go by convention to get the id of this component, rather
			 * than actually reading it.
			 *
			 * first chop the folder prefix at the beginning
			 */
			String prefix = AppConventions.Name.COMP_FOLDER_NAMES[ct.getIdx()];
			String id = path.substring(prefix.length());
			/*
			 * now chop ".xml" at the end
			 */
			id = id.substring(0, id.length() - EXTENSION_WITH_DOT.length());
			id = id.replace('/', '.');
			this.newComp(resource, ct, id);
			return;
		}
		try {
			Map<String, Object> objects = new HashMap<>();
			String packageName = ct.getCompClass().getPackage().getName() + '.';
			XmlUtil.xmlToCollection(stream, objects, packageName);
			for (Object obj : objects.values()) {
				if (obj != null) {
					IComponent component = (IComponent) obj;
					this.newComp(resource, ct, component.getQualifiedName());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Unable to load resource {}. Error:{}", path, e.getMessage());
		}
	}

	private Comp newComp(Resource resource, ComponentType ct, String id) {
		Map<String, Comp> comps = this.allComps[ct.getIdx()];
		Comp comp = comps.get(id);
		if (comp == null) {
			comp = new Comp(resource, ct, id);
			comps.put(id, comp);
			resource.addComp(comp);
			return comp;
		}

		if (comp.getResource() == null) {
			comp.setResource(resource);
			resource.addComp(comp);
			return comp;
		}

		logger.error("Component {} of type{} is already loaded from {} and hence it is skipped from resource {}",
				comp.getCompId(), comp.getComponentType(), comp.getResource().getPath(), resource.getPath());
		return null;
	}

	private void resourceRemoved(String path) {
		Resource resource = this.resources.remove(path);
		if (resource == null) {
			return;
		}

		Comp[] removedComps = resource.delete();
		if (removedComps == null || removedComps.length == 0) {
			return;
		}

		ComponentType ct = resource.getComponentType();
		if (ct == null) {
			this.appComp = null;
			return;
		}

		int idx = ct.getIdx();
		Map<String, Comp> comps = this.allComps[idx];

		for (Comp comp : removedComps) {
			comps.remove(comp.getCompId());
		}
	}

	/**
	 * convenient class to get component type that can be null for application.
	 */
	protected static class CompType {
		protected ComponentType ct;

		protected CompType(ComponentType ct) {
			this.ct = ct;
		}
	}

	private static CompType getCompType(String path) {
		if (path.endsWith(EXTENSION_WITH_DOT) == false) {
			return null;
		}
		int idx = 0;
		for (String prefix : Constants.COMP_FOLDER_NAMES) {
			if (path.startsWith(prefix)) {
				return new CompType(Constants.COMP_TYPES[idx]);
			}
			idx++;
		}
		if (path.equals(Constants.Names.APP_RESOURCE_NAME)) {
			return new CompType(null);
		}
		return null;
	}

	private void validateApp(InputStream stream, IValidationListener listener) throws XmlParseException {
		Application app = new Application();
		Map<Object, String> lineNumbers = new HashMap<>();
		Document xml = XmlParser.readXmlWithLineNumbers(stream, XmlUtil.ATTR_LINE_NUMBER);
		XmlUtil.elementToObject(xml.getDocumentElement(), app, lineNumbers);
		this.validateComp(app, listener, lineNumbers);
	}

	private void validateSingle(ComponentType ct, InputStream stream, IValidationListener listener)
			throws XmlParseException, InstantiationException, IllegalAccessException {

		IComponent component = (IComponent) ct.getCompClass().newInstance();
		Map<Object, String> lineNumbers = new HashMap<>();
		Document xml = XmlParser.readXmlWithLineNumbers(stream, XmlUtil.ATTR_LINE_NUMBER);
		XmlUtil.elementToObject(xml.getDocumentElement(), component, lineNumbers);
		this.validateComp(component, listener, lineNumbers);
	}

	private void validateMulti(ComponentType ct, InputStream stream, IValidationListener listener)
			throws XmlParseException {
		Map<String, Object> objects = new HashMap<>();
		Map<Object, String> lineNumbers = new HashMap<>();
		Document xml = XmlParser.readXmlWithLineNumbers(stream, XmlUtil.ATTR_LINE_NUMBER);
		String packageName = ct.getCompClass().getPackage().getName() + '.';
		XmlUtil.elementToCollection(xml.getDocumentElement(), objects, packageName, lineNumbers);

		for (Object obj : objects.values()) {
			if (obj != null) {
				IComponent component = (IComponent) obj;
				this.validateComp(component, listener, lineNumbers);
			}
		}
	}

	private void validateComp(Object object, IValidationListener listener, Map<Object, String> lineNumbers) {
		IValidationContext vtx = new IValidationContext() {
			@Override
			public void reference(ValidationReference reference) {
				ValidationMessage message = Repository.this.checkReference(reference, lineNumbers);
				if (message != null) {
					listener.message(message);
				}
			}

			@Override
			public void message(ValidationMessage message) {
				addLineNo(message, lineNumbers);
				listener.message(message);
			}
		};

		if (object instanceof IComponent) {
			((IComponent) object).validate(vtx);
		} else {
			((Application) object).validate(vtx);
		}
	}

	protected ValidationMessage checkReference(ValidationReference ref, Map<Object, String> lineNumbers) {
		ComponentType ct = ref.componentType;
		if (ct == null) {
			logger.debug("We didn't expect reference to applicaiton.xml");
			return null;
		}

		String id = ref.qualifiedName;
		int idx = ct.getIdx();

		Map<String, Comp> comps = this.allComps[idx];
		Comp refComp = comps.get(id);
		if (refComp != null && refComp.getResource() != null) {
			return null;
		}
		ValidationMessage msg = new ValidationMessage(ref.referringObject, ValidationMessage.SEVERITY_ERROR,
				"Component named " + id + " of type " + ct + " is referred here, but it is not defined.",
				ref.fieldName);
		addLineNo(msg, lineNumbers);
		return msg;
	}

	protected static void addLineNo(ValidationMessage msg, Map<Object, String> lineNumbers) {
		Object ref = msg.refObject;
		if (ref == null) {
			return;
		}
		/*
		 * object has no meaning for the listener. its sole purpose was to map
		 * the message to a line number. release it as a good practice, becaue
		 * the object could be heavy
		 */
		msg.refObject = null;

		String text = lineNumbers.get(ref);
		if (text == null || text.equals("null")) {
			return;
		}
		try {
			msg.lineNo = Integer.parseInt(text);
		} catch (NumberFormatException e) {
			logger.error("object {} had an invalid lineNumber : {} " + ref, text);
		}
	}

	/**
	 * just log statistics
	 */
	public void logStats() {
		logger.info("Repository is built with components:");
		for (ComponentType ct : ComponentType.values()) {
			logger.info("{} : {}", ct, this.allComps[ct.getIdx()].size());
		}

	}

	@Override
	public String[] getSuggestedComps(String fieldName, String typedValue) {
		ComponentType ct = FIELD_COMP_XREF.get(fieldName);
		if (ct == null) {
			return null;
		}
		/*
		 * as of now we just send all of them.. we do not match starting etc...
		 */
		String[] comps = this.allComps[ct.getIdx()].keySet().toArray(new String[0]);
		Arrays.sort(comps);
		return comps;
	}

	@Override
	public String getResourceForComp(String fieldName, String compName) {
		ComponentType ct = FIELD_COMP_XREF.get(fieldName);
		if (ct == null) {
			return null;
		}

		Comp comp = this.allComps[ct.getIdx()].get(compName);
		if (comp == null) {
			return null;
		}

		Resource res = comp.getResource();
		if (res == null) {
			return null;
		}

		return res.getPath();
	}
}
