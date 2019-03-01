package org.simplity.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class Plugin extends AbstractUIPlugin {
	protected static final Logger logger = LoggerFactory.getLogger(Plugin.class);
	/**
	 * The shared instance
	 */
	private static Plugin plugin;

	/**
	 * The constructor
	 */
	public Plugin() {
		super();
		logger.info("Simplity plugin created");
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(new ResourceChangeListener(),
				IResourceChangeEvent.POST_CHANGE);
		/*
		 * we need to keep our repo updated whenever any resource is modified.
		 * We register our listener for the same
		 */
		logger.info("Simplity plugin started");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		logger.info("Simplity plugin stopped");
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Plugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(Constants.Ids.PLUGIN, path);
	}

	protected static class ResourceChangeListener implements IResourceChangeListener {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event == null) {
				return;
			}
			IResourceDelta delta = event.getDelta();
			if (delta == null) {
				return;
			}
			try {
				delta.accept(resDelta -> {
					int kind = resDelta.getKind();
					int flags = resDelta.getFlags();
					IResource resource = resDelta.getResource();
					switch (resource.getType()) {
					case IResource.ROOT:
						return true;

					case IResource.PROJECT:
						IProject project = (IProject) resource;
						return EclipseUtil.isSimplityProject(project);

					case IResource.FOLDER:
						return EclipseUtil.isSimplityFolder(resource.getProject(),
								resource.getProjectRelativePath().toString());

					case IResource.FILE:
						return handleFile(delta, (IFile) resource, kind, flags);

					default:
						break;
					}
					logger.error("We are not designed to process resource {}", resource.getFullPath());
					return false;
				});
			} catch (CoreException e) {
				logger.error("Core exception while processing resource change : {}", e.getMessage());
			}
			return;
		}
	}

	protected static boolean handleFile(IResourceDelta delta, IFile file, int kind, int flags) {
		if ((kind & IResourceDelta.CHANGED) != 0) {
			if (flags == 0 || (flags & IResourceDelta.CONTENT) != 0) {
				EclipseUtil.fileChanged(file);
				return true;
			}
			if ((flags & IResourceDelta.MOVED_TO) != 0) {
				// this resource is moved.we will deal with that
				// when we visit moved-to-resource
				return true;
			}
			if ((flags & IResourceDelta.MOVED_FROM) != 0) {
				EclipseUtil.fileReplaced(file, delta.getMovedFromPath().toString());
				return true;
			}
			EclipseUtil.fileChanged(file);
			return true;

		}
		if ((kind & IResourceDelta.ADDED) != 0) {
			EclipseUtil.fileAdded(file);
			return true;
		}
		if ((kind & IResourceDelta.REMOVED) != 0) {
			EclipseUtil.fileRemoved(file);
			return true;
		}
		logger.error("Resource {} had an event kind : {} which we are not designed to handle. event skipped",
				file.getFullPath(), kind);
		return false;
	}
}
