package auth.api

import auth.entities.User
import auth.repositories.UserRepository
import auth.services.AuthService
import core.AppConfig
import middlewares.PermissionAdmin
import middlewares.AuthBearer

import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

import java.util.UUID

final case class UserDTO(
   id: UUID,
   name: Option[String],
 )

object UserDTO:
  given Schema[UserDTO] = DeriveSchema.gen[UserDTO]

object UserRoutes:
  def apply() =
    val routes = Routes(
      Method.GET / "user" / "my" -> handler { (_: Request) =>
        ZIO.serviceWith[User](user => Response.text(s"Welcome ${user.name.getOrElse("user")}!"))
      }
    )
    ++
    Routes(
      Method.GET / "user" / uuid("id") -> handler { (id: UUID, _: Request) =>
        UserRepository.getById(id).map {
          case Some(user) => Response(body = Body.from(UserDTO(id = user.id, name = user.name)))
          case None       => Response.notFound(s"User is not found!")
        }.mapError(error => Response.internalServerError(s"Failed to sign in the user: $error"))
      },
    ) @@ PermissionAdmin

    routes @@ AuthBearer