import auth.api.UserRoutes
import auth.api.AuthRoutes
import auth.repositories.UserRepositoryImpl
import auth.services.AuthServiceImpl
import core.AppConfig
import io.getquill.SnakeCase
import zio.*
import zio.http.Server
import zio.http.Middleware
import zio.http.Client
import zio.nio.file.Path
import io.getquill.jdbczio.Quill
import middlewares.bearerAuthAspect
import product.api.ProductRoutes
import product.repositories.ProductRepositoryImpl
import storage.api.StorageRoutes
import storage.services.LocalFileStorage

val protectedRoutes   = (UserRoutes() ++ StorageRoutes() ++ ProductRoutes()) @@ bearerAuthAspect
val unProtectedRoutes = AuthRoutes()

object App extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(AppConfig.configProvider)
    
  def run = Server
    .serve(protectedRoutes ++ unProtectedRoutes @@ Middleware.debug)
    .provide(
      AppConfig.layer,
      Server.defaultWithPort(7777),
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("database"),
      UserRepositoryImpl.layer,
      ProductRepositoryImpl.layer,
      AuthServiceImpl.layer,
      Client.default,
      Scope.default,
      ZLayer.succeed(LocalFileStorage(Path("/Users/burize/Desktop/rest_api_storage"))),
    )
