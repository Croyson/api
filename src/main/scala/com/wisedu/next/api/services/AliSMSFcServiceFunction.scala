package com.wisedu.next.api.services

import java.net.URLEncoder
import java.security.MessageDigest
import javax.inject.{Inject, Singleton}

import com.github.nscala_time.time.StaticDateTimeFormat
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.models.AuthCode
import com.wisedu.next.services.{BaseFunctions, ExClient, UserBaseService}
import org.joda.time.DateTime
import scala.collection.immutable.HashMap
import scala.collection.mutable.{Map => MutMap}

@Singleton
class AliSMSFcServiceFunction {
  @Inject var userBaseService: UserBaseService = _
  @Inject var exClient: ExClient = _
  def sendRegSms(phoneNo: String,method:Int): Future[PostUserRegSendMsgResponse] = {

    val headers = HashMap[String,String]("Content-Type" -> "Content-Type:application/x-www-form-urlencoded;charset=utf-8")
    val params = MutMap("app_key" -> "23348971",
      "method" -> "alibaba.aliqin.fc.sms.num.send",
      "format" -> "json",
      "sign_method" -> "md5",
      "v" -> "2.0")
    params("timestamp") = DateTime.now().toString(StaticDateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss"))
    params("sms_type") = "normal"
    params("sms_free_sign_name") = "今日校园"
    params("rec_num") = phoneNo
    params("sms_template_code") = if(method == 0)  "SMS_8155506" else "SMS_8155506"

    val start = 10000;
    val end = 99999;
    val rnd = new scala.util.Random
    val code = start + rnd.nextInt((end - start) + 1)
    params("sms_param") = "{\"code\":\"" + code + "\"}"
    val sign = signTopRequest(params.toMap, "01296fbc9fef20323bfb320760d6d585")
    params("sign") = sign
    val urlParam = paramConvertUrl(params.toMap)
    exClient.post[AlibabaAliqinFcSmsNumSendResponse]("/router/rest"+urlParam,headers,"").map {
      case Some(item) => item.alibaba_aliqin_fc_sms_num_send_response match {
        case Some(t) => if (t.result.success) {
          userBaseService.insAuthCode(
              AuthCode(phoneNo, code.toString, DateTime.now, DateTime.now().plusMinutes(30)))
          PostUserRegSendMsgResponse(PostUserRegPhone(phoneNo), "success")
        } else {
          PostUserRegSendMsgResponse(PostUserRegPhone(phoneNo), "fail")
        }
        case None => PostUserRegSendMsgResponse(PostUserRegPhone(phoneNo), "fail")
      }

      case None => PostUserRegSendMsgResponse(PostUserRegPhone(phoneNo), "fail")
    }

  }

  def signTopRequest(params: Map[String, String], secret: String): String = {
    // 第一步：检查参数是否已经排序
    val keys = params.keySet.toArray.sorted
    val query = new StringBuilder()
    // 第二步：把所有参数名和参数值串在一起
    query.append(secret)
    for (key <- keys) {
      val value = params.getOrElse(key, "")
      if (!key.isEmpty && !value.isEmpty) {
        query.append(key).append(value)
      }
    }
    query.append(secret)
    val md = MessageDigest.getInstance("MD5")

    BaseFunctions.byte2hex(md.digest(query.toString().getBytes("utf-8")))

  }

  def paramConvertUrl(params: Map[String, String]): String = {
    val keys = params.keys.toArray.sorted
    val paramsUrl = new StringBuilder()
    paramsUrl.append("?")
    for (key <- keys) {
      val value = params.getOrElse(key, "")
      paramsUrl.append(key).append("=").append(URLEncoder.encode(value, "utf-8")).append("&")
    }

    paramsUrl.substring(0, paramsUrl.length - 1).toString
  }

  def sendRegVerifySms(phoneNo: String, authCode: String): Future[Boolean] = {
    userBaseService.getAuthCodeById(phoneNo).map {
      case Some(item) => if (item.authCode.equals(authCode) && item.exTime.getMillis > DateTime.now().getMillis) {
         true
      } else {
        false
      }
      case None => false
    }
  }

}