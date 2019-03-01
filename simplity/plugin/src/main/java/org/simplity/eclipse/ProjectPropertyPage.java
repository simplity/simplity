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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * @author simplity.org
 *
 */
public class ProjectPropertyPage extends PropertyPage {
	protected Text rootField;
	private static final String LABEL = "Simplity Folder:";
	private static final String HELP = "(relative to project-root e.g. src/main/resources/res/";

	@Override
	public Control createContents(Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		createLabel(parent, LABEL);
		this.rootField = new Text(parent, SWT.BORDER);
		createLabel(parent, HELP);

		String root = this.getCurrentRoot();
		if (root != null) {
			this.rootField.setText(root);
		}
		this.rootField.addModifyListener(event -> {
			ProjectPropertyPage.this.setCurrentRoot(ProjectPropertyPage.this.rootField.getText());

		});
		return new Canvas(parent, 0);
	}

	/**
	 * Utility method that creates a new label and sets up its layout data.
	 *
	 * @param parent
	 *            the parent of the label
	 * @param text
	 *            the text of the label
	 * @return the newly-created label
	 */
	private static Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}

	/**
	 * (non-Javadoc)
	 * Method declared on PreferencePage
	 */
	@Override
	public boolean performOk() {
		// nothing to do - read-only page
		return true;
	}

	protected String getCurrentRoot() {
		return EclipseUtil.getSimplityRoot(EclipseUtil.getProject(this.getElement()));
	}

	protected void setCurrentRoot(String root) {
		EclipseUtil.setSimplityRoot(EclipseUtil.getProject(this.getElement()), root);
	}
}
