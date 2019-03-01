package org.simplity.eclipse;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.slf4j.Logger;

/**
 * Handler for command "toggle". Enable/Disable simplity nature
 *
 * @author simplity.org
 *
 */
public class ToggleHandler extends AbstractHandler {
	protected static final Logger logger = org.slf4j.LoggerFactory.getLogger(ToggleHandler.class);
	private static final String ENABLED = "Simplity nature enabled.";
	private static final String DISABLED = "Simplity nature disabled.";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = EclipseUtil.getProject(event);
		if (project == null) {
			return null;
		}
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			if (natures == null) {
				natures = new String[0];
			}
			int idx = getIdx(natures);
			int n = natures.length;
			String[] newNatures;
			String msg;
			if (idx == -1) {
				// append our nature
				newNatures = new String[n + 1];
				System.arraycopy(natures, 0, newNatures, 0, n);
				newNatures[n] = Constants.Ids.NATURE;
				validateRoot(project);
				msg = ENABLED;
			} else {
				// remove our nature
				newNatures = new String[n - 1];
				System.arraycopy(natures, 0, newNatures, 0, idx);
				System.arraycopy(natures, idx + 1, newNatures, idx, n - idx - 1);
				msg = DISABLED;
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
			logger.info(msg);
		} catch (CoreException e) {
			throw new ExecutionException(e.getMessage());
		}
		return null;
	}

	/**
	 * @param project
	 * @throws CoreException
	 */
	private static void validateRoot(IProject project) throws CoreException {
		String rootName = EclipseUtil.getSimplityRoot(project);
		if (rootName != null) {
			if (project.getFolder(rootName).exists()) {
				logger.info("{} is a valid path, and hence it is retained as SimplityRoot.", rootName);

			} else {
				logger.info("{} is not a valid path, and hence this existing entry is discarded for SimplityRoot.",
						rootName);
				rootName = null;
			}
		}
		if (rootName == null) {
			rootName = getPossibleRootFolders(project);
		}
		String msg;
		if (rootName == null) {
			msg = "Select simplity root folder using project->properties->Simplity";
		} else {
			EclipseUtil.setSimplityRoot(project, rootName);
			msg = "Simplity root set to " + rootName + ". You may change it using project->properties->Simplity";
		}
		// TODO: display this message
		logger.info(msg);
	}

	/**
	 *
	 * @param natures
	 * @return index at which simplity nature is found, -1 simplity nature is
	 *         not
	 *         found
	 */
	private static int getIdx(String[] natures) {
		for (int i = 0; i < natures.length; i++) {
			if (Constants.Ids.NATURE.equals(natures[i])) {
				return i;
			}
		}
		return -1;
	}

	private static String getPossibleRootFolders(IProject project) throws CoreException {
		// TODO : how do I ask the visitor to abandon visits and come out
		// We have a WRONG design right now. We throw exception when we
		// succeed!!!
		try {
			project.accept(res -> {
				if (res.getType() == IResource.FOLDER) {
					IFolder folder = (IFolder) res;
					if (folder.getName().startsWith(".")) {
						return false;
					}
					if (folder.exists((Constants.Names.APP_CONFIG_PATH))) {
						throw new SuccessException(res.getProjectRelativePath().toString());
					}
				}
				return true;
			});
		} catch (SuccessException e) {
			return e.getFolderName();
		}
		return null;
	}

	static class SuccessException extends CoreException {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private String folderName;

		/**
		 * @param status
		 */
		public SuccessException(IStatus status) {
			super(status);
		}

		/**
		 * @param folderName
		 */
		public SuccessException(String folderName) {
			super(new Status(IStatus.OK, Constants.Ids.PLUGIN, "all OK"));
			this.folderName = folderName;
		}

		/**
		 * @return the folderName
		 */
		public String getFolderName() {
			return this.folderName;
		}
	}
}