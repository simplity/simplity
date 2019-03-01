package org.simplity.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We are not sure whether this is an over-kill at this time, but we have a
 * nature specifically for Simplity.
 * Once we standardize on Maven, may be maven arch-type is more appropriate..
 *
 * @author simplity.org
 *
 */
public class Nature implements IProjectNature {
	private static final Logger logger = LoggerFactory.getLogger(Nature.class);

	private IProject project;

	@Override
	public void configure() throws CoreException {
		logger.info("Simplity Nature is being configured");
		return;
	}

	@Override
	public void deconfigure() throws CoreException {
		logger.info("Simplity Nature is being de-configured");
		return;
	}

	@Override
	public IProject getProject() {
		return this.project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
		logger.info("Project {} is marked as a Simplity Project", project.getName());
	}
}
