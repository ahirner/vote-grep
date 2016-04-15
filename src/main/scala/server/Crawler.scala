package server

import java.net.URL

import com.gargoylesoftware.htmlunit.html.{HtmlElement, HtmlPage}
import com.gargoylesoftware.htmlunit.{BrowserVersion, CookieManager, SilentCssErrorHandler, WebClient}

import scala.concurrent.{Await, ExecutionContext, Future}

// Initialization code from https://github.com/bplawler/crawler
// Can be refactored into stateless Future?
class Crawler( version: BrowserVersion = BrowserVersion.CHROME
               , failOnJSError: Boolean = false
               , javaScriptEnabled: Boolean = true
               , throwExceptionOnFailingStatusCode: Boolean = false
               , cssEnabled: Boolean = false
               , useInsecureSSL: Boolean = true
               , cookiesEnabled: Boolean = true
             ) {

  import ExecutionContext.Implicits.global

  val client = new WebClient(version)
  var currentPage: HtmlPage = null
  var currentUrl: URL = new URL("http://dirty.ref.com")

  // Set the various switches to affect the behavior of this client.
  client.getOptions.setThrowExceptionOnScriptError(failOnJSError)
  client.getOptions.setJavaScriptEnabled(javaScriptEnabled)
  client.getOptions.setThrowExceptionOnFailingStatusCode(throwExceptionOnFailingStatusCode)
  client.getOptions.setUseInsecureSSL(useInsecureSSL)
  client.getOptions.setCssEnabled(cssEnabled)
  if (!cssEnabled) {
    client.setCssErrorHandler(new SilentCssErrorHandler())
  }

  if (cookiesEnabled) {
    client.setCookieManager(new CookieManager)
  }

  def navigateTo(url: URL) = {
    currentUrl = url
    currentPage = client.getPage(currentUrl)
  }

  def click = {
    val element = currentPage.asInstanceOf[HtmlElement]
    val clickResult = Await.result(
      Future { element.click[HtmlPage]() }
      , 120.second
    )
    val start = (new java.util.Date).getTime
    val stillRunning = client.waitForBackgroundJavaScript(1000 /* ms */)
    println("waited %d ms for background JS, %d still running..."
      .format((new java.util.Date).getTime - start, stillRunning))
    currentPage = clickResult
    currentUrl = clickResult.getUrl
  }

  def executeScript(script: String) = {
    val element = currentPage.asInstanceOf[HtmlPage]
    val scriptResult = Await.result(
      Future { element.executeJavaScript(script).getNewPage }
      , 120.second
    )
    val start = (new java.util.Date).getTime
    val stillRunning = client.waitForBackgroundJavaScript(1000 /* ms */)
    println("waited %d ms for background JS, %d still running..."
      .format((new java.util.Date).getTime - start, stillRunning))
    currentPage = scriptResult.asInstanceOf[HtmlPage]
    currentUrl = currentPage.getUrl
  }


}


class CrawlBot(output: java.io.OutputStream = System.out,   version: BrowserVersion = BrowserVersion.CHROME
               , failOnJSError: Boolean = false
               , javaScriptEnabled: Boolean = true
               , throwExceptionOnFailingStatusCode: Boolean = false
               , cssEnabled: Boolean = false
               , useInsecureSSL: Boolean = true
               , cookiesEnabled: Boolean = true) extends Crawler(
  version = version,
  failOnJSError = failOnJSError,
  javaScriptEnabled = javaScriptEnabled,
  throwExceptionOnFailingStatusCode = throwExceptionOnFailingStatusCode,
  cssEnabled = cssEnabled,
  useInsecureSSL = useInsecureSSL,
  cookiesEnabled = cookiesEnabled
) {

  def topXml = currentPage.asXml
}