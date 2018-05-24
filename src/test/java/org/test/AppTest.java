package org.test;


import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;
import org.junit.Test;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.concurrent.TimeUnit;

/**
 * Unit test for simple App.
 */
public class AppTest
{

    @Test
    public void seleniumTest() throws Exception
    {
        //Change this to the actual chromedriver location
        System.setProperty("webdriver.chrome.driver", "/Users/evgenyp/Downloads/chromedriver");

        // start the proxy
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.start(0);

        // get the Selenium proxy object
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

        // configure it as a desired capability
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

        ChromeOptions options = new ChromeOptions();
        options.merge(capabilities);

        // start the browser up
        WebDriver driver = new ChromeDriver(options);

        // enable more detailed HAR capture, if desired (see CaptureType for the complete list)
        proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT, CaptureType.REQUEST_HEADERS, CaptureType.RESPONSE_HEADERS);

        /**
         * add custom header to request*
         **/
        //proxy.addHeader("User-Agent", "TEST_AGENT");
        //proxy.addHeader("Custom-header", "CUSTOM_HEADER");


        /**
         * Enable a response filter, which will monitor all responses and do some actions
         */

        ResponseFilter filter = new ResponseFilter() {
            public void filterResponse(HttpResponse httpResponse, HttpMessageContents httpMessageContents, HttpMessageInfo httpMessageInfo) {


                //Print all url's, so we can later use it to filter particular responses:
                System.out.println("intercepting response for url :" + httpMessageInfo.getOriginalUrl());

                //Change http status to another for some url
                if (httpMessageInfo.getOriginalUrl().contains("homepage-hero-large.jpg")) {

                    System.out.println("FOUND URL");
                    HttpResponseStatus newStatus = new HttpResponseStatus(500,"ERROR");
                    HttpResponse res = httpResponse;
                    res.setStatus(newStatus);

                }

                //change all occurrences of the word Wix to SUCCESS
                //String messageContents = httpMessageContents.getTextContents();
                //String newContents = messageContents.replaceAll("Wix", "SUCCESS");
                //httpMessageContents.setTextContents(newContents);
            }
        };

        proxy.addResponseFilter(filter);

        // create a new HAR with the label "wix.com"
        proxy.newHar("wix.com");

        // open yahoo.com
        driver.get("http://wix.com");
        TimeUnit.SECONDS.sleep(15);
        // get the HAR data which can be used later to analyze statistics
        Har har = proxy.getHar();

        String requestType = har.getLog().getEntries().get(0).getRequest().getMethod();
        String url = har.getLog().getEntries().get(0).getRequest().getUrl();
        int number = har.getLog().getEntries().size();


        System.out.println("Total: " + number + " requests/responses ");
        System.out.println("The first request was of type " + requestType + ", to URL " + url);

        TimeUnit.SECONDS.sleep(10);
        driver.close();
        driver.quit();
    }

}
