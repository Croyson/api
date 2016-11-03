package com.wisedu.next.api.controllers

import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.annotations.Flag
import com.twitter.finatra.http.Controller
import com.twitter.finatra.utils.FuturePools
import com.twitter.util.Future
import com.wisedu.next.api.domains.ImgUploadRequest
import com.wisedu.next.api.services.StaticsService

import scala.reflect.io.File

class StaticsController extends Controller {

  private val futurePool = FuturePools.unboundedPool("CallbackConverter")
  @Inject
  @Flag("local.doc.root") var filePath: String = _
  @Inject var staticService: StaticsService = _
  get("/v2/statics/:*") { request: Request =>
    futurePool {
      if (request.params("*").contains("../"))
        response.forbidden
      else
      if (request.params("*").contains(".html")) {
        response.ok.file(request.params("*"))
      } else {
        val index = if (request.params("*").indexOf("?") > -1) request.params("*").indexOf("?") else request.params("*").length
        val tag = File(filePath + "/" + request.params("*").substring(0, index)).hashCode().toString
        if (tag.equals(request.headerMap.get("If-None-Match").getOrElse(""))) {
          response.notModified.headers(Map("Cache-Control" -> "max-age=259200", "ETag" -> tag))
        } else {
          response.ok.headers(Map("Cache-Control" -> "max-age=259200", "ETag" -> tag)).file(request.params("*"))
        }

      }
    }
  }

  get("/") { request: Request =>
    Future {
      response.ok.file("home/html/home.html")
    }
  }

  post("/v2/img/upload") { request: ImgUploadRequest =>
    staticService.imgUpload(request)
  }
}
