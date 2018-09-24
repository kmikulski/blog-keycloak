package io.scalac

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }

class UserRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with UserRoutes {

  override val userRegistryActor: ActorRef =
    system.actorOf(UserRegistryActor.props, "userRegistry")

  lazy val routes = userRoutes

  "UserRoutes" should {
    "return no users if no present (GET /users)" in {
      val request = HttpRequest(uri = "/users")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[String] should ===("""{"users":[]}""")
      }
    }
  }
}
