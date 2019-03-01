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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;

/**
 * content assist to suggest data-type and other referred components
 *
 * @author simplity.org
 *
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class ProposalComputer implements ICompletionProposalComputer {

	@Override
	public List computeCompletionProposals(CompletionProposalInvocationContext context, IProgressMonitor monitor) {
		int cp = context.getInvocationOffset();
		EclipseUtil.XmlAttributeDetails att = EclipseUtil.getAttributeDetails(context.getViewer(), cp);
		if (att == null) {
			return Collections.EMPTY_LIST;
		}

		String[] comps = EclipseUtil.getSuggestedComps(att.name, att.value);
		if (comps == null || comps.length == 0) {
			return Collections.EMPTY_LIST;
		}
		List<ICompletionProposal> props = new ArrayList<>(comps.length);
		int len = att.value.length();
		if (att.openingQuote != 0) {
			len++;
		}
		if (att.closingQuote != 0) {
			len++;
		}
		for (String comp : comps) {
			String replace = '"' + comp + '"';
			props.add(new CustomCompletionProposal(replace, att.offset, len, cp, null, comp, null, null, 1, true));
		}
		return props;
	}

	@Override
	public void sessionEnded() {
		// none
	}

	@Override
	public void sessionStarted() {
		// none
	}

	@Override
	public List computeContextInformation(CompletionProposalInvocationContext arg0, IProgressMonitor arg1) {
		return Collections.EMPTY_LIST;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}
}
