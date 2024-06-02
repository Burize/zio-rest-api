import auth.api.UserRoutes
import auth.repositories.UserRepositoryImpl
import io.getquill.SnakeCase
import zio.*
import zio.http.*
import io.getquill.jdbczio.Quill


object App extends ZIOAppDefault:
  def run = Server.serve(UserRoutes()).provide(
    Server.defaultWithPort(7777),
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("database"),
    UserRepositoryImpl.layer,
  )
