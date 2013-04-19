package edu.emory.cci.bindaas.webconsole;

import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import edu.emory.cci.bindaas.core.util.DynamicObject;
import edu.emory.cci.bindaas.webconsole.config.BindaasAdminConsoleConfiguration;
import edu.emory.cci.bindaas.webconsole.servlet.usermgmt.LogoutServlet;
import edu.emory.cci.bindaas.webconsole.servlet.usermgmt.PostUserLoginServlet;
import edu.emory.cci.bindaas.webconsole.servlet.usermgmt.UserLoginServlet;
import edu.emory.cci.bindaas.webconsole.servlet.usermgmt.UserOpenIDAuthServlet;
import edu.emory.cci.bindaas.webconsole.servlet.usermgmt.UserQueryBrowserServlet;
import edu.emory.cci.bindaas.webconsole.servlet.util.DocumentationFetcherServlet;

public class WebConsoleStarter {

	public PostLoginAction getPostLoginAction() {
		return postLoginAction;
	}


	public void setPostLoginAction(PostLoginAction postLoginAction) {
		this.postLoginAction = postLoginAction;
	}



	// Start the web-console for bindaas
	private MainController mainController;
	private LoginAction loginAction;
	private PostLoginAction postLoginAction;
	private Log log = LogFactory.getLog(getClass());
	private boolean initialized = false;
	@SuppressWarnings("unused")
	private DynamicObject<BindaasAdminConsoleConfiguration> bindaasAdminConsoleConfiguration;
	private BindaasAdminConsoleConfiguration defaultBindaasAdminConsoleConfiguration;
	private OpenIDAuth openIdAuth;
	private UserQueryBrowserServlet userQueryBrowserServlet;
	private UserOpenIDAuthServlet userOpenIDAuthServlet;
	private PostUserLoginServlet postUserLoginServlet;
	private LogoutServlet logoutServlet;
	private UserLoginServlet userLoginServlet;
	
	
	public UserQueryBrowserServlet getUserQueryBrowserServlet() {
		return userQueryBrowserServlet;
	}


	public void setUserQueryBrowserServlet(
			UserQueryBrowserServlet userQueryBrowserServlet) {
		this.userQueryBrowserServlet = userQueryBrowserServlet;
	}


	public UserOpenIDAuthServlet getUserOpenIDAuthServlet() {
		return userOpenIDAuthServlet;
	}


	public void setUserOpenIDAuthServlet(UserOpenIDAuthServlet userOpenIDAuthServlet) {
		this.userOpenIDAuthServlet = userOpenIDAuthServlet;
	}


	public PostUserLoginServlet getPostUserLoginServlet() {
		return postUserLoginServlet;
	}


	public void setPostUserLoginServlet(PostUserLoginServlet postUserLoginServlet) {
		this.postUserLoginServlet = postUserLoginServlet;
	}


	public LogoutServlet getLogoutServlet() {
		return logoutServlet;
	}


	public void setLogoutServlet(LogoutServlet logoutServlet) {
		this.logoutServlet = logoutServlet;
	}


	public UserLoginServlet getUserLoginServlet() {
		return userLoginServlet;
	}


	public void setUserLoginServlet(UserLoginServlet userLoginServlet) {
		this.userLoginServlet = userLoginServlet;
	}


	public OpenIDAuth getOpenIdAuth() {
		return openIdAuth;
	}


	public void setOpenIdAuth(OpenIDAuth openIdAuth) {
		this.openIdAuth = openIdAuth;
	}


	public DocumentationFetcherServlet getDocumentationFetcher() {
		return documentationFetcher;
	}


	public void setDocumentationFetcher(
			DocumentationFetcherServlet documentationFetcher) {
		this.documentationFetcher = documentationFetcher;
	}



	private DocumentationFetcherServlet documentationFetcher;
	
	
	public BindaasAdminConsoleConfiguration getDefaultBindaasAdminConsoleConfiguration() {
		return defaultBindaasAdminConsoleConfiguration;
	}


	public void setDefaultBindaasAdminConsoleConfiguration(
			BindaasAdminConsoleConfiguration defaultBindaasAdminConsoleConfiguration) {
		this.defaultBindaasAdminConsoleConfiguration = defaultBindaasAdminConsoleConfiguration;
	}


	public MainController getMainController() {
		return mainController;
	}


	public void setMainController(MainController mainController) {
		this.mainController = mainController;
	}


	public LoginAction getLoginAction() {
		return loginAction;
	}


	public void setLoginAction(LoginAction loginAction) {
		this.loginAction = loginAction;
	}


	public void init() throws Exception
	{
		log.debug("Initializing bean WebConsoleStarter");
		final BundleContext context = Activator.getContext();
		// set config
		bindaasAdminConsoleConfiguration = new DynamicObject<BindaasAdminConsoleConfiguration>("bindaas.adminconsole", defaultBindaasAdminConsoleConfiguration, context);
		
		Hashtable<String,String> loginFilterprops = new Hashtable<String,String>();
		loginFilterprops.put("pattern", "/dashboard/.*");
		loginFilterprops.put("init.message", "Login Filter");

		String filter = "(objectclass=" + HttpService.class.getName() + ")";
		
		ServiceListener httpServiceListener = new ServiceListener() {
			
			@Override
			public void serviceChanged(ServiceEvent sv) {
				if(!initialized)
				{

					ServiceReference<?> serviceRef = sv.getServiceReference();
					      switch(sv.getType()) {
					        case ServiceEvent.REGISTERED:
					        case ServiceEvent.MODIFIED:
					          {
					        	  HttpService  service  = (HttpService) context.getService(serviceRef);
					        	  // register
					        	  HttpContext defaultContext = service.createDefaultHttpContext();
									try {
										service.registerResources("/foundation", "/foundation", defaultContext);
										service.registerServlet("/dashboard", mainController, null, defaultContext);
										service.registerServlet("/authenticate", loginAction, null, defaultContext);
										service.registerServlet("/postAuthenticate", postLoginAction, null, defaultContext);
										
										
										((org.apache.felix.http.api.ExtHttpService) service) .registerFilter(loginAction, "/dashboard/.*", null, 0 ,  defaultContext);
										service.registerServlet(openIdAuth.getServletLocation(), openIdAuth, null, defaultContext);
										
										service.registerServlet("/fetchDocumentation", documentationFetcher, null, defaultContext);
							
										
										service.registerServlet("/user/login", userLoginServlet , null, defaultContext);
										service.registerServlet("/user/dashboard/queryBrowser", userQueryBrowserServlet , null, defaultContext);
										service.registerServlet("/user/openid", userOpenIDAuthServlet , null, defaultContext);
										service.registerServlet("/user/postAuthenticate", postUserLoginServlet , null, defaultContext);
										service.registerServlet("/user/logout", logoutServlet , null, defaultContext);
										
										log.info("Bindaas WebConsole Started");  
									} catch (Exception e) {
											log.error(e);
									}
									
					        	  
					        	  break;
					          }
					         
					        case ServiceEvent.UNREGISTERING :
					        {
					        	// do unregistration;
					        	break;
					        }
					        
					       default:
					          break;
					     }
					      
					initialized = true;
				}
				
			}
		};
		
		
		// add existing
		@SuppressWarnings("rawtypes")
		ServiceReference[] serviceReferences = context.getAllServiceReferences(HttpService.class.getName(), null);
		if(serviceReferences!=null)
		{
			for(@SuppressWarnings("rawtypes") ServiceReference serviceRef : serviceReferences)
			{
				httpServiceListener.serviceChanged( new ServiceEvent(ServiceEvent.REGISTERED, serviceRef));
				break;
			}
		}
		
		
		
		// listen for new providers
		context.addServiceListener( httpServiceListener , filter);
		
		
		
	}
	
	
}
