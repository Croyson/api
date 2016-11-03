package com.wisedu.next.api.controllers

import com.google.inject.Inject
import com.twitter.finatra.http.Controller
import com.wisedu.next.api.domains._

import com.wisedu.next.api.services.MediasService


class MediasController extends Controller {

  @Inject var mediasService: MediasService = _

  // 获取媒体列表
  get("/v2/medias") { request: GetMediasRequest =>
    mediasService.getMediasList(request).map {
      resp =>
        response.ok.json(resp)
    }
  }

  // 获取用户所在学校相关的媒体列表
  get("/v2/medias/:college_id/relative") { request: GetMediasRequest =>
    mediasService.getCollegeMedias(request, "relative").map {
      resp => response.ok.json(resp)
    }
  }

  // 获取用户所在学校不相关的媒体列表
  get("/v2/medias/:college_id/unrelative") { request: GetMediasRequest =>
    mediasService.getCollegeMedias(request, "unrelative").map {
      resp => response.ok.json(resp)
    }
  }

  // 关注媒体
  put("/v2/medias/:media_id/follow") { request: PutMediaRequest =>
    mediasService.followMedias(request, "follow").map {
      resp =>
        if (resp) response.ok.json(GetJsonResponse("success", ""))
        else response.ok.json(GetJsonResponse("fail", ""))
    }
  }

  // 取消关注媒体
  put("/v2/medias/:media_id/unfollow") { request: PutMediaRequest =>
    mediasService.followMedias(request, "unfollow").map {
      resp =>
        if(resp) response.ok.json(GetJsonResponse("success", ""))
        else response.ok.json(GetJsonResponse("fail", ""))
    }
  }

  // 获取媒体详情
  get("/v2/medias/:media_id") { request: GetMediaRequest =>
    mediasService.getMediaInfo(request).map{
      resp => response.ok.json(resp)
    }
  }

  //获取媒体下的资讯集合
  get("/v2/medias/:media_id/feeds") { request: GetMediaFeedsRequest =>
    mediasService.getMediaFeeds(request).map {
      resp => response.ok.json(resp)
    }
  }

  // 获取已关注媒体下的所有资讯集合
  get("/v2/medias/followed/feeds") { request: GetMediasRequest =>
    mediasService.getFollowedMediaFeeds(request).map{
      resp => response.ok.json(resp)
    }
  }
}