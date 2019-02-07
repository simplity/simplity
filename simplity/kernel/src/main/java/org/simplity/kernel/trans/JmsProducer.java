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

package org.simplity.kernel.trans;

import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationMessage;
import org.simplity.kernel.jms.JmsDestination;
import org.simplity.kernel.service.ServiceContext;

/** @author simplity.org */
public class JmsProducer extends AbstractNonDbAction {

	/** queue to be used to send a message as request */
	@FieldMetaData(isRequired = true)
	JmsDestination requestDestination;

	/** queue to be used to get back a response. optional. */
	JmsDestination responseDestination;

	@Override
	protected boolean act(ServiceContext ctx) {
		return this.requestDestination.produce(ctx, this.responseDestination);
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);

		this.requestDestination.getReady();
		if (this.responseDestination != null) {
			this.responseDestination.getReady();
		}
	}

	@Override
	public void validateSpecific(IValidationContext vtx, TransactionProcessor task) {
		super.validateSpecific(vtx, task);
		if (this.requestDestination != null) {
			if (this.requestDestination.getName() == null) {
				vtx.message(new ValidationMessage(this.requestDestination, ValidationMessage.SEVERITY_ERROR,
						"queName is required for requestQueue", "name"));
			}
			this.requestDestination.validate(vtx, true);
		}
		if (this.responseDestination != null) {
			this.responseDestination.validate(vtx, false);
		}
		if (task.getJmsUsage() == null) {
			vtx.message(new ValidationMessage(task, ValidationMessage.SEVERITY_ERROR,
					"Service uses JMS but has not specified jmsUsage attribute.", "jmsUsage"));
		}
	}
}
