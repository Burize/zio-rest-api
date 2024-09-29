import io.getquill.SnakeCase
import zio.*
import zio.http.{Client, Middleware, Response, Server}
import zio.nio.file.Path
import io.getquill.jdbczio.Quill

import api.apiRoutes
import core.AppConfig
import auth.{AuthServiceImpl, UserRepositoryImpl}
import product.ProductRepositoryImpl
import storage.LocalFileStorage


object App extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(AppConfig.configProvider)
    
  def run = Server
    .serve(apiRoutes)
    .provide(
      AppConfig.layer,
      Server.defaultWithPort(7777),
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("database"),
      UserRepositoryImpl.layer,
      ProductRepositoryImpl.layer,
      AuthServiceImpl.layer,
      ZLayer.succeed(LocalFileStorage(Path("/Users/burize/Desktop/rest_api_storage"))),
    )
