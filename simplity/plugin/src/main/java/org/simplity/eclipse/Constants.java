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

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.simplity.core.app.AppConventions;
import org.simplity.core.comp.ComponentType;

/**
 * Single place for all constants that are shared across classes
 *
 * @author simplity.org
 *
 */
public class Constants {

	/**
	 * plugin ids
	 *
	 * @author simplity.org
	 *
	 */
	public static class Ids {
		/**
		 * The plug-in ID. MUST match the id specified in plugin.xml
		 */
		public static final String PLUGIN = "org.simplity"; //$NON-NLS-1$
		/**
		 * builderId as specified in plugin.xml
		 */
		public static final String BUILDER = "org.simplity.builder";
		/**
		 * ID of this project nature
		 */
		public static final String NATURE = "org.simplity.nature";
		/**
		 * we do not use any custom markers, yet. standard problem marker is
		 * used
		 */
		public static final String PROBLEM_MARKER = "org.simplity.marker";
	}

	/**
	 * names of files/folders/paths
	 *
	 * @author simplity.org
	 *
	 */
	public static class Names {
		/**
		 * name of file that contains the configuration details for project/app
		 */
		public static final Path APP_CONFIG_PATH = new Path("application.xml");

		/**
		 * folder name relative to project where simplity resource folder is to
		 * be copied to
		 */
		public static final String DEFAULT_TARGET = "target/classes";
		/**
		 * alternate folder to be tried if default is not present, before
		 * setting the default
		 */
		public static final String ALTERNATE_TARGET = "bin/classes";
		/**
		 * folder name of simplity resources. this is used as default
		 * resource-prefix by Simplity run-time
		 */
		public static final String SIMPLITY_RESOURCE_FOLDER = "res";

		/**
		 * line number in the source file where an element is located is added
		 * to that element with this attribute name
		 */
		public static final String LINE_NO_ATTR = "_locn";

		/**
		 * file extension of simplity components
		 */
		public static final String EXTENSION = "xml";

		/**
		 * file name for application configuration
		 */
		public static final Object APP_RESOURCE_NAME = "application.xml";
	}

	/**
	 * names with which simplity related attributes are saved in IProject
	 *
	 * @author simplity.org
	 *
	 */
	public static class QualifiedNames {
		/**
		 * name of root path
		 */
		public static final QualifiedName ROOT_PATH = new QualifiedName(Ids.PLUGIN, "simplityRoot");

		/**
		 * name of repository
		 */
		public static final QualifiedName REPOSITORY = new QualifiedName(Ids.PLUGIN, "repository");

	}

	/**
	 * folder prefix of component types
	 */
	public static String[] COMP_FOLDER_NAMES = AppConventions.Name.COMP_FOLDER_NAMES;

	/**
	 * comp Types order by their idx
	 */
	public static ComponentType[] COMP_TYPES = AppConventions.COMP_TYPES;

}
