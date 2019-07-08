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

package org.simplity.core.jms;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.app.internal.JsonReqReader;
import org.simplity.core.app.internal.JsonRespWriter;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.data.DataSerializationType;
import org.simplity.core.dm.Record;
import org.simplity.core.msg.Messages;
import org.simplity.core.service.IDataDeserializer;
import org.simplity.core.service.IDataSerializer;
import org.simplity.core.service.InputData;
import org.simplity.core.service.OutputData;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.trans.IBatchInput;
import org.simplity.core.trans.IBatchOutput;
import org.simplity.core.trans.InvalidRowException;
import org.simplity.core.value.Value;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all aspects of a JmsQueue
 *
 * @author simplity.org
 */
public class JmsDestination {
	protected static final Logger logger = LoggerFactory.getLogger(JmsDestination.class);

	/**
	 * name of the queue (destination) used for requesting a service. This is
	 * the jndi name that is available in the context.
	 */
	String name;

	/**
	 * is this destination a topic? false means it is a queue. This is for
	 * documentation. JNDI set up for name decides whether it is a queue or
	 * topic
	 */
	boolean isTopic;
	/**
	 * if this is used for working with other services, we need to accept body
	 * of the message as a payload and parse it into our data structures
	 */
	InputData inputData;
	/**
	 * if this is used for working with other services, we need to accept body
	 * of the message as a payload and parse it into our data structures
	 */
	OutputData outputData;
	/**
	 * null if message body is not used, but header parameters are used to
	 * transport data. How the body of the message is used to transport data.
	 */
	DataSerializationType messageBodyType;
	/**
	 * field that is associated with body. Entire body is
	 * assigned-to/retrieved-from this field
	 */
	String bodyFieldName;
	/**
	 * comma separated list of fields that supply/receive data.
	 */
	String fieldNames[];
	/**
	 * data structure on which this message data is based on
	 */
	String recordName;
	/**
	 * just extract all fields with no validation, if this queue is being
	 * consumed
	 */
	boolean extractAll;
	/**
	 * java class that implements org.simplity.service.DataFormatter interface.
	 * message body text is formatted using this class.
	 */
	String messageFormatter;
	/**
	 * java class that implements org.simplity.service.DataExtractor interface
	 * to extract data from message body text
	 */
	String messageExtractor;

	/**
	 * sheet name with two columns, first one name, second one value, both text.
	 * bodyMessageType must be set to COMMA_SEPARATED_PAIRS. Consumer extracts
	 * into this sheet, while producer formats the text using data in this sheet
	 */
	String nameValueSheetName;
	/**
	 * subset of messages that we are interested. As per JMS selector syntax.
	 */
	String messageSelector;
	/**
	 * message type of the message header. Use this ONLY if the provider insists
	 * on this, or your application uses this.
	 */
	String messageType;

	/** object instance for re-use */
	private IDataSerializer dataFormatter;
	/** object instance for re-use */
	private IDataDeserializer dataExtractor;
	/** jms queue instance for this queue */
	protected Destination destination;

