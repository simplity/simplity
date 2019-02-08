/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
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
package org.simplity.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.jms.Session;

import org.simplity.core.ApplicationError;
import org.simplity.core.MessageBox;
import org.simplity.core.adapter.IDataAdapterExtension;
import org.simplity.core.adapter.IDataListSource;
import org.simplity.core.adapter.IDataListTarget;
import org.simplity.core.adapter.IDataSource;
import org.simplity.core.adapter.IDataTarget;
import org.simplity.core.adapter.source.DataSheetListSource;
import org.simplity.core.adapter.source.FieldsDataSource;
import org.simplity.core.adapter.source.JsonDataSource;
import org.simplity.core.adapter.source.JsonListSource;
import org.simplity.core.adapter.target.JsonDataTarget;
import org.simplity.core.adapter.target.JsonListTarget;
import org.simplity.core.adapter.target.PojoDataTarget;
import org.simplity.core.adapter.target.PojoListTarget;
import org.simplity.core.app.AppUser;
import org.simplity.core.app.IRequestReader;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.data.CommonData;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.MessageType;
import org.simplity.core.msg.Messages;
import org.simplity.core.util.TextUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context is created for an execution of a service. A service execution
 * requires a number of actions to be performed. However, each action is on its
 * own, and does not assume presence of any other action. Of course, it expects
 * that certain data is available, and it's job may be to extract some more
 * data.
 *
 * <p>
 * We use a common (we can call this global for the service) data context for
 * all such action. While a DataScape is good enough data structure, we would
 * like to make use this concept for other design aspects, like accumulating
 * messages and tracking error states etc.. Hence this class extends DataScape.
 *
 * @author simplity.org
 */
public class ServiceContext extends CommonData {
	private static final Logger logger = LoggerFactory.getLogger(ServiceContext.class);

	protected static IDataAdapterExtension adapterExtension;

	/**
	 * set app-specific data adapter extension
	 *
	 * @param extension
	 */
	public static void setDataAdapterExtension(IDataAdapterExtension extension) {
		adapterExtension = extension;
	}

	/**
	 *
	 * @return app-specific data adapter extension. null if no such extension is
	 *         set
	 */
	public static IDataAdapterExtension getDataAdapterExtension() {
		return adapterExtension;
	}

	protected final String serviceName;
	protected final Value userId;
	protected final AppUser appUser;
	protected List<FormattedMessage> messages = new ArrayList<FormattedMessage>();
	protected int nbrErrors = 0;

	/**
	 * payload from client wrapped as a reader. Used in special cases where
	 * service may want to read it. use InputData.managedByService = treu;
	 */
	protected IRequestReader reqReader;

	/**
	 * writer to which the service may choose to directly write to.
	 */
	protected IResponseWriter respWriter;

	/** jms session associated with this service */
	protected Session jmsSession;

	/** message box */
	protected MessageBox messageBox;

	/**
	 * client-layer context carried to service layer
	 */
	protected Object clientContext;
	/**
	 * key by which this response can be cached. It is serviceName, possibly
	 * appended with values of some key-fields. null means can not be cached
	 */
	protected String cachingKey;

	/**
	 * valid if cachingKey is non-null. number of minutes after which this cache
	 * is to be invalidated. 0 means no such
	 */
	protected int cacheValidityMinutes;
	/**
	 * service caches to be invalidated after execution this service. Null means
	 * nothing is to be invalidated
	 */
	protected String[] invalidations;

	/**
	 * current db handler that is opened and managed by the main service
	 */
	protected IDbHandle serviceDbHandle;

	/**
	 * true if the main service has declared that it is not managing any
	 * transactions, and that the steps/tasks are to manage their own
	 * transactions if required. They are free to use the IdbHandle for any
	 * read-only operation outside the transaction
	 */
	protected boolean transactionIsDelegeated;

