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

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

/**
 * @author simplity.org
 *
 */
public class HyperlinkDetector implements IHyperlinkDetector {

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer viewer, IRegion region, boolean canShowMultipleLinks) {
		EclipseUtil.XmlAttributeDetails attr = EclipseUtil.getAttributeDetails(viewer, region.getOffset());
		if (attr == null) {
			return null;
		}

		String resourceName = EclipseUtil.getResourceForComp(attr.name, attr.value);
		if (resourceName == null) {
			return null;
		}
		IRegion linkRegion = new Region(attr.offset, attr.value.length());
		IHyperlink link = new IHyperlink() {

			@Override
			public void open() {
				EclipseUtil.openCompResource(resourceName);
			}

			@Override
			public String getTypeLabel() {
				return null;
			}

			@Override
			public String getHyperlinkText() {
				return "Open '" + resourceName + "' with XML Editor";
			}

			@Override
			public IRegion getHyperlinkRegion() {
				return linkRegion;
			}
		};
		IHyperlink[] links = { link };
		return links;
	}

}
