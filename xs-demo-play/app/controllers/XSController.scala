/**
 * Copyright 2013 Andrew Conway. All rights reserved.
 */

package controllers

import play.api._
import play.api.mvc._
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import play.api.templates.Html
import views.html.defaultpages.badRequest
import org.greatcactus.xs.frontend.XSToolBar
import org.greatcactus.xs.test.DemoPopup


/**
 * @author Andrew
 */
object XSController extends Controller {
  HTML5DetailsPane.addCustom(DemoPopup)
  
  def icon(name:String) = Action { implicit request =>
//    val lastMod = request.headers.get(IF_MODIFIED_SINCE).flatMap(ResAssets.parseDate)
    if (name==null || name.isEmpty) BadRequest("Query string doesn't make sense")
    else IconManifests.iconFromFilename(name) match {
      case None =>     NotFound("No icon "+name)
//      case Some(icon) if icon.contents.lastModified>0 &&  icon.contents.lastModified <= lastMod => NotModified
      case Some(icon) =>
        println("Found icon ")
        val res = Ok(icon.data).as(icon.mimeType)
        if (icon.contents.lastModified>0) res.withHeaders(LAST_MODIFIED -> icon.contents.lastModified.toString) else res 
    }
  }
  
  def resultOfMessage(message:ClientMessage) : PlainResult = {
        val resBytes = new ByteArrayOutputStream()
        message.serialize(resBytes)
        Ok(resBytes.toByteArray()).as(JSON)
  }
  
  def userDidSomething(xsSessionID:String,messageCount:Long) = Action(parse.raw){ request =>
    request.body.asBytes(10*1024*1024) match {
      case Some(d) =>
        val resMessage = SessionManagement.get(xsSessionID) match {
          case None => ClientMessage.lostSession
          case Some(session) =>
            val message = ClientMessage.deserialize(new ByteArrayInputStream(d)) 
            session.receivedPossiblyUnorderedMessage(message, messageCount)
        }
        resultOfMessage(resMessage)
      case None => BadRequest("Too long")
    }
  }
  
  import scala.concurrent.ExecutionContext.Implicits.global
  import play.api.data._
  import play.api.data.Forms._
  
  val xsCometForm = Form("xsSessionID"->text)
  
  def xsComet = Action { implicit request =>
        SessionManagement.get(xsCometForm.bindFromRequest.get) match {
          case None => resultOfMessage(ClientMessage.lostSession)
          case Some(session) =>
            Async {
              session.cometCallFuture map {
                case Some(message) => resultOfMessage(message)
                case None => Ok("null").as(JSON)
              }
            }
        }
  }

  IconManifests.urlOfIcon = new URLOfIcon {
     def apply(icon:ResolvedIcon) : String = routes.XSController.icon(icon.fileName).url  // XSController.icon(icon.fileName)  // "/icon/"+URLEncoder.encode(icon.fileName,"UTF-8") //  FIXME
  }
  
  val file = new File("""C:\tmp\Space.xml""")
  var loadedProperly = true;
  val xsEditor = new XSEdit(
      try {
        XMLDeserialize.deserialize[Space](new FileInputStream(file))
    } catch { case t:Throwable => t.printStackTrace(); loadedProperly=false; new Space(new History(""),Nil) }
  );

  def demo = Action {
    val client = new HTML5Client(xsEditor,Some(new XSToolBar),Locale.FRENCH,scala.concurrent.ExecutionContext.Implicits.global)
    Ok(views.html.demo(client.baseHTML))
  }
  
}