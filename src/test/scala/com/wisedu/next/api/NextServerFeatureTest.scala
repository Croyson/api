package com.wisedu.next.api

import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTest

class NextServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedHttpServer(new NextServer)
}