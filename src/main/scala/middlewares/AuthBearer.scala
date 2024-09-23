package middlewares

import auth.entities.{User, UserSession}
import auth.repositories.UserRepository
import core.AppConfig
import zio.*
import zio.http.*
import zio.json.*

import scala.util.Try
import java.util.UUID
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import utils.jwtDecode


val INVALID_JWT_ERROR = "Invalid or expired token!"


val AuthBearer: HandlerAspect[AppConfig & UserRepository, User] =
  HandlerAspect.interceptIncomingHandler(Handler.fromFunctionZIO[Request] { request =>
    request.header(Header.Authorization) match
      case Some(Header.Authorization.Bearer(token)) =>
        for
          config <- ZIO.service[AppConfig]
          userRepository <- ZIO.service[UserRepository]
          claim <- ZIO.fromTry(jwtDecode(token.value.asString, config.session.jwt_secret_key)).orElseFail(Response.unauthorized(INVALID_JWT_ERROR))
          userSession <- ZIO.fromEither(claim.content.fromJson[UserSession]).orElseFail(Response.unauthorized(INVALID_JWT_ERROR))
          userOpt <- userRepository.getById(userSession.userId).orElseFail(Response.unauthorized("Invalid user id!"))
          user <- ZIO.fromOption(userOpt).orElseFail(Response.unauthorized("There is no user with specified id!"))
        yield (request, user)
      case _ => ZIO.fail(Response.unauthorized.addHeaders(Headers(Header.WWWAuthenticate.Bearer(realm = "Access"))))
  })
