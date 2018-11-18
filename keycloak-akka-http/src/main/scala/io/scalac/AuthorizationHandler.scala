package io.scalac

import java.math.BigInteger
import java.security.{ KeyFactory, PublicKey }
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, Directive1 }
import akka.http.scaladsl.server.Directives.{ extractCredentials, onSuccess, provide, reject }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.keycloak.RSATokenVerifier
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.common.VerificationException
import org.keycloak.jose.jws.{ AlgorithmType, JWSHeader }
import org.keycloak.representations.AccessToken
import spray.json.{ DefaultJsonProtocol, JsObject }

import scala.concurrent.{ ExecutionContext, Future }

trait AuthorizationHandler extends SprayJsonSupport with DefaultJsonProtocol {

  implicit def executionContext: ExecutionContext
  implicit def materializer: ActorMaterializer
  implicit def system: ActorSystem

  def keycloakDeployment: KeycloakDeployment
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

  private def verifyToken2(token: String): Future[AccessToken] = {
    for {
      tokenVerifier <- Future(RSATokenVerifier.create(token).realmUrl(keycloakDeployment.getRealmInfoUrl))
      publicKey <- getPublicKey(tokenVerifier.getHeader)
    } yield publicKey match {
      case Some(publicKey) =>
        tokenVerifier.publicKey(publicKey).verify().getToken
      case None =>
        throw new VerificationException("No matching public key found.")
    }
  }

  case class Keys(keys: Seq[KeyData])
  case class KeyData(kid: String, n: String, e: String)

  implicit val keyDataFormat = jsonFormat3(KeyData)
  implicit val keysFormat = jsonFormat1(Keys)

  private def getPublicKey(jwsHeader: JWSHeader): Future[Option[PublicKey]] = {
    Http().singleRequest(HttpRequest(uri = keycloakDeployment.getJwksUrl)).flatMap(response => {
      Unmarshal(response).to[Keys].map(_.keys.find(_.kid == jwsHeader.getKeyId).map(generateKey))
    })
  }

  private def generateKey(keyData: KeyData): PublicKey = {
    val keyFactory = KeyFactory.getInstance(AlgorithmType.RSA.toString)
    val urlDecoder = Base64.getUrlDecoder
    val modulus = new BigInteger(1, urlDecoder.decode(keyData.n))
    val publicExponent = new BigInteger(1, urlDecoder.decode(keyData.e))
    keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent))
  }

}
