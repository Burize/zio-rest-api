package auth.entities

import zio.schema.{ DeriveSchema, Schema }
import java.util.UUID

final case class User(
    id: UUID,
    username: String,
    password: String,
    name: Option[String],
    admin: Boolean
  )
