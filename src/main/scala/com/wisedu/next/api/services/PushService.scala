package com.wisedu.next.api.services

import javax.inject.Singleton

import com.tencent.xinge.{Message, MessageIOS, XingeApp}
import com.twitter.util.Future

import scala.collection.JavaConverters._

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/6/28 下午1:44
 * Desc:
 */

@Singleton
class PushService {

  val accessId = 2200246387L
  val secretKey = "26826be5a0244a163512ce486884b9c9"
  val push: XingeApp = new XingeApp(accessId, secretKey)

  def pushDeviceMessageByTags(tags: Seq[String]) = {
    Future {
      val customMap = new java.util.HashMap[String, Object]()
      customMap.put("newFeed", "1")
       val mess: Message = new Message()
       mess.setCustom(customMap)
       mess.setType(Message.TYPE_MESSAGE)
       mess.setExpireTime(86400)
      // push.pushTags(0, tags.asJava, "OR", mess)


      val messIOS: MessageIOS = new MessageIOS()
      messIOS.setExpireTime(86400)
      messIOS.setType(MessageIOS.TYPE_REMOTE_NOTIFICATION)
      messIOS.setCustom(customMap)
      val rtn = push.pushTags(0, tags.asJava, "OR", messIOS, XingeApp.IOSENV_PROD)
      rtn
    }
  }

}
