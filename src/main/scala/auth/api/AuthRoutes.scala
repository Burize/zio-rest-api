package auth.api

import auth.repositories.UserRepository
import auth.services.AuthService
import core.AppConfig
import utils.jwtEncode
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID
import scala.util.*

case class SignUpDTO(username: String, password: String)

given Schema[SignUpDTO] = DeriveSchema.gen[SignUpDTO]

case class SignInDTO(username: String, password: String)

given Schema[SignInDTO] = DeriveSchema.gen[SignInDTO]

object AuthRoutes:
  def apply(): Routes[AuthService & UserRepository & AppConfig, Response] =
    Routes(
      Method.POST / "signup" -> handler { (request: Request) =>
        for
          dto      <- request.body.to[SignUpDTO].orElseFail(Response.badRequest)
          response <- AuthService
                        .signUp(dto.username, dto.password)
                        .mapBoth(
                          error => Response.internalServerError(s"Failed to register the user: $error"),
                          user => Response.text(user.id.toString),
                        )
        yield response
      },
      Method.POST / "signin" -> handler { (request: Request) =>
        for
          config <- ZIO.service[AppConfig]
          dto       <- request.body.to[SignInDTO].orElseFail(Response.badRequest)
          user  <- AuthService.signIn(dto.username, dto.password)
                              .mapError(
                                error => Response.internalServerError(s"Failed to sign in the user: $error"),
                              )
          sessionHeader = Header.Authorization.Bearer(jwtEncode(user.id.toString, config.session.jwt_secret_key))
        yield Response.ok.addHeader(sessionHeader)
      },
    )
