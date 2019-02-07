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

package org.simplity.kernel.trans;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.AppConventions;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.app.IService;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IComponent;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationUtil;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.service.InputData;
import org.simplity.kernel.service.InputField;
import org.simplity.kernel.service.OutputData;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class Service implements IService, IComponent {
	protected static final Logger logger = LoggerFactory.getLogger(Service.class);
	private static final ComponentType MY_TYPE = ComponentType.SERVICE;
	/**
	 * simple name
	 */
	String name;

	/**
	 * module name.simpleName would be fully qualified name.
	 */
	String moduleName;

	/**
	 * input fields/grids for this service. not valid if requestTextFieldName is
	 * specified
	 */
	InputData inputData;

	/**
	 * copy input records from another service
	 */

	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.SERVICE)
	String referredServiceForInput;

	/**
	 * output fields and grids for this service. Not valid if
	 * responseTextFieldName is specified
	 */
	OutputData outputData;
	/**
	 * copy output records from another service
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.SERVICE)
	String referredServiceForOutput;
	/**
	 * can the response from this service be cached. If this is false, other
	 * caching related attributes are irrelevant.
	 */
	boolean okToCache;

	/**
	 * Should this response cache be discarded after certain time? 0 means no
	 * such predetermined validity
	 */
	@FieldMetaData(leaderField = "okToCache")
	int cacheValidityMinutes;

	/**
	 * <p>
	 * valid only if okToCache is set to true. Use this if the service response
	 * can be cached for the same values for this list of fields. For example if
	 * getDistricts is a service that responds with all districts for the given
	 * state in the given countryCode then "countryCode,stateCode" is the value
	 * of this attribute.
	 * </p>
	 *
	 * <p>
	 * If the response is specific to given user, then _userId should be the
	 * first field in the list. Skip this if the response is not dependent on
	 * any input
	 * </p>
	 */
	@FieldMetaData(leaderField = "okToCache")
	String[] cacheKeyNames;

	/**
	 * The cached response for services that are to be invalidated when this
	 * service is executed. for example updateStates service would invalidate
	 * cached responses from getStates service
	 */
	@FieldMetaData(irrelevantBasedOnField = "okToCache")
	String[] serviceCachesToInvalidate;

	/**
	 * should this be executed in the background ALWAYS?.
	 */
	boolean executeInBackground;

	/**
	 * in case this service is outputting large amount of data, and there is a
	 * need to tweak performance, service may write directly to the writer
	 * instead of populating data in the service context which will ultimately
	 * find its way to the payload.
	 */
	boolean writesDataDirectlyToWriter;
	/**
	 * actual processing is done by the processor.
	 */
	@FieldMetaData(isRequired = true)
	IProcessor processor;

	/**
	 * avoid repeated call to getReay()
	 */
	private boolean gotReady;
	/**
	 * key names for services that are to be invalidated
	 */
	private String[][] invalidationKeys;

	/**
	 * if we want to offer cache-by-all-input-fields feature, this is the field
	 * that is populated at getReay()
	 */
	private String[] parsedCacheKeys;

	@Override
	public final String getSimpleName() {
		return this.name;
	}

	@Override
	public final boolean toBeRunInBackground() {
		return this.executeInBackground;
	}

	/**
	 * should the service be fired in the background (in a separate thread)?
	 *
	 * @return true if this service is marked for background execution
	 */
	@Override
	public final boolean okToCache() {
		return this.okToCache;
	}

	@Override
	public final String getQualifiedName() {
		if (this.moduleName == null) {
			return this.name;
		}
		return this.moduleName + '.' + this.name;
	}

	@Override
	public final String getServiceName() {
		return this.getQualifiedName();
	}

	@Override
	public final ComponentType getComponentType() {
		return MY_TYPE;
	}

	@Override
	public final InputData getInputSpecification() {
		return this.inputData;
	}

	@Override
	public final OutputData getOutputSpecification() {
		return this.outputData;
	}

	@Override
	public final String[] getCacheKeyNames() {
		return this.cacheKeyNames;
	}

	@Override
	public void serve(ServiceContext ctx) {
		this.processor.execute(ctx);
		if (this.okToCache()) {
			String key = createCachingKey(this.getQualifiedName(), this.cacheKeyNames, ctx);
			ctx.setCaching(key, this.cacheValidityMinutes);
		} else if (this.serviceCachesToInvalidate != null) {
			ctx.setInvalidations(this.getInvalidations(ctx));
		}
	}

	@Override
	public boolean executeAsSubProcess(ServiceContext ctx) {
		return this.processor.executeAsAction(ctx);
	}

	@Override
	public void getReady() {
		if (this.gotReady) {

			logger.info("Service " + this.getQualifiedName()
					+ " is being harassed by repeatedly asking it to getReady(). Please look into this..");

			return;
		}
		this.gotReady = true;
		Application app = Application.getActiveInstance();

		/*
		 * input record may have to be copied form referred service
		 */
		if (this.referredServiceForInput != null) {
			if (this.inputData != null) {
				throw new ApplicationError("Service " + this.getQualifiedName() + " refers to service "
						+ this.referredServiceForInput + " but also specifies its own input records.");
			}
			IService service = app.getService(this.referredServiceForInput);
			this.inputData = service.getInputSpecification();
		}
		if (this.inputData != null) {
			this.inputData.getReady();
		}
		/*
		 * output record may have to be copied form referred service
		 */
		if (this.referredServiceForOutput != null) {
			if (this.outputData != null) {
				throw new ApplicationError("Service " + this.getQualifiedName() + " refers to service "
						+ this.referredServiceForOutput + " but also specifies its own output records.");
			}
			IService service = app.getService(this.referredServiceForOutput);
			this.outputData = service.getOutputSpecification();
		}
		if (this.outputData != null) {
			this.outputData.getReady();
		}

		if (this.serviceCachesToInvalidate != null) {
			this.invalidationKeys = new String[this.serviceCachesToInvalidate.length][];
			for (int i = 0; i < this.invalidationKeys.length; i++) {
				IService service = app.getService(this.serviceCachesToInvalidate[i]);
				this.invalidationKeys[i] = service.getCacheKeyNames();
			}
		}

		if (this.okToCache) {
			this.setCacheKeys();
		}

		this.processor.getReady(this);
	}

	/**
	 * if caching keys are not set, we may infer it from input specification
	 */
	private void setCacheKeys() {
		if (this.cacheKeyNames != null) {
			this.parsedCacheKeys = this.cacheKeyNames;
			return;
		}

		InputField[] fields = null;
		if (this.inputData != null) {
			fields = this.inputData.getInputFields();
		}
		if (fields == null || fields.length == 0) {
			return;
		}

		this.parsedCacheKeys = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			this.parsedCacheKeys[i] = fields[i].getName();
		}
	}

	@Override
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		this.processor.validate(vtx, this);
	}

	/**
	 * @param ctx
	 * @return
	 */
	private static String createCachingKey(String serviceName, String[] keyNames, ServiceContext ctx) {
		if (keyNames == null) {
			return createCachingKey(serviceName, null);
		}
		String[] vals = new String[keyNames.length];
		/*
		 * first field could be userId
		 */
		int startAt = 0;
		if (keyNames[0].equals(AppConventions.Name.USER_ID)) {
			startAt = 1;
			vals[0] = ctx.getUserId().toString();
		}
		for (int i = startAt; i < keyNames.length; i++) {
			Value val = ctx.getValue(keyNames[i]);
			if (val != null) {
				vals[i] = val.toString();
			}
		}

		return createCachingKey(serviceName, vals);
	}

	/**
	 * form a key to be used for caching based on service name and values of
	 * keys. This method to be used for caching and retrieving
	 *
	 * @param serviceName
	 * @param keyValues
	 * @return key to be used for caching
	 */
	public static String createCachingKey(String serviceName, String[] keyValues) {
		if (keyValues == null) {
			return serviceName;
		}
		StringBuilder result = new StringBuilder(serviceName);
		for (String val : keyValues) {
			result.append(AppConventions.CACHE_KEY_SEP);
			if (val != null) {
				result.append(val);
			}
		}

		return result.toString();
	}

	private String[] getInvalidations(ServiceContext ctx) {
		String[] result = new String[this.serviceCachesToInvalidate.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = createCachingKey(this.serviceCachesToInvalidate[i], this.invalidationKeys[i], ctx);
		}
		return result;
	}

	@Override
	public DbUsage getDbUsage() {
		return this.processor.getDbUsage();
	}

	/**
	 * @param possiblyQualifiedName
	 */
	public void setName(String possiblyQualifiedName) {
		int idx = possiblyQualifiedName.lastIndexOf('.');
		if (idx == -1) {
			this.name = possiblyQualifiedName;
			this.moduleName = null;
		} else {
			this.name = possiblyQualifiedName.substring(idx + 1);
			this.moduleName = possiblyQualifiedName.substring(0, idx);
		}
	}

	@Override
	public boolean directlyWritesDataToResponse() {
		return this.writesDataDirectlyToWriter;
	}

	@Override
	public boolean responseIsAnArray() {
		return this.outputData != null && this.outputData.outputIsAnArray();
	}

}
