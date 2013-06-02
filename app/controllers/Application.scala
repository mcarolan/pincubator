package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }

  def add = Action {
    Ok(views.html.add())
  }

  def grab = Action {
    Ok(views.html.grab())
  }
  
}