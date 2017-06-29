package org.simplity.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.ConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQMessage;
import org.h2.tools.RunScript;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;
import org.simplity.kernel.Application;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Property;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.ldap.LdapProperties;
import org.simplity.kernel.mail.MailConnector;
import org.simplity.kernel.mail.MailProperties;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.DataExtractor;
import org.simplity.service.JavaAgent;
import org.simplity.service.PayloadType;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;
import org.simplity.test.mock.ldap.MockInitialContextFactory;
import org.simplity.test.mock.ldap.MockInitialDirContextFactory;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

public class ActionsTest extends Mockito {
	private static final String COMP_PATH = "comp/";

	private static InitialContext initialContext;

	private static javax.jms.Session jmsSession;

	@Mock
	HttpServletRequest request;

	@Mock
	HttpServletResponse response;

	@Mock
	HttpSession session;

	@Mock
	ServletContext servletContext;

	@Mock
	RequestDispatcher rd;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@BeforeClass
	public static void setUp() throws Exception {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		String compFolder = classloader.getResource(".").getPath().concat(COMP_PATH);
		final String testFolder = classloader.getResource(".").getPath();

		ServletContext context = mock(ServletContext.class);

		MockInitialDirContextFactory dirfactory = mock(MockInitialDirContextFactory.class);

		Class<Hashtable<?, ?>> clazz = null;
		when(dirfactory.getInitialContext(any(clazz))).thenAnswer(new Answer<Context>() {

			@Override
			public Context answer(InvocationOnMock invocation) throws Throwable {
				System.out.println("hello");
				return (DirContext) Mockito.mock(DirContext.class);
			}
		});

		ComponentType.setComponentFolder(compFolder);
		FileManager.setContext(context);

		when(context.getResourceAsStream(anyString())).thenAnswer(new Answer<InputStream>() {
			public InputStream answer(InvocationOnMock invocation) throws Throwable {
				InputStream is = null;
				String newPath = (String) invocation.getArguments()[0];
				if (newPath.startsWith("/")) {
					newPath = newPath.substring(1);
				}
				File file = new File(TestUtils.getFile(newPath));
				if (file.exists()) {
					is = new FileInputStream(file);
					return is;
				}
				file = new File(TestUtils.getFile(newPath, testFolder));
				is = new FileInputStream(file);

				return is;
			}
		});

		when(context.getResourcePaths(anyString())).thenAnswer(new Answer<Set<String>>() {
			public Set<String> answer(InvocationOnMock invocation) throws Throwable {
				return TestUtils
						.listFiles(new File(TestUtils.getFile((String) invocation.getArguments()[0], testFolder)));
			}
		});

		Application app = new Application();
		XmlUtil.xmlToObject(compFolder + "applicationH2.xml", app);
		/*
		 * app.configure() takes care of all initial set up
		 */
		app.configure();

		initialContext = new InitialContext();
		ConnectionFactory connectionFactory = (ConnectionFactory) initialContext
				.lookup("vm://localhost?broker.persistent=false");
		javax.jms.Connection connection = (javax.jms.Connection) connectionFactory.createConnection();
		jmsSession = connection.createSession(false, javax.jms.Session.DUPS_OK_ACKNOWLEDGE);
		connection.start();

		DirContext mockContext = LdapProperties.getInitialDirContext();

		when(mockContext.getAttributes("CN=Sunita Williams")).thenAnswer(new Answer<Attributes>() {
			@Override
			public Attributes answer(InvocationOnMock invocation) throws NamingException {
				Attributes attrs = new BasicAttributes();
				attrs.put("surname", "Williams");
				return attrs;
			}

		});

		when(mockContext.lookup("CN=Sunita Williams")).thenAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) {
				return new Boolean(true);
			}

		});

		String[] attrIDs = { "surname" };
		when(mockContext.getAttributes("CN=Sunita Williams", attrIDs)).then(new Answer<Attributes>() {

			@Override
			public Attributes answer(InvocationOnMock invocation) throws Throwable {
				Attributes attrs = new BasicAttributes();
				attrs.put("surname", "Williams");
				return attrs;
			}

		});
	}

	/**
	 * Test for setValue action with fieldnames @throws Exception
	 */
	@Test
	public void setValueTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("test.SetValue", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals("Peter Pan", obj.getString("leader"));
		assertEquals("Captain Hook,Mr.Smee", obj.getString("adversaries"));
	}

	/**
	 * Test for setValue action with fieldnames @throws Exception
	 */
	@Test
	public void LogicTest() {
		String payLoad = "{'switch':'winner'}";
		ServiceData outData = JavaAgent.getAgent("100",null).serve("test.Logic", payLoad,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals("Peter Pan", obj.getString("leader"));

		assertEquals("Captain Hook,Mr.Smee", obj.getString("adversaries"));

		assertEquals(((JSONObject) ((JSONArray) obj.get("weekendBoxOffice")).get(0)).get("Theaters"), "465");

		assertEquals(obj.get("emptyDS"), "Sheet is empty");

		assertEquals(obj.getInt("checkExpression"), 3);

		// Check for defaultValue assignment to Inputfield
		assertEquals(obj.get("switch"), "winner");

		exception.expect(JSONException.class);
		obj.get("NotEmptyDS");
	}

	/**
	 * Test for addColumn action
	 */
	@Test
	public void addColumnTest() {

		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.addColumn", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("weekendBoxOffice")).get(0)).get("testcolumn"), "testValue");
	}

	/**
	 * Test for copyRows action
	 */
	@Test
	public void copyRowsTest() {

		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.copyRows", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("copiedweekendBoxOffice")).get(0)).get("Theaters"), "465");
	}

	/**
	 * Test for renameSheet action
	 */
	@Test
	public void renameSheetTest() {

		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.renameSheet", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("newweekendBoxOffice")).get(0)).get("Theaters"), "465");
	}

	/**
	 * Test for add Message action
	 */
	@Test
	public void addMessageTest() {

		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.addMessage", null,PayloadType.JSON);
		FormattedMessage[] msgs = outData.getMessages();

		assertEquals(msgs[0].text, "NeverLand Custom Message From My Messages XML File");
	}

	/**
	 * Test for add createSheet action
	 */
	@Test
	public void createSheetTest() {

		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.createSheet", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("newsheet")).get(0)).get("text"),
				"first row first column text");
	}

	/**
	 * Test for jumpToTest action
	 */
	@Test
	public void jumpToTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.jumpTo", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(false, obj.has("adversary1"));
	}

	/**
	 * Test for copyUserIdTest action
	 */
	@Test
	public void copyUserIdTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.copyUserId", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("UserIDCopy"), "100");
	}

	/**
	 * Test for filter action
	 */
	@Test
	public void filterTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.filterTest", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("customers")).get(0)).get("customerName"),
				"Signal Gift Stores");
	}

	/**
	 * Test for rowExists action
	 */
	@Test
	public void rowExistsTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.rowExists", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for read action
	 */
	@Test
	public void readDataTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.readData", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("customerName"), "Signal Gift Stores");
	}

	/**
	 * Test for suggest action
	 */
	@Test
	public void suggestTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.suggest", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("Employees")).get(0)).get("firstName"), "Mary");
	}

	/**
	 * Test for saveDataDelete action
	 */
	@Test
	public void saveDataDeleteTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.saveDataDelete", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for saveDataAdd action
	 */
	@Test
	public void saveDataAddTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("dbactions.saveDataAdd", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for saveDataAdd action
	 */
	@Test
	public void saveDataModifyTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.saveDataModify", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for read with sql action
	 */
	@Test
	public void readWithSqlTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("dbactions.readWithSql", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("Employees")).get(0)).get("lastName"), "Patterson");
	}

	/**
	 * Test for executesql action
	 */
	@Test
	public void executeSqlTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.executeSql", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("updateSql"), 1);
	}

	@Test
	public void schemaReadWithSqlTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("tutorial.schemaReadWithSql", null,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("Students")).get(0)).get("name"), "Sham");
	}

	@Test
	public void ldapAuthenticate1Test() {
		String payLoad = "{'_userId':'winner','_userToken':'pwd'}";
		ServiceData outData = JavaAgent.getAgent("100",null).serve("ldap.ldapAuth", payLoad,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals((String) obj.get("_userId"), "winner");
	}

	@Test
	public void ldapAuthenticate2Test() {
		String payLoad = "{'_userId':'rogue','_userToken':'guess'}";
		ServiceData outData = JavaAgent.getAgent("100",null).serve("ldap.ldapAuth", payLoad,PayloadType.JSON);
		assertEquals(outData.hasErrors(), true);
	}



	/*
	 * Test method for org.simplity.tp.SendMail
	 */
	@Test
	public void sendMailTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("test.sendMail", null,PayloadType.JSON);

		try {
			
			Properties props = System.getProperties();			
			props.putAll(MailProperties.getProperties());
			
			props.setProperty("mail.store.protocol", "imaps");
			props.setProperty("mail.imap.partialfetch", "0");


			Session session = Session.getInstance(props, null);
			
			Store store = session.getStore("imap");
			store.connect(props.getProperty("mail.smtp.host"), "bar", "samplepassword");
			Folder folder = store.getDefaultFolder();
			folder = folder.getFolder("inbox");
			folder.open(Folder.READ_ONLY);
			assertEquals(1, folder.getMessages().length);
			for (Message message : folder.getMessages()) {
				assertEquals((String) message.getSubject(), "Simplity - sample subject");
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void ldapLookupExistsTest() {
		String payLoad = "{'objectId':'CN=Sunita Williams','fieldname':'ldapLookup'}";
		ServiceData outData = JavaAgent.getAgent("100",null).serve("ldap.ldapLookupExists", payLoad,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals((Boolean) obj.get("ldapLookup"), true);
	}

	@Test
	public void ldapLookupSingleAttrTest() {
		String payLoad = "{'objectId':'CN=Sunita Williams','attrName':'surname'}";
		ServiceData outData = JavaAgent.getAgent("100",null).serve("ldap.ldapLookupSingleAttr", payLoad,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals((String) obj.get("surname"), "Williams");
	}

	@Test
	public void ldapLookupMultiAttrTest() {
		String payLoad = "{'objectId':'CN=Sunita Williams','outputDataSheetName':'outsheet'}";
		ServiceData outData = JavaAgent.getAgent("100",null).serve("ldap.ldapLookupMultiAttr", payLoad,PayloadType.JSON);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		JSONArray arr = obj.getJSONArray("outsheet");
		assertEquals(arr.getJSONObject(0).get("value").toString(), "Williams");
	}

	/**
	 * Test method for
	 * {@link org.simplity.kernel.util.XmlUtil#xmlToObject(java.io.InputStream, java.lang.Object)}
	 * .
	 */
	@Test
	public final void recordProcXmlToObject() {
		Object object = new Record();
		try {
			File fs = new File("src/test/resources/xml/record.xml");
			InputStream fis = new FileInputStream(fs);

			XmlUtil.xmlToObject(fis, object);

		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNotNull(object);
		assertEquals(Record.class, object.getClass());

	}

	@Test
	public void fileProcessingTest() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("batchProcess.FileInFileOutGroupBy", null,PayloadType.JSON);
	}

	@Test
	public void batchProcessingTest() {
		InitialContext ic;
		try {
			ic = new InitialContext();

			ConnectionFactory connectionFactory = (ConnectionFactory) ic
					.lookup("vm://localhost?broker.persistent=false");
			javax.jms.Connection connection = (javax.jms.Connection) connectionFactory.createConnection();
			javax.jms.Session jmsSession = connection.createSession(false,
					javax.jms.Session.DUPS_OK_ACKNOWLEDGE);
			connection.start();

			Destination destination = (Destination) ic.lookup("jms/Queue02");
			MessageConsumer consumer = jmsSession.createConsumer(destination);
			MessageListener messageListener = jmsSession.getMessageListener();
			consumer.setMessageListener(messageListener);
			ServiceData outData = JavaAgent.getAgent("100",null).serve("batchProcess.DbInFileOut", null,PayloadType.JSON);
			QueueBrowser qBrowser = jmsSession.createBrowser((Queue) destination);
			Enumeration qe = qBrowser.getEnumeration();
			while (qe.hasMoreElements()) {
				ActiveMQMessage receiveMessage = (ActiveMQMessage) qe.nextElement();
				// assertEquals(receiveMessage.getProperty("personId"),
				// "personid123");
			}

		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void batchProcessingTest1() {
		ServiceData outData = JavaAgent.getAgent("100",null).serve("batchProcess.FileInFileOut", null,PayloadType.JSON);
	}

	@Test
	public void batchProcessingTest2() {
		InitialContext ic;
		try {
			ic = new InitialContext();

			ConnectionFactory connectionFactory = (ConnectionFactory) ic
					.lookup("vm://localhost?broker.persistent=false");
			javax.jms.Connection connection = (javax.jms.Connection) connectionFactory.createConnection();
			javax.jms.Session jmsSession = connection.createSession(false,
					javax.jms.Session.DUPS_OK_ACKNOWLEDGE);
			connection.start();

			Destination destination = (Destination) ic.lookup("jms/Queue02");
			MessageConsumer consumer = jmsSession.createConsumer(destination);
			MessageListener messageListener = jmsSession.getMessageListener();
			consumer.setMessageListener(messageListener);
			ServiceData outData = JavaAgent.getAgent("100",null).serve("batchProcess.FileInJMSOut", null,PayloadType.JSON);
			QueueBrowser qBrowser = jmsSession.createBrowser((Queue) destination);
			Enumeration qe = qBrowser.getEnumeration();
			int outMessagesCount = 0;
			while (qe.hasMoreElements()) {
				ActiveMQMessage receiveMessage = (ActiveMQMessage) qe.nextElement();
				outMessagesCount++;
			}
			assertEquals(5, outMessagesCount);
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void batchProcessingTest3() {
		try {
			Destination destination = (Destination) initialContext.lookup("jms/Queue02");
			MessageProducer producer = jmsSession.createProducer(destination);

			// loop
			javax.jms.Message message = jmsSession.createMessage();
			message.setObjectProperty("id2", "1");
			message.setObjectProperty("name2", "abcd");
			message.setObjectProperty("address2", "addr1");

			producer.send(message);
			// end loop

			QueueBrowser queueBrowser = jmsSession.createBrowser((Queue) destination);

			int numOfTries = 3;
			Enumeration<Object> queueBrowserEnumeration = null;
			for (numOfTries = 3; numOfTries > 0; numOfTries--) {
				try {
					Thread.currentThread().sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				queueBrowserEnumeration = queueBrowser.getEnumeration();
				if (queueBrowserEnumeration.hasMoreElements()) {
					break;
				}
			}
			if (queueBrowserEnumeration.hasMoreElements()) {
				ServiceData outData = JavaAgent.getAgent("100",null).serve("batchProcess.JMSInFileOut", null,PayloadType.JSON);
			}
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void batchProcessingTest4() {
		try {
			Destination destination = (Destination) initialContext.lookup("jms/Queue02");
			MessageProducer producer = jmsSession.createProducer(destination);

			// loop
			javax.jms.Message message = jmsSession.createMessage();
			message.setObjectProperty("id2", "1");
			message.setObjectProperty("name2", "abcd");
			message.setObjectProperty("address2", "addr1");

			producer.send(message);
			// end loop

			QueueBrowser queueBrowser = jmsSession.createBrowser((Queue) destination);

			int numOfTries = 3;
			Enumeration<Object> queueBrowserEnumeration = null;
			for (numOfTries = 3; numOfTries > 0; numOfTries--) {
				try {
					Thread.currentThread().sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				queueBrowserEnumeration = queueBrowser.getEnumeration();
				if (queueBrowserEnumeration.hasMoreElements()) {
					break;
				}
			}
			if (queueBrowserEnumeration.hasMoreElements()) {
				ServiceData outData = JavaAgent.getAgent("100",null).serve("batchProcess.JMSInFileOut", null,PayloadType.JSON);
			}
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	
	@Test
	public void jmsQueueProducerTest() {
		try {
			Destination destination = (Destination) initialContext.lookup("jms/Queue01");
			String payLoad = "{'id':'1'," + "'personId':'personid123'," + "'comments':'comments123',"
					+ "'tokens':'token123'}";
			ServiceData producerData = JavaAgent.getAgent("100",null).serve("jms.jmsProducer", payLoad,PayloadType.JSON);
			QueueBrowser queueBrowser = jmsSession.createBrowser((Queue) destination);

			int numOfTries = 3;
			Enumeration<Object> queueBrowserEnumeration = null;
			for (numOfTries = 3; numOfTries > 0; numOfTries--) {
				queueBrowserEnumeration = queueBrowser.getEnumeration();
				if (queueBrowserEnumeration.hasMoreElements()) {
					break;
				}
			}

			assertEquals(queueBrowserEnumeration.hasMoreElements(), true);
			if (queueBrowserEnumeration.hasMoreElements()) {
				ActiveMQMessage queueMessage = (ActiveMQMessage) queueBrowserEnumeration.nextElement();
				assertEquals(queueMessage.getProperty("personId"), "personid123");
				assertEquals(queueMessage.getProperty("comments"), "comments123");
				assertEquals(queueMessage.getProperty("tokens"), "token123");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void jmsQueueConsumerTest() {
		try {
			Destination destination = (Destination) initialContext.lookup("jms/Queue01");
			String payLoad = "{'id':'1'," + "'personId':'personid123'," + "'comments':'comments123',"
					+ "'tokens':'token123'}";
			ServiceData producerData = JavaAgent.getAgent("100",null).serve("jms.jmsProducer", payLoad,PayloadType.JSON);
			QueueBrowser queueBrowser = jmsSession.createBrowser((Queue) destination);
			int numOfTries = 3;
			Enumeration queueBrowserEnumeration = null;
			for (numOfTries = 3; numOfTries > 0; numOfTries--) {
				queueBrowserEnumeration = queueBrowser.getEnumeration();
				if (queueBrowserEnumeration.hasMoreElements()) {
					break;
				}
			}
			assertEquals(queueBrowserEnumeration.hasMoreElements(), true);

			if (queueBrowser.getEnumeration().hasMoreElements()) {
				ServiceData consumerData = JavaAgent.getAgent("100",null).serve("jms.jmsConsumer", null,PayloadType.JSON);
				JSONObject consumerObject = new JSONObject(consumerData.getPayLoad());
				JSONArray commentSheet = (JSONArray) consumerObject.get("commentSheet");				
				for (int i = 0; i < commentSheet.length(); i++) {
					JSONObject commentSheetRow = (JSONObject) commentSheet.get(i);
					assertEquals(commentSheetRow.get("personId"), "personid123");
					assertEquals(commentSheetRow.get("comments"), "comments123");
					assertEquals(commentSheetRow.get("tokens"), "token123");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void jmsTopicProducerTest() {
		try {
			Topic topic = (Topic) initialContext.lookup("jms/Topic01");
			TopicSubscriber topicSubscriber01 = ((TopicSession)jmsSession).createSubscriber(topic);
			TopicSubscriber topicSubscriber02 = ((TopicSession)jmsSession).createSubscriber(topic);
		    TopicPublisher topicPublisher = ((TopicSession)jmsSession).createPublisher(topic);
			
			String payLoad = "{'id':'1'," + "'personId':'personid123'," + "'comments':'comments123',"
					+ "'tokens':'token123'}";
			ServiceData producerData = JavaAgent.getAgent("100",null).serve("jms.jmsTopicProducer", payLoad,PayloadType.JSON);
			
			ActiveMQMessage topicMessage01 = (ActiveMQMessage) topicSubscriber01.receive();
			assertEquals(topicMessage01.getProperty("personId"), "personid123");
			assertEquals(topicMessage01.getProperty("comments"), "comments123");
			assertEquals(topicMessage01.getProperty("tokens"), "token123");
			
			ActiveMQMessage topicMessage02 = (ActiveMQMessage) topicSubscriber02.receive();
			assertEquals(topicMessage02.getProperty("personId"), "personid123");
			assertEquals(topicMessage02.getProperty("comments"), "comments123");
			assertEquals(topicMessage02.getProperty("tokens"), "token123");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*@SuppressWarnings("unused")
	@Test
	public void jmsTopicConsumerTest() {
		try {
			Topic topic = (Topic) initialContext.lookup("jms/Topic01");
			TopicSubscriber topicSubscriber = ((TopicSession)jmsSession).createSubscriber(topic);
		    TopicPublisher topicPublisher = ((TopicSession)jmsSession).createPublisher(topic);
		    topicPublisher.setTimeToLive(10000);
		    topicPublisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			TextMessage message = ((TopicSession)jmsSession).createTextMessage();
			String payLoad = "commentSheet={'id':'1'," + "'personId':'personid123'," + "'comments':'comments123',"
					+ "'tokens':'token123'}";
			message.setText(payLoad);
			topicPublisher.publish(message);
			
			ServiceData consumerData = JavaAgent.getAgent("100",null).serve("jms.jmsTopicConsumer", null,PayloadType.JSON);
			JSONObject consumerObject = new JSONObject(consumerData.getPayLoad());
			JSONArray commentSheet = (JSONArray) consumerObject.get("commentSheet");				
			for (int i = 0; i < commentSheet.length(); i++) {
				JSONObject commentSheetRow = (JSONObject) commentSheet.get(i);
				assertEquals(commentSheetRow.get("personId"), "personid123");
				assertEquals(commentSheetRow.get("comments"), "comments123");
				assertEquals(commentSheetRow.get("tokens"), "token123");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	@Test
	public void hystricsTestSynchronous() {
		HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
        	String payLoad = "{'inputData':'Passed'}";
        	ServiceData outData = JavaAgent.getAgent("100",null).serve("test.hystrixTestSynchronous", payLoad,PayloadType.JSON);
        	JSONObject obj = new JSONObject(outData.getPayLoad());
        	assertEquals((String) obj.get("response"), "Test Passed");
        } finally {
        	context.shutdown();
        }
	}
	
	@Test
	public void hystricsTestAsynchronous() {
		HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
			String payLoad = "{'inputData':'Passed'}";
			ServiceData outData = JavaAgent.getAgent("100",null).serve("test.hystrixTestAsynchronous", payLoad,PayloadType.JSON);
			JSONObject obj = new JSONObject(outData.getPayLoad());
			assertEquals((String) obj.get("response"), "Test Passed");
        } finally {
        	context.shutdown();
        }
	}
	
	@Test
	public void hystricsFallbackTest() {
		HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
			String payLoad = "{'inputData':'Fallback'}";
			ServiceData outData = JavaAgent.getAgent("100",null).serve("test.hystrixTestSynchronous", payLoad,PayloadType.JSON);
			JSONObject obj = new JSONObject(outData.getPayLoad());
			assertEquals((String) obj.get("response"), "Fallback activated");
	    } finally {
	    	context.shutdown();
	    }
	}
	
	@Test
	public void hystricsCacheTestSynchronous() {
		HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
        	String payLoad = "{'inputData':'Passed'}";
    		ServiceData outData = JavaAgent.getAgent("100",null).serve("test.hystrixTestSynchronous", payLoad,PayloadType.JSON);
    		JSONObject obj = new JSONObject(outData.getPayLoad());
    		assertEquals((String) obj.get("response"), "Test Passed");
    		assertEquals((String) obj.get("isResponseFromCache"), "false");
    		
    		outData = JavaAgent.getAgent("100",null).serve("test.hystrixTestSynchronous", payLoad,PayloadType.JSON);
    		obj = new JSONObject(outData.getPayLoad());
    		assertEquals((String) obj.get("response"), "Test Passed");
    		assertEquals((String) obj.get("isResponseFromCache"), "true");
        } finally {
        	context.shutdown();
        }
		
        context = HystrixRequestContext.initializeContext();
        try {
        	String payLoad = "{'inputData':'Passed'}";
    		ServiceData outData = JavaAgent.getAgent("100",null).serve("test.hystrixTestSynchronous", payLoad,PayloadType.JSON);
    		JSONObject obj = new JSONObject(outData.getPayLoad());
    		assertEquals((String) obj.get("response"), "Test Passed");
    		assertEquals((String) obj.get("isResponseFromCache"), "false");
        } finally {
        	context.shutdown();
        }
        
	}
}
