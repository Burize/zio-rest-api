package auth.repositories

import auth.entities.User
//import auth.tables.userTable
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import zio.*
import zio.json.{SnakeCase as _, *}
import zio.*

import java.util.UUID

trait UserRepository:
  def getById(id: UUID): Task[Option[User]]


object UserRepository:
  def getById(id: UUID): ZIO[UserRepository, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepository](_.getById(id))


case class UserRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends UserRepository:
  import quill.*

  def getById(id: UUID): Task[Option[User]] =
    run(
        quote {
          querySchema[User]("auth_user")
          .filter(row => row.id == lift(id))
          .map(row => User(row.id, row.username, row.password))
      }
    ).map(_.headOption)

object UserRepositoryImpl:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Any, UserRepository] = ZLayer.fromFunction(UserRepositoryImpl(_))
