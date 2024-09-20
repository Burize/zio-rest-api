package middlewares

import core.AppConfig
import zio.*
import zio.http.*

import scala.util.Try
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import utils.jwtDecode


val bearerAuthAspect: HandlerAspect[AppConfig, String] =
  HandlerAspect.interceptIncomingHandler(Handler.fromFunctionZIO[Request] { request =>
    request.header(Header.Authorization) match
      case Some(Header.Authorization.Bearer(token)) =>
        for
          config <- ZIO.service[AppConfig]
          claim <- ZIO.fromTry(jwtDecode(token.value.asString, config.session.jwt_secret_key)).orElseFail(Response.badRequest("Invalid or expired token!"))
          payload <- ZIO.fromOption(claim.subject).orElseFail(Response.badRequest("Missing subject claim!"))
        yield (request, payload)
      case _ => ZIO.fail(Response.unauthorized.addHeaders(Headers(Header.WWWAuthenticate.Bearer(realm = "Access"))))
  })
