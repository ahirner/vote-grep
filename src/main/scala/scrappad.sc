import org.votegrep._
import org.jsoup.Jsoup

val ps = PageSource.fromFile("test_html_seeds/www.fountainvalley.org_9060a1127cc21e721466d7afdf0511dd.html").get
val doc = Jsoup.parse(ps.source)

