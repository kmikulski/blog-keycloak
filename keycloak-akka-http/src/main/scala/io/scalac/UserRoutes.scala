package io.scalac

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.pattern.ask
import akka.util.Timeout
import io.scalac.UserRegistryActor.GetUsers

trait UserRoutes extends JsonSupport with AuthorizationHandler {

  implicit val system: ActorSystem

  lazy val log = Logging(system, classOf[UserRoutes])

  def userRegistryActor: ActorRef

  implicit lazy val timeout = Timeout(5.seconds)

  lazy val userRoutes: Route =
    path("users") {
      authorize { token =>
        val resultF = (userRegistryActor ? GetUsers).mapTo[Users]
        onSuccess(resultF)(u => complete(u))
      }
    }
}
