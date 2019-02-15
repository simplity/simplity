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

package org.simplity.core.trans;

import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.jms.JmsDestination;
import org.simplity.core.service.ServiceContext;

/** @author simplity.org */
public class JmsConsumer extends Block {
	/** queue from which to consume requests to be processed as requests */
	@FieldMetaData(isRequired = true)
	JmsDestination requestDestination;
	/** optional queue on which responses to be sent on */
	JmsDestination responseDestination;

	/** should we consume just one or all of them on the queue? */
	boolean consumeAll;

	/**
	 * true means wait for the message. consumeAll=false means wait for one, but
	 * then come out. consumeAll=true means keep listening till cows come home
	 * :-). false means do not wait, even for one.
	 */
	boolean waitForMessage;

	@Override
	protected String execute(ServiceContext ctx, IDbHandle dbHandle, boolean transIsDelegated) {
		BlockWorker worker = new BlockWorker(this.actions, this.indexedActions, ctx, dbHandle,
				transIsDelegated);
		this.requestDestination.consume(
				ctx, worker, this.responseDestination, this.consumeAll, this.waitForMessage);
		return worker.getWhatNext();
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
