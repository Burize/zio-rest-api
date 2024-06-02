package auth.api

import auth.repositories.UserRepository
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

import java.util.UUID

object UserRoutes:
  def apply(): Routes[UserRepository, Response] =
    Routes(
      Method.GET / "user" -> handler { (_: Request) => Response.text("WOORK")},

      Method.GET / "user" / uuid("id") -> handler { (id: UUID, _: Request) =>
        UserRepository.getById(id).map(
            {
              case Some(user) => Response(body = Body.from(user))
              case None => Response.notFound(s"User $id not found!")
            }
          )
       },
    ).handleErrorRequestCauseZIO((request, error) =>
      for {
        _ <- ZIO.logErrorCause(
            s"Failed to lookup user.",
            Cause.fail(error)
          )
      } yield Response.internalServerError("Error 200")
    )
