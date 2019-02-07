/*
 * Copyright (c) 2019 simplity.org
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

package org.simplity.kernel.app;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.AttachmentManager;
import org.simplity.kernel.adapter.IDataAdapterExtension;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationMessage;
import org.simplity.kernel.comp.ValidationUtil;
import org.simplity.kernel.file.FileBasedAssistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sub-component of APP to organize plugins used in the app. This is not
 * designed as "generic" like eclipse. INstead, any addition of plugin requires
 * altering this code. This approach is good enough for our purpose.
 *
 * @author simplity.org
 *
 */
class Plugins {
	protected static final Logger logger = LoggerFactory.getLogger(Plugins.class);
	/**
	 * Utility class that gets an instance of a Bean. Like the context in
	 * Spring. Useful when you want to work within a Spring container. Must
	 * implement <code>BeanFinderInterface</code> This is also used to get
	 * instance of any fully qualified name provided for configuration
	 */
	@FieldMetaData(superClass = IBeanFinder.class)
	String beanFinder;
	private IBeanFinder beanFinderInstance = new IBeanFinder() {
		/*
		 * default is to use Class.forName()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getBean(String className, Class<T> clazz) {
			try {
				return (T) Class.forName(className).newInstance();
			} catch (Exception e) {
				logger.error("{} could not be used to create an object insance of {}, ERROR: {}", className,
						clazz.getName(), e.getMessage());
				return null;
			}
		}
	};

	IBeanFinder getBeanFinder() {
		return this.beanFinderInstance;
	}

	/**
	 * Cache manager to be used by <code>ServiceAgent</code> to cache responses
	 * to services that are designed for caching. This class should implement
	 * <code>ServiceCacherInterface</code> null if caching is not to be enabled.
	 */
	@FieldMetaData(superClass = IServiceCacher.class)
	String serviceCacher;
	private IServiceCacher serviceCacherInstance;

	IServiceCacher getServiceCacher() {
		return this.serviceCacherInstance;
	}

	/**
	 * Service level access control to be implemented by
	 * <code>ServiceAgent</code> null if service agent is not responsible for
	 * this. Any service specific access control is to be managed by the service
	 * itself. must implement <code>AccessControllerInterface</code>
	 */
	@FieldMetaData(superClass = IAccessController.class)
	String accessController;
	private IAccessController accessControllerInstance;

	IAccessController getAccessController() {
		return this.accessControllerInstance;
	}

	/**
	 * App specific hooks during service invocation life-cycle used by
	 * <code>ServiceAgent</code> null if no service agent is not responsible for
	 * this. Any service specific access control is to be managed by the service
	 * itself. must implement <code>ServicePrePostProcessorInterface</code>
	 */
	@FieldMetaData(superClass = IServicePrePostProcessor.class)
	String servicePrePostProcessor;
	private IServicePrePostProcessor servicePrePostProcessorInstance;

	IServicePrePostProcessor getServicePrePostProcessor() {
		return this.servicePrePostProcessorInstance;
	}

	/**
	 * way to wire exception to corporate utility. null if so such requirement.
	 * must implement <code>ExceptionListenerInterface</code>
	 */
	@FieldMetaData(superClass = IExceptionListener.class)
	String exceptionListener;
	private IExceptionListener exceptionListenerInstance = new IExceptionListener() {

		@Override
		public void listen(ApplicationError e) {
			logger.error("Listener : Application Error : {}", e.getMessage());
		}

		@Override
		public void listen(Exception e) {
			logger.error("Listener : Exception : {}", e.getMessage());
		}

		@Override
		public void listen(IServiceRequest request, Exception e) {
			if (request != null) {
				logger.error("Listener : Service {} ended with exception :{}", request.getServiceName(),
						e.getMessage());
			} else {
				logger.error("Listener : Unknown service ended with exception :{}", e.getMessage());
			}
		}
	};

	IExceptionListener getExceptionListener() {
		return this.exceptionListenerInstance;
	}

	/**
	 * class that can be used for caching app data. must implement
	 * <code>AppDataCacherInterface</code>
	 */
	@FieldMetaData(superClass = IAppDataCacher.class)
	String appDataCacher;
	private IAppDataCacher appDataCacherInstance;

	IAppDataCacher getAppDataCacher() {
		return this.appDataCacherInstance;
	}

	/**
	 * fully qualified class name that can be used for getting value for
	 * parameter/Property at run time. must implement
	 * <code>ParameterRetrieverInterface</code>
	 */
	@FieldMetaData(superClass = IParameterRetriever.class)
	String parameterRetriever;
	private IParameterRetriever parameterRetrieverInstance;

	IParameterRetriever getParameterRetriever() {
		return this.parameterRetrieverInstance;
	}

	/**
	 * fully qualified class name that can be used for getting data/list source
	 * for dataAdapters. must implement <code>DataAdapterExtension</code>
	 */
	@FieldMetaData(superClass = IDataAdapterExtension.class)
	String dataAdapterExtension;
	private IDataAdapterExtension dataAdapterExtensionInstance;

	IDataAdapterExtension getDataAdapterExtension() {
		return this.dataAdapterExtensionInstance;
	}

	/**
	 * class name that implements <code>CommonCodeValidatorInterface</code>.
	 * null is no such concept used in this app
	 */
	@FieldMetaData(superClass = ICommonCodeValidator.class)
	String commonCodeValidator;
	private ICommonCodeValidator commonCodeValidatorInstance;

	ICommonCodeValidator getCommonCodeValidator() {
		return this.commonCodeValidatorInstance;
	}

	/**
	 * Simplity provides a rudimentary, folder-based system that can be used for
	 * storing and retrieving attachments. If you want to use that, provide the
	 * folder that is available for the server instance
	 */
	String attachmentsFolderPath;
	/**
	 * if attachments are managed by a custom code, specify the class name to
	 * wire it. It should implement <code>AttachmentAssistantInterface</code>
	 */
	@FieldMetaData(irrelevantBasedOnField = "attachmentsFolderPath", superClass = IAttachmentAssistant.class)
	String attachmentAssistant;
	private IAttachmentAssistant attachmentAssistantInstance;

	IAttachmentAssistant getttachmentAssistant() {
		return this.attachmentAssistantInstance;
	}

	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);

