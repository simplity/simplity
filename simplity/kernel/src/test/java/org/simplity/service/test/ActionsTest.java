package org.simplity.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.ldap.LdapAgent;
import org.simplity.kernel.mail.MailConnector;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;
import org.simplity.test.mock.ldap.MockInitialDirContextFactory;

public class ActionsTest extends Mockito {
	private static final String COMP_PATH = "comp/";

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

	// private static final int SMTP_TEST_PORT = 3025;

	@BeforeClass
	public static void setUp() throws Exception {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		String compFolder = classloader.getResource(".").getPath().concat(COMP_PATH);
		final String testFolder = classloader.getResource(".").getPath();
		MockitoAnnotations.initMocks(ActionsTest.class);

		ServletContext context = mock(ServletContext.class);

		MockInitialDirContextFactory factory = mock(MockInitialDirContextFactory.class);
		
		Class<Hashtable<?, ?>> clazz = null;
		when(factory.getInitialContext(any(clazz))).thenAnswer(new Answer<Context>() {

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

		MailConnector mailConnector = mock(MailConnector.class);
		when(mailConnector.initialize()).then(new Answer<Session>() {

			@Override
			public Session answer(InvocationOnMock invocation) throws Throwable {
				return ActionsTest.getMailSession();
			}

		});

		DirContext mockContext = LdapAgent.getInitialDirContext();
			
		
		 when(mockContext.getAttributes("CN=Sunita Williams")).thenAnswer(new Answer<Attributes>(){

			@Override
			public Attributes answer(InvocationOnMock invocation) throws NamingException {
				Attributes attrs = new BasicAttributes();
				attrs.put("surname", "Williams");
				return attrs;
			}
			 
		 });

		/*
		 * Load the db data
		 */
		Class.forName("org.h2.Driver");
		Connection conn = DriverManager.getConnection("jdbc:h2:~/classicmodels");
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("CALL DATABASE_PATH()");
		rs.next();
		String dbPath = rs.getString(1);

		// create destination dir
		File destFile = new File(dbPath);
		if (!destFile.exists()) {
			destFile.mkdir();
		}
		TestUtils.copyDirectory(new File("src/test/resources/data/datafiles"), destFile);
		RunScript.execute(conn,
				new FileReader(new File("src/test/resources/data/scripts/create_classicmodels.sql").getAbsolutePath()));

		// Schema Details
		Connection schemaConn = DriverManager.getConnection("jdbc:h2:~/simplitykernel/db/classicmodels;");
		Statement st1 = schemaConn.createStatement();
		ResultSet rs1 = st1.executeQuery("CALL DATABASE_PATH()");
		rs1.next();
		String dbPath1 = rs1.getString(1);
		// create destination dir
		File destFile1 = new File(dbPath1);
		if (!destFile1.exists()) {
			destFile1.mkdir();
		}
		TestUtils.copyDirectory(new File("src/test/resources/data/datafiles"), destFile1);
		RunScript.execute(schemaConn, new FileReader(
				new File("src/test/resources/data/scripts/create_classicmodels1.sql").getAbsolutePath()));

	}

	private ServiceData serviceAgentSetup(String servicename, String payload) {
		ServiceData outData = new ServiceData();
		ServiceData inData = new ServiceData();
		if (payload != null)
			inData.setPayLoad(payload);
		else
			inData.setPayLoad("");
		inData.setServiceName(servicename);
		inData.setUserId(Value.newTextValue("100"));
		outData = ServiceAgent.getAgent().executeService(inData);
		return outData;
	}

	/**
	 * Test for setValue action with fieldnames @throws Exception
	 */
	@Test
	public void setValueTest() {
		ServiceData outData = serviceAgentSetup("test.SetValue", null);
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
		ServiceData outData = serviceAgentSetup("test.Logic", payLoad);
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

		ServiceData outData = serviceAgentSetup("tutorial.addColumn", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("weekendBoxOffice")).get(0)).get("testcolumn"), "testValue");
	}

