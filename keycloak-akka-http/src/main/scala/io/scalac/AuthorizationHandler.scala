package io.scalac

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, Directive1 }
import akka.http.scaladsl.server.Directives.{ extractCredentials, onSuccess, provide, reject }

import scala.concurrent.{ ExecutionContext, Future }

trait AuthorizationHandler {

  implicit val executionContext: ExecutionContext
  def log: LoggingAdapter

  def authorize: Directive1[String] =
    extractCredentials.flatMap {
      case Some(OAuth2BearerToken(token)) =>
        onSuccess(verifyToken(token)).flatMap {
          case true =>
            provide(token)
          case false =>
            log.warning(s"token $token is not valid")
            reject(AuthorizationFailedRejection)
        }
      case _ =>
        log.warning("no token present in request")
        reject(AuthorizationFailedRejection)
    }

  private def verifyToken(token: String): Future[Boolean] = Future(true)
}
