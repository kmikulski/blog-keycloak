package io.scalac

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, RejectionHandler, Route }
import akka.stream.ActorMaterializer

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

object QuickstartServer extends App with UserRoutes with CORSHandler {

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder().handle {
    case AuthorizationFailedRejection => complete(StatusCodes.Unauthorized -> None)
  }.result().mapRejectionResponse(addCORSHeaders)

  lazy val routes: Route = corsHandler(userRoutes)

  implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistryActor")

  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 9000)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
