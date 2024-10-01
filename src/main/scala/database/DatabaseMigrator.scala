package database

import io.getquill.*
import io.getquill.jdbczio.Quill
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import zio.*

import javax.sql.DataSource

object DatabaseMigrator:
  def migrate: RIO[DataSource, MigrateResult] =
    for
      dataSource <- ZIO.service[DataSource]
      res        <- ZIO.attempt(Flyway.configure().dataSource(dataSource).load().migrate()).orDie
    yield res
