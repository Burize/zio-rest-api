import auth.api.UserRoutes
import auth.api.AuthRoutes
import auth.repositories.UserRepositoryImpl
import auth.services.AuthServiceImpl
import io.getquill.SnakeCase
import zio.*
import zio.http.*
import io.getquill.jdbczio.Quill
import middlewares.bearerAuthAspect


val protectedRoutes = UserRoutes() @@ bearerAuthAspect
val unProtectedRoutes = AuthRoutes()

object App extends ZIOAppDefault:
  def run = Server.serve(protectedRoutes ++ unProtectedRoutes @@ Middleware.debug).provide(
    Server.defaultWithPort(7777),
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("database"),
    UserRepositoryImpl.layer,
    AuthServiceImpl.layer,
  )