	/**
	 * Test for copyRows action
	 */
	@Test
	public void copyRowsTest() {

		ServiceData outData = serviceAgentSetup("tutorial.copyRows", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("copiedweekendBoxOffice")).get(0)).get("Theaters"), "465");
	}

	/**
	 * Test for renameSheet action
	 */
	@Test
	public void renameSheetTest() {

		ServiceData outData = serviceAgentSetup("tutorial.renameSheet", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("newweekendBoxOffice")).get(0)).get("Theaters"), "465");
	}

	/**
	 * Test for add Message action
	 */
	@Test
	public void addMessageTest() {

		ServiceData outData = serviceAgentSetup("tutorial.addMessage", null);
		FormattedMessage[] msgs = outData.getMessages();

		assertEquals(msgs[0].text, "NeverLand Custom Message From My Messages XML File");
	}

	/**
	 * Test for add createSheet action
	 */
	@Test
	public void createSheetTest() {

		ServiceData outData = serviceAgentSetup("tutorial.createSheet", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());

		assertEquals(((JSONObject) ((JSONArray) obj.get("newsheet")).get(0)).get("text"),
				"first row first column text");
	}

	/**
	 * Test for jumpToTest action
	 */
	@Test
	public void jumpToTest() {
		ServiceData outData = serviceAgentSetup("tutorial.jumpTo", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(false, obj.has("adversary1"));
	}

	/**
	 * Test for copyUserIdTest action
	 */
	@Test
	public void copyUserIdTest() {
		ServiceData outData = serviceAgentSetup("tutorial.copyUserId", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("UserIDCopy"), "100");
	}

	/**
	 * Test for filter action
	 */
	@Test
	public void filterTest() {
		ServiceData outData = serviceAgentSetup("tutorial.filterTest", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("customers")).get(0)).get("customerName"),
				"Signal Gift Stores");
	}

	/**
	 * Test for rowExists action
	 */
	@Test
	public void rowExistsTest() {
		ServiceData outData = serviceAgentSetup("tutorial.rowExists", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for read action
	 */
	@Test
	public void readDataTest() {
		ServiceData outData = serviceAgentSetup("tutorial.readData", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("customerName"), "Signal Gift Stores");
	}

	/**
	 * Test for suggest action
	 */
	@Test
	public void suggestTest() {
		ServiceData outData = serviceAgentSetup("tutorial.suggest", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("Employees")).get(0)).get("firstName"), "Mary");
	}

	/**
	 * Test for saveDataDelete action
	 */
	@Test
	public void saveDataDeleteTest() {
		ServiceData outData = serviceAgentSetup("tutorial.saveDataDelete", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for saveDataAdd action
	 */
	@Test
	public void saveDataAddTest() {
		ServiceData outData = serviceAgentSetup("dbactions.saveDataAdd", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for saveDataAdd action
	 */
	@Test
	public void saveDataModifyTest() {
		ServiceData outData = serviceAgentSetup("tutorial.saveDataModify", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("testValue"), 1234);
	}

	/**
	 * Test for read with sql action
	 */
	@Test
	public void readWithSqlTest() {
		ServiceData outData = serviceAgentSetup("dbactions.readWithSql", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("Employees")).get(0)).get("lastName"), "Patterson");
	}

	/**
	 * Test for executesql action
	 */
	@Test
	public void executeSqlTest() {
		ServiceData outData = serviceAgentSetup("tutorial.executeSql", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(obj.get("updateSql"), 1);
	}

	@Test
	public void schemaReadWithSqlTest() {
		ServiceData outData = serviceAgentSetup("tutorial.schemaReadWithSql", null);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals(((JSONObject) ((JSONArray) obj.get("Students")).get(0)).get("name"), "Sham");
	}

	@Test
	public void ldapAuthenticate1Test() {
		String payLoad = "{'_userId':'winner','_userToken':'pwd'}";
		ServiceData outData = serviceAgentSetup("ldap.ldapAuth", payLoad);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals((String) obj.get("_userId"), "winner");
	}

	@Test
	public void ldapAuthenticate2Test() {
		String payLoad = "{'_userId':'rogue','_userToken':'guess'}";
		ServiceData outData = serviceAgentSetup("ldap.ldapAuth", payLoad);
		assertEquals(outData.hasErrors(), true);
	}

	public static Session getMailSession() {
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imap.partialfetch", "0");
		return Session.getDefaultInstance(props, null);
	}

	/*
	 * Test method for 
	 * org.simplity.tp.SendMail
	 */
	@Test
	public void sendMailTest() {
		ServiceData outData = serviceAgentSetup("test.sendMail", null);

		try {
			Session session = getMailSession();
			Store store = session.getStore("imap");
			store.connect("mockserver.com", "bar", "samplepassword");
			Folder folder = store.getDefaultFolder();
			folder = folder.getFolder("inbox");
			folder.open(Folder.READ_ONLY);
			for (Message message : folder.getMessages()) {
				assertEquals((String) message.getSubject(), "Simplity - sample subject");
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}

		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals((String) obj.get("_requestStatus"), "ok");
	}
	
	@Test
	public void ldapLookupTest() {
		String payLoad = "{'objectId':'CN=Sunita Williams','attrName':'surname'}";
		ServiceData outData = serviceAgentSetup("ldap.ldapLookup", payLoad);
		JSONObject obj = new JSONObject(outData.getPayLoad());
		assertEquals((String) obj.get("ldapLookup"), "Williams");

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
		ServiceData outData = serviceAgentSetup("fileactions.fileProcessing", null);
	}
}
