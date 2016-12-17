package com.wisedu.next.admin.services

import javax.inject.{Inject, Singleton}

import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.wisedu.next.annotations.JPushHttpClient

import scala.collection.mutable.{Map => MutMap}
case class Extras(newFeed:String)

case class AndroidNotification(alert: String, title: String, extras: Extras)

case class IosNotification(alert: String, extras: Extras,sound:String,badge:Int)

case class PushOptions(time_to_live: Int, apns_production: Boolean)

case class Notification(android: Option[AndroidNotification], ios: Option[IosNotification])

case class Message(msg_content:String,title:String,content_type:String,extras:Extras)

case class Audience(tag: Option[Seq[String]], tag_and: Option[Seq[String]], alias: Option[Seq[String]])

case class PushResponse(sendno: String, msg_id: String)
@Singleton
class JPushService {
  @Inject
  @JPushHttpClient var jPushHttpClient: HttpClient = _
  @Inject var objectMapper: FinatraObjectMapper = _

  def pushDeviceMessageByTags(tags: Seq[String]) = {
    val headers = Map("Authorization" -> "Basic OGJjNGFlNWM1MmY1YmJhMDM1NzY3MTZiOjU2NjYxZWRhODQyZTMyYWI3MzViNmNmZQ==")
    val extras = Extras("1")
    //val iosNotification = IosNotification("", extras,"sound.caf",1)
    //val androidNotification = AndroidNotification("","", extras)
   // val notification = Notification(Some(androidNotification), Some(iosNotification))
    val message = Message("有新的新鲜事","msg","text",extras)
    val platform = "all"
    val options = PushOptions(86400,true)
    val msg = MutMap("platform" -> platform, "message" -> message, "options" -> options,"audience" -> Audience(Some(tags), None, None))
    val body = objectMapper.writePrettyString(msg)
    jPushHttpClient.execute(RequestBuilder.post("/v3/push").headers(headers).body(body)).map {
      ret =>PushResponse(ret.getContentString(),tags.mkString(","))
    }

  }


}