	/**
	 * apps that deal with multi-tenancy need to track the tenantId. For example
	 * if it is a crm app that is hosted on the cloud offering SAAS, we may use
	 * orgId as the column in every table to stored data for all customers in
	 * the same db.
	 */
	protected Value tenantId;

	/**
	 * @param serviceName
	 * @param appUser
	 */
	public ServiceContext(String serviceName, AppUser appUser) {
		this.userId = appUser.getUserId();
		this.serviceName = serviceName;
		this.appUser = appUser;
	}

	/**
	 * to be used by the main service, and SHOULD NOT be used by other actions
	 *
	 * @param dbHandle
	 *            the dbDriver to set
	 */
	public void setDbDriver(IDbHandle dbHandle) {
		this.serviceDbHandle = dbHandle;
	}

	/**
	 * @return the dbDriver. null if no db operation is envisaged by the main
	 *         service. Also refer to isTransactionDelegeated()
	 */
	public IDbHandle getDbHandle() {
		return this.serviceDbHandle;
	}

	/**
	 * @return the transactionIsDelegeated
	 */
	public boolean isTransactionDelegeated() {
		return this.transactionIsDelegeated;
	}

	/**
	 * @param tenantId
	 *            the tenantId to set
	 */
	public void setTenantId(Value tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * @return the tenantId
	 */
	public Value getTenantId() {
		return this.tenantId;
	}

	/**
	 * to be used by the main service to guide steps/tasks/actions as to how
	 * they should manage their transactions.
	 *
	 * @param isDelegated
	 *            true means that the main service is not managing transactions,
	 *            but expects the children to manage their own transactions. In
	 *            this case, db handle, if open, is a read-only handle that can
	 *            be used for all read-only operations.
	 *            <p>
	 *            false means that the transaction is managed by the main
	 *            service. Children should not do any commit/roll-back. ANy
	 *            update should simply use the db handle.
	 */
	public void setTransactionDelegation(boolean isDelegated) {
		this.transactionIsDelegeated = isDelegated;
	}

	/**
	 * add a message that is associated with a data element in the input data
	 *
	 * @param messageName
	 * @param referredField
	 *            if this is about a specific field, so that client can
	 *            associate this message with a field/column
	 * @param otherReferredField
	 *            if the message has more than one referred field, like from-to
	 *            validation failed
	 * @param referredTable
	 *            if the referred field is actually a column in a table
	 * @param rowNumber
	 *            if table is referred, 1 based.
	 * @param params
	 *            if the message is parameterized, provide values for those
	 *            parameters. $name means name is a field, and the field value
	 *            is to be taken from context
	 * @return type of message that got added
	 */
	public MessageType addValidationMessage(String messageName, String referredField, String otherReferredField,
			String referredTable, int rowNumber, String... params) {
		String[] values = null;
		if (params != null && params.length > 0) {
			values = new String[params.length];
			for (int i = 0; i < values.length; i++) {
				String val = params[i];
				/*
				 * if it is a field name, we get its value
				 */
				String fieldName = TextUtil.getFieldName(val);
				if (fieldName != null) {
					Value v = this.getValue(fieldName);
					if (v != null) {
						val = v.toString();
					}
				}
				values[i] = val;
			}
		}

		FormattedMessage msg = Messages.getMessage(messageName, values);
		if (msg.messageType == MessageType.ERROR) {
			this.nbrErrors++;
		}
		msg.fieldName = referredField;
		msg.relatedFieldName = otherReferredField;
		msg.tableName = referredTable;
		msg.rowNumber = rowNumber;
		this.messages.add(msg);
		return msg.messageType;
	}

	/**
	 * add a message that has no reference to a data element
	 *
	 * @param messageName
	 * @param paramValues
	 *            if the message is parameterized, provide values for those
	 *            parameters
	 * @return type of message that got added
	 */
	public MessageType addMessage(String messageName, String... paramValues) {
		return this.addValidationMessage(messageName, null, null, null, 0, paramValues);
	}

	/**
	 * add row to the messages list, but after checking for error status
	 *
	 * @param messageName
	 * @param messageType
	 * @param messageText
	 * @param referredField
	 * @param otherReferredField
	 * @param referredTable
	 * @param rowNumber
	 */
	public void addMessageRow(String messageName, MessageType messageType, String messageText, String referredField,
			String otherReferredField, String referredTable, int rowNumber) {
		if (messageType == MessageType.ERROR) {
			this.nbrErrors++;
		}
		FormattedMessage msg = new FormattedMessage(messageName, messageType, messageText);
		msg.fieldName = referredField;
		msg.relatedFieldName = otherReferredField;
		msg.tableName = referredTable;
		msg.rowNumber = rowNumber;
		this.messages.add(msg);
	}

	/**
	 * has any one raised an error message?
	 *
	 * @return true if any error message is added. False otherwise
	 */
	public boolean isInError() {
		return this.nbrErrors > 0;
	}

	/**
	 * get all messages
	 *
	 * @return messages
	 */
	public List<FormattedMessage> getMessages() {
		return this.messages;
	}

	/**
	 * get all messages as a DataSheet
	 *
	 * @return messages
	 */
	public IDataSheet getMessagesAsDs() {
		String[] columnNames = { "name", "text", "messageType", "fieldName" };
		Collection<FormattedMessage> msgs = this.getMessages();
		int nbr = msgs.size();
		if (nbr == 0) {
			ValueType[] types = { ValueType.TEXT, ValueType.TEXT, ValueType.TEXT, ValueType.TEXT };
			return new MultiRowsSheet(columnNames, types);
		}
		/*
		 * data is by column
		 */
		Value[][] data = new Value[4][nbr];

		int i = 0;
		for (FormattedMessage msg : msgs) {
			data[0][i] = Value.newTextValue(msg.name);
			data[1][i] = Value.newTextValue(msg.text);
			data[2][i] = Value.newTextValue(msg.messageType.name());
			data[3][i] = Value.newTextValue(msg.fieldName);
			i++;
		}

		return new MultiRowsSheet(columnNames, data);
	}

	/** sreset/remove all messages */
	public void resetMessages() {
		this.messages.clear();
		this.nbrErrors = 0;
	}

	/** @return userId for whom this context is created */
	public Value getUserId() {
		return this.userId;
	}

	/** @return appUser for whom this context is created */
	public AppUser getAppUser() {
		return this.appUser;
	}

	/** @return service for which this context is created */
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * @param dataRow
	 */
	public void copyFrom(IFieldsCollection dataRow) {
		for (Entry<String, Value> entry : dataRow.getAllFields()) {
			this.allFields.put(entry.getKey(), entry.getValue());
		}
	}

	/** @return summary for tracing */
	public String getSummaryInfo() {
		StringBuilder sbf = new StringBuilder("Context has ");
		sbf.append(this.allFields.size()).append(" fields and ").append(this.allSheets.size()).append(" sheets and ")
				.append(this.messages.size()).append(" messages.");
		return sbf.toString();
	}

	/**
	 * add a formatted message
	 *
	 * @param formattedMessage
	 */
	public void addMessage(FormattedMessage formattedMessage) {
		this.messages.add(formattedMessage);
		if (formattedMessage.messageType == MessageType.ERROR) {
			this.nbrErrors++;
		}
	}

	/**
	 * @param sheetName
	 * @return 0 if sheet does not exist
	 */
	public int nbrRowsInSheet(String sheetName) {
		IDataSheet ds = this.getDataSheet(sheetName);
		if (ds == null) {
			return 0;
		}
		return ds.length();
	}

	/**
	 * set a default response writer to the context
	 *
	 * @param writer
	 */
	public void setWriter(IResponseWriter writer) {
		this.respWriter = writer;
	}

	/** @return writer, null if this was not set */
	public IResponseWriter getWriter() {
		return this.respWriter;
	}

	/**
	 * set the inputReader
	 *
	 * @param reqReader
	 */
	public void setReader(IRequestReader reqReader) {
		this.reqReader = reqReader;
	}

	/** @return writer, never null */
	public IRequestReader getReader() {
		if (this.reqReader == null) {
			logger.error(
					"No reqReader is associated with the context. Service.INputData needs to have the right directive for this");
		}
		return this.reqReader;
	}

	/**
	 * @param session
	 */
	public void setJmsSession(Session session) {
		this.jmsSession = session;
	}

	/**
	 * @return jms connector associated with this service. never null.
	 * @throws ApplicationError
	 *             in case this service is not associated with a connector
	 */
	public Session getJmsSession() {
		if (this.jmsSession == null) {
			throw new ApplicationError("This service is not set up for a JMS operation.");
		}
		return this.jmsSession;
	}

	/**
	 * @param message
	 */
	public void putMessageInBox(Object message) {
		if (this.messageBox == null) {
			this.messageBox = new MessageBox();
		}
		this.messageBox.setMessage(message);
	}

	/** @return message, or null if there is none */
	public Object getMessageFromBox() {
		if (this.messageBox == null) {
			return null;
		}
		return this.messageBox.getMessage();
	}

	/**
	 * @param box
	 */
	public void setMessageBox(MessageBox box) {
		this.messageBox = box;
	}

	/** @return message box, or null if there is none */
	public MessageBox getMessageBox() {
		return this.messageBox;
	}

	/**
	 * @return get the key based on which the response can be cached.
	 *         emptyString means no key is used for caching. null means it can
	 *         not be cached.
	 */
	public String getCachingKey() {
		return this.cachingKey;
	}

	/**
	 * @return number of minutes the cache is valid for. 0 means it has no
	 *         expiry. This method is relevant only if getCachingKey returns
	 *         non-null (indication that the service can be cached)
	 */
	public int getCacheValidity() {
		return this.cacheValidityMinutes;
	}

	/**
	 * @param key
	 *            key based on which the response can be cached. emptyString
	 *            means no key is used for caching. null means it can not be
	 *            cached
	 * @param minutes
	 *            if non-null cache is to be invalidated after these many
	 *            minutes
	 */
	public void setCaching(String key, int minutes) {
		this.cachingKey = key;
		this.cacheValidityMinutes = minutes;
	}

	/**
	 * @return cached keys that need to be invalidated
	 */
	public String[] getInvalidations() {
		return this.invalidations;
	}

	/**
	 * @param invalidations
	 *            cached keys that need to be invalidated
	 */
	public void setInvalidations(String[] invalidations) {
		this.invalidations = invalidations;
	}

	/**
	 * @return the clientContext.
	 */
	public Object getClientContext() {
		return this.clientContext;
	}

	/**
	 * @param clientContext
	 *            the clientContext to set
	 */
	public void setClientContext(Object clientContext) {
		this.clientContext = clientContext;
	}

	/**
	 * used internally in batch environment etc..
	 */
	public void clearMessages() {
		this.messages.clear();

	}

	/**
	 * get a data source to be used for a data adapter for copying data from
	 *
	 * @param sourceName
	 *            non-null name of the source
	 * @return source. null if no object exists in the context for this.
	 */
	public IDataSource getDataSource(String sourceName) {
		Object obj = this.getObject(sourceName);
		IDataSource source = null;
		/*
		 * try app specific object
		 */
		IDataAdapterExtension extension = getDataAdapterExtension();
		if (extension != null) {
			source = extension.getDataSource(obj, sourceName, this);
			if (source != null) {
				return source;
			}
		}

		source = JsonDataSource.getSource(obj);
		if (source != null) {
			return source;
		}
		/*
		 * try Data sheet
		 */
		IDataSheet sheet = this.getDataSheet(sourceName);
		if (sheet != null) {
			return new FieldsDataSource(sheet);
		}
		return null;
	}

	/**
	 * get a list data source to be used for a data adapter for copying data
	 * from
	 *
	 * @param sourceName
	 * @return source. null if no object exists in the context for this.
	 */
	public IDataListSource getListSource(String sourceName) {
		Object obj = this.getObject(sourceName);
		/*
		 * try app specific object
		 */
		IDataAdapterExtension extension = ServiceContext.getDataAdapterExtension();
		if (extension != null) {
			return extension.getListSource(obj, sourceName, this);
		}
		/*
		 * try JSON object first
		 */
		IDataListSource source = JsonListSource.getSource(obj);
		if (source != null) {
			return source;
		}
		/*
		 * try data sheet
		 */
		IDataSheet sheet = this.getDataSheet(sourceName);
		if (sheet != null) {
			return new DataSheetListSource(sheet);
		}
		return null;
	}

	/**
	 * get a target to be used for a data adapter for copying data
	 *
	 * @param targetName
	 * @param childClassName
	 * @return target. non-null, but application error in case of issues with
	 *         specifications.
	 */
	public IDataTarget getDataTarget(String targetName, String childClassName) {
		IDataTarget target = null;
		/*
		 * let the app-specific feature get the priority
		 */
		IDataAdapterExtension extension = ServiceContext.getDataAdapterExtension();
		Object obj = this.getObject(targetName);
		if (extension != null) {
			target = extension.getDataTarget(obj, childClassName, targetName, this);
			if (target != null) {
				logger.info("App-specific target returned for field {} ", targetName);
				return target;
			}
		}

		if (obj != null) {
			target = JsonDataTarget.getTarget(obj);
			if (target != null) {
				logger.info("JSON target created for {} with an existing object", targetName);
				return target;
			}

			target = PojoDataTarget.getTarget(obj);
			if (target != null) {
				logger.info("Pojo target created for {} with an existing object", targetName);
				return target;
			}
		}

		if (childClassName != null) {
			try {
				obj = Class.forName(childClassName).newInstance();
				logger.info("New Pojo target created for {}", targetName);
				return PojoDataTarget.getTarget(obj);
			} catch (Exception e) {
				throw new ApplicationError(e,
						"Error while using " + childClassName + " as class name for target for field " + targetName);
			}
		}
		/*
		 * let us create a JSON target
		 */
		JSONObject json = new JSONObject();
		this.setObject(targetName, json);
		logger.info("New JSON target created for {}", targetName);
		return JsonDataTarget.getTarget(json);
	}

	/**
	 * get a list target to be used for a data adapter for copying data
	 *
	 * @param targetName
	 * @param childClassName
	 * @return target. non-null, but application error in case of issues with
	 *         specifications.
	 */
	public IDataListTarget getListTarget(String targetName, String childClassName) {
		IDataListTarget target = null;
		/*
		 * do we have this already to be managed by app-specific target?
		 */
		IDataAdapterExtension extension = ServiceContext.getDataAdapterExtension();
		Object obj = this.getObject(targetName);
		if (extension != null) {
			target = extension.getListTarget(obj, childClassName, targetName, this);
			if (target != null) {
				logger.info("App-specific list target returned for field {} ", targetName);
				return target;
			}
		}

		if (obj != null) {
			target = JsonListTarget.getTarget(obj);
			if (target != null) {
				logger.info("JSON list target created for {} with an existing object", targetName);
				return target;
			}

			target = PojoListTarget.getTarget(obj);
			if (target != null) {
				logger.info("Pojo list target created for {} with an existing object", targetName);
				return target;
			}
		}

		if (childClassName != null) {
			@SuppressWarnings("rawtypes")
			List list = new ArrayList();
			target = PojoListTarget.getTarget(list, childClassName);
			if (target != null) {
				this.setObject(targetName, list);
				logger.info("New Pojo list target created for {}", targetName);
				return target;
			}
		}

		JSONArray arr = new JSONArray();
		this.setObject(targetName, arr);
		logger.info("New JSON list target created {}", targetName);
		return JsonListTarget.getTarget(arr);
	}
}