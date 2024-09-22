package auth.services

import auth.entities.User
import auth.repositories.UserRepository
import core.{AlreadyExist, Unauthorized}
import utils.{checkPassword, hashPassword}
import zio.*

import scala.util.*

trait AuthService:
  def signUp(username: String, password: String, name: Option[String]): ZIO[UserRepository, Throwable, User]
  def signIn(username: String, password: String): ZIO[UserRepository, Throwable, User]

object AuthService:
  def signUp(username: String, password: String, name: Option[String]) =
    ZIO.serviceWithZIO[AuthService](_.signUp(username, password, name))

  def signIn(username: String, password: String) =
    ZIO.serviceWithZIO[AuthService](_.signIn(username, password))

class AuthServiceImpl extends AuthService:
  def signUp(username: String, password: String, name: Option[String]): ZIO[UserRepository, Throwable, User] =
    for
      existingUser   <- UserRepository.findByUsername(username)
      _              <- if existingUser.nonEmpty then ZIO.fail(AlreadyExist("Already taken")) else ZIO.succeed("")
      hashedPassword <- ZIO.fromTry(hashPassword(password))
      user           <- UserRepository.create(username, hashedPassword, name)
    yield user

  def signIn(username: String, password: String): ZIO[UserRepository, Throwable, User] =
    for
      existingUserOpt <- UserRepository.findByUsername(username)
      existingUser <- ZIO.fromOption(existingUserOpt).orElseFail(Unauthorized("Username or password is not correct"))
      isPasswordCorrect <- ZIO.fromTry(checkPassword(password, existingUser.password))
      _ <- if (isPasswordCorrect) ZIO.succeed("") else ZIO.fail(Unauthorized("Username or password is not correct"))
    yield existingUser
object AuthServiceImpl:
  val layer: ULayer[AuthService] = ZLayer.succeed(AuthServiceImpl())
