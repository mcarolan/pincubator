package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.db._
import play.api.Play.current
import anorm._

object TagController extends Controller {

  def getTags = Action { req =>
    val term = req.queryString.get("term").map(t => t.head.toLowerCase.trim)

    val result = term map { t =>
      val tags = DB.withConnection(implicit c => {
        val selectTags = SQL("SELECT DISTINCT tag FROM tag")

        selectTags().map(row => row[String]("tag").toLowerCase.trim).toList
      })

      tags filter (_.startsWith(t))
    }

    result.map(tagList => Ok(Json.toJson(tagList))).getOrElse(BadRequest("Expected term on query string"))
  }

}
