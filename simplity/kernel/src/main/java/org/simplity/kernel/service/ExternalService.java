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

package org.simplity.kernel.service;

import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IComponent;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationMessage;
import org.simplity.kernel.comp.ValidationUtil;

/**
 * Represents an external service dependency for this app.
 *
 * @author simplity.org
 *
 */
public class ExternalService implements IComponent {

	/**
	 * name of service. unique for an applicationName
	 */
	@FieldMetaData(isRequired = true)
	String serviceName;
	/**
	 * external application that serves this service.
	 */
	@FieldMetaData(isRequired = true)
	String applicationName;

	/**
	 * data with which to make a request for this service
	 */
	OutputData requestData;
	/**
	 * Data that this service responds back with
	 */
	InputData responseData;

	@Override
	public void getReady() {
		this.requestData.getReady();
		this.responseData.getReady();
	}

	@Override
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.requestData == null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_INFO,
					"This service is not expecting any data.", "requestData"));
		} else {
			this.requestData.validate(vtx);
		}
		if (this.responseData == null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_INFO,
					"This service is not outputting any data.", "responseData"));
		} else {
			this.responseData.validate(vtx);
		}
	}

	@Override
	public String getSimpleName() {
		return this.serviceName;
	}

	@Override
	public String getQualifiedName() {
		return this.applicationName + '.' + this.serviceName;
	}

	@Override
	public ComponentType getComponentType() {
		return ComponentType.EXTERN;
	}

	/**
	 *
	 * @return specification of expected data that is received as
	 *         payload/response from this service
	 */
	public InputData getResponseSpec() {
		return this.responseData;
	}

	/**
	 *
	 * @return specification of expected data to be sent as payload/request for
	 *         this service
	 */
	public OutputData getRequestSpec() {
		return this.requestData;
	}
}
