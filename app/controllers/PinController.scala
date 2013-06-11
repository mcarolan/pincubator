package controllers

import play.api.mvc._
import play.api.db.DB
import play.api.Play.current
import anorm._
import java.sql.Connection
import play.api.libs.json.Json
import models.AnormImplicits._
import models.Pin

object PinController extends Controller {

  def parseTags(rawTags: String) = rawTags.split(',').map(_.trim.toLowerCase)

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
    val tags = parseTags(rawTags)
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

  def updatePin(pinId: Int, rawTags: String, note: String, redeemed: Boolean): Unit = {
    DB.withConnection(implicit c => {
      val deleteTagMapping = SQL("DELETE FROM pin_tag WHERE pin_id = {pinId}").on('pinId -> pinId)
      deleteTagMapping.execute()

      val tags = parseTags(rawTags)
      val mappedTags = mapTags(tags)

      mappedTags.foreach(tagId => savePinTagMapping(pinId, tagId))

      val updatePin = SQL("UPDATE pin SET note = {note}, redeemed = {redeemed} WHERE id = {pinId}").on('note -> note, 'redeemed -> redeemed, 'pinId -> pinId)
      updatePin.execute()
    })
  }

  def update = Action(parse.json) { req =>
    val pinIdJson = (req.body \ "pinId").asOpt[Int]
    val tagsJson = (req.body \ "tags").asOpt[String]
    val noteJson = (req.body \ "note").asOpt[String]
    val redeemedJson = (req.body \ "redeemed").asOpt[Boolean]

    val result = for
    {
      pinId <- pinIdJson
      tags <- tagsJson
      note <- noteJson
      redeemed <- redeemedJson
    }
      yield updatePin(pinId, tags, note, redeemed)

    result.map(_ => Ok).getOrElse(BadRequest("Expected pinId, tags, note, redeemed"))
  }

  def findPin(rawTags: String, redeemed: Boolean, lastId: Option[Int]) = {
    val tags = rawTags.split(',').map(_.trim.toLowerCase).toSet

    DB.withConnection(implicit c => {
      val sql = lastId.map(id => RichSQL("SELECT pin.id as id, pin, note FROM pin WHERE (SELECT COUNT(1) FROM pin_tag INNER JOIN tag on tag.id = pin_tag.tag_id WHERE tag.tag IN ({tags}) AND pin_tag.pin_id = pin.id) >= {tagCount} AND pin.redeemed = {redeemed} AND pin.id < {lastId} order by id desc limit 1").onList("tags" -> tags).toSQL.on('tagCount -> tags.size, 'redeemed -> redeemed, 'lastId -> lastId))
      val selectPins = sql.getOrElse(RichSQL("SELECT pin.id as id, pin, note FROM pin WHERE (SELECT COUNT(1) FROM pin_tag INNER JOIN tag on tag.id = pin_tag.tag_id WHERE tag.tag IN ({tags}) AND pin_tag.pin_id = pin.id) >= {tagCount} AND pin.redeemed = {redeemed} order by id desc limit 1").onList("tags" -> tags).toSQL.on('tagCount -> tags.size, 'redeemed -> redeemed))
      selectPins().map(row => Pin(row[Int]("id"), row[String]("pin"), row[String]("note")))
    }).headOption
  }

  def grab = Action(parse.json) { req =>
    val tagsJson = (req.body \ "tags").asOpt[String]
    val redeemedJson = (req.body \ "redeemed").asOpt[Boolean]
    val lastIdJson = (req.body \ "lastId").asOpt[Int]

     val result = for {
       rawTags <- tagsJson
       redeemed <- redeemedJson
     }
       yield findPin(rawTags, redeemed, lastIdJson)

    result.map(res => Ok(Json.toJson(res))).getOrElse(BadRequest("Expected tags and redeemed in json"))
  }

}
