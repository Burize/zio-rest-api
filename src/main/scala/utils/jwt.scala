package utils
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import java.time.Clock
import scala.util.Try

given clock: Clock =  Clock.systemUTC

def jwtDecode(token: String, key: String): Try[JwtClaim] =
  Jwt.decode(token, key, Seq(JwtAlgorithm.HS512))

def jwtEncode(payload: String, key: String): String =
  Jwt.encode(JwtClaim(subject = Some(payload)).issuedNow.expiresIn(300), key, JwtAlgorithm.HS512)