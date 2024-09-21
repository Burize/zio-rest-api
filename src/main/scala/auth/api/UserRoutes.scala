package auth.api

import auth.entities.User
import auth.repositories.UserRepository
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
    Routes(
      Method.GET / "user" / "my"       -> handler { (_: Request) =>
        ZIO.serviceWith[User](user => Response.text(s"Welcome ${user.name.getOrElse("user")}!"))
      },
      Method.GET / "user" / uuid("id") -> handler { (id: UUID, _: Request) =>
        UserRepository.getById(id).map {
          case Some(user) => Response(body = Body.from(UserDTO(id = user.id, name = user.name)))
          case None       => Response.notFound(s"User $id not found!")
        }
      },
    ).handleErrorRequestCauseZIO((request, error) =>
      for _ <- ZIO.logErrorCause(s"Failed to lookup user.", Cause.fail(error))
      yield Response.internalServerError("500 error")
    )