	/**
	 * consume a request queue, and optionally put a message on to the response
	 * queue. Keep doing this for messages in the request queue, till the queue
	 * is closed, or the processor signals a shut-down
	 *
	 * @param ctx
	 *            service context where all this is happening
	 * @param processor
	 *            object instance that is interested in processing the message
	 * @param responseQ
	 *            optional response queue to be used to respond back to the
	 *            incoming message
	 * @param consumeAll
	 *            false means we will process (at most) one message. true means
	 *            no such restrictions.
	 * @param waitForMessage
	 *            true means we will wait for at least the first message. If
	 *            consumeAll is true, then we do not come-out till interrupted,
	 *            or the queue closes
	 */
	@SuppressWarnings("resource")
	public void consume(ServiceContext ctx, IJmsClient processor, JmsDestination responseQ, boolean consumeAll,
			boolean waitForMessage) {

		Session session = ctx.getJmsSession();
		MessageConsumer consumer = null;
		MessageProducer producer = null;

		try {
			consumer = session.createConsumer(this.destination, this.messageSelector);
			String nam = this.name;

			logger.info("Started consumer for destination " + nam);

			/*
			 * We may not use producer at all, but an empty producer does not
			 * hurt as much as creating it repeatedly..
			 */
			producer = session.createProducer(null);
			/*
			 * wait 0 means blocking-wait, 1 means try and come out.
			 */
			long wait = waitForMessage ? 0 : 1;
			/*
			 * loop for each message.
			 */
			do {
				if (waitForMessage) {
					logger.info("Looking/waiting for next message on " + nam);
				}
				Message msg = consumer.receive(wait);
				if (msg == null) {
					logger.info("No message in " + this.name + ". Consumer will not continue;");
					/*
					 * queue is shut down
					 */
					break;
				}
				/*
				 * let exception in one message not affect the over-all process
				 */
				try {
					/*
					 * data content of message is extracted into ctx
					 */
					this.extractMessage(msg, ctx);
					/*
					 * is the requester asking us to respond on a specific
					 * queue?
					 */
					Destination replyQ = msg.getJMSReplyTo();
					/*
					 * and the all important correlation id for the requester to
					 * select the message back
					 */
					String corId = msg.getJMSCorrelationID();
					if (replyQ == null && responseQ != null) {
						replyQ = responseQ.getDestination();
					}

					processor.process(ctx);
					if (replyQ != null) {
						Message respMsg = null;
						if (responseQ == null) {
							logger.info(
									"No response is specified for this consumer, but producer is asking for a reply. Sending a blank message");
							respMsg = session.createMessage();
						} else {
							/*
							 * prepare a reply based on specification
							 */
							respMsg = responseQ.createMessage(ctx);
						}
						if (corId != null) {
							respMsg.setJMSCorrelationID(corId);
						}
						producer.send(replyQ, respMsg);
					}
				} catch (Exception e) {
					ctx.addMessage(Messages.INTERNAL_ERROR, "Message processor threw an excpetion. " + e.getMessage());
				}
				if (consumeAll == false) {
					break;
				}
			} while (processor.toContinue());
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while consuming and procesing JMS queue " + this.name);

		} finally {
			if (consumer != null) {
				try {
					consumer.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (producer != null) {
				try {
					producer.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * produce message on this queue
	 *
	 * @param ctx
	 *            service context where this is all happening
	 * @param responseQ
	 *            in case we are to get a response for this message-send
	 *            operation
	 * @return true if a message indeed was put on the queue. False otherwise
	 */
	@SuppressWarnings("resource")
	public boolean produce(ServiceContext ctx, JmsDestination responseQ) {
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		Destination response = null;
		String corId = null;
		Session session = ctx.getJmsSession();
		try {
			producer = session.createProducer(this.destination);
			/*
			 * create a message with data from ctx
			 */
			Message msg = this.createMessage(ctx);

			/*
			 * should we ask for a return message?
			 */
			if (responseQ != null) {
				if (responseQ.name == null) {
					response = session.createTemporaryQueue();
					consumer = session.createConsumer(response);
				} else {
					response = responseQ.getDestination();
					corId = UUID.randomUUID().toString();
					consumer = session.createConsumer(response, "JMSCorrelationID='" + corId + '\'');
					msg.setJMSCorrelationID(corId);
				}
				msg.setJMSReplyTo(response);
			}
			producer.send(msg);
			/*
			 * checking for one of them is good enough, but compiler would crib.
			 * We would rather live with whatever is the over-head of checking
			 * for another null than loosing the null-check facility for this
			 * method
			 */
			if (consumer != null && responseQ != null) {
				Message message = consumer.receive();
				if (message == null) {
					/*
					 * some issue in the queue
					 */
					logger.info("Response message is null. Probably some issue with the queue provider");
					return false;
				}
				responseQ.extractMessage(message, ctx);
			}
			return true;
		} catch (Exception e) {
			logger.info("Error while putting mesage on to a queue. " + e.getMessage());
			return false;
		} finally {
			if (consumer != null) {
				try {
					consumer.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (producer != null) {
				try {
					producer.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * @param session
	 * @param ctx
	 * @return
	 * @throws JMSException
	 */
	@SuppressWarnings("resource")
	protected Message createMessage(ServiceContext ctx) throws JMSException {
		Session session = ctx.getJmsSession();
		if (this.dataFormatter != null) {
			String text = this.dataFormatter.format(ctx);
			return session.createTextMessage(text);
		}

		Application app = Application.getActiveInstance();
		if (this.messageBodyType == null) {
			/*
			 * properties are used for transporting data
			 */
			Message message = session.createMessage();
			if (this.fieldNames != null) {
				this.setHeaderFields(message, ctx, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = app.getRecord(this.recordName);
				this.setHeaderFields(message, ctx, record.getFieldNames());
			} else {

				logger.info("No fields specified to be added to the message.");
			}
			return message;
		}

		if (this.messageBodyType == DataSerializationType.MAP) {
			MapMessage message = session.createMapMessage();
			if (this.fieldNames != null) {
				this.setMapFields(message, ctx, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = app.getRecord(this.recordName);
				this.setMapFields(message, ctx, record.getFieldNames());
			} else {

				logger.info("No fields specified to be added Map.");
			}
			return message;
		}

		if (this.messageBodyType == DataSerializationType.OBJECT) {
			if (this.bodyFieldName == null) {
				throw new ApplicationError(
						"bodyFieldName is not specified for messaage body when messageBodyType is set to object");
			}
			Object object = ctx.getObject(this.bodyFieldName);
			if (object == null) {

				logger.info("Service context has no object named " + this.bodyFieldName
						+ ". No object assigned to message.");

				return session.createObjectMessage();
			}
			if (object instanceof Serializable) {
				return session.createObjectMessage((Serializable) object);
			}
			throw new ApplicationError("Service context has an instance of " + object.getClass().getName()
					+ " as object for message. This class must be serializable.");
		}

		/*
		 * so, it is a TextMessage. Our task is to create the text to be set to
		 * the message body
		 */
		TextMessage message = session.createTextMessage();
		String text = null;

		if (this.outputData != null) {
			StringWriter sr = new StringWriter();
			JsonRespWriter writer = new JsonRespWriter(sr);
			this.outputData.write(writer, ctx);
			writer.done();
			message.setText(sr.toString());
			return message;
		}

		if (this.bodyFieldName != null) {
			/*
			 * simplest of our task. text is readily available in this field.
			 */
			text = ctx.getTextValue(this.bodyFieldName);
			if (text == null) {

				logger.info("No value found for body text with field name " + this.bodyFieldName + ". Data not set.");

			} else {
				message.setText(text);
			}
			return message;
		}

		if (this.recordName != null) {
			Record record = app.getRecord(this.recordName);
			message.setText(this.messageBodyType.serializeFields(ctx, record.getFields()));
			return message;
		}
		if (this.fieldNames != null) {
			message.setText(this.messageBodyType.serializeFields(ctx, this.fieldNames));
			return message;
		}
		throw new ApplicationError("Record or field details are required for creating message");
	}

	/**
	 * @param message
	 * @param ctx
	 * @throws JMSException
	 */
	public void extractMessage(Message message, ServiceContext ctx) throws JMSException {
		if (this.dataExtractor != null) {
			logger.info("Using a custom class to extract data " + this.messageExtractor);
			if (message instanceof TextMessage == false) {
				throw new ApplicationError("Expecting a TextMessage on queue " + this.bodyFieldName + " but we got a "
						+ message.getClass().getSimpleName());
			}
			String text = ((TextMessage) message).getText();
			this.dataExtractor.extract(text, ctx);
			return;
		}
		Application app = Application.getActiveInstance();
		if (this.messageBodyType == null) {
			/*
			 * properties are used for transporting data
			 */
			if (this.extractAll) {
				this.extractAllFromHeader(message, ctx);
			} else if (this.fieldNames != null && this.fieldNames.length > 0) {
				this.extractHeaderFields(message, ctx, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = app.getRecord(this.recordName);
				this.extractHeaderFields(message, ctx, record.getFieldNames());
			} else {
				logger.info("No fields specified to be extracted from the message. Nothing extracted. ");
			}
			return;
		}
		/*
		 * we use three types of message body. TEXT, MAP and OBJECT
		 */
		if (this.messageBodyType == DataSerializationType.OBJECT) {
			if (message instanceof ObjectMessage == false) {
				logger.info("We expected a ObjectMessage but got " + message.getClass().getSimpleName()
						+ ". No object extracted.");
				return;
			}
			Object object = ((ObjectMessage) message).getObject();
			if (object == null) {
				logger.info("Messaage object is null. No object extracted.");
			} else if (this.bodyFieldName == null) {
				logger.info("bodyFieldName not set, and hence the object of instance " + object.getClass().getName()
						+ "  " + object + " not added to context.");
			} else {
				ctx.setObject(this.bodyFieldName, object);
			}
			return;
		}

		if (this.messageBodyType == DataSerializationType.MAP) {
			if (message instanceof MapMessage == false) {

				logger.info("We expected a MapMessage but got " + message.getClass().getSimpleName()
						+ ". No data extracted.");

				return;
			}
			MapMessage msg = (MapMessage) message;
			if (this.extractAll) {
				this.extractAllFromMap(ctx, msg);
			} else if (this.fieldNames != null) {
				this.extractMapFields(ctx, msg, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = app.getRecord(this.recordName);
				this.extractMapFields(ctx, msg, record.getFieldNames());
			} else {

				logger.info("No directive to extract any fields from this MapMessage.");
			}
			return;
		}

		if (message instanceof TextMessage == false) {
			logger.info(
					"We expected a TextMessage but got " + message.getClass().getSimpleName() + ". No data extracted.");
			return;
		}

		String text = ((TextMessage) message).getText();
		if (text == null) {
			logger.info("Messaage text is null. No data extracted.");
			return;
		}
		if (this.inputData != null) {
			JSONObject json;
			if (text.isEmpty()) {
				json = new JSONObject();
			} else {
				json = new JSONObject(text);
			}
			JsonReqReader reader = new JsonReqReader(json, null);
			this.inputData.read(reader, ctx);
			return;
		}
		if (this.bodyFieldName != null) {
			ctx.setTextValue(this.bodyFieldName, text);
			return;
		}
		if (this.recordName != null) {
			Record record = app.getRecord(this.recordName);
			this.messageBodyType.parseFields(text, ctx, record.getFields());
			return;
		}
		this.messageBodyType.parseFields(text, ctx, this.fieldNames, null);
		return;
	}

	/**
	 * @param ctx
	 * @param message
	 * @param names
	 * @throws JMSException
	 */
	private void extractMapFields(ServiceContext ctx, MapMessage message, String[] names) throws JMSException {
		for (String nam : names) {
			Object val = message.getObject(nam);
			if (val != null) {
				ctx.setValue(nam, Value.parse(val));
			}
		}
	}

	/**
	 * @param ctx
	 * @param message
	 * @throws JMSException
	 */
	private void extractAllFromMap(ServiceContext ctx, MapMessage message) throws JMSException {
		@SuppressWarnings("unchecked")
		Enumeration<String> names = message.getMapNames();
		while (true) {
			try {
				String nam = names.nextElement();
				Object val = message.getObject(nam);
				if (val != null) {
					ctx.setValue(nam, Value.parse(val));
				}
			} catch (NoSuchElementException e) {
				/*
				 * unfortunately we have to live with this old-styled exception
				 * for a normal event!!!
				 */
				return;
			}
		}
	}

	/**
	 * @param message
	 * @param ctx
	 * @throws JMSException
	 */
	private void extractAllFromHeader(Message message, ServiceContext ctx) throws JMSException {
		@SuppressWarnings("unchecked")
		Enumeration<String> names = message.getPropertyNames();
		while (true) {
			try {
				String nam = names.nextElement();
				Object val = message.getObjectProperty(nam);
				if (val != null) {
					ctx.setValue(nam, Value.parse(val));
				}
			} catch (NoSuchElementException e) {
				/*
				 * unfortunately we have to live with this old-styled exception
				 * for a normal event!!!
				 */
				return;
			}
		}
	}

	/**
	 * @param message
	 * @param ctx
	 * @param names
	 * @throws JMSException
	 */
	private void extractHeaderFields(Message message, ServiceContext ctx, String[] names) throws JMSException {
		for (String nam : names) {
			Object val = message.getObjectProperty(nam);
			if (val != null) {
				ctx.setValue(nam, Value.parse(val));
			}
		}
	}

	/**
	 * @param message
	 * @param ctx
	 * @param names
	 * @throws JMSException
	 */
	private void setHeaderFields(Message message, ServiceContext ctx, String[] names) throws JMSException {
		for (String nam : names) {
			Value val = ctx.getValue(nam);
			if (val == null) {

				logger.info("No value for " + nam + ". Value not set to message header.");

			} else {
				message.setObjectProperty(nam, val.toObject());
			}
		}
	}

	/**
	 * @param message
	 * @param ctx
	 * @param names
	 * @throws JMSException
	 */
	private void setMapFields(MapMessage message, ServiceContext ctx, String[] names) throws JMSException {
		for (String nam : names) {
			Value val = ctx.getValue(nam);
			if (val == null) {

				logger.info("No value for " + nam + ". Value not set to Map.");

			} else {
				message.setObject(nam, val.toObject());
			}
		}
	}

	/** open shop and be ready for a repeated use */
	public void getReady() {
		/*
		 * avoid repeated check for empty array
		 */
		if (this.fieldNames != null && this.fieldNames.length == 0) {
			this.fieldNames = null;
		}
		/*
		 * cache object instance
		 */
		if (this.messageExtractor != null) {
			try {
				this.dataExtractor = (IDataDeserializer) Class.forName(this.messageExtractor).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						"Error while creating an instance of DataExtractor for " + this.messageExtractor);
			}
		}

		/*
		 * cache object instance
		 */
		if (this.messageFormatter != null) {
			try {
				this.dataFormatter = (IDataSerializer) Class.forName(this.messageFormatter).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						"Error while creating an instance of DataFormatter for " + this.messageFormatter);
			}
		}
		try {
			this.destination = (Destination) new InitialContext().lookup(this.name);
		} catch (Exception e) {
			throw new ApplicationError("Jms destination name " + this.name
					+ " could not be used as a JNDI name to locate a queue name. " + e.getMessage());
		}
		if (this.inputData != null) {
			this.inputData.getReady();
		}
		if (this.outputData != null) {
			this.outputData.getReady();
		}
	}

	/** @return name of this queue */
	public Destination getDestination() {
		return this.destination;
	}

	/**
	 * validate attributes
	 *
	 * @param vtx
	 *            validation context
	 * @param forProducer
	 */
	public void validate(IValidationContext vtx, boolean forProducer) {
		/*
		 * fieldNames and recordName are two ways to specify list if fields
		 */
		boolean fieldListSpecified = this.recordName != null || (this.fieldNames != null && this.fieldNames.length > 0);
		/*
		 * fieldName is required if we are set/get message body directly from
		 * one field rather than constructing it from other fields, or
		 * extracting it to other fields
		 */
		boolean fieldNameRequired = this.messageBodyType == DataSerializationType.TEXT
				|| this.messageBodyType == DataSerializationType.OBJECT;

		/*
		 * now let is start our role as an auditor - find anything unusual :-)
		 */
		if (this.bodyFieldName == null) {
			if (fieldNameRequired) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"messageBodyType=text/object requires bodyFieldName to which this text/object is to be assigned from/to",
						"bodyFieldName"));
			}
		} else {
			if (!fieldNameRequired) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"bodyFieldName is used for messageBodyType of object and text only. It is ignored otherwise.",
						"messageBodyType"));
			}
		}
		/*
		 * custom extractor
		 */
		if (this.messageExtractor != null) {
			if (forProducer) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"messageExtractor is used when the queue is used for consuming/reading message.",
						"messageExtractor"));
			} else {
				if (fieldListSpecified) {
					vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
							"messageExtractor is used for extrating data. fieldNames, and reordName settings are ignored.",
							null));
				}
			}
			if (this.messageBodyType != DataSerializationType.TEXT) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"messageExtractor is used when the message body usage is text. this.messageBodyType is set to "
								+ this.messageBodyType + ". we will ignore this setting and assume text body.",
						"messageBodyType"));
			}
		}

		/*
		 * custom formatter
		 */
		if (this.messageFormatter != null) {
			if (!forProducer) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"messageFormatter is used when message is to be created/produced. Setting ignored",
						"messageFormatter"));
			} else {
				if (fieldListSpecified) {
					vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
							"messageFormatter is used for formatting message body. fieldNames, and reordName settings are ignored.",
							"messageFormatter"));
				}
			}
			if (this.messageBodyType != DataSerializationType.TEXT) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"messageFormatter is used when the message body usage is text. this.messageBodyType is set to "
								+ this.messageBodyType + ". we will ignore this setting and assume text body.",
						"messageBodyType"));
			}
		}

		/*
		 * record name is required for fixed-width
		 */
		if (this.recordName != null) {
			if (fieldNameRequired) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"recordName is ignored for message body type of text/object.", "recordName"));
			}
		}

		/*
		 * no data specification?
		 */
		if (fieldNameRequired == false && fieldListSpecified == false) {
			if (forProducer) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"No fields/records specified. Message is designed to carry no data.", null));
			} else if (!this.extractAll) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"No fields/records specified, and extractAll is set to false. Message consumer is not looking for any data in this message.",
						"extractAll"));
			}
		}

		if (this.bodyFieldName == null) {
			if (this.messageBodyType == DataSerializationType.OBJECT
					|| this.messageBodyType == DataSerializationType.TEXT) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"messageBodyType=object or text requires bodyFieldName to which the body object/text is to be assigned to.",
						"bodyFieldName"));
			}
		}

		if (this.extractAll && forProducer) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
					"extractAll is not relevant when the queue is used to produce/send message. Attribute ignored.",
					"extractAll"));
		}
	}

	/**
	 * @param ctx
	 * @return a worker instance that can work as a driver input for a batch
	 */
	public JmsInput getBatchInput(ServiceContext ctx) {
		return new JmsInput();
	}

	/**
	 * @param ctx
	 * @return a worker instance that can produce messages onto a queue
	 */
	public JmsOutput getBatchOutput(ServiceContext ctx) {
		return new JmsOutput();
	}

	/** @return name of the queue */
	public String getName() {
		return this.name;
	}

	/**
	 * worker class that inputs a message from this queue as input for a batch
	 * process
	 *
	 * @author simplity.org
	 */
	public class JmsInput implements IBatchInput {
		private MessageConsumer consumer;

		protected JmsInput() {
			//
		}

		@Override
		public void openShop(ServiceContext ctx) throws JMSException {
			@SuppressWarnings("resource")
			Session session = ctx.getJmsSession();
			this.consumer = session.createConsumer(JmsDestination.this.destination,
					JmsDestination.this.messageSelector);

			logger.info("Started consumer for queue " + JmsDestination.this.name);
		}

		@Override
		public void closeShop(ServiceContext ctx) {
			if (this.consumer != null) {
				try {
					this.consumer.close();
				} catch (Exception ignore) {
					//
				}
			}
		}

		@Override
		public boolean possiblyMultipleRowsPerParent() {
			return false;
		}

		@Override
		public boolean inputARow(ServiceContext ctx) throws JMSException {
			/*
			 * we should read, if there is some message. We use 1ms for this
			 */
			Message msg = this.consumer.receive(1);
			if (msg == null) {

				logger.info("No more messages in " + JmsDestination.this.name + ". Queue consumer will not continue;");

				return false;
			}
			JmsDestination.this.extractMessage(msg, ctx);
			return true;
		}

		/*
		 * this method should never be used
		 */
		@Override
		public boolean inputARow(String parentKey, ServiceContext ctx)
				throws InvalidRowException {
			throw new ApplicationError("JMS Queue can not be used to get messages for a gievn parent.");
		}

		@Override
		public String getParentKeyValue(ServiceContext ctx) {
			return null;
		}

		@Override
		public String getFileName() {
			return null;
		}
	}

	/**
	 * class that works as output for a batch row processor
	 *
	 * @author simplity.org
	 */
	public class JmsOutput implements IBatchOutput {
		private MessageProducer producer;

		@Override
		public void openShop(ServiceContext ctx) throws JMSException {
			@SuppressWarnings("resource")
			Session session = ctx.getJmsSession();
			this.producer = session.createProducer(JmsDestination.this.destination);
		}

		@Override
		public void closeShop(ServiceContext ctx) {
			if (this.producer != null) {
				try {
					this.producer.close();
				} catch (Exception ignore) {
					//
				}
			}
		}

		@Override
		public boolean outputARow(ServiceContext ctx) throws JMSException {
			/*
			 * create a message with data from ctx
			 */
			Message msg = JmsDestination.this.createMessage(ctx);
			this.producer.send(msg);
			return true;
		}
	}
}
