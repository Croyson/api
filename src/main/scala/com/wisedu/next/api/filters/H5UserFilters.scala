package com.wisedu.next.api.filters

import java.util.UUID
import javax.inject.Inject

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.annotations.Flag
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.Future
import com.wisedu.next.api.services.ImUserService
import com.wisedu.next.models.{User, UserSnsInfo}
import com.wisedu.next.services.{CollegeBaseService, StaticBaseService, UserBaseService}
import com.wisorg.scc.core.util.CloudUser
import org.joda.time.DateTime
import com.wisorg.scc.core.util.Codecs

case class H5InjectUser(tokenS: String, userId: UUID, isVisitor: Boolean, hasCookie: Boolean)

case class H5InjectUserTemp(tokenS: String, userId: UUID, openId: String, hasCookie: Boolean)

case class MSign(m_sign: Option[String], env: Option[String], user_level: Option[Int])

class H5UserFilters extends SimpleFilter[Request, Response] {
  @Inject var responseBuilder: ResponseBuilder = _
  @Inject var userBaseService: UserBaseService = _
  @Inject var objectMapper: FinatraObjectMapper = _
  @Inject var collegeBaseService: CollegeBaseService = _
  @Inject var staticBaseService: StaticBaseService = _
  @Inject var imUserService: ImUserService = _

  @Inject @Flag("app.secret.njxz") var njxzSecretKey: String = _
  @Inject @Flag("app.secret.jzdx") var jzdxSecretKey: String = _

  def check(cu: CloudUser, appSecret: String): Boolean = {
    val temp = Codecs.sha1Hex(cu.toString + appSecret)
    cu.getSign().getCheck.equals(temp)
  }

  //0  新生   1 老生    2 老师
  def getUserRole(openId: String, userLevel: Int): Int = {
    val lenT = openId.length
    val firstLet = openId.charAt(0)
    //val isTech = firstLet.equals('p') || firstLet.equals('P') || firstLet.equals('z') || firstLet.equals('Z') || lenT.equals(7)
    val isFresh = lenT.equals(14)
    if (userLevel.equals(1)) {
      2
    } else if (isFresh) {
      0
    } else {
      1
    }
  }

  def toUser(cloudUser: CloudUser, userLevel: Int): Future[Option[User]] = {

    val userRole = getUserRole(cloudUser.getIdsNo, userLevel)
    //cloudUser.getAvatar.getUrl

    val password = ""

    val name = cloudUser.getRealName

    //cloudUser.getGender
    val sex = cloudUser.getGender match {
      case 1 => 2
      case 0 => 1
      case _ => 0
    }

    val phoneNo = ""

    val home = ""

    val loveStatus = ""

    val departIdF = Future { cloudUser.getDept.getName}.flatMap {
      departName => collegeBaseService.getDepartsByName(departName).map {
        case Some(dept) => dept.departId
        case None => ""
      }
    }

    val majors = ""

    val cla = ""

    val dorm = ""

    val mschool = ""

    val sign = ""

    val birth_date = new DateTime(cloudUser.getBirthday)

    val reg = """(^\d{4}$)""".r
    val fresh_date = cloudUser.getEnterYear.toString match {
      case reg(m) => m
      case _ =>
        userRole match {
          case 2 => ""
          case 1 => "20" + cloudUser.getIdsNo.substring(0,2)
          case _ => DateTime.now().toString("yyyy")
        }
    }

    val interests = ""

    val actives = ""

    val deviceId = ""

    val anonymous = 0

    val backgroundImg = ""

    val user_id = UUID.randomUUID()
    val collegeId = "njxz"
    val collegeNameF = collegeBaseService.getCollegeById(collegeId).map {
      case Some(college) => college.name
      case None => ""
    }

    val nickName = userRole match {
      case 0 => cloudUser.getRealName
      case _ => cloudUser.getNickName
    }
    val aliasF = userBaseService.getUniqueAlice(nickName)

    val imgUrlF = Future { cloudUser.getAvatar.getUrl }.rescue {
      case e: Exception => Future { "" }
    }.flatMap {
      url => if (url.nonEmpty) {
        staticBaseService.fetchUrl(url, user_id.toString, UUID.randomUUID().toString)
      } else {
        Future("")
      }
    }

    val degreeT = Future { Some(DateTime.now().toString("yyyy").toInt - fresh_date.toInt + 1) }.rescue {
      case e: Exception => Future { None }
    }
    val degreeF = userRole match {
      case 2 => Future { "" }
      case _ => degreeT.map {
        case Some(str) => str.toString
        case None => ""
      }
    }

    {
      for {
      collegeName <- collegeNameF
      alias <- aliasF
      imgUrl <- imgUrlF
      depart <- departIdF
      degree <- degreeF
    } yield Some(User(user_id, alias, name, password, imgUrl, sex, phoneNo, mschool, collegeId, collegeName, depart, cla,
      majors, dorm, degree, fresh_date, birth_date, home, loveStatus, sign, interests, actives,
      DateTime.now, UUID.randomUUID().toString, deviceId, anonymous, 1, backgroundImg, "", userRole))
    }.rescue {
      case e: Exception =>
        e.printStackTrace()
        Future { None }
    }
  }

  def toUserSnsInfo(userId: UUID, cloudUser: CloudUser): UserSnsInfo = {

    val openId = cloudUser.getIdsNo

    val snsCode = ""

    val snsType = 4

    val snsAlias = cloudUser.getNickName

    UserSnsInfo(userId, openId, snsCode, snsAlias, snsType, DateTime.now())
  }

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    val userF = request.cookies.get("token") match {
      case Some(auth) => userBaseService.getUserByToken(auth.value)
      case None => Future {
        None
      }
    }
    val mSign = objectMapper.parse[MSign](request.content)
    val envT = mSign.env match {
      case Some(envTemp) => envTemp
      case None => "jzdx"
    }
    val secretKey = envT match {
      case "njxz" => njxzSecretKey
      case _ => jzdxSecretKey
    }

    val useFT = userF.flatMap {
      case Some(user) => {
        val bool = mSign.m_sign match {
          case Some(sign) => {
            val cu: CloudUser = objectMapper.parse[CloudUser](sign)
            if (!check(cu, secretKey)) {
              None
            } else {
              Some(cu)
            }
          }
          case None => None
        }

        bool match {
          case Some(cu) => {
            //验证headers与token的匹配
        userBaseService.getSnsByOpenId(cu.getIdsNo, 4).map {
              case Some(snsInfo) =>
                if (snsInfo.userId != user.userId)
                  None
                else
                  Some(user, cu)
              case None => Some(user, cu)
            }
          }
          case None => Future { None }
        }
      }
      case None => Future { None }
    }

    val tokenF = useFT.flatMap {
      case Some((user, cu)) => Future {
        Some(H5InjectUserTemp(user.tokenS, user.userId, cu.getIdsNo, true))
      }
      case None => {
        val bool = mSign.m_sign match {
          case Some(sign) => {
            val cu: CloudUser = objectMapper.parse[CloudUser](sign)
            if (!check(cu, secretKey)) {
              None
            } else {
              Some(cu)
            }
          }
          case None => None
        }

        bool match {
          case Some(cu) => {
            userBaseService.getSnsByOpenId(cu.getIdsNo, 4).flatMap {
              case Some(snsInfo) => userBaseService.getUserById(snsInfo.userId).map {
                case Some(user) => Some(H5InjectUserTemp(user.tokenS, user.userId, snsInfo.openId, false))
                case None => None
              }
              case None => mSign.user_level match {
                case Some(userLevel) => toUser(cu, userLevel).flatMap {
                  case Some(user) =>
                    val userSnsInfo = toUserSnsInfo(user.userId, cu)
                    userBaseService.insUser(user).flatMap {
                      result => {
                        imUserService.addImUsers(Seq(user))
                        userBaseService.sysUserTags(user.userId)
                        userBaseService.insUserSnsInfo(userSnsInfo)
                      }
                    }.map {
                      result => Some(H5InjectUserTemp(user.tokenS, user.userId, cu.getIdsNo, false))
                    }
                  case None => Future { None }
                }
                case None => Future { None }
              }
            }
          }
          case None => Future {
            None
          }
        }
      }
    }

    tokenF.flatMap {
      case Some(userTemp) => {
        val isVisitor =
          if (userTemp.openId.equals("guest"))
            true
          else
            false
        H5UserContext.setH5User(request, userTemp.tokenS, userTemp.userId, isVisitor, userTemp.hasCookie)
        service(request)
      }
      case None => {
        val defaultUser = User(UUID.fromString("7d6582e1-4986-11e6-8181-00909e9a7ba0"), "i晓庄默认用户", "i晓庄默认用户",
          "5fcbd735-4986-11e6-bcd8-00909e9a7ba0", " ", 0, " ", " ", "njxz", " ", " ", " ", " ", "", "", "",
          DateTime.now, "", " ", "", "", "", DateTime.now, "7d6582e1-4986-11e6-8181-00909e9a7ba0", "", 1, 1, "","",1)
        userBaseService.getUserById(UUID.fromString("7d6582e1-4986-11e6-8181-00909e9a7ba0")).flatMap {
          case Some(user) => Future {H5UserContext.setH5User(request, "7d6582e1-4986-11e6-8181-00909e9a7ba0",
            UUID.fromString("bcc3cbc0-4985-11e6-acd6-00909e9a7ba0"), true, false)}
          case None => userBaseService.insUser(defaultUser).map {
            result => H5UserContext.setH5User(request, "7d6582e1-4986-11e6-8181-00909e9a7ba0",
              UUID.fromString("bcc3cbc0-4985-11e6-acd6-00909e9a7ba0"), true, false)
          }
        }.flatMap {
          result => service(request)
        }
      }
    }
  }
}

object H5UserContext {
  private val H5UserField = Request.Schema.newField[H5InjectUser]()

  implicit class H5UserContextSyntax(val request: Request) extends AnyVal {
    def h5User: H5InjectUser = request.ctx(H5UserField)
  }

  private[api] def setH5User(request: Request, tokenS: String, userId: UUID, isVisitor: Boolean, hasCookie: Boolean): Unit = {
    val h5User = H5InjectUser(tokenS, userId, isVisitor, hasCookie)
    request.ctx.update(H5UserField, h5User)
  }
}