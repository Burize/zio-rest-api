package auth

import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import zio.*

import java.util.UUID

trait UserRepository:
  def create(
      username: String,
      password: String,
      name: Option[String] = None,
      isAdmin: Boolean = false,
    ): ZIO[UserRepository, Throwable, User]
  def getById(id: UUID): ZIO[UserRepository, Throwable, Option[User]]
  def findByUsername(username: String): ZIO[UserRepository, Throwable, Option[User]]

object UserRepository:
  def create(
      username: String,
      password: String,
      name: Option[String] = None,
      isAdmin: Boolean = false,
    ): ZIO[UserRepository, Throwable, User] =
    ZIO.serviceWithZIO[UserRepository](_.create(username, password, name, isAdmin))

  def getById(id: UUID): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.getById(id))

  def findByUsername(username: String): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.findByUsername(username))

case class UserRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends UserRepository:
  import quill.*

  def create(
      username: String,
      password: String,
      name: Option[String] = None,
      isAdmin: Boolean = false,
    ): Task[User] =
    val user = User(UUID.randomUUID(), username, password, name, isAdmin)
    run(
      quote {
        querySchema[User]("auth_user").insertValue(lift(user))
      }
    ).map(_ => user)

  def getById(id: UUID): Task[Option[User]] =
    run(
      quote {
        querySchema[User]("auth_user").filter(row => row.id == lift(id))
      }
    ).map(_.headOption)

  def findByUsername(username: String): Task[Option[User]] =
    run(
      quote {
        querySchema[User]("auth_user").filter(row => row.username == lift(username))
      }
    ).map(_.headOption)
end UserRepositoryImpl

object UserRepositoryImpl:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Any, UserRepository] = ZLayer.fromFunction(UserRepositoryImpl(_))