		if (this.attachmentsFolderPath != null) {
			File file = new File(this.attachmentsFolderPath);
			if (file.exists() == false) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						this.attachmentsFolderPath + " is not a valid path in the system file system.",
						"attachmentsFolderPath"));
			}
		}
	}

	/*
	 * since bootstrap is called only one during app-bootstrapping, we have
	 * written this concise, but ineffecient code. MUST HAVE beanFInder as the
	 * first one
	 */
	private static final String[] PLUGINS = { "beanFinder", "serviceCacher", "accessController",
			"servicePrePostProcessor", "exceptionListener", "appDataCacher", "parameterRetriever",
			"dataAdapterExtension", "commonCodeValidator", "attachmentAssistant" };

	void configure(List<String> messages) {
		Class<?> cls = this.getClass();
		String className = null;
		for (String fn : PLUGINS) {
			try {
				Field field = cls.getDeclaredField(fn);
				className = (String) field.get(this);
				if (className == null) {
					logger.info("No plugin set for {}.", fn);
					continue;
				}
				Object instance = this.getInstance(className, field.getType());
				if (instance != null) {
					field = cls.getDeclaredField(fn + "Instance");
					field.set(this, instance);
					logger.info("Plugin for {} set to {}", fn, className);

				} else {
					messages.add("No bean/object could be located for " + fn);
				}

			} catch (Exception e) {
				messages.add("Error while initializing plugin field " + fn + "=" + className + " Error: "
						+ e.getMessage());
			}
		}

		if (this.attachmentsFolderPath != null) {
			this.attachmentAssistantInstance = new FileBasedAssistant(this.attachmentsFolderPath);
		}
		if (this.attachmentAssistantInstance != null) {
			AttachmentManager.setAssistant(this.attachmentAssistantInstance);
		}

	}

	<T> T getInstance(String className, Class<T> cls) {

		try {
			return this.beanFinderInstance.getBean(className, cls);
		} catch (Exception e) {
			logger.error("Error while creating an instance of {} using {}. Error : {}", cls.getName(), className,
					e.getMessage());
			return null;
		}

	}
}
