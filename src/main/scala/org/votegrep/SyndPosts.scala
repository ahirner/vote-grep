package org.votegrep

import java.util.Date
import java.net.URL
import com.sun.syndication.io.{ XmlReader, SyndFeedInput }
import com.sun.syndication.feed.synd.{ SyndCategory, SyndContent, SyndEntry }
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

// adopted from Scala Vienna User Group
// https://github.com/scala-vienna/scala-vienna-web



case class SyndPost(title: String,
                    link: String,
                    author: String,
                    description: Option[String],
                    publishedDate: Option[Date],
                    categories: Set[String])
object SyndPost {

  def contains(p: SyndPost, str: String = "minute"): Boolean = {
    val contents: List[String] = (p.categories.toList ++ p.description.toList ++ List(p.title))
    contents.map(c => c.toLowerCase.contains(str)).reduce(_||_)
  }

}

object SyndPosts {

  def fromURL(url: URL, timeout: Duration = Duration.Zero): Future[List[SyndPost]] = {
    val fetchFuture = Future {

      val input = new SyndFeedInput()
      val feed = input.build(new XmlReader(url))
      feed.getEntries.toList.map({
        case e: SyndEntry => {
          SyndPost(
            title = e.getTitle,
            author = Option(e.getAuthor).getOrElse(e.getAuthors.mkString(", ")),
            link = e.getLink,
            description = Option(Option(e.getDescription).map(_.getValue).getOrElse(e.getContents.map({
              case c: SyndContent => c.getValue
            }).mkString("\n"))),
            publishedDate = Option(Option(e.getPublishedDate).getOrElse(e.getUpdatedDate)),
            categories = e.getCategories.map({
              case c: SyndCategory => c.getName.toLowerCase
            }).toSet
          )
        }
      })
    }
    if (timeout > Duration.Zero)
      Future.firstCompletedOf(Seq(fetchFuture, Future[List[SyndPost]] { blocking { Thread.sleep(timeout.toMillis); Nil } }))
    else
      Future.firstCompletedOf(Seq(fetchFuture))
  }


}