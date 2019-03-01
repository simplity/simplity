package org.simplity.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * validator for simplity components
 *
 * @author simplity.org
 *
 */
public class Validator extends AbstractValidator {
	protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

	/**
	 * @return name of this validator
	 */
	public String getValidatorName() {
		return "Simplity Validator";
	}

	@Override
	public ValidationResult validate(IResource resource, int kind, ValidationState state, IProgressMonitor monitor) {
		ValidationResult result = new ValidationResult();
		EclipseUtil.validateResource((IFile) resource, result);
		return result;
	}
}