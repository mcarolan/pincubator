package models

import play.api.libs.json._

case class Pin(id: Int, pin: String, note: String)

object Pin {
  implicit object PinFormat extends Format[Pin] {
    def reads(json: JsValue): Pin = Pin(
      (json \ "id").as[Int],
      (json \ "pin").as[String],
      (json \ "note").as[String]
    )

    def writes(p: Pin): JsValue = JsObject(Seq(
      "id" -> JsNumber(p.id),
      "pin" -> JsString(p.pin),
      "note" -> JsString(p.note)
    ))
  }
}