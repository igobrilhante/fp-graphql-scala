package com.igobrilhante.graphqlscraper.scraper.core.application

import java.util.logging.Level

import scala.concurrent.duration.DurationInt

import cats.effect.Resource
import cats.effect.kernel.Async
import cats.syntax.all._
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.igobrilhante.graphqlscraper.core.entities.News
import com.igobrilhante.graphqlscraper.core.scraper.WebScraper
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser.HtmlUnitDocument
import net.ruippeixotog.scalascraper.browser.JsoupBrowser.JsoupDocument
import net.ruippeixotog.scalascraper.browser.{Browser, HtmlUnitBrowser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{Document, Element}
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.{element, elementList}
import org.apache.http.conn.HttpHostConnectException
import org.typelevel.log4cats.Logger

/**
  * Implements methods to support typeclass instances of [[WebScraper[F]] ].
  */
trait WebScraperF[F[_]] { self: WebScraper[F] with LoggingF =>

  /**
    * Parse a html file to attempt to get a [[JsoupDocument]].
    */
  def parseFile(browser: JsoupBrowser, file: String)(implicit F: Async[F]): F[Either[Throwable, JsoupDocument]] = {
    F.delay(browser.parseFile(file)).attempt
  }

  /**
    * Create a browser of type [[HtmlUnitBrowser]].
    */
  def createHtmlUnitBrowser()(implicit F: Async[F]): F[HtmlUnitBrowser] = F.delay {
    val browser = HtmlUnitBrowser.typed()
    java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF)
    browser.underlying.getOptions.setThrowExceptionOnFailingStatusCode(false)
    browser.underlying.getOptions.setThrowExceptionOnScriptError(false)
    browser.underlying.getOptions.setPrintContentOnFailingStatusCode(false)
    browser.underlying.getOptions.setScreenHeight(1080)
    browser.underlying.getOptions.setScreenWidth(1920)
    browser.underlying.getOptions.setDownloadImages(false)
    browser.underlying.getOptions.setCssEnabled(false)
    browser.underlying.getOptions.setJavaScriptEnabled(true)
    browser.underlying.setJavaScriptTimeout(30000)
    browser.underlying.setAjaxController(new NicelyResynchronizingAjaxController)

    browser.underlying.waitForBackgroundJavaScriptStartingBefore(1000)
    browser.underlying.waitForBackgroundJavaScript(1000)

    browser
  }

  /**
    * Interact with the a document by scrolling down n times.
    */
  def simulateScrollDown(doc: Document, scrolls: Int)(implicit F: Async[F]): F[Unit] = {
    def htmlUnitScroll(document: HtmlUnitDocument) = {
      val computation = F.delay[Unit] {
        document.underlying
          .asInstanceOf[HtmlPage]
          .executeJavaScript("window.scrollTo(0, document.body.scrollHeight);")
        ()
      }

      if (scrolls > 1) computation *> F.sleep(1.second) *> simulateScrollDown(doc, scrolls - 1)
      else computation
    }

    doc match {
      case document: HtmlUnitDocument => htmlUnitScroll(document)
      case _                          => F.unit
    }

  }

  /**
    * Scrape the document to look up for the news.
    */
  def scrapeDoc(doc: Document)(implicit F: Async[F]): F[Either[Throwable, List[News]]] = {

    val stories = F.delay(doc >> elementList("a"))

    /* For links with more than one H3 headlines apply the following strategy */
    def getH3Strategy(elem: Element): Element = {
      val h3 = elem >> elementList("h3") match {
        case singleH3 :: Nil    => singleH3
        case _ :: second :: Nil => second
        case list               => list.head
      }
      h3
    }

    /* Get a possible headline from a html element */
    def getHeadline(doc: Element): Option[String] = {
      val elem = (doc >?> element("h2"), doc >?> element("h3")) match {
        // found h2
        case (Some(h2), _) => Some(h2)
        // found h3
        case (_, Some(_)) => Some(getH3Strategy(doc))
        // otherwise
        case _ => None
      }
      // handle empty titles
      elem.map(_.text.trim).flatMap {
        case "" => none[String]
        case s  => s.some
      }
    }

    def getElem(elem: Element): F[Option[News]] = F.delay {
      val link     = elem
      val headline = getHeadline(link)
      (link.attr("href").some, headline).mapN(News.apply)
    }

    def getElems(elems: List[Element]) = F
      .parSequenceN(2)(elems.map(getElem))
      .map(_.collect { case Some(news) => news })

    val result = for {
      newsList <- stories.flatMap(getElems)
      _        <- Logger[F].info(s"Retrieved ${newsList.length} news. Sample:\n${newsList.take(5).mkString("\n")}\n...")
    } yield newsList.distinctBy(_.link)

    result.attempt
  }

  /**
    * Get a HtmlUnit browser wrapped as a resource.
    */
  def getBrowserHtmlUnitBrowser(implicit F: Async[F]): Resource[F, HtmlUnitBrowser] = {
    def acquire = createHtmlUnitBrowser()

    def release(browser: HtmlUnitBrowser) = {
      F.delay(browser.closeAll())
    }

    Resource.make(acquire)(release)
  }

  /**
    * Get a Jsoup browser wrapped as a resource.
    */
  def getJsoupBrowser(implicit F: Async[F]): Resource[F, JsoupBrowser] = {
    def acquire = F.delay(JsoupBrowser.typed())

    Resource.make(acquire)(_ => F.unit)
  }

  /**
    * Use the browser to get the given page document.
    */
  def getPage(browser: Browser, page: String)(implicit F: Async[F]): F[browser.DocumentType] =
    Logger[F].info(s"Scraping page $page") *> F
      .delay(browser.get(page))
      .adaptErr {
        case _: HttpHostConnectException      => ConnectHostException(page)
        case _: java.net.UnknownHostException => ConnectHostException(page)
      }

}
