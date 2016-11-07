package com.wisedu.next.api.services

import javax.inject.Inject
import collection.JavaConversions._
import com.google.inject.Singleton
import com.taobao.api.DefaultTaobaoClient
import com.taobao.api.domain.{OpenImUser, Userinfos}
import com.taobao.api.request.OpenimImmsgPushRequest.ImMsg
import com.taobao.api.request._
import com.taobao.api.response._
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FuturePools
import com.twitter.inject.Logging
import com.twitter.util.Future
import com.wisedu.next.models.User
import com.wisedu.next.services.BaseFunctions
import org.joda.time.DateTime


/**
 * ImUserService
 *
 * @author croyson
 *         contact 14116004@wisedu.com
 *         date 16/9/23
 *         {
  "imUrl":"http://gw.api.taobao.com/router/rest",
  "appKey":"23461455",
  "appSecret":"626b6ab53995fcea2c7e89c0e566feeb"
}
 */
@Singleton
class ImUserService extends Logging {
  @Inject var objectMapper: FinatraObjectMapper = _
  private val futurePool = FuturePools.unboundedPool("CallbackConverter")
  val appKey = "23461455"
  val appSecret = "626b6ab53995fcea2c7e89c0e566feeb"
  val imUrl = "http://gw.api.taobao.com/router/rest"
  val client = new DefaultTaobaoClient(imUrl, appKey, appSecret)

  /*
  *  新增阿里百川用戶
  * */
  def addImUsers(users: Seq[User]): Future[OpenimUsersAddResponse] = {
    futurePool {
      val req = new OpenimUsersAddRequest()
      val imUsers: java.util.List[Userinfos] = new java.util.ArrayList[Userinfos]()
      users.map {
        user => {
          val imUser = new Userinfos()
          imUser.setNick(user.name)
          imUser.setName(user.name)
          imUser.setUserid(user.userId.toString)
          if (user.imgUrl.nonEmpty) {
            val img = if (user.imgUrl.startsWith("http://")) user.imgUrl else "http://www.cpdaily.com/" + user.imgUrl
            imUser.setIconUrl(img)
          }

          imUser.setGender(user.sex match {
            case 1 => "M"
            case 2 => "F"
            case 0 => "M"
          })
          imUser.setPassword(BaseFunctions.md5Encryption(user.userId.toString, "cpdaily123"))
          imUsers.add(imUser)
        }
      }
      req.setUserinfos(imUsers)
      val resp = client.execute(req)
      Thread.sleep(500)
      info("IM账号添加,请求参数:" + objectMapper.writePrettyString(imUsers) + ",返回参数:" + objectMapper.writePrettyString(resp))
      resp
    }
  }


  def queryImUser(): Unit = {
    val req2 = new OpenAccountListRequest()
    req2.setIsvAccountIds("34534ea2-0bcc-44f3-8fcd-849ef4a717a3")
    val rsp = client.execute(new OpenAccountListRequest())
    info("IM用户列表:" + objectMapper.writePrettyString(rsp))
  }

  /*
  *  更新阿里百川用戶
  * */
  def updImUsers(users: Seq[User]): Future[OpenimUsersUpdateResponse] = {
    futurePool {
      val req = new OpenimUsersUpdateRequest()
      val imUsers: java.util.List[Userinfos] = new java.util.ArrayList[Userinfos]()
      users.map {
        user => {
          val imUser = new Userinfos()
          imUser.setNick(user.name)
          imUser.setName(user.name)
          imUser.setUserid(user.userId.toString)
          if (user.imgUrl.nonEmpty) {
            val img = if (user.imgUrl.startsWith("http://")) user.imgUrl else "http://www.cpdaily.com/" + user.imgUrl
            imUser.setIconUrl(img)
          }

          imUser.setGender(user.sex match {
            case 1 => "M"
            case 2 => "F"
            case 0 => "M"
          })
          imUser.setPassword(BaseFunctions.md5Encryption(user.userId.toString, "cpdaily123"))
          imUsers.add(imUser)
        }
      }
      req.setUserinfos(imUsers)
      val resp = client.execute(req)
      Thread.sleep(500)
      info("IM账号更新,请求参数:" + objectMapper.writePrettyString(imUsers) + ",返回参数:" + objectMapper.writePrettyString(resp))
      resp
    }
  }


  /*
  *  删除阿里百川用戶
  * */
  def delImUsers(users: Seq[User]): Future[OpenimUsersDeleteResponse] = {
    futurePool {
      val req = new OpenimUsersDeleteRequest()
      val userIds = users.map {
        user => user.userId
      }.mkString(",")

      req.setUserids(userIds)
      val resp = client.execute(req)
      Thread.sleep(5000)
      info("IM账号删除,请求参数:" + userIds + ",返回参数:" + objectMapper.writePrettyString(resp))
      resp
    }
  }

  /*
   *  获取阿里百川用戶
   * */
  def getImgUsers(userId: String): Future[OpenimUsersGetResponse] = {
    futurePool {
      val req = new OpenimUsersGetRequest()
      req.setUserids(userId)
      val resp = client.execute(req)
      info("IM账号更新,请求参数:" + userId + ",返回参数:" + objectMapper.writePrettyString(resp))
      resp
    }
  }


  /*
  *  查询聊天记录
  * */
  def getImgUsersChatLogs(userId1: String,userId2:String): Future[OpenimChatlogsGetResponse] = {
    futurePool {
      val req = new OpenimChatlogsGetRequest()
      val user1 = new OpenImUser()
      user1.setUid(userId1)
      user1.setTaobaoAccount(false)
      val user2 = new OpenImUser()
      user2.setUid(userId2)
      user2.setTaobaoAccount(false)
      req.setUser1(user1)
      req.setUser2(user2)
      req.setBegin(DateTime.now().plusDays(-1).getMillis.toString().substring(0,10).toLong)
      req.setEnd(DateTime.now().getMillis.toString().substring(0,10).toLong)
      req.setCount(100L)
      val resp = client.execute(req)
      resp
    }
  }

  // 服务端推送消息,群发
  def pushMsgToUsers(from: String, to: Seq[String], content: String) = {
    futurePool{
      val req = new OpenimImmsgPushRequest
      val imMsg = new ImMsg
      imMsg.setFromUser(from)
      imMsg.setToUsers(seqAsJavaList(to))
      imMsg.setContext(content)
      imMsg.setMsgType(0L)
      req.setImmsg(imMsg)
      val resp = client.execute(req)
      resp
    }
  }
}

