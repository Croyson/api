package com.wisedu.next.api.services

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.inject.Inject

import com.google.inject.Singleton
import com.twitter.finatra.http.request.RequestUtils
import com.twitter.util.Future
import com.wisedu.next.api.domains.{ImageUpdateResponse, ImgUploadRequest}
import com.wisedu.next.services.{BaseFunctions, StaticBaseService}
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/7/6 下午4:16
 * Desc:
 */

@Singleton
class StaticsService {
  @Inject var staticBaseService: StaticBaseService = _


  def imgUpload(request: ImgUploadRequest): Future[ImageUpdateResponse] = {
    val id = request.id.getOrElse("")
    val file = RequestUtils.multiParams(request.request).values.head
    val path = if (id.isEmpty) DateTime.now().toString("yyyyMMdd")
    else id
    var filename = ""
    try {
      val in = new ByteArrayInputStream(file.data)
      val image = ImageIO.read(in)
      val width = image.getWidth()
      val height = image.getHeight()
      filename = BaseFunctions.getRandomStringWithWH(5, file.filename.getOrElse("wuming"), width, height)
    } catch {
      case _: Exception =>
    }
    if (filename.isEmpty) {
      filename = BaseFunctions.getRandomString(5, file.filename.getOrElse("wuming"))
    }
    staticBaseService.putBucket(path).flatMap {
      rst => if (rst) {
        staticBaseService.putObject(path, filename, file.data).flatMap {
          filePath => {
            val mImgF = staticBaseService.compressionUpdateImgs(filePath, 300, 300, "m")
            val lImgF = staticBaseService.compressionUpdateImgs(filePath, 400, 400, "l")
            val sImgF = staticBaseService.compressionUpdateImgs(filePath, 80, 80, "s")
            val ys = for {
              mImg <- mImgF
              lImg <- lImgF
              sImg <- sImgF
            } yield (mImg, lImg, sImg)
            ys.map {
              case (mImg, lImg, sImg) =>
                Thread.sleep(2000) //预留2000毫秒服务器图片同步时间
                ImageUpdateResponse(filePath, lImg.mkString(","), mImg.mkString(","), sImg.mkString(","))
            }
          }
        }
      } else {
        Future {
          ImageUpdateResponse("", "", "", "")
        }
      }
    }
  }


}


