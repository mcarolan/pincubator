package controllers

import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import anorm._
import java.sql.Connection

object PinController extends Controller {

  def savePinTagMapping(pinId: Int, tagId: Int) = {
    DB.withConnection(implicit c => {
      val insertTag = SQL("INSERT INTO pin_tag(pin_id, tag_id) VALUES ({pinId}, {tagId})").on('pinId -> pinId, 'tagId -> tagId)
      insertTag.execute()
    })
  }

  def savePin(pin: String, note: String, tagIds: Set[Int]) = {

    val pinId = DB.withConnection(implicit c => {
      val insertPin = SQL("INSERT INTO pin(pin, redeemed, note) VALUES ({pin}, 0, {note})").on('pin -> pin, 'note -> note)
      insertPin.execute()

      lastId
    })

    tagIds.foreach(savePinTagMapping(pinId, _))
  }

  def saveToDatabase(rawPins: String, note: String, rawTags:String): Unit = {

    val pins = rawPins.split('\n').map(_.trim)
    val tags = rawTags.split(',').map(_.trim.toLowerCase)
    val tagIds = mapTags(tags)

    pins.foreach(pin => savePin(pin, note, tagIds))
  }

  def lastId(implicit c: Connection): Int = {
    val selectId = SQL("select last_insert_rowid() as id")
    selectId().map(row => row[Int]("id")).head
  }

  def mapTag(tag: String): Int = {
    DB.withConnection(implicit c => {
      val selectTag = SQL("SELECT id FROM tag WHERE tag = {tag}").on('tag -> tag)
      val tagId = selectTag().map(r => r[Int]("id")).headOption

      tagId.getOrElse({
        val insertTag = SQL("INSERT INTO tag(tag) VALUES ({tag})").on('tag -> tag)
        insertTag.execute()

        lastId
      })
    })
  }

  def mapTags(tags: Seq[String]) =
    tags.map(mapTag(_)).toSet

  def save = Action(parse.json) { req =>
    val pinsJson = (req.body \ "pins").asOpt[String]
    val tagsJson = (req.body \ "tags").asOpt[String]
    val noteJson = (req.body \ "note").asOpt[String]

    val result = for
    {
      rawPins <- pinsJson
      rawTags <- tagsJson
      note <- noteJson
    }
      yield saveToDatabase(rawPins, note, rawTags)


    result.map(_ => Ok).getOrElse(BadRequest("Expected tags, note, and pins in json"))
  }

}
