package auth.services

import auth.entities.User
import auth.repositories.UserRepository
import utils.hashPassword
import zio.*

import scala.util.*

trait AuthService:
  def signUp(username: String, password: String): ZIO[UserRepository, Throwable | String, User]

object AuthService:
  def signUp(username: String, password: String) =
    ZIO.serviceWithZIO[AuthService](_.signUp(username, password))

class AuthServiceImpl extends AuthService:
  def signUp(username: String, password: String): ZIO[UserRepository, Throwable | String, User] =
    for
      existingUser   <- UserRepository.findByUsername(username)
      _              <- if existingUser.nonEmpty then ZIO.fail("Already taken") else ZIO.succeed("")
      hashedPassword <- ZIO.fromTry(hashPassword(password))
      user           <- UserRepository.create(username, hashedPassword)
    yield user

object AuthServiceImpl:
  val layer: ULayer[AuthService] = ZLayer.succeed(AuthServiceImpl())
