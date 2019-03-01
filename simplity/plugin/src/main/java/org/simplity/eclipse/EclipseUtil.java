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

package org.simplity.eclipse;

import java.io.InputStream;
import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidatorMessage;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.ide.comp.IRepository;
import org.simplity.ide.comp.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utilities related to plugin development
 *
 * @author simplity.org
 *
 */
@SuppressWarnings("restriction")
public class EclipseUtil {
	protected static final Logger logger = LoggerFactory.getLogger(EclipseUtil.class);
	/**
	 * property name for root folder
	 */
	private static final QualifiedName SIMPLITY_ROOT = new QualifiedName(Constants.Ids.PLUGIN, "simplityRoot");
	private static final QualifiedName REPOSITORY = new QualifiedName(Constants.Ids.PLUGIN, "repository");

	/**
	 * get the project associated with the current event, or with the active
	 * editor
	 *
	 * @param event
	 *            non-null if you are processing an event. null if an active
	 *            editor is present, and your code is doing something with that
	 * @return project, if this event is associated with an event, null
	 *         otherwise
	 */
	public static IProject getProject(ExecutionEvent event) {
		if (event != null) {
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			if (selection instanceof IStructuredSelection) {
				for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
					IProject project = getProject(it.next());
					if (project != null) {
						return project;
					}
				}
			}
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IEditorInput input = window.getActivePage().getActiveEditor().getEditorInput();
			{
				if (input instanceof FileEditorInput) {
					return ((FileEditorInput) input).getFile().getProject();
				}
			}
		}
		logger.info("Project could not be determined based on the context");
		return null;
	}

	/**
	 * gets the IProject as cast from the object, if possible.
	 *
	 * @param element
	 *            instance that could be project
	 * @return project or null
	 */
	public static IProject getProject(Object element) {
		if (element == null) {
			return null;
		}
		if (element instanceof IProject) {
			return (IProject) element;
		}
		if (element instanceof IAdaptable) {
			IProject project = ((IAdaptable) element).getAdapter(IProject.class);
			if (project != null) {
				return project;
			}
		}
		return null;
	}

	/**
	 * get current shell
	 *
	 * @return current shell or null
	 */

	public static Shell getActiveShell() {
		Display display = Display.getDefault();
		Shell shell = display.getActiveShell();
		if (shell != null) {
			return shell;
		}
		Shell[] shells = display.getShells();
		for (Shell sh : shells) {
			if (sh.getShells().length == 0) {
				if (shell != null) {
					return null;
				}
				shell = sh;
			}
		}

		return shell;
	}

	/**
	 * get root folder for Simplity
	 *
	 * @param project
	 * @return root folder (relative to project root) or null if it is not set
	 */
	public static String getSimplityRoot(IProject project) {
		if (!isSimplityProject(project)) {
			return null;
		}
		try {
			return project.getPersistentProperty(SIMPLITY_ROOT);
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * set simplity root folder for this project
	 *
	 * @param project
	 * @param root
	 *            null if it is to be removed
	 */
	public static void setSimplityRoot(IProject project, String root) {
		try {
			project.setPersistentProperty(SIMPLITY_ROOT, root);
		} catch (CoreException e) {
			// ;
		}
	}

	/**
	 *
	 * @param resource
	 * @return path of this resource relative to Simplity root. Null if SImplity
	 *         Root is not set, or this is not a file, or this is not inside
	 *         simplity root folder
	 */
	public static String getSimplityRelativePath(IResource resource) {
		IProject project = resource.getProject();
		String root = getSimplityRoot(project);
		if (root == null) {
			return null;
		}

		if (resource.getType() != IResource.FILE) {
			return null;
		}
		String path = resource.getProjectRelativePath().toString();
		if (path.startsWith(root) == false) {
			return null;
		}

		return path.substring(root.length() + 1);
	}

	/**
	 *
	 * @param project
	 * @param repository
	 */
	public static void setRepository(IProject project, Repository repository) {
		try {
			project.setSessionProperty(REPOSITORY, repository);
		} catch (CoreException e) {
			logger.error("Error while setting project session attribute. {}", e.getMessage());
		}
	}

	/**
	 *
	 * @param project
	 * @param forceCreation
	 *            if set to true, repository is created if required.
	 * @return repository, or null if it not forced and not set, or any
	 *         exception
	 */
	public static IRepository getRepository(IProject project, boolean forceCreation) {
		IFolder folder = getSimplityRootFolder(project);
		if (folder == null) {
			logger.error("No valid root folder for project {}. Repository not created. ", project.getName());
			return null;
		}
		IRepository repo = null;
		try {
			Object obj = project.getSessionProperty(REPOSITORY);
			if (obj == null) {
				if (forceCreation) {
					repo = createRepo(folder);
					project.setSessionProperty(REPOSITORY, repo);
				}
			} else if (obj instanceof Repository) {
				repo = (Repository) obj;
			} else {
				logger.error("project attribute named {} should be a Repository, but it turned out to be {}",
						REPOSITORY.toString(), obj.getClass().getName());
			}
		} catch (CoreException e) {
			logger.error("Error while retrieving project session attribute. {}", e.getMessage());
		}
		return repo;
	}

	/**
	 * get root folder for simplity components
	 *
	 * @param project
	 * @return root folder, or null if simplity root is not set
	 */
	public static IFolder getSimplityRootFolder(IProject project) {
		if (!isSimplityProject(project)) {
			return null;
		}
		String path = getSimplityRoot(project);
		if (path == null) {
			return null;
		}
		return project.getFolder(path);
	}

	/**
	 * is simplity nature enabled for this project?
	 *
	 * @param project
	 * @return true is simplity nature is enabled. false otherwise
	 */
	public static boolean isSimplityProject(IProject project) {
		try {
			return project.getNature(Constants.Ids.NATURE) != null;
		} catch (CoreException e) {
			// What can we really do??
		}
		return false;
	}

	/**
	 * @param project
	 * @param folderPath
	 *            relative to project
	 * @return true if this folder is of concern to simplity. false otherwise
	 */
	public static boolean isSimplityFolder(IProject project, String folderPath) {
		String root = getSimplityRoot(project);
		if (root != null) {
			return folderPath.startsWith(root) || root.startsWith(folderPath);
		}
		return false;
	}

	/**
	 * is this resource a simplity component?
	 *
	 * @param resource
	 * @return true if this indeed is a Simplity component. False, if simplity
	 *         root is not set, or this resource is not inside the simplity root
	 *         folder
	 */
	public static boolean isSimplityResource(IResource resource) {
		IProject project = resource.getProject();
		String root = getSimplityRoot(project);
		if (root == null) {
			return false;
		}

		if (resource.getType() != IResource.FILE) {
			return false;
		}
		return resource.getProjectRelativePath().toString().startsWith(root);
	}

	/**
	 * create a repository with the folder as the root
	 *
	 * @param folder
	 * @return repository, or null in case of exceptions
	 */
	public static IRepository createRepo(IFolder folder) {
		String rootPath = folder.getProjectRelativePath().toString();
		if (rootPath.endsWith("/") == false) {
			rootPath += '/';
		}
		Repository repo = new Repository(rootPath);
		int len = rootPath.length();
		try {
			folder.accept(resource -> {
				if (resource instanceof IFile) {
					String path = resource.getProjectRelativePath().toString().substring(len);
					try (InputStream stream = ((IFile) resource).getContents()) {
						if (stream == null) {
							logger.error("Unable to read resource {}. Skipped.", path);
						} else {
							repo.addResource(path, stream);
						}
					} catch (Exception e) {
						logger.error("Unable to read resource {}. Error : {}", path, e.getMessage());
					}
				}

				return true;
			});

		} catch (CoreException e) {
			logger.error("Exception while creating repository. {}", e.getMessage());
			return null;
		}
		repo.logStats();
		return repo;
	}

	/**
	 * @param project
	 * @throws CoreException
	 */
	public static void projectClosed(IProject project) throws CoreException {
		project.setPersistentProperty(REPOSITORY, null);
	}

	/**
	 * @param file
	 */
	public static void fileChanged(IFile file) {
		String path = getSimplityRelativePath(file);
		if (path == null) {
			return;
		}
		try (InputStream stream = file.getContents()) {
			if (stream == null) {
				logger.error("Unable to read contents of file {} ", path);
				return;
			}
			IProject project = file.getProject();
			IRepository repo = getRepository(project, true);
			if (repo == null) {
				return;
			}
			repo.modifyResource(path, stream);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("unable to add file {} : {}", path, e.getMessage());
		}
	}

	/**
	 * @param file
	 */
	public static void fileRemoved(IFile file) {
		String path = getSimplityRelativePath(file);
		if (path == null) {
			return;
		}
		IProject project = file.getProject();
		IRepository repo = getRepository(project, true);
		if (repo == null) {
			return;
		}
		repo.removeResource(path);
	}

	/**
	 * @param file
	 */
	public static void fileAdded(IFile file) {
		String path = getSimplityRelativePath(file);
		if (path == null) {
			return;
		}
		logger.info("Add - file {} ", path);
		try (InputStream stream = file.getContents()) {
			if (stream == null) {
				logger.error("Unable to read contents of file {} ", path);
				return;
			}
			IProject project = file.getProject();
			IRepository repo = getRepository(project, true);
			if (repo == null) {
				return;
			}
			repo.addResource(path, stream);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("unable to add file {} : {}", path, e.getMessage());
		}
	}

	/**
	 * @param file
	 * @param oldPath
	 */
	public static void fileReplaced(IFile file, String oldPath) {
		String path = getSimplityRelativePath(file);
		if (path == null) {
			return;
		}
		try (InputStream stream = file.getContents()) {
			if (stream == null) {
				logger.error("Unable to read contents of file {} ", path);
				return;
			}
			IProject project = file.getProject();
			IRepository repo = getRepository(project, true);
			if (repo == null) {
				return;
			}
			repo.replaceResource(path, oldPath, stream);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("unable to add file {} : {}", path, e.getMessage());
		}
	}

	/**
	 * @param project
	 * @throws CoreException
	 */
	public static void projectOpened(IProject project) throws CoreException {
		// we don't do anything
	}

	/**
	 *
	 * @param file
	 * @param result
	 */
	public static void validateResource(IFile file, ValidationResult result) {
		String path = getSimplityRelativePath(file);
		if (path == null) {
			return;
		}
		IProject project = file.getProject();
		try (InputStream stream = file.getContents()) {
			if (stream == null) {
				logger.error("Unable to read contents of file {} ", path);
				return;
			}
			IRepository repo = getRepository(project, true);
			if (repo == null) {
				return;
			}
			repo.validateResource(path, stream, message -> {
				result.add(createMessage(file, message));
			});

		} catch (Exception e) {
			logger.error("Unable to validate file {} : {}", path, e.getMessage());
		}
	}

	/**
	 *
	 * @param res
	 * @param message
	 * @return message
	 */
	public static ValidatorMessage createMessage(IResource res, ValidationMessage message) {
		ValidatorMessage msg = ValidatorMessage.create(message.messageText, res);
		msg.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		int lineNo = message.lineNo;
		if (lineNo <= 0) {
			lineNo = 1;
		}
		msg.setAttribute(IMarker.LINE_NUMBER, lineNo);
		return msg;
	}

	/**
	 * get a list of comps that are valid for the attribute name.
	 *
	 * @param fieldName
	 *            attribute name for which values are suggested
	 * @param typedValue
	 *            current value for thus attribute. Not used as of now, but we
	 *            will use it later.
	 * @return List of valid component names. all possible valid values for this
	 *         attribute. null if this attribute does not refer to any
	 *         component. Empty string if there are no valid suggestions.
	 */
	public static String[] getSuggestedComps(String fieldName, String typedValue) {
		IProject project = getProject(null);
		if (project == null) {
			return null;
		}
		IRepository repo = getRepository(project, true);
		return repo.getSuggestedComps(fieldName, typedValue);
	}

	/**
	 * get fields for an attribute-value region when cursor is on such a region
	 *
	 * @param viewer
	 * @param cursorAt
	 * @return Object with fields populated. null if cursor is not within an
	 *         attribute-value region.
	 */
	public static XmlAttributeDetails getAttributeDetails(ITextViewer viewer, int cursorAt) {
		IStructuredDocumentRegion docRegion = ContentAssistUtils.getStructuredDocumentRegion(viewer, cursorAt);
		ITextRegion valueRegion = docRegion.getRegionAtCharacterOffset(cursorAt);

		if (!valueRegion.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE)) {
			return null;
		}

		ITextRegionList regions = docRegion.getRegions();
		int idx = regions.indexOf(valueRegion);
		if (idx < 3) {
			return null;
		}

		idx = idx - 2;
		ITextRegion nameRegion = regions.get(idx);
		if (!nameRegion.getType().equals(DOMRegionContext.XML_TAG_ATTRIBUTE_NAME)) {
			return null;
		}

		int offset = docRegion.getStartOffset() + valueRegion.getStart();
		String value = docRegion.getText(valueRegion);

		char openingQuote = 0;
		if (value.isEmpty() == false) {
			openingQuote = value.charAt(0);

			if (openingQuote == '"' || openingQuote == '\'') {
				if (value.length() == 1) {
					value = "";
				} else {
					value = value.substring(1);
				}
			} else {
				openingQuote = 0;
			}
		}

		char closingQuote = 0;
		if (value.isEmpty() == false) {
			int len = value.length() - 1;
			closingQuote = value.charAt(len);
			if (closingQuote == '"' || closingQuote == '\'') {
				if (value.length() == 1) {
					value = "";
				} else {
					value = value.substring(0, len);
				}
			} else {
				closingQuote = 0;
			}
		}

		return new XmlAttributeDetails(docRegion.getText(nameRegion), value, offset, openingQuote, closingQuote);
	}

	/**
	 * fields that are relevant when we want to deal with a attribute-value
	 * region in an xml editor context
	 *
	 * @author simplity.org
	 *
	 */
	public static class XmlAttributeDetails {
		/**
		 * name of attribute.
		 */
		public final String name;

		/**
		 * attribute value without the enclosing quotes. could be empty string
		 */
		public final String value;

		/**
		 * absolute offset of the start of attribute value (falls on the opening
		 * quote)
		 */
		public final int offset;

		/**
		 * 0 (null) if there is no quote. ' or " otherwise
		 */
		public char openingQuote;

		/**
		 * 0 (null) if there is no quote. ' or " otherwise
		 */
		public char closingQuote;

		protected XmlAttributeDetails(String attName, String attValue, int offset, char openingQuote,
				char closingQuote) {
			this.name = attName;
			this.value = attValue;
			this.offset = offset;
			this.openingQuote = openingQuote;
			this.closingQuote = closingQuote;
		}

	}

	/**
	 * gets resource name in which the referred comp is defined.
	 *
	 * @param fieldName
	 *            field name that refers to the comp
	 * @param compName
	 *            fully qualified name of the comp
	 * @return path relative to the repository root in which this comp is
	 *         defined. null if this comp is not defined. if multiple comps are
	 *         defined in a resource, there is no information about the location
	 *         at this time. We intend to extend this into path with #tag for
	 *         location information within the resource
	 */
	public static String getResourceForComp(String fieldName, String compName) {
		IProject project = getProject(null);
		if (project == null) {
			return null;
		}
		IRepository repo = getRepository(project, true);
		return repo.getResourceForComp(fieldName, compName);
	}

	/**
	 * @param resourceName
	 *            during an active editor session. THIS WORKS ONLY WHEN an
	 *            editor is active.
	 */
	public static void openCompResource(String resourceName) {
		IProject project = null;
		IWorkbenchPage page = null;
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			page = window.getActivePage();
			IEditorInput input = window.getActivePage().getActiveEditor().getEditorInput();
			if (input instanceof FileEditorInput == false) {
				logger.error("input is an instance of {} and not FileEditorInput. Unable to get project",
						input.getClass().getName());
				return;
			}
			project = ((FileEditorInput) input).getFile().getProject();
		} catch (Exception e) {
			logger.error("Unable to get the project. Probably the context is not an editor.");
			return;
		}

		if (project == null || page == null) {
			logger.error("Unable to get the project or the active page");
			return;
		}
		IFolder folder = getSimplityRootFolder(project);
		if (folder == null) {
			logger.error("Unable to get root folder for project {}", project.getName());
			return;
		}
		IFile file = folder.getFile(resourceName);
		if (file.exists() == false) {
			logger.error("Resource {} does not exist insode root folder {}", resourceName,
					folder.getProjectRelativePath());
			return;
		}
		IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());

		try {
			page.openEditor(new FileEditorInput(file), desc.getId());
		} catch (PartInitException e) {
			logger.error("Unable to open default editor {} for file {} . error : {}", desc.getId(), file.getFullPath(),
					e.getMessage());
		}
	}
}
