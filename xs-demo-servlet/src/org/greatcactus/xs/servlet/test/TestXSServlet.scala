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
import org.greatcactus.xs.frontend.XSToolBar
import org.greatcactus.xs.test.DemoPopup
import org.greatcactus.xs.frontend.html.servlet.XSServlet

/**
 * @author Andrew
 *
 */
class TestXSServlet extends XSServlet {  
  
    OneTimeSetup.ensureStarted()

  
  
  override def destroy() {
    if (loadedProperly || !file.exists()) save()
    super.destroy()
  }

  def save() {XMLSerialize.serialize(xsEditor.currentObject,new FileOutputStream(file)) }
  val file = new File("""C:\tmp\Space.xml""")
  var loadedProperly = true;
  val xsEditor = new XSEdit(
      try {
        loadFile
      } catch { case t:Throwable => t.printStackTrace(); loadedProperly=false; new Space(new History(""),Nil) }
  );
  
  def loadFile : Space = XMLDeserialize.deserialize[Space](new FileInputStream(file))
  object Toolbar extends XSToolBar {
    override def onSave() {save()}
    override def onRevert() { xsEditor.replaceRoot(loadFile)}
    override def useRevert = true
  }
  override def mainPage(request:HttpServletRequest,response:HttpServletResponse) {
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8")
    val out = response.getWriter();
    out.println("""<!DOCTYPE html>""")
    val client = new HTML5Client(xsEditor,Some(Toolbar),Locale.FRENCH,executionContext)
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