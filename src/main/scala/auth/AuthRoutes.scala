package auth

import core.{AlreadyExist, AppConfig, Unauthorized}
import utils.jwtEncode
import zio.*
import zio.http.*
import zio.json.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID
import scala.util.*

case class SignUpDTO(username: String, password: String, name: Option[String])

object SignUpDTO:
  given Schema[SignUpDTO] = DeriveSchema.gen[SignUpDTO]

case class SignInDTO(username: String, password: String)

object SignInDTO:
  given Schema[SignInDTO] = DeriveSchema.gen[SignInDTO]

object AuthRoutes:
  def apply() =
    Routes(
      Method.POST / "signup" -> handler { (request: Request) =>
        for
          dto      <- request.body.to[SignUpDTO].orElseFail(Response.badRequest)
          response <- AuthService
                        .signUp(dto.username, dto.password, dto.name)
                        .map(user => Response.text(user.id.toString))
        yield response
      },
      Method.POST / "signin" -> handler { (request: Request) =>
        for
          config <- ZIO.service[AppConfig]
          dto       <- request.body.to[SignInDTO].orElseFail(Response.badRequest)
          user  <- AuthService.signIn(dto.username, dto.password)
          userSession = UserSession(user.id).toJson
          sessionHeader = Header.Authorization.Bearer(jwtEncode(userSession, config.session.jwt_secret_key))
        yield Response.ok.addHeader(sessionHeader)
      },
    )
