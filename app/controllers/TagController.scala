package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.db._
import play.api.Play.current
import anorm._

object TagController extends Controller {

  def getAllTags = Action { req =>
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

  def getTagsForPin(id: Int) = Action { req =>
    val result =
      DB.withConnection(implicit c => {
        val selectTags = SQL("SELECT DISTINCT tag.tag FROM tag INNER JOIN pin_tag ON pin_tag.tag_id = tag.id INNER JOIN pin ON pin_tag.pin_id = pin.id WHERE pin.id = {pinId}").on('pinId -> id)
        selectTags().map(row => row[String]("tag").toLowerCase.trim).toList
      })

    Ok(Json.toJson(result.mkString(",")))
  }

}
