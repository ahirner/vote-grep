
package server

import java.net.URL

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._
//import org.apache.http.HttpEntity
import scala.collection.JavaConversions._
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

class EchoActor extends Actor {
  override def receive: Receive = {
    case message => sender() ! message
  }
}

object WebServer {

  val IndexHTML =
    """
       <h1>Scrape complex sites with ease</h2>
      <div>
      <form action="navigate" method="post">
        <input type="text" value ="http://www.villapark.org/2015-villa-park-city-council-meeting-archives/" name="url"
         size="80">
        <input type="submit" value="Navigate To" autofocus>
      </form>
      <a href="s" target="_blank">Open sanitized Site</a>
      <hr>
      <form action="execute" method="post">
        <textarea cols="70" rows="5" name="js-code"></textarea>
        <input type="submit" value="Execute Code">
      </form>
      </dv>
    """.stripMargin

  def run(c: String = "<h1>sanitized HTML goes here</h1>") = {

    val crawler = new CrawlBot(cssEnabled = true)

    var content = c
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    // needed for thefuture flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val echo = system.actorOf(Props(new EchoActor))

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
              complete(s"navigated to: $urlStr")
            }
          }
        }
      } ~
      path("execute") {
        post {
          formField("js-code") { codeStr => {
            crawler.executeScript(codeStr)
            content = crawler.topXml
            complete(s"executed: $codeStr")
          }
          }
        }
      }
    }
    //start and block
    val bindingFuture = Http().bindAndHandle(route(echo), "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    scala.io.StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }
}


// http4s server
/*

import org.http4s._, org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Response, Request}
import org.http4s.server.blaze._

val serviceTest = HttpService {
  case GET -> Root / "hello" / name =>
    Ok(s"Hello, $name.")
}

val serviceTopCrawlXml = HttpService {
  case req @ GET -> Root / "v" / detail =>
    Ok(c.topXml).putHeaders(`Content-Type`(MediaType.`text/html`))
}


val builder = BlazeBuilder
  .mountService(serviceTopCrawlXml)
  .mountService(serviceTest)

println(c.topXml)
val server = builder.run
scala.io.StdIn.readLine()
server.shutdownNow
*/

