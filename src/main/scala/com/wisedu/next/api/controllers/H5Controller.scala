package com.wisedu.next.api.controllers

import java.util.UUID
import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.utils.FuturePools
import com.twitter.util.Future
import com.wisedu.next.api.domains._
import com.wisedu.next.api.filters.H5UserContext._
import com.wisedu.next.api.filters.H5UserFilters
import com.wisedu.next.api.services.{ServiceFunctions, UpdatesService}
import com.wisedu.next.models._
import com.wisedu.next.services._
import org.joda.time.DateTime

/**
 * Version: 1.1
 * Author: pattywgm
 * Time: 16/6/13 下午2:48
 * Desc:
 */
class H5Controller extends Controller {
  @Inject var feedBaseService: FeedBaseService = _
  @Inject var serviceFunctions: ServiceFunctions = _
  @Inject var updateBaseService: UpdateBaseService = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var serviceBaseService: ServiceBaseService = _
  @Inject var feedAndStatsBaseService: FeedAndStatsBaseService = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var emotionService: EmotionService = _
  @Inject var sysCodeBaseService: SysCodeBaseService = _
  @Inject var mediaBaseService: ServiceBaseService = _
  @Inject var updatesService: UpdatesService = _

  val defaultUserId = UUID.fromString("b41c7fab-2087-11e6-b36d-acbc327c3dc9")

  private val futurePool = FuturePools.unboundedPool("CallbackConverter")

  // 微信页面认证
  filter[H5UserFilters].get("/v2/web/:*") { request: Request =>
    futurePool {
      if (request.params("*").contains("../"))
        response.forbidden
      else
        response.ok.cookie("token", request.h5User.tokenS).file("web/" + request.params("*"))
    }
  }

  // 内容阅读
  post("/v2/feeds/:feed_id/read") { request: PostFeedReadRequest =>
    val src_value = request.src_value
    val ip_addr = request.request.remoteAddress.getHostAddress
    val feed_id = request.feed_id
    val read_type = request.read_type

    val feedReadDetail = serviceFunctions.toFeedReadDetail(feed_id, "", read_type.toInt, src_value.toString, "", ip_addr)

    val feedStatF = feedBaseService.getFeedStatsById(feed_id).flatMap {
      case Some(item) => feedBaseService.incFeedReadNum(feed_id)
      case None => feedBaseService.insFeedStat(FeedStat(feed_id, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L))
    }
    for {
      feedStat <- feedStatF
    } yield {
      feedBaseService.insFeedReadDetails(feedReadDetail).map {
        result =>
          response.ok.header("Access-Control-Allow-Origin", "*").json(PostFeedReadResponse("success"))
      }
    }
  }

  // 获取评论列表
  get("/v2/h5/feeds/:feed_id/updates") { request: GetUpdatesRequest =>
    val update_id = request.update_id.getOrElse("")
    val method = request.method
    val limits = request.limits.getOrElse(10)
    val offset = request.offset.getOrElse(0)
    val feed_id = request.feed_id


    if (method.equals("0")) {
      feedBaseService.getFeedById(feed_id).flatMap {
        case Some(feed) =>
          updateBaseService.collUpdatesByIdWithUserIds(feed_id.toString, feed.topicInviter, limits, offset).flatMap {
            updates => Future.collect(updates.map {
              update => {

                val userF = userBaseService.getUserById(update.userId).map(_.getOrElse(userBaseService.getDefaultUser()))
                // 子评论数
                val updateNumF = updateBaseService.getSubUpdateNumById(update.updateId)
                // 评论点赞数
                val updateLikeNumF = feedBaseService.getUpdateLikeNumById(update.updateId.toString)

                // 评论点赞用户
                val updateLikeUsersF = userBaseService.getUsersByUpdateId(update.updateId.toString, 5, 0).flatMap {
                  users => Future.collect(users.map {
                    user => val departNameF = collegeBaseService.getDepartById(user.depart).map {
                      case Some(dep) => (dep.departName, dep.departShortName)
                      case None => ("", "")
                    }
                      val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
                        case Some(college) => college.shortName
                        case None => ""
                      }
                      for {
                        departName <- departNameF
                        collegeShortName <- collegeShortNameF
                      } yield GetEmotionsUserResp(user.userId.toString, user.alias, user.name, user.freshDate, user.imgUrl, user.sex.toString, user.collegeName, collegeShortName,
                        departName._2, departName._1)

                  })
                }

                // emotion 情绪数据
                val respF = emotionService.collEmotionSize(update.updateId.toString, "1", "mode1").flatMap {
                  emotionStats => Future.collect(emotionStats.map {
                    emotionStat => {
                      val emotionF = emotionService.getByModeIdAndSelectId(emotionStat.modelId, emotionStat.selectId).map {
                        case Some(emotion) => emotion
                        case None => Emotion("", "", "", "", "")
                      }
                      val usersF = emotionService.collectUserById(emotionStat.modelId, emotionStat.selectId, "1", update.updateId.toString, 5, 0).flatMap {
                        users => Future.collect(users.map {
                          user => val departNameF = collegeBaseService.getDepartById(user.depart).map {
                            case Some(dep) => (dep.departName, dep.departShortName)
                            case None => ("", "")
                          }
                            val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
                              case Some(college) => college.shortName
                              case None => ""
                            }
                            for {
                              departName <- departNameF
                              collegeShortName <- collegeShortNameF
                            } yield GetEmotionsUserResp(user.userId.toString, user.alias, user.name, user.freshDate, user.imgUrl, user.sex.toString, user.collegeName, collegeShortName,
                              departName._2, departName._1)

                        })
                      }
                      for {
                        emotion <- emotionF
                        user <- usersF
                      } yield GetEmotionsResp(emotion.modelId, emotion.selectId, emotion.selectImg, user, emotionStat.count)
                    }
                  })
                }
                for {
                  user <- userF
                  updateNum <- updateNumF
                  updateLikeNum <- updateLikeNumF
                  resp <- respF
                  updateLikeUsers <- updateLikeUsersF
                } yield (user, updateNum, updateLikeNum, resp, updateLikeUsers)
              }.flatMap {
                case (user, updateNum, updateLikeNum, resp, updateLikeUsers) =>
                  val departNameF = collegeBaseService.getDepartById(user.depart).map {
                    case Some(dep) => dep.departName
                    case None => ""
                  }

                  val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
                    case Some(college) => college.shortName
                    case None => ""
                  }
                  for {
                    departName <- departNameF
                    collegeShortName <- collegeShortNameF
                  } yield (user, updateNum, departName, 0, updateLikeNum, resp, updateLikeUsers, collegeShortName)
              }.map {
                case (user, updateNum, departName, like, updateLikeNum, resp, updateLikeUsers, collegeShortName) =>
                  serviceFunctions.toGetUpdatesResp(feed, update, user, updateNum, departName, "0", "0", resp,
                    updateLikeNum, updateLikeUsers, collegeShortName, 0, "", "", "", "")
              }
            })
          }.map(item => response.ok.header("Access-Control-Allow-Origin", "*").json(GetUpdatesResponse("success", "", item)))
        case None => response.badRequest.json(Map("errMsg" -> "The feed does not exists!")).toFuture
      }
    }
    else {
      updateBaseService.collUpdatesById(update_id, "updateId", limits, offset).flatMap {
        updates => Future.collect(updates.map {
          update => {

            val userF = userBaseService.getUserById(update.userId).map(_.getOrElse(userBaseService.getDefaultUser()))
            val updateNumF = updateBaseService.getSubUpdateNumById(update.updateId)
            val updateLikeNumF = feedBaseService.getUpdateLikeNumById(update.updateId.toString)

            // 评论点赞用户
            val updateLikeUsersF = userBaseService.getUsersByUpdateId(update.updateId.toString, 5, 0).flatMap {
              users => Future.collect(users.map {
                user => val departNameF = collegeBaseService.getDepartById(user.depart).map {
                  case Some(dep) => (dep.departName, dep.departShortName)
                  case None => ("", "")
                }
                  val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
                    case Some(college) => college.shortName
                    case None => ""
                  }
                  for {
                    departName <- departNameF
                    collegeShortName <- collegeShortNameF
                  } yield GetEmotionsUserResp(user.userId.toString, user.alias, user.name, user.freshDate, user.imgUrl, user.sex.toString, user.collegeName, collegeShortName,
                    departName._2, departName._1)

              })
            }
            val respF = emotionService.collEmotionSize(update.updateId.toString, "1", "mode1").flatMap {
              emotionStats => Future.collect(emotionStats.map {
                emotionStat => {
                  val emotionF = emotionService.getByModeIdAndSelectId(emotionStat.modelId, emotionStat.selectId).map {
                    case Some(emotion) => emotion
                    case None => Emotion("", "", "", "", "")
                  }
                  val usersF = emotionService.collectUserById(emotionStat.modelId, emotionStat.selectId, "1", update.updateId.toString, 5, 0).flatMap {
                    users => Future.collect(users.map {
                      user => val departNameF = collegeBaseService.getDepartById(user.depart).map {
                        case Some(dep) => (dep.departName, dep.departShortName)
                        case None => ("", "")
                      }
                        val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
                          case Some(college) => college.shortName
                          case None => ""
                        }
                        for {
                          departName <- departNameF
                          collegeShortName <- collegeShortNameF
                        } yield GetEmotionsUserResp(user.userId.toString, user.alias, user.name, user.freshDate, user.imgUrl, user.sex.toString, user.collegeName, collegeShortName,
                          departName._2, departName._1)

                    })
                  }
                  for {
                    emotion <- emotionF
                    user <- usersF
                  } yield GetEmotionsResp(emotion.modelId, emotion.selectId, emotion.selectImg, user, emotionStat.count)
                }
              })
            }
            for {
              user <- userF
              updateNum <- updateNumF
              updateLikeNum <- updateLikeNumF
              resp <- respF
              updateLikeUsers <- updateLikeUsersF
            } yield (user, updateNum, updateLikeNum, resp, updateLikeUsers)
          }.flatMap {
            case (user, updateNum, updateLikeNum, resp, updateLikeUsers) =>
              val departNameF = collegeBaseService.getDepartById(user.depart).map {
                case Some(dep) => dep.departName
                case None => ""
              }

              val collegeShortNameF = collegeBaseService.getCollegeById(user.collegeId).map {
                case Some(college) => college.shortName
                case None => ""
              }
              for {
                departName <- departNameF
                collegeShortName <- collegeShortNameF
              } yield (user, updateNum, departName, 0, updateLikeNum, resp, updateLikeUsers, collegeShortName)
          }.map {
            case (user, updateNum, departName, like, updateLikeNum, resp, updateLikeUsers, collegeShortName) =>
              serviceFunctions.toGetUpdatesResp(update, user, updateNum, departName, "0", "0", resp,
                updateLikeNum, updateLikeUsers, collegeShortName, 0, "", "", "", "")
          }
        })
      }.map(item => response.ok.header("Access-Control-Allow-Origin", "*").json(GetUpdatesResponse("success", "", item)))
    }
  }

  // 获取话题详细信息
  get("/v2/h5/topic/:id") { request: GetTopicRequest =>
    val feed_id = request.id
    val read_type = request.read_type.toInt
    val src_value = request.src_value.getOrElse("")
    val push_value = request.push_value.getOrElse("")
    val ip_addr = request.ip_addr.getOrElse("")

    feedBaseService.getFeedById(UUID.fromString(feed_id)).flatMap {
      case Some(feed) =>
        val feedReadDetail = serviceFunctions.toFeedReadDetail(UUID.fromString(feed_id), defaultUserId.toString, read_type, src_value, push_value, ip_addr)
        val feedStatF = feedBaseService.getFeedReadStatsById(defaultUserId.toString, UUID.fromString(feed_id)).flatMap {
          case Some(readStat) =>
            val stat = serviceFunctions.toFeedReadStat(readStat)
            feedBaseService.updFeedReadStat(stat).flatMap {
              result =>
                feedBaseService.insFeedReadDetails(feedReadDetail).flatMap {
                  result =>
                    feedBaseService.getFeedStatsById(feed.feedId).flatMap {
                      case Some(feedstat) =>
                        feedBaseService.incFeedReadNum(UUID.fromString(feed_id)).map {
                          result => FeedStat(UUID.fromString(feed_id), feedstat.readNum + 1, feedstat.likeNum, feedstat.unLikeNum,
                            feedstat.collectNum, feedstat.updateNum, feedstat.shareNum, feedstat.onlineNum,
                            feedstat.voteNum, feedstat.imgNum)
                        }
                      case None => feedBaseService.insFeedStat(FeedStat(UUID.fromString(feed_id), 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)).map {
                        result => FeedStat(UUID.fromString(feed_id), 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
                      }
                    }
                }
            }
          case None =>
            val stat = serviceFunctions.toFeedReadStat(UUID.fromString(feed_id), defaultUserId.toString)
            feedBaseService.insFeedReadStat(stat).flatMap {
              result =>
                feedBaseService.insFeedReadDetails(feedReadDetail).flatMap {
                  result => feedBaseService.getFeedStatsById(feed.feedId).flatMap {
                    case Some(feedstat) => feedBaseService.incFeedReadNum(UUID.fromString(feed_id)).map {
                      result => FeedStat(UUID.fromString(feed_id), feedstat.readNum + 1, feedstat.likeNum, feedstat.unLikeNum,
                        feedstat.collectNum, feedstat.updateNum, feedstat.shareNum, feedstat.onlineNum,
                        feedstat.voteNum, feedstat.imgNum)
                    }
                    case None => feedBaseService.insFeedStat(FeedStat(UUID.fromString(feed_id), 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)).map {
                      result => FeedStat(UUID.fromString(feed_id), 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
                    }
                  }
                }
            }
        }
        val lotteryF = feedBaseService.getFeedLotteryByFeedId(feed.feedId.toString).map {
          case Some(lottery) => lottery
          case None =>
            FeedLotteryDraw(feed.feedId.toString, "", "", "", "", "", DateTime.now, DateTime.now)
        }

        val invitersF = userBaseService.collUsersByIds(feed.topicInviter).map {
          users => users.map(item => GetInvitersResp(item.userId, item.imgUrl, s"/v2/users/${item.userId.toString}", item.sex))
        }

        if (feed.srcType == 1) {
          val userF = userBaseService.getUserById(UUID.fromString(feed.serId)).map(_.getOrElse(User(UUID.fromString(feed.serId), "不存在用户,逻辑数据有误", "不存在用户,逻辑数据有误",
            " ", " ", 1, " ", " ", " ", " ", " ", " ", " ", "", "", "",
            DateTime.now, "", " ", "", "", "", DateTime.now, "", "", 1, 1, "", "", 1)))

          for {
            feedStat <- feedStatF
            user <- userF
            lottery <- lotteryF
            inviters <- invitersF

          } yield response.ok.header("Access-Control-Allow-Origin", "*").json(serviceFunctions.toGetTopicResponse(feed, user, feedStat, lottery, inviters, "0"))
        } else {
          val userF = mediaBaseService.getServiceById(feed.serId).map(_.getOrElse(Service("数据逻辑错误", "", 0, "", 0, "", "", "", DateTime.now, DateTime.now, "", 0, 1)))
          for {
            feedStat <- feedStatF
            user <- userF
            lottery <- lotteryF
            inviters <- invitersF
          } yield response.ok.header("Access-Control-Allow-Origin", "*").json(serviceFunctions.toGetTopicResponse(feed, user, feedStat, lottery, inviters, "0"))
        }
      case None => response.badRequest.header("Access-Control-Allow-Origin", "*").json(Map("errMsg" -> "The topic does not exists!")).toFuture
    }
  }

  //获取评论详情
  get("/v2/h5/updates/:update_id") { request: GetFeedUpdateRequest =>
    updatesService.getById(request.update_id, defaultUserId).map {
      resp => response.ok.header("Access-Control-Allow-Origin", "*").json(resp)
    }
  }


  options("/v2/:*") { request: Request =>
    val domain = request.headerMap.getOrElse("Origin","")
    response.accepted.headers(Map("Access-Control-Allow-Origin"-> domain,
      "Access-Control-Allow-Credentials" ->"true","Access-Control-Allow-Methods"-> "POST,GET,OPTIONS,DELETE",
      "Access-Control-Allow-Headers"-> "Origin, X-Requested-With,accept, authorization,Content-Type,sessionToken,tenantId"))

  }

}


