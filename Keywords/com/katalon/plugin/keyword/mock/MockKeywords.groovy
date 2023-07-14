package com.katalon.plugin.keyword.mock

import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.checkpoint.Checkpoint
import com.kms.katalon.core.checkpoint.CheckpointFactory
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testcase.TestCase
import com.kms.katalon.core.testcase.TestCaseFactory
import com.kms.katalon.core.testdata.TestData
import com.kms.katalon.core.testdata.TestDataFactory
import com.kms.katalon.core.testobject.ObjectRepository
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords

import internal.GlobalVariable

import org.openqa.selenium.WebElement
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By

import com.kms.katalon.core.mobile.keyword.internal.MobileDriverFactory
import com.kms.katalon.core.webui.driver.DriverFactory

import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObjectProperty

import com.kms.katalon.core.mobile.helper.MobileElementCommonHelper
import com.kms.katalon.core.util.KeywordUtil

import com.kms.katalon.core.webui.exception.WebElementNotFoundException

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

class MockKeywords {

	private static WireMockServer wireMockServer;


	/**
	 * This will create a api mock on the specified host/domain and port
	 * @param host - Optional - Specify the host/domain as string. Usually localhost, IP Address of the host or domain names. Default value is "localhost".
	 * @param httpPort - Optional - Specify the port your api mock will listen to. Default value is 8080.
	 * @param httpsPort - Optional - Specify the https port your api mock will listen to. Default value is 443.
	 * examples: createAPIMock("localhost", 8080); createAPIMock("127.0.0.1", 443)
	 */
	@Keyword
	def createAPIMockSession(String host, Number httpPort, Number httpsPort) {

		if (host=="") {
			host = "localhost"
			Logger.getAnonymousLogger().log(Level.INFO, MessageFormat.format("Apply default host 'localhost'"));
		}

		if (httpPort==0 || httpPort==null) {
			httpPort = 8080
			Logger.getAnonymousLogger().log(Level.INFO, MessageFormat.format("Apply default http port 8080"));
		}

		WireMockConfiguration wmc = WireMockConfiguration.options().bindAddress(host);
		wmc.port(httpPort);
		if (httpsPort!=0 || httpsPort!=null) wmc.httpsPort(httpsPort);
		wireMockServer = new WireMockServer(wmc);
	}

	/**
	 * This keyword is used to create mock rules for your responses
	 * @param httpMethod - Optional - parameter used to specify request that match http methods GET, POST, PUT and DELETE. If no method is specific then a common response will be sent for any method.
	 * @param urlPath - Optional - if this parameter is not specified then the host provided during creating mock session will be used as url/endpoint for the mock, else specified url path such as "/getNames" or "/deleteCity" will append to the host to form the url/endpoint. 
	 * @param basicAuthUsername - Optional - do not specify if you don't have basi auth for the request.
	 * @param basicAuthPassword - Optional - do not specify if you don't have basi auth for the request.
	 * @param requestQueryParams - Optional - the url query parameter condition that will be part of the request url.
	 * @param requestHeaders - Optional - the header conditions for the request
	 * @param requestBody - Optional - this is the body of the request as string
	 * @param requestCookies - Optional - cookie conditions on the request to send response
	 * @param responseStatus - Optional - the response status code that will be sent back to the client.
	 * @param responseHeaders - Optional - response headers to be sent.
	 * @param responseBody - Optional - response body that has to be sent for the request condition specified above.
	 */
	@Keyword
	def apiMockRule(String httpMethod, String urlPath, String basicAuthUsername, String basicAuthPassword, Map requestQueryParams,
			Map requestHeaders, String requestBody, Map requestCookies, Number responseStatus, Map responseHeaders, String responseBody) {

		if (httpMethod==null) httpMethod="ANY"
		if (urlPath==null) urlPath=""
		if (basicAuthUsername==null) basicAuthUsername=""
		if (basicAuthPassword==null) basicAuthPassword="";
		if (requestQueryParams==null) requestQueryParams=[:]
		if (requestHeaders==null) requestHeaders=[:]
		if (requestBody==null) requestBody=""
		if (requestCookies==null) requestCookies=[:]
		if (responseStatus==0 || responseStatus==null) responseStatus=200
		if (responseHeaders==null) responseHeaders=[:]
		if (responseBody==null) responseBody=""

		RequestPatternBuilder rpb;

		switch (httpMethod.toUpperCase()) {
			case "GET":
				rpb = RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlMatching(urlPath));
				break;
			case "POST":
				rpb = RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching(urlPath));
				break;
			case "PUT":
				rpb = RequestPatternBuilder.newRequestPattern(RequestMethod.PUT, urlMatching(urlPath));
				break;
			case "DELETE":
				rpb = RequestPatternBuilder.newRequestPattern(RequestMethod.DELETE, urlMatching(urlPath));
				break;
			default:
				rpb = RequestPatternBuilder.newRequestPattern(RequestMethod.ANY, urlMatching(urlPath));
		}

		if (!basicAuthUsername.equals("") && !basicAuthPassword.equals(""))
			rpb.withBasicAuth(new BasicCredentials(basicAuthUsername, basicAuthPassword));

		if (!requestBody.equals(""))
			rpb.withRequestBody(matching(requestBody));

		if (requestQueryParams.size() != 0) {
			for (Entry<String, String> reqQuPrm : requestQueryParams.entrySet())
				rpb.withQueryParam(reqQuPrm.getKey(), matching(reqQuPrm.getValue()));
		}

		if (requestHeaders.size() != 0) {
			for (Entry<String, String> reqHeader : requestHeaders.entrySet())
				rpb.withHeader(reqHeader.getKey(), matching(reqHeader.getValue()));
		}

		if (requestCookies.size() != 0) {
			for (Entry<String, String> reqCookie : requestCookies.entrySet())
				rpb.withCookie(reqCookie.getKey(), matching(reqCookie.getValue()));
		}

		RequestPattern rp = rpb.build();

		ResponseDefinitionBuilder rdb = new ResponseDefinitionBuilder();

		rdb.withStatus(responseStatus).withBody(responseBody);

		if (responseHeaders.size() != 0) {
			for (Entry<String, String> respHeader : responseHeaders.entrySet())
				rdb.withHeader(respHeader.getKey(), respHeader.getValue());
		}

		ResponseDefinition rd = rdb.build();

		StubMapping smp = new StubMapping(rp, rd);

		wireMockServer.addStubMapping(smp);

		try {
			wireMockServer.start();
		}
		catch(Exception e) {
			Logger.getAnonymousLogger().log(Level.SEVERE, MessageFormat.format("Unable to start mock : {0}", e.getMessage()));
		}

		Logger.getAnonymousLogger().log(Level.INFO, MessageFormat.format("Mock server started for http port : {0} and https port : {1}", wireMockServer.port(), (wireMockServer.httpsPort()==null || wireMockServer.httpsPort()==0)?"None":wireMockServer.httpsPort()));
	}


	/**
	 * Stops the running mock server
	 */
	@Keyword
	def stopAPIMockSession() {
		Logger.getAnonymousLogger().log(Level.INFO, "Mock Http Enabled : " + wireMockServer.isHttpEnabled())
		Logger.getAnonymousLogger().log(Level.INFO, "Mock is running : " + wireMockServer.isRunning())
		try {
			wireMockServer.stop()
		}
		catch(Exception e) {
			Logger.getAnonymousLogger().log(Level.WARNING, MessageFormat.format("Unable to stop mock : {0}", e.getMessage()));
		}
		Logger.getAnonymousLogger().log(Level.INFO, "Mock server successfully stopped");
	}

	/**
	 * Check if HTTP port is enabled
	 */
	@Keyword
	def boolean isHttpPortEnabled() {
		boolean enabled = wireMockServer.isHttpEnabled()
		Logger.getAnonymousLogger().log(Level.INFO, "Is Http port enabled? : " + enabled?"Yes, Enabled":"Not Enabled")
	}

	/**
	 * Check if HTTPS port is enabled
	 */
	@Keyword
	def boolean isHttpsPortEnabled() {
		boolean enabled = wireMockServer.isHttpsEnabled()
		Logger.getAnonymousLogger().log(Level.INFO, "Is Https port enabled? : " + enabled?"Yes, Enabled":"Not Enabled")
	}

	/**
	 * Check if Mock server is active and running
	 */
	@Keyword
	def boolean isMockServerRunning() {
		boolean running = wireMockServer.isRunning()
		Logger.getAnonymousLogger().log(Level.INFO, "Is Mock Server running : " + running?"Yes, Running":"Not Running")
		return running
	}
}