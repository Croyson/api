package com.wisedu.next.api.filters

import java.util.UUID
import javax.inject.Inject

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import com.twitter.finatra.http.response.ResponseBuilder
import com.wisedu.next.services.UserBaseService

case class InjectUser(userId: UUID, collegeId: String)

class UserFilters extends SimpleFilter[Request, Response] {
  @Inject var responseBuilder: ResponseBuilder = _
  @Inject var userBaseService: UserBaseService = _

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    request.authorization match {
      case Some(auth) => userBaseService.getUserByToken(auth).flatMap {
        case Some(token) => {
          UserContext.setUser(request, token.userId, token.collegeId)
          service(request)
        }
        case None =>
          responseBuilder.unauthorized("Error: The request you have made requires authentication\n").toFuture
      }
      case None => responseBuilder.unauthorized("Error: The request you have made requires authentication\n").toFuture
    }
  }
}

object UserContext {
  private val UserField = Request.Schema.newField[InjectUser]()

  implicit class UserContextSyntax(val request: Request) extends AnyVal {
    def user: InjectUser = request.ctx(UserField)
  }

  private[api] def setUser(request: Request, userId: UUID, collegeId: String): Unit = {
    val user = InjectUser(userId, collegeId)
    request.ctx.update(UserField, user)
  }
}
