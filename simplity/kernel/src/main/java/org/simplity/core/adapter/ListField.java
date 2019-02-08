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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.adapter;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * field that has a list of objects
 *
 * @author simplity.org
 *
 */
public class ListField extends AbstractField {
	private static final Logger logger = LoggerFactory.getLogger(ListField.class);

	/**
	 * class name of the target list member. Required in case the target saves
	 * in a generic list and has no meta data about the member types.
	 */
	String targetListMemberClassName;
	/**
	 * fields to be copied for each object in this list. optional if
	 * referredAdapter is specified
	 */
	AbstractField[] fields;

	/**
	 * adapter from which to copy/include fields into this specification
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.ADAPTER)
	String referredAdapter;

	/**
	 * validate the component
	 */
	@Override
	public void getReady() {
		super.getReady();
		if (this.referredAdapter != null) {
			DataAdapter ref = Application.getActiveInstance().getDataAdapter(this.referredAdapter);
			this.fields = ref.getFields();
		} else {
			if (this.fields == null) {
				throw new ApplicationError("No child fields specified for list field " + this.fromName);
			}
			for (AbstractField field : this.fields) {
				field.getReady();
			}
		}
	}

	@Override
	public void copy(IDataSource source, IDataTarget target, ServiceContext ctx) {
		IDataListSource sourceList = source.getChildListSource(this.fromName);
		if (sourceList == null) {
			logger.info("No list source named {}. Data not copied", this.fromName);
			return;
		}
		int nbrRows = sourceList.length();
		if (nbrRows == 0) {
			return;
		}

		IDataListTarget targetList = target.getChildListTarget(this.toName, this.targetListMemberClassName);
		if (targetList == null) {
			logger.info("No target list field named {}. Data not copied", this.toName);
			return;
		}

		for (int i = 0; i < nbrRows; i++) {
			IDataSource src = sourceList.getChildSource(i);
			if (src == null) {
				logger.info("element at 0-based index {} is null in source {}. element skipped", i,
						this.fromName);
				continue;
			}

			IDataTarget t = targetList.getChildTarget(i);
			if (t == null) {
				logger.info("Target list field {} is unable to receive data for 0-based index {}. element skipped",
						this.toName, i);
				continue;
			}
			for (AbstractField field : this.fields) {
				field.copy(src, t, ctx);
			}
		}
		/*
		 * important to invoke end of list
		 */
		targetList.listDone();
	}

	@Override
	protected void validate(IValidationContext vtx) {
		super.validate(vtx);
		ValidationUtil.validateMeta(vtx, this);
		if (this.fields != null && this.referredAdapter != null) {
			for (AbstractField field : this.fields) {
				field.validate(vtx);
			}
		}
	}
}
