package middlewares

import auth.{User, UserRepository}
import core.AppConfig
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import utils.jwtDecode
import zio.*
import zio.http.*
import zio.json.*

import java.util.UUID
import scala.util.Try

val PermissionAdmin: HandlerAspect[User, Unit] =
  HandlerAspect.interceptIncomingHandler(Handler.fromFunctionZIO[Request] { request =>
    for
      currentUser <- ZIO.service[User]
      _ <- if(currentUser.admin) ZIO.succeed("") else ZIO.fail(Response.unauthorized("It is available only for admin"))
    yield (request, ())
  })
