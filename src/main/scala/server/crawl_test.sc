
import scala.collection.JavaConversions._
import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._

import crawler._

class CrawlBot(output: java.io.OutputStream = System.out) extends Crawler {

  def crawl = {
  }

  def topXml = nodeStack.head.asXml

}


val c = new CrawlBot()
c.navigateTo("http://www.villapark.org/2015-villa-park-city-council-meeting-archives/")

//akka server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{HttpEntity, ContentTypes}
import akka.stream.ActorMaterializer

object WebServer {
  def run(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route =
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
        path("nihao") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<html>Ni Hao</html>"))
          }

        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    scala.io.StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }
}

WebServer.run(Array("bla"))
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