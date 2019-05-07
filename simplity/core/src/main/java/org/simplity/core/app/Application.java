/*
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
package org.simplity.core.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.simplity.core.ApplicationError;
import org.simplity.core.adapter.DataAdapter;
import org.simplity.core.auth.OAuthSetup;
import org.simplity.core.batch.BatchJobs;
import org.simplity.core.batch.BatchSetup;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IComponent;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.dm.Record;
import org.simplity.core.dt.DataType;
import org.simplity.core.fn.Concat;
import org.simplity.core.fn.IFunction;
import org.simplity.core.gateway.Gateways;
import org.simplity.core.gateway.ServiceGateway;
import org.simplity.core.jms.JmsSetup;
import org.simplity.core.mail.MailSetup;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.Message;
import org.simplity.core.msg.Messages;
import org.simplity.core.rdb.RdbSetup;
import org.simplity.core.service.ExternalService;
import org.simplity.core.service.InputData;
import org.simplity.core.service.OutputData;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.sql.Sql;
import org.simplity.core.sql.StoredProcedure;
import org.simplity.core.test.TestRun;
import org.simplity.core.trans.Service;
import org.simplity.core.util.TextUtil;
import org.simplity.core.util.XmlParseException;
import org.simplity.core.util.XmlUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents the entire App set-up. Designed with the flexibility of running
 * more than one applications in a JVM.
 *
 *
 * @author simplity.org
 */
public class Application implements IApp {

	protected static final Logger logger = LoggerFactory.getLogger(Application.class);

	protected static final String FOLDER_STR = "/";
	/**
	 * we use a default user id during testing
	 */
	private static final Value DEFAULT_NUMERIC_USER_ID = Value.newIntegerValue(420);
	private static final Value DEFAULT_TEXT_USER_ID = Value.newTextValue("420");
	/*
	 * all these definitions just to use an array instead of map to keep all
	 * components
	 */
	private static final int DT_IDX = ComponentType.DT.getIdx();
	private static final int MSG_IDX = ComponentType.MSG.getIdx();
	private static final int REC_IDX = ComponentType.REC.getIdx();
	private static final int SQL_IDX = ComponentType.SQL.getIdx();
	private static final int SP_IDX = ComponentType.SP.getIdx();
	private static final int FUNCTION_IDX = ComponentType.FUNCTION.getIdx();
	private static final int SERVICE_IDX = ComponentType.SERVICE.getIdx();
	private static final int JOBS_IDX = ComponentType.JOBS.getIdx();
	private static final int TEST_IDX = ComponentType.TEST_RUN.getIdx();
	private static final int EXTERN_IDX = ComponentType.EXTERN.getIdx();
	private static final int ADAPTER_IDX = ComponentType.ADAPTER.getIdx();
	/*
	 * list of built-in functions
	 */
	protected static final IFunction[] BUILT_IN_FUNCTIONS = { new Concat() };

	/*
	 * either soleApplicaiton is non-null, or currentApplication is non-null. We
	 * should never have both being non-null
	 */
	/**
	 * used only if multiple simplity applications are running in this JVM
	 */
	private static ThreadLocal<Stack<Application>> currentApplication;

	/**
	 * sole app running in this JVM. null if no application is loaded, OR more
	 * than one are loaded. (
	 */

	private static Application soleApplication;

	/**
	 * we use this ONLY if we have at least two apps loaded.
	 */
	private static Map<String, Application> loadedApps;

	private static void appLoaded(Application newApp) {
		if (loadedApps != null) {
			loadedApps.put(newApp.applicationId, newApp);
			logger.info(
					"{} is added to an existing multi-app scenario",
					newApp.applicationId);
			return;
		}
		if (soleApplication == null) {
			/*
			 * this is the first/sole
			 */
			soleApplication = newApp;
			logger.info("{} set as the sole simplity app", newApp.applicationId);
			return;
		}
		/*
		 * this is the second one. we are getting into multi-app situation
		 */
		loadedApps = new HashMap<>();
		loadedApps.put(newApp.applicationId, newApp);
		loadedApps.put(soleApplication.applicationId, soleApplication);
		soleApplication = null;
		logger.info(
				"{} is the second simplity app to be set-up. Moving to a multi-app scenario with {} as the default app",
				newApp.applicationId, soleApplication.applicationId);
	}

	private static void appShutdown(Application app) {
		String id = app.applicationId;

		if (loadedApps != null) {
			if (loadedApps.remove(app.applicationId) == null) {
				logger.error("{} is shutting down, but it was not registered. {} apps are running.", id,
						loadedApps.size());
				return;
			}

			int nbr = loadedApps.size();
			if (nbr > 1) {
				logger.info("{} shut down, leaving behind {} running applications.", id, nbr);
				return;
			}
			/*
			 * we do not keep one app in the map. shift it out to the sole one.
			 */
			soleApplication = loadedApps.values().toArray(new Application[1])[0];
			logger.info("{} shutdown. {} is the only one applicaiton running. Switched to single-appscenario", id,
					soleApplication.applicationId);
			loadedApps = null;
			return;
		}

		if (soleApplication == null) {
			logger.error("{} is being shut down, but there are no apps running!! unusual.", app.applicationId);
			return;
		}

		if (!soleApplication.applicationId.equals(app.applicationId)) {
			logger.error("{} is shutdown. But that is not regsitered as a running app.", app.applicationId);
			return;
		}
		soleApplication = null;
		logger.info("Sole applicaiton {} shutdown. No apps are running now", app.applicationId);
	}

	private static void appStartedServing(Application app) {
		if (soleApplication != null) {
			/*
			 * single app scenario. we assume it is the same as app
			 */
			return;
		}
		if (currentApplication == null) {
			currentApplication = new ThreadLocal<>();
			currentApplication.set(new Stack<>());
		}
		currentApplication.get().push(app);
	}

	private static void appDoneWithServing(Application app) {
		if (soleApplication != null) {
			/*
			 * single app scenario. we assume it is the same as app
			 */
			return;
		}
		if (currentApplication != null) {
			Stack<Application> stack = currentApplication.get();
			if (!stack.isEmpty()) {
				stack.pop();
				return;
			}
		}
		throw new ApplicationError("Design Error: trying to end service execution inside app " + app.applicationId
				+ " but there is no ative app");
	}

	/**
	 * used by classes that are invoked as part of executing the service. This
	 * is a replacement to static method, and allow multi-apps concurrently
	 * running in a
	 *
	 * @return get the current app that is executing the service on this thread
	 */
	public static Application getActiveInstance() {
		if (soleApplication != null) {
			return soleApplication;
		}
		if (currentApplication != null) {
			Stack<Application> stack = currentApplication.get();
			if (!stack.isEmpty()) {
				return stack.peek();
			}
		}
		throw new ApplicationError("No active application running in the JVM while there is a request to refer to one");
	}

	/**
	 * run a function inside an app environment. utility method for testing
	 * during development
	 *
	 * @param resRoot
	 *            resource root folder e.g.
	 *            "c:/myFolder/myProject/src/main/res/"
	 * @param fn
	 *            that is to be run
	 * @return true if the app could be started and the fn was executed. false
	 *         in case of any error
	 */
	public static boolean runInsideApp(String resRoot, AppRunnable fn) {
		Application app = new Application();
		Map<String, String> params = new HashMap<>();
		params.put(AppConventions.Name.RESOURCE_ROOT, resRoot);
		List<String> messages = new ArrayList<>();
		boolean allOk = app.openShop(params, messages);
		if (!allOk) {
			System.out.println("Application could not be started because of followiing errors");
			for (String msg : messages) {
				System.out.println(msg);
			}
			return false;
		}
		try {
			fn.runInsideApp();
			allOk = true;
		} catch (Exception e) {
			e.printStackTrace();
			allOk = false;
		}
		app.closeShop();
		return allOk;
	}

	/**
	 *
	 * lambda that runs inside an app
	 */
	@FunctionalInterface
	public interface AppRunnable {
		/**
		 * run whatever you want to run, when an app is active
		 */
		public void runInsideApp();
	}

	/*
	 * instance members start here
	 */
	/**
	 * unique name of this application within a corporate. This may be used as
	 * identity while trying to communicate with other applications within the
	 * corporate cluster
	 */
	@FieldMetaData(isRequired = true)
	String applicationId;

	/**
	 * user id is a mandatory concept. Every service is meant to be executed for
	 * a specified (logged-in) user id. Apps can choose it to be either string
	 * or number
	 */
	boolean userIdIsNumber;

	/**
	 * do we cache components as they are loaded. typically true in production,
	 * and false in development environment
	 */
	boolean cacheComponents;

	/**
	 * during development/testing,we can simulate service executions with local
	 * data. service.xml is used for input/output, but the execution is skipped.
	 * json from data folder is used to populate serviceContext
	 */
	boolean simulateWithLocalData;

	/**
	 * jndi name for user transaction for using JTA based transactions
	 */
	String jtaUserTransaction;

	/**
	 * rdb set up
	 */
	RdbSetup rdbSetup;

	/**
	 * batch (background job) set up
	 */
	BatchSetup batchSetup;
	/**
	 * jms setup
	 */
	JmsSetup jmsSetup;
	/**
	 * Configure the Mail Setup for the application
	 */
	MailSetup mailSetup;
	/**
	 * OAuth parameters
	 */
	OAuthSetup oAuthSetup;
	/**
	 * gateways for external applications, indexed by id
	 */
	@FieldMetaData(memberClass = ServiceGateway.class, indexFieldName = "applicationName")
	Map<String, ServiceGateway> externalApplications = new HashMap<>();

	/**
	 * plugin instances for this app, we use a default non-null to ensure
	 * default functionality for all plugins
	 */
	Plugins plugins = new Plugins();

	protected String resourceRoot;
	/**
	 * instance of a UserTransaction for JTA/JCA based transaction management
	 */
	private Object userTransactionInstance;

	private boolean userIdIsNumeric;

	private Comp[] allComps;
	private Value dummyUser;
	/*
	 * keep track of configuration error in case of calls to failed components
	 */
	private static final String NOT_SET_UP = " not set up for this app";
	private String rdbError = "Rdb " + NOT_SET_UP;
	private String jmsError = " JMS " + NOT_SET_UP;
	private String batchError = "Batch " + NOT_SET_UP;
	private String mailError = "MAIL " + NOT_SET_UP;
	private String oAuthError = "Open Authentication " + NOT_SET_UP;

	@Override
	public String getAppId() {
		return this.applicationId;
	}

	/**
	 * get root/prefix for all resources
	 *
	 * @return folder/package prefix for resources
	 */
	public String getResourceRoot() {
		return this.resourceRoot;
	}

	@Override
	public boolean openShop(Map<String, String> params, List<String> messages) {
		this.resourceRoot = params.get(AppConventions.Name.RESOURCE_ROOT);
		if (this.resourceRoot == null) {
			logger.info("{} not set for resoure root. \"res/\" being tried.");
			this.resourceRoot = AppConventions.Value.RESOURCE_ROOT_DEFAULT;
		}
		if (this.resourceRoot.endsWith(FOLDER_STR) == false) {
			this.resourceRoot += FOLDER_STR;
		}

		logger.info("Bootstrapping with " + this.resourceRoot);

		String configRes = this.resourceRoot + AppConventions.Name.CONFIG_FILE_NAME;
		try {
			if (XmlUtil.xmlToObject(configRes, this) == false) {
				messages.add("Error while loading appilcation configuration resource " + configRes);
				return false;
			}
		} catch (XmlParseException e) {
			messages.add("Appilcation configuration resource " + configRes + " has errors. Error: " + e.getMessage());
			return false;
		}
		int nbr = messages.size();
		this.configure(messages);
		if (messages.size() > nbr) {
			logger.warn("{} is not started because of errors. This app is not running now.", this.applicationId);
			return false;
		}
		if (this.userIdIsNumber) {
			this.dummyUser = Value.newIntegerValue(100);
		} else {
			this.dummyUser = Value.newTextValue("100");
		}
		appLoaded(this);
		logger.info("{} configured properly, and is running now..", this.applicationId);
		return true;
	}

	@Override
	public void serve(IServiceRequest request, IServiceResponse response) {
		String serviceName = request.getServiceName();
		IService service = (IService) this.getComponentOrNull(ComponentType.SERVICE, serviceName);
		if (service == null) {
			logger.error("Service {} is not served on this server", serviceName);
			response.setResult(ServiceResult.NO_SUCH_SERVICE, 0);
			return;
		}

		/*
		 * is it accessible to user?
		 */
		IAccessController guard = this.plugins.getAccessController();
		if (guard != null && guard.okToServe(service, request) == false) {
			logger.error("Logged in user is not authorized for Service {} ", serviceName);
			response.setResult(ServiceResult.INSUFFICIENT_PRIVILEGE, 0);
			return;
		}

		long bigin = System.currentTimeMillis();
		AppUser user = request.getUser();
		if (user == null) {
			logger.info("Service requested with no user. Dummy user is assumed.");
			user = new AppUser(this.dummyUser, null, null);
		}
		ServiceContext ctx = new ServiceContext(this, serviceName, user);
		appStartedServing(this);
		try {
			this.callService(ctx, request, response, service);
		} catch (Exception e) {
			logger.error("Exception thrown by service {}, {}" + service.getServiceName(), e.getMessage());
			this.reportApplicationError(request, e);
			ctx.addMessage(Messages.INTERNAL_ERROR, e.getMessage());
		}
		appDoneWithServing(this);

		int milli = (int) (System.currentTimeMillis() - bigin);
		List<FormattedMessage> messages = ctx.getMessages();
		if (messages != null && messages.size() > 0) {
			response.setMessages(messages.toArray(new FormattedMessage[0]));
		}
		if (ctx.isInError()) {
			response.setResult(ServiceResult.INVALID_DATA, milli);
		} else {
			response.setResult(ServiceResult.ALL_OK, milli);
		}
	}

	private void callService(ServiceContext ctx, IServiceRequest request, IServiceResponse response, IService service) {
		IServicePrePostProcessor hook = this.plugins.getServicePrePostProcessor();
		if (hook != null) {
			if (hook.beforeInput(request, response, ctx) == false) {
				logger.info("App specific hook requested that the service be abandoned before inputting data.");
				return;
			}
		}
		IRequestReader reader = request.getPayloadReader();
		InputData inSpec = service.getInputSpecification();
		if (inSpec == null) {
			logger.info("Service expects no input data from payload");
		} else {
			inSpec.read(reader, ctx);
		}

		if (ctx.isInError()) {
			logger.info("Input data had errors. Service not invoked.");
			logger.error(FormattedMessage.toString(ctx.getMessages()));
			return;
		}

		if (hook != null) {
			if (hook.beforeService(request, response, ctx) == false) {
				logger.info("App specific hook requested that the service be abandoned after inputting data.");
				return;
			}
		}

		if (service.directlyWritesDataToResponse()) {
			IResponseWriter writer = response.getPayloadWriter(service.responseIsAnArray());
			ctx.setWriter(writer);
			logger.info(
					"Writer set to service context. Service is expected to write response directly to an object writer.");
		}
		/*
		 * is this to be run in the background always? TODO: batch mode
		 */

		/*
		 * TODO : manage cache
		 *
		 * is it cached?
		 */

		logger.info("Control handed over to service");
		service.serve(ctx);
		if (ctx.isInError()) {
			logger.info("service execution returned with errors");
			return;
		}
		if (hook != null) {
			if (hook.afterService(response, ctx) == false) {
				logger.info("App specific hook aftrer service signalled that we do not output data.");
				return;
			}
		}
		logger.info("Going to write output data");
		this.writeResponse(ctx, service, response);

		/*
		 *
		 * TODO: cache to be invalidated or this is to be cached.
		 */
	}

	/**
	 * @param ctx
	 * @param service
	 * @param response
	 */
	private void writeResponse(ServiceContext ctx, IService service, IServiceResponse response) {
		if (service.directlyWritesDataToResponse()) {
			ctx.getWriter().done();
			logger.info(
					"Service wrote response directly to the response writerve output response directly to the stream.");
		} else {
			OutputData outSpec = service.getOutputSpecification();
			if (outSpec == null) {
				logger.warn("Service has no output specification and hence no response is emitted.");
			} else {
				IResponseWriter respWriter = response.getPayloadWriter(service.responseIsAnArray());
				outSpec.write(respWriter, ctx);
				respWriter.done();
			}
		}
		String[] sessionFields = service.getSessionFields();
		if (sessionFields != null) {
			for (String f : sessionFields) {
				Value value = ctx.getValue(f);
				if (value == null) {
					logger.info("Session field {} not set because value is absent in ctx", f);
				} else {
					response.setSessionField(f, value.toString());
				}
			}
		}
		IServicePrePostProcessor hook = this.plugins.getServicePrePostProcessor();
		if (hook != null) {
			hook.afterOutput(response, ctx);
		}
	}

	/**
	 * invalidate any cached response for this service
	 *
	 * @param serviceName
	 */
	public void invalidateCache(String serviceName) {
		IServiceCacher cacher = this.plugins.getServiceCacher();
		if (cacher != null) {
			logger.info("Invalidating cache for the service " + serviceName);
			cacher.invalidate(serviceName);
		}
	}

	@Override
	public void closeShop() {
		appShutdown(this);
	}

	/**
	 *
	 * @return app data cacher that is set up for this app. null if no cacher is
	 *         configured
	 */
	public IAppDataCacher getAppDataCacher() {
		return this.plugins.getAppDataCacher();
	}

	/**
	 * report an application error that needs attention from admin
	 *
	 * @param e
	 */
	public void reportApplicationError(ApplicationError e) {
		soleApplication.plugins.getExceptionListener().listen(e);
	}

	/**
	 * report an application error that needs attention from admin
	 *
	 * @param e
	 */
	public void reportApplicationError(Exception e) {
		soleApplication.plugins.getExceptionListener().listen(e);
	}

	/**
	 * report an exception that needs attention from admin
	 *
	 * @param request
	 *            data with which service was invoked. null if the error has no
	 *            such reference
	 * @param e
	 */
	public void reportApplicationError(IServiceRequest request, Exception e) {
		soleApplication.plugins.getExceptionListener().listen(request, new ApplicationError(e, ""));
	}

	/** @return get a UserTrnsaction instance */
	public UserTransaction getUserTransaction() {
		if (this.userTransactionInstance == null) {
			throw new ApplicationError("Application is not set up for a JTA based user transaction");
		}
		return (UserTransaction) this.userTransactionInstance;
	}

	/**
	 * @return default user id, typically during tests. null if it is not set
	 */
	public Value getDefaultUserId() {
		if (this.userIdIsNumeric) {
			return DEFAULT_NUMERIC_USER_ID;
		}
		return DEFAULT_TEXT_USER_ID;
	}

	/** @return is the userId a number? default is text/string */
	public boolean userIdIsNumeric() {
		return this.userIdIsNumeric;
	}

	private void configure(List<String> msgs) {
		this.plugins.configure(msgs);

		if (this.rdbSetup == null) {
			logger.info("No rdb has been set up for this app.");
		} else {
			this.rdbError = this.rdbSetup.configure();
			if (this.rdbError != null) {
				msgs.add(this.rdbError);
				this.rdbSetup = null;
			}
		}

		if (this.batchSetup == null) {
			logger.info("No Batch Environment has been set up for this app.");
		} else {
			this.batchError = this.batchSetup.configure();
			if (this.batchError == null) {
				msgs.add(this.batchError);
				this.batchSetup = null;
			}
		}

		if (this.jmsSetup == null) {
			logger.info("No JMS infrastructure has been set up for this app.");
		} else {
			this.jmsError = this.jmsSetup.configure();
			if (this.jmsError != null) {
				msgs.add(this.jmsError);
				this.jmsSetup = null;
			}
		}

		if (this.mailSetup == null) {
			logger.info("No mail facility has been set up for this app.");
		} else {
			this.mailError = this.mailSetup.configure();
			if (this.mailError != null) {
				msgs.add(this.mailError);
				this.mailSetup = null;
			}
		}

		if (this.oAuthSetup == null) {
			logger.info("No Open Authentication has been set up for this app.");
		} else {
			this.oAuthError = this.oAuthSetup.configure();
			if (this.oAuthError != null) {
				msgs.add(this.oAuthError);
				this.oAuthSetup = null;
			}
		}

		this.loadComps();
		if (this.jtaUserTransaction != null) {
			try {
				this.userTransactionInstance = new InitialContext().lookup(this.jtaUserTransaction);
				if (this.userTransactionInstance instanceof UserTransaction == false) {
					msgs.add(this.jtaUserTransaction + " is located but it is not UserTransaction but "
							+ this.userTransactionInstance.getClass().getName());
				} else {
					logger.info("userTransactionInstance set to " + this.userTransactionInstance.getClass().getName());
				}
			} catch (Exception e) {
				msgs.add("Error while instantiating UserTransaction using jndi name " + this.jtaUserTransaction + ". "
						+ e.getMessage());
			}
		}

		/*
		 * in production, we cache components as they are loaded, but in
		 * development we prefer to load the latest
		 */
		if (this.cacheComponents) {
			this.startCaching();
		}

		/*
		 * gate ways
		 */

		if (this.externalApplications.isEmpty() == false) {
			Gateways.setGateways(this.externalApplications);
			this.externalApplications = null;
		}
	}

