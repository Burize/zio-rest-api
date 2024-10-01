package auth

import zio.json.{ DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder }

import java.util.UUID

final case class UserSession(
    userId: UUID
  )

object UserSession:
  given decoder: JsonDecoder[UserSession] = DeriveJsonDecoder.gen[UserSession]
  given encoder: JsonEncoder[UserSession] = DeriveJsonEncoder.gen[UserSession]
