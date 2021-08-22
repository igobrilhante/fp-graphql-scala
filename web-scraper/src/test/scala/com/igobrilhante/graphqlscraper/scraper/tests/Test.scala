package com.igobrilhante.graphqlscraper.scraper.tests

import java.util.logging.Level

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

object Test extends App {

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

  val doc = browser.get("https://www.nytimes.com/")




  // PrintWriter
  import java.io._
  val pw = new PrintWriter(new File("/tmp/nytimes.com.html" ))
  pw.write(doc.toHtml)
  pw.close()

}
