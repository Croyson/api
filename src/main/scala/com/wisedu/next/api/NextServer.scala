package com.wisedu.next.api

import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy
import com.wisedu.next.api.controllers._
import com.wisedu.next.api.filters.UserFilters
import com.wisedu.next.modules.{IdxNjxzHttpClientModule, DataBaseModule, ExHttpClientModule, MobileCampusModule}

object NextServerMain extends NextServer

class NextServer extends HttpServer {
  override val modules = Seq(DataBaseModule, ExHttpClientModule, MobileCampusModule,IdxNjxzHttpClientModule)

  override def jacksonModule = CustomJacksonModule

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .add[StaticsController]
      .add[UserFilters, FeedsController]
      .add[UserFilters, TagsController]
      .add[UserFilters, MediasController]
      .add[UserFilters, TopicsController]
      .add[UserFilters, UpdatesController]
      .add[UserFilters, EmotionsController]
      .add[UserFilters, GroupController]
      .add[UserFilters, MsgController]
      .add[UserFilters,CircleController]
      .add[UserFilters,CircleUserController]
      .add[CollegesController]
      .add[AuthController]
      .add[H5Controller]


  }

}

object CustomJacksonModule extends FinatraJacksonModule {
  override val propertyNamingStrategy = CamelCasePropertyNamingStrategy
}