/**
 * Copyright (c) 2014 Andrew Conway. All rights reserved.
 */
package org.greatcactus.xs.servlet.test

import org.greatcactus.xs.api.icon.IconManifests
import org.greatcactus.xs.api.icon.URLOfIcon
import org.greatcactus.xs.api.icon.ResolvedIcon
import org.greatcactus.xs.frontend.html.HTML5DetailsPane
import org.greatcactus.xs.impl.DependencyInjectionCurrentStatus
import org.greatcactus.xs.test.DemoPopup

/**
 * Things that need to be executed before any pages are served.
 */
object OneTimeSetup {
  /*
  val machinebase = "localhost:8080"
  val contextPath = "xs-demo-servlet"
  val wsbase = "ws://"+machinebase+"/"+contextPath
  */
  def ensureStarted() {}
  
  HTML5DetailsPane.addCustom(DemoPopup)
/*
  IconManifests.urlOfIcon = new URLOfIcon {
     def apply(icon:ResolvedIcon) : String = routes.XSController.icon(icon.fileName).url  
  }
*/
  // DependencyInjectionCurrentStatus.debugDependencyInjections=true
}