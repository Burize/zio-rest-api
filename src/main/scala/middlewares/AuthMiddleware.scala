package middlewares

import zio._
import zio.http._
import scala.util.Try
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

// Secret Authentication key
val SECRET_KEY = "secretKey"

def jwtDecode(token: String, key: String): Try[JwtClaim] =
  Jwt.decode(token, key, Seq(JwtAlgorithm.HS512))

val bearerAuthAspect: HandlerAspect[Any, String] =
  HandlerAspect.interceptIncomingHandler(
    Handler.fromFunctionZIO[Request] { request =>
      request.header(Header.Authorization) match {
        case Some(Header.Authorization.Bearer(token)) =>
          ZIO
            .fromTry(jwtDecode(token.value.asString, SECRET_KEY))
            .orElseFail(Response.badRequest("Invalid or expired token!"))
            .flatMap(claim => ZIO.fromOption(claim.subject).orElseFail(Response.badRequest("Missing subject claim!")))
            .map(u => (request, u))
        case _ => ZIO.fail(Response.unauthorized.addHeaders(Headers(Header.WWWAuthenticate.Bearer(realm = "Access"))))
      }
    })