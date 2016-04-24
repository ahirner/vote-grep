
package server

import java.net.URL

import scala.concurrent._
import scala.concurrent.duration._

//akka server

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.model.{HttpEntity, ContentTypes}
import akka.stream.ActorMaterializer


class BrowserActor extends Actor {
  override def receive: Receive = {
    case message => sender() ! message
  }
}

object WebServer {

  val scriptPreamble =
    """
      |//preamble
      |function getElementByXpath(path) {
      |return document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
      |}
      |function logRemote(msg) {
      |xhttp = new XMLHttpRequest();
      |xhttp.open("POST", "http://localhost:8080/log", true);
      |xhttp.send(msg);
      |}
    """.stripMargin
  val IndexHTML =
    """
      | <h1>Scrape complex sites with ease</h2>
      |<div>
      |<form action="navigate" method="post">
      |  <input type="text" value ="http://placentia.granicus.com/ViewPublisher.php?view_id=4" name="url"
      |   size="80">
      |  <input type="submit" value="Navigate To" autofocus>
      |</form>
      |<a href="s" target="_blank">Open sanitized Site</a>
      |<hr>
      |<form action="execute" method="post">
      |  code (delineate commands with ||) <br>
      |  <textarea cols="70" rows="10" name="code">
      |getElementByXpath("//*[@id='VoteLogTabs']/ul/li[2]/a").click(); return("changed to VoteLog"); ||
      |getElementByXpath("//tr/td[@class=\"gvl_details\"]/a").click(); return("opened details for most recent vote"); ||
      |var date = getElementByXpath("//tr/td[@class=\"gvl_date\"]/span").textContent;
      |var result = getElementByXpath("//div[@id='gvl_voteDetails']").textContent;
      |return "date = " + date + "\n\n" + result;</textarea>
      |  <br>
      |  <input type="submit" value="Execute Code">
      |</form>
      |</dv>
    """.stripMargin

  val RedirectHTML=
    """
      |<p>..redirecting in 2 seconds </p>
      |<script>window.setTimeout(function(){ window.location = ".."; },2000);</script>
    """.stripMargin

  def run(c: String = "<h1>Navigate to Page first..</h1>") = {

    val crawler = new CrawlBot()

    var content = c
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    // needed for thefuture flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val browser = system.actorOf(Props(new BrowserActor))

    def route(echo: ActorRef)(implicit mat: ActorMaterializer, ec: ExecutionContext) = {
      path("") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, IndexHTML))
        }
      } ~
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      } ~
      path("s") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, content))
        }
      } ~
      path("e" / Segment) { message => {
        get {
          complete {
            implicit val timeout: Timeout = 1.second
            (echo ? message).mapTo[String]
          }
        }
      }
      } ~
      path("navigate") {
        post {
          formField("url") { urlStr => {
              //todo error handling
              val url = new URL(urlStr)
              crawler.navigateTo(url)
              content = crawler.topXml
              val res = s"<h2>navigated to: $urlStr</h2>" + RedirectHTML
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, res))
            }
          }
        }
      } ~
      path("execute") {
        post {
          formField("code") { codeStr => {

            val results = codeStr.split("""\|\|""").map(s => {
              val res = crawler.executeScript(scriptPreamble + s)
              Thread sleep 2500
              res.toString
            })

            val resStr = results mkString "\n"
            println("script run returned: \n" + resStr)
            content = crawler.topXml

            val htmlStr = resStr.replaceAll("\n", "<br>")
            val res = s"<h3>executed: ${codeStr.take(10)}...</h2><p> yielded $htmlStr" + RedirectHTML
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, res))
          }
          }
        }
      } ~
      path("log") {
        post {
          decodeRequest {
            entity(as[String]) { msg =>
              println(msg)
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, ""))
            }
          }
        }
      }

    }
    //start and block
    val bindingFuture = Http().bindAndHandle(route(browser), "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    scala.io.StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }
}