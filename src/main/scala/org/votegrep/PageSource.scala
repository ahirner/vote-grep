
package server.process

import org.jsoup.Jsoup
import java.net.URL
import java.security.MessageDigest

import scala.util.{ Try, Success, Failure }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object hashMD5 {

  val digest = MessageDigest.getInstance("MD5")

  def apply(bytes: Array[Byte]) : String = {
    digest.digest(bytes).map("%02x".format(_)).mkString
  }

  def apply(str: String) : String = {
    this(str.getBytes)
  }
}


//Timestamp for retrieved compatible with
// * java 8 time: java.time.Instant.now.toEpochMillis
// * System.currentTimeMillis
case class PageSource(source: String, contentType: String, url: URL, retrieved: Long, md5hash: String)

object PageSource {
  def fromURL(url: URL): Future[PageSource] = Future {
    //Todo: implement logging strategy, make userAgent configurable

    val strURL = url.toString
    println("downloading " + strURL)

    val response = Jsoup.connect(strURL).ignoreContentType(true).ignoreHttpErrors(false).followRedirects(true)
      .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1").execute()

    val contentType = response.contentType
    if (!contentType.startsWith("text/html")) throw new RuntimeException(s"Doesn't support page source with content: $contentType")

    val hash = hashMD5(response.bodyAsBytes())
    PageSource(response.body, contentType, response.url, System.currentTimeMillis, hash)
  }

  def fromFile(fn: String): Option[PageSource] = {

    //Todo: restore retrieved timestamp and URL
    val f = Try(scala.io.Source.fromFile(fn))
    f match {
      case Success(s) => {
        val url = new URL("http://from_file_system.com/" + fn)
        val content = s.mkString
        Some(PageSource(content, "text/html", url, 0, hashMD5(content)))
      }
      case Failure(e) => None
    }
  }

}