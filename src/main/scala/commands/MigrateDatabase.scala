package commands

import auth.{ AuthServiceImpl, UserRepositoryImpl }
import database.DatabaseMigrator
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import product.ProductRepositoryImpl
import zio.{ Scope, ZIOAppDefault, ZLayer }
import zio.http.{ Client, Middleware, Server }
import zio.nio.file.Path

object MigrateDatabase extends ZIOAppDefault:
  def run = DatabaseMigrator
    .migrate
    .provide(
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("database"),
    )