	/**
	 * validate all field values of this as a component
	 *
	 * @param vtx
	 *            validation context
	 */
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		this.plugins.validate(vtx);
		if (this.rdbSetup != null) {
			this.rdbSetup.validate(vtx);
		}
	}

	/**
	 * @return the rdb setup for this app. throws applicationError it not set-up
	 */
	public RdbSetup getRdbSetup() {
		if (this.rdbSetup == null) {
			throw new ApplicationError(this.rdbError);
		}
		return this.rdbSetup;
	}

	/**
	 *
	 * @return non-null batch setup associated with this app. throws
	 *         ApplicationError in case it is not setup
	 */
	public BatchSetup getBatchSetup() {
		if (this.batchSetup == null) {
			throw new ApplicationError(this.batchError);
		}
		return this.batchSetup;
	}

	/**
	 *
	 * @return non-null JMS driver associated with this app. throws
	 *         ApplicationError in case it is not setup
	 */
	public JmsSetup getJmsSetup() {
		if (this.jmsSetup == null) {
			if (this.jmsError == null) {
				throw new ApplicationError(
						"JMS is not set up for this project, but a service is trying to use a jms conenction");
			}
			throw new ApplicationError(this.jmsError);
		}
		return this.jmsSetup;
	}

	/**
	 * @return the rdbDriver set up with this app, or null if it not set-up
	 */
	public OAuthSetup getOAuthSetup() {
		if (this.oAuthSetup == null) {
			throw new ApplicationError(this.oAuthError);
		}
		return this.oAuthSetup;
	}

	/**
	 *
	 * @return non-null MailSetp associated with this app. throws
	 *         ApplicationError in case it is not setup
	 */
	public MailSetup getMailSetup() {
		if (this.mailSetup == null) {
			throw new ApplicationError(this.mailError);
		}
		return this.mailSetup;
	}

	/**
	 * get a bean from the container
	 *
	 * @param className
	 * @param cls
	 * @return instance of the class, or null if such an object could not be
	 *         located
	 */
	public <T> T getBean(String className, Class<T> cls) {
		return this.plugins.getInstance(className, cls);
	}

	/**
	 *
	 * @return plugins set up for this app
	 */
	public Plugins getPlugins() {
		return this.plugins;
	}

	/**
	 * @param userId
	 * @param tenantId
	 * @param authToken
	 * @return app user for this user id
	 */
	public AppUser createAppUser(String userId, String tenantId, String authToken) {
		Value uid = null;
		if (this.userIdIsNumeric) {
			uid = Value.parseValue(userId, ValueType.INTEGER);
		} else {
			uid = Value.newTextValue(userId);
		}
		Value tid = null;
		if (tenantId != null) {
			tid = Value.parseValue(tenantId);
		}
		return new AppUser(uid, tid, authToken);
	}

	/**
	 * let components be cached once they are loaded. Typically used in
	 * production environment
	 */
	public void startCaching() {
		for (Comp c : this.allComps) {
			c.startCaching();
		}
	}

	/**
	 * purge cached components, and do not cache any more. USed during
	 * development.
	 */
	public void stopCaching() {
		for (Comp c : this.allComps) {
			c.stopCaching();
		}
	}

	/**
	 * get all pre-loaded Components
	 *
	 * @param ct
	 *
	 * @return map of all pre-loaded components. null if this comp is not
	 *         pre-loaded
	 */
	public Collection<?> getPreloadedComps(ComponentType ct) {
		return this.allComps[ct.getIdx()].getPreloadedComps();
	}

	/**
	 * @param ct
	 *            component type
	 * @param compName
	 *            qualified component name
	 * @return instance of the desired component. null if no such component
	 */
	public IComponent getComponentOrNull(ComponentType ct, String compName) {
		return this.allComps[ct.getIdx()].getComp(compName);
	}

	/**
	 * get dataType. to be used by classes that do expect this to be present,
	 * and they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param dataTypeName
	 *            qualified component name
	 * @return non-null data type for that name. ApplicationError is thrown if
	 *         it is not found
	 */
	public DataType getDataType(String dataTypeName) {
		return this.allComps[DT_IDX].getComp(dataTypeName, DataType.class);
	}

	/**
	 * get message. to be used by classes that do expect this to be present, and
	 * they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param messageName
	 *            qualified name
	 * @return non-null message for that name. ApplicationError is thrown if it
	 *         is not found
	 */
	public Message getMessage(String messageName) {
		return this.allComps[MSG_IDX].getComp(messageName, Message.class);
	}

	/**
	 * get record. to be used by classes that do expect this to be present, and
	 * they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param recordName
	 *            qualified name
	 * @return non-null record for that name. ApplicationError is thrown if it
	 *         is not found
	 */
	public Record getRecord(String recordName) {
		return this.allComps[REC_IDX].getComp(recordName, Record.class);
	}

	/**
	 * get Sql. to be used by classes that do expect this to be present, and
	 * they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param sqlName
	 *            qualified name
	 * @return non-null Sql for that name. ApplicationError is thrown if it is
	 *         not found
	 */
	public Sql getSql(String sqlName) {
		return this.allComps[SQL_IDX].getComp(sqlName, Sql.class);
	}

	/**
	 * get stored procedure. to be used by classes that do expect this to be
	 * present, and they do not want to handle the scenario when it is not
	 * found. use the generic getComponentOrNull() to handle null as well
	 *
	 * @param spName
	 *            qualified name
	 * @return non-null stored procedure for that name. ApplicationError is
	 *         thrown if it is not found
	 */
	public StoredProcedure getStoredProcedure(String spName) {
		return this.allComps[SP_IDX].getComp(spName, StoredProcedure.class);
	}

	/**
	 * get function. to be used by classes that do expect this to be present,
	 * and they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param functionName
	 *            qualified name
	 * @return non-null function for that name. ApplicationError is thrown if it
	 *         is not found
	 */
	public IFunction getFunction(String functionName) {
		return this.allComps[FUNCTION_IDX].getComp(functionName, IFunction.class);
	}

	/**
	 * get service. to be used by classes that do expect this to be present, and
	 * they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param serviceName
	 *            qualified name
	 * @return non-null service for that name. ApplicationError is thrown if it
	 *         is not found
	 */
	public Service getService(String serviceName) {
		return this.allComps[SERVICE_IDX].getComp(serviceName, Service.class);
	}

	/**
	 * get Test Run. to be used by classes that do expect this to be present,
	 * and they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param testName
	 *            qualified name
	 * @return non-null test run for that name. ApplicationError is thrown if it
	 *         is not found
	 */
	public TestRun getTestRun(String testName) {
		return this.allComps[TEST_IDX].getComp(testName, TestRun.class);
	}

	/**
	 * get batch jobs. to be used by classes that do expect this to be present,
	 * and they do not want to handle the scenario when it is not found. use the
	 * generic getComponentOrNull() to handle null as well
	 *
	 * @param batchName
	 *            qualified name
	 * @return non-null batch jobs for that name. ApplicationError is thrown if
	 *         it is not found
	 */
	public BatchJobs getBatchJobs(String batchName) {
		return this.allComps[JOBS_IDX].getComp(batchName, BatchJobs.class);
	}

	/**
	 * get data adapter. to be used by classes that do expect this to be
	 * present, and they do not want to handle the scenario when it is not
	 * found. use the generic getComponentOrNull() to handle null as well
	 *
	 * @param adapterName
	 *            qualified name
	 * @return non-null data adapter for that name. ApplicationError is thrown
	 *         if it is not found
	 */
	public DataAdapter getDataAdapter(String adapterName) {
		return this.allComps[ADAPTER_IDX].getComp(adapterName, DataAdapter.class);
	}

	/**
	 * get external service. to be used by classes that do expect this to be
	 * present, and they do not want to handle the scenario when it is not
	 * found. use the generic getComponentOrNull() to handle null as well
	 *
	 * @param appName
	 *            unique name of this app
	 *
	 * @param serviceName
	 *            qualified name
	 * @return non-null external service for that name. ApplicationError is
	 *         thrown if it is not found
	 */
	public ExternalService getExternalService(String appName, String serviceName) {
		return this.allComps[EXTERN_IDX].getComp(appName + '.' + serviceName, ExternalService.class);
	}

	/**
	 * @param worker
	 * @return a thread for this worker
	 */
	public Thread createThread(Runnable worker) {
		if (this.batchSetup == null) {
			return new Thread(worker);
		}
		return this.batchSetup.createThread(worker);
	}

	private void loadComps() {
		int nbr = AppConventions.COMP_TYPES.length;
		this.allComps = new Comp[nbr];
		for (int i = 0; i < nbr; i++) {
			ComponentType ct = AppConventions.COMP_TYPES[i];
			Comp comp = new Comp(ct, AppConventions.Name.COMP_FOLDER_NAMES[i]);
			comp.loadAll();
			this.allComps[i] = comp;
		}

		for (IFunction fn : BUILT_IN_FUNCTIONS) {
			this.allComps[ComponentType.FUNCTION.getIdx()].addComp(fn);
		}
	}

	private class Comp {
		protected static final char DELIMITER = '.';
		protected static final char FOLDER_CHAR = '/';
		protected static final String EXTN = ".xml";

		private final Class<?> compClass;
		private final String folderPrefix;
		private final boolean isPreloaded;
		private final String compType;
		private final String packageName;
		protected Map<String, IComponent> cachedOnes;

		Comp(ComponentType compType, String folder) {
			this.compClass = compType.getCompClass();
			this.folderPrefix = folder;
			this.isPreloaded = compType.isPreloaded();
			this.compType = compType.toString();
			if (compType.allowExtensions()) {
				this.packageName = this.compClass.getPackage().getName() + '.';
			} else {
				this.packageName = null;
			}
		}

		Collection<?> getPreloadedComps() {
			if (this.isPreloaded) {
				return this.cachedOnes.values();
			}
			return null;
		}

		void stopCaching() {
			if (this.isPreloaded == false) {
				this.cachedOnes = null;
			}
		}

		void startCaching() {
			if (this.isPreloaded == false) {
				this.cachedOnes = new HashMap<>();
			}
		}

		@SuppressWarnings({ "unchecked", "unused" })
		<T> T getComp(String compName, Class<T> cls) {
			IComponent comp = this.getComp(compName);
			if (comp == null) {
				throw new ApplicationError("No component of type" + this.compType + " found named " + compName);
			}
			return (T) comp;
		}

		IComponent getComp(String compName) {
			if (this.cachedOnes != null) {
				Object object = this.cachedOnes.get(compName);
				if (object != null) {
					return (IComponent) object;
				}
			}

			if (this.isPreloaded) {
				return null;
			}

			IComponent comp = this.load(compName);
			if (comp == null) {
				return null;
			}

			if (this.cachedOnes != null) {
				this.cachedOnes.put(compName, comp);
			}
			return comp;
		}

		private IComponent load(String compName) {
			String fileName = Application.this.resourceRoot + this.folderPrefix
					+ compName.replace(DELIMITER, FOLDER_CHAR) + EXTN;
			Object obj = null;
			try {
				Document doc = XmlUtil.fromResource(fileName);
				if (doc == null || doc.getDocumentElement() == null) {
					logger.error("Component {} is not loaded. Either it is not defined, or it has syntax errors.",
							compName);
					return null;
				}
				Element ele = doc.getDocumentElement();
				obj = this.getNewInstance(ele.getTagName());
				if (obj == null) {
					logger.error("Tag {} could not be used to create an object instance for component {}",
							ele.getTagName(), compName);
					return null;
				}

				XmlUtil.elementToObject(ele, obj);
			} catch (Exception e) {
				logger.error("error while loading component " + compName, e);
				return null;
			}

			/*
			 * we insist that components be stored with the right naming
			 * convention
			 */
			IComponent comp = (IComponent) obj;
			String fullName = comp.getQualifiedName();

			if (compName.equals(fullName) == false) {
				logger.info("Component has a qualified name of {}  that is different from its storage name {}",
						fullName,
						compName);
				return null;
			}
			comp.getReady();
			return comp;
		}

		private Object getNewInstance(String tagName) throws Exception {
			if (this.packageName == null) {
				return this.compClass.newInstance();
			}
			return Class.forName(this.packageName + TextUtil.nameToClassName(tagName)).newInstance();
		}

		/**
		 * load all components inside folder. This is used by components that
		 * are pre-loaded. These are saved as collections, and not within their
		 * own files
		 *
		 * @param folder
		 * @param packageName
		 * @param objects
		 */
		void loadAll() {
			if (this.isPreloaded == false) {
				return;
			}
			this.cachedOnes = new HashMap<>();
			try {
				String pkg = this.compClass.getPackage().getName() + '.';
				/*
				 * load one for system-defined and one for app defined
				 */
				this.loadOne(AppConventions.Name.BUILT_IN_COMP_PREFIX + this.folderPrefix
						+ AppConventions.Name.BUILT_IN_COMP_FILE_NAME, pkg);

				this.loadOne(Application.this.resourceRoot + this.folderPrefix
						+ AppConventions.Name.APP_COMP_FILE_NAME, pkg);
				/*
				 * we have to initialize the components
				 */
				for (Object obj : this.cachedOnes.values()) {
					((IComponent) obj).getReady();
				}
				logger.info("{} {} loaded.", this.cachedOnes.size(), this.compType);
			} catch (Exception e) {
				this.cachedOnes.clear();
				logger.error(
						"pre-loading of " + this
								+ " failed. No component of this type is available till we successfully pre-load them again.",
						e);
			}
		}

		void addComp(IComponent comp) {
			if (this.cachedOnes == null || comp == null) {
				return;
			}

			if (this.compClass.isInstance(comp)) {
				String name = comp.getQualifiedName();
				this.cachedOnes.put(name, comp);
				logger.info("{} added/replaced", name);
			} else {
				throw new ApplicationError(
						"An object of type " + comp.getClass().getName() + " is being passed as component " + this);
			}
		}

		private void loadOne(String resName, String pkg) {
			logger.info("Going to load components from {}", resName);
			try {
				XmlUtil.xmlToCollection(resName, this.cachedOnes, pkg);
			} catch (Exception e) {
				logger.error("Resource " + resName + " failed to load.", e);
			}
		}
	}
}
