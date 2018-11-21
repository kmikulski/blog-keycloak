package io.scalac

import java.math.BigInteger
import java.security.spec.RSAPublicKeySpec
import java.security.{ KeyFactory, PublicKey }
import java.util.Base64

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.Directives.{ extractCredentials, onComplete, provide, reject }
import akka.http.scaladsl.server.{ AuthorizationFailedRejection, Directive1 }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import org.keycloak.RSATokenVerifier
import org.keycloak.adapters.{ KeycloakDeployment, KeycloakDeploymentBuilder }
import org.keycloak.jose.jws.AlgorithmType
import org.keycloak.representations.AccessToken
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

trait AuthorizationHandler extends SprayJsonSupport with DefaultJsonProtocol {

  implicit def executionContext: ExecutionContext
  implicit def materializer: ActorMaterializer
  implicit def system: ActorSystem

  def log: LoggingAdapter

  def authorize: Directive1[AccessToken] =
    extractCredentials.flatMap {
      case Some(OAuth2BearerToken(token)) =>
        onComplete(verifyToken(token)).flatMap {
          case Success(Some(t)) =>
            provide(t)
          case _ =>
            log.warning(s"token $token is not valid")
            reject(AuthorizationFailedRejection)
        }
      case _ =>
        log.warning("no token present in request")
        reject(AuthorizationFailedRejection)
    }

  def verifyToken(token: String): Future[Option[AccessToken]] = {
    val tokenVerifier = RSATokenVerifier.create(token).realmUrl(keycloakDeployment.getRealmInfoUrl)
    for {
      publicKey <- publicKeys.map(_.get(tokenVerifier.getHeader.getKeyId))
    } yield publicKey match {
      case Some(pk) =>
        val token = tokenVerifier.publicKey(pk).verify().getToken
        Some(token)
      case None =>
        log.warning(s"no public key found for id ${tokenVerifier.getHeader.getKeyId}")
        None
    }
  }

  val keycloakDeployment: KeycloakDeployment =
    KeycloakDeploymentBuilder.build(getClass.getResourceAsStream("/keycloak.json"))

  def getVerifier(token: String): Future[RSATokenVerifier] =
    Future(RSATokenVerifier.create(token).realmUrl(keycloakDeployment.getRealmInfoUrl))

  case class Keys(keys: Seq[KeyData])
  case class KeyData(kid: String, n: String, e: String)

  implicit val keyDataFormat = jsonFormat3(KeyData)
  implicit val keysFormat = jsonFormat1(Keys)

  lazy val publicKeys: Future[Map[String, PublicKey]] =
    Http().singleRequest(HttpRequest(uri = keycloakDeployment.getJwksUrl)).flatMap(response => {
      Unmarshal(response).to[Keys].map(_.keys.map(k => (k.kid, generateKey(k))).toMap)
    })

  private def generateKey(keyData: KeyData): PublicKey = {
    val keyFactory = KeyFactory.getInstance(AlgorithmType.RSA.toString)
    val urlDecoder = Base64.getUrlDecoder
    val modulus = new BigInteger(1, urlDecoder.decode(keyData.n))
    val publicExponent = new BigInteger(1, urlDecoder.decode(keyData.e))
    keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent))
  }

}
