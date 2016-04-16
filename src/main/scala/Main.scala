
import org.votegrep._
import server._


import java.net.URL
import org.jsoup.Jsoup
import scala.collection.JavaConverters._


import purecsv.safe._
import purecsv.safe.converter.StringConverter
import scala.util.{ Try, Success, Failure }
import scala.concurrent.{ Future }
import scala.concurrent.ExecutionContext.Implicits.global


object Server extends App {

  WebServer.run()
}


case class SeedURL(id: Int, provenance: String, org: String, seedURL: URL)

case class Content(url: URL, strURL: String, content: String, URLs: List[String]) {

  override def toString(): String = {
    val l = URLs.length
    val u = url.toString
    s"$u - $strURL($content) \t contained $l URLs"
  }
}


/*
object Main {

  implicit val URLConverter = new StringConverter[URL] {
    override def tryFrom(str: String): Try[URL] = {
      Try(new URL(str))
    }
    override def to(url: URL): String = {
      url.toString
    }
  }

  val seedsFileURL = """file:///Users/dafcok/Documents/Development/scala-vote-grep/conf/seeds.csv"""
  val seedsFile = scala.io.Source.fromURL(seedsFileURL)
  val truncatedCSV = seedsFile
    .getLines
    .map(_.split(";").take(4).reduce(_ + ";" + _)).map(_ + "\n")
    .toList
    .tail.reduce(_ + _)


  val seedsResult = CSVReader[SeedURL].readCSVFromString(truncatedCSV, delimiter = ';')

  import purecsv.safe.tryutil._
  val (seeds, seedsFailure) = seedsResult.getSuccessesAndFailures

  if (seedsFailure.length > 0) {
    println("got the following errors in seed URLs: %s"
      .format(seedsFailure.map(s => s._1.toString + ": " + s._2.toString +"/n").reduce(_+_)))
  }
  else println("got %d good seeds".format(seeds.length))

  //courtesy of https://github.com/Foat/articles/blob/master/akka-web-crawler/src/main/scala/me/foat/crawler/Scraper.scala
  def process(url: URL): Content = {

//    Try {
      //unsafe
      //println("parsing " + url.toString)
      val strURL = url.toString
      val response = Jsoup.connect(strURL).ignoreContentType(true)
        .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1").execute()

      val status = response.statusCode
      val contentType = response.contentType

      //safe
      if (contentType.startsWith("text/html")) {
        val doc = response.parse()
        val title: String = doc.getElementsByTag("title").asScala.map(e => e.text()).head
        val descriptionTag = doc.getElementsByTag("meta").asScala.filter(e => e.attr("name") == "description")
        val description = if (descriptionTag.isEmpty) "" else descriptionTag.map(e => e.attr("content")).head
        val links = doc.getElementsByTag("a").
          asScala.map(e => e.attr("href")).toList
        Content(url, title, description, links)
      } else {
        // e.g. if this is an image
        Content(url, strURL, contentType, List())
      }
  //  }
  }

  import java.io.File
  def savePageSource(source: PageSource, prefix: String = "") = {

    val urlNormalized = source.url.toURI.normalize.toURL
    val fn = urlNormalized.getHost + "_" + source.md5hash + ".html"

    val pw = new java.io.PrintWriter(new File(prefix + fn))
    try pw.write(source.source) finally pw.close()
    
  }

  def downloadPage(url: URL): Future[PageSource] = Future {
    //Todo: implement logging strategy, make userAgent configurable

    val strURL = url.toString
    //println("downloading " + strURL)

    val response = Jsoup.connect(strURL).ignoreContentType(true).ignoreHttpErrors(false).followRedirects(true)
      .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1").execute()

    val contentType = response.contentType
    if (!contentType.startsWith("text/html")) throw new RuntimeException(s"Doesn't support page source with content: $contentType")

    val hash = hashMD5(response.bodyAsBytes())
    PageSource(response.body, contentType, response.url, System.currentTimeMillis, hash)
  }

  //val pageSources = seeds.take(2).map(s => PageSource.fromURL(s._2.seedURL))
/*
  pageSources.foreach((s) => s onComplete {
    case Success(source) => {
      println("✓ " + source.md5hash + " from " + source.url.getHost)
      //savePageSource(source)
    }
    case Failure(e) => println("! " + e)
  })
*/
  val contents = seeds.map(s => (s, Future(process(s._2.seedURL))))
  contents.foreach(s => s._2 onComplete {
    case Success(r) => println(if (r.URLs.nonEmpty) ("✓\t" + r) else ("?\t" + r)  )
    case Failure(e) => println("!\t" + e.toString+" "+s._1._2.seedURL.toString+"("+s._1._1+")")
  })


  scala.io.StdIn.readLine()
  //seeds.foreach(s => println(s._1 + " **** " + parse(s._2.seedURL).toString))

}*/
