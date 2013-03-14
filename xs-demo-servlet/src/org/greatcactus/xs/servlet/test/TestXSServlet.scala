/**
 * Copyright 2012-2013 Andrew Conway. All rights reserved.
 */
package org.greatcactus.xs.servlet.test

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.greatcactus.xs.frontend.XSEdit
import org.greatcactus.xs.test.Space
import java.util.Locale
import org.greatcactus.xs.frontend.html._
import scala.collection.mutable.Queue
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue
import org.greatcactus.xs.api.serialization.XMLDeserialize
import java.io.FileInputStream
import java.io.File
import org.greatcactus.xs.api.serialization.XMLSerialize
import java.io.FileOutputStream
import org.greatcactus.xs.api.icon.IconManifests
import org.greatcactus.xs.api.icon.URLOfIcon
import org.greatcactus.xs.api.icon.ResolvedIcon
import java.net.URLEncoder
import org.greatcactus.xs.test.History
import org.greatcactus.xs.test.SpaceExternalDependencyResolver
import scala.concurrent.Await

/**
 * @author Andrew
 *
 */
class TestXSServlet extends HttpServlet {  
  
  override def doGet(request:HttpServletRequest,response:HttpServletResponse) {
    request.getParameter("sub") match {
      case null => mainPage(response)
      case "icon" => icon(response,request.getParameter("name"),request.getDateHeader("If-Modified-Since"))
    }
  }

  IconManifests.urlOfIcon = new URLOfIcon {
    def apply(icon:ResolvedIcon) : String = "?sub=icon&name="+URLEncoder.encode(icon.fileName,"UTF-8")
  }
  def icon(response:HttpServletResponse,name:String,lastMod:Long) {
    if (name==null || name.isEmpty) response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Query string doesn't make sense")
    else IconManifests.iconFromFilename(name) match {
      case None => response.sendError(HttpServletResponse.SC_NOT_FOUND)
      case Some(icon) if icon.contents.lastModified>0 &&  icon.contents.lastModified <= lastMod => response.sendError(HttpServletResponse.SC_NOT_MODIFIED) 
      case Some(icon) => 
        response.setContentType(icon.mimeType)
        if (icon.contents.lastModified>0) response.setDateHeader("Last-Modified",icon.contents.lastModified)
        val os = response.getOutputStream();
        os.write(icon.data)
        os.close()
    }
  }
  
  /** Sending via comet or message */
  override def doPost(request:HttpServletRequest,response:HttpServletResponse) {
    val responseMessage = (for (sessionID<-Option(request.getParameter("xsSessionID"));e<-SessionManagement.get(sessionID)) yield e) match {
      case Some(session) =>
        request.getParameter("sub") match {
          case "comet" => 
            val res = Await.result(session.cometCallFuture,scala.concurrent.duration.Duration.Inf) // TODO should do asynchronously rather than await.
              //session.cometCallShouldReturnImmediately().orElse{session.cometCall()}
           // Thread.sleep(3000L) // simulate a slow connection
            res
          case "message" =>
            val message = ClientMessage.deserialize(request.getInputStream())
            println("Received "+message)
            //Thread.sleep((java.lang.Math.random()*3000).toInt) // simulate a slow connection with packet reordering
            //if (java.lang.Math.random()>0.8) throw new IllegalArgumentException // simulate a server error
            //if (java.lang.Math.random()>0.8) Thread.sleep(20000) // simulate a lost packet (with a long delay to stop error messages arriving)
            val messageCountString = request.getParameter("count")
            try {
              if (messageCountString!=null) Some(session.receivedPossiblyUnorderedMessage(message, messageCountString.toLong))
              else None
            } catch { case _:NumberFormatException => None }
          case _ => None
        }
      case None => Some(ClientMessage.lostSession)
    } 
    response.setContentType("text/json");
    responseMessage match {
      case Some(message) => message.serialize(response.getOutputStream())
      case None => response.getWriter().print("null")
    }
  }
  
  override def destroy() {
    if (loadedProperly || !file.exists())
      XMLSerialize.serialize(xsEditor.currentObject,new FileOutputStream(file))
  }

  val file = new File("""C:\tmp\Space.xml""")
  var loadedProperly = true;
  val xsEditor = new XSEdit(
      try {
        XMLDeserialize.deserialize[Space](new FileInputStream(file))
    } catch { case t:Throwable => t.printStackTrace(); loadedProperly=false; new Space(new History(""),Nil) },Some(SpaceExternalDependencyResolver)
  );
  
  def mainPage(response:HttpServletResponse) {
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8")
    val out = response.getWriter();
    out.println("""<!DOCTYPE html>""")
    val client = new HTML5Client(xsEditor,Locale.FRENCH)
    val page =  
      <html>
        <head>
          <meta charset="utf-8"/>
          <link rel="stylesheet" href="../SlickGrid/slick.grid.css"/>
          <link rel="stylesheet" href="../jQueryUI/css/smoothness/jquery-ui.css"/>
          <link rel="stylesheet" href="../jquery.contextMenu/jquery.contextMenu.css"/>
          <link rel="stylesheet" href="../xs/xsedit.css"/>
          <script src="../jQuery/jquery-1.9.1.min.js"> </script>
          <script src="../jQuery/jquery-migrate-1.1.0.js"> </script>
          <script src="../xs/xsedit.js"> </script>
          <script src="../xs/xsPTF.js"> </script>
          <script src="../xs/xsGrid.js"> </script>
          <script src="../jQueryUI/js/jquery-ui-1.10.0.custom.min.js"></script>
          <script src="../jQuery/jquery.event.drag-2.2.js"></script>
          <script src="../jQuery/jquery.event.drop-2.2.js"></script>
          <script src="../SlickGrid/slick.core.js"></script>
          <script src="../SlickGrid/plugins/slick.cellrangedecorator.js"></script>
          <script src="../SlickGrid/plugins/slick.cellrangeselector.js"></script>
          <script src="../SlickGrid/plugins/slick.cellselectionmodel.js"></script>
          <script src="../SlickGrid/plugins/slick.rowselectionmodel.js"></script>
          <script src="../SlickGrid/plugins/slick.rowmovemanager.js"></script>
          <script src="../SlickGrid/slick.formatters.js"></script>
          <script src="../SlickGrid/slick.editors.js"></script>
          <script src="../SlickGrid/slick.grid.js"></script>
          <script src="../jquery.contextMenu/jquery.ui.position.js"></script>
          <script src="../jquery.contextMenu/jquery.contextMenu.js"></script>
          <title>Test ▶XS◀ in a Servlet</title>
        </head>
        <body spellcheck="false">
          
          <h1>Test XS</h1>
          
          { client.baseHTML }
        </body>
      </html>
    //println(page.toString)
    out.println(page.toString)
  }
}  