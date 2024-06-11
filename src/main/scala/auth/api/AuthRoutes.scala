package auth.api

import auth.repositories.UserRepository
import auth.services.AuthService
import utils.hashPassword
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID
import scala.util._

case class SignupDTO(username: String, password: String)
object SignupDTO:
  given Schema[SignupDTO] = DeriveSchema.gen[SignupDTO]


object AuthRoutes:
  def apply(): Routes[AuthService & UserRepository, Response]=
    Routes(
      Method.POST / "signup" -> handler { (request: Request) =>
        for {
          dto <- request.body.to[SignupDTO].orElseFail(Response.badRequest)
          response <- AuthService.signUp(dto.username, dto.password).mapBoth(
            error => Response.internalServerError(s"Failed to register the user: $error"),
            user => Response.text(user.id.toString)
          )
        } yield response
      }
    )
