package testUtils

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*

import java.sql.Connection
import javax.sql.DataSource

// TODO: does not work with api tests. Probably, Isolation and Route handler use different db connection.
def rollbackDataBase(): ZIO[DataSource & Quill.Postgres[SnakeCase] & Scope, Throwable, Unit] =
  for {
    ctx <- ZIO.service[Quill.Postgres[SnakeCase]]
    env <- ZIO.service[DataSource]
    connection <- ZIO.acquireRelease(ZIO.attemptBlocking[Connection](env.getConnection))(resource => ZIO.attemptBlocking(resource.close()).orDie)
    _ <- ZIO.acquireRelease(ctx.underlying.currentConnection.set(Some(connection)))(_ => ctx.underlying.currentConnection.set(None))
    _ <- ZIO.attemptBlocking(connection.setAutoCommit(false))
    _ <- ZIO.addFinalizer(ZIO.blocking(ZIO.succeed(connection.rollback())))
  } yield ()


object DataBaseIsolation:
  val layer: ZLayer[DataSource & Quill.Postgres[SnakeCase], Throwable, Unit] = ZLayer.scoped(rollbackDataBase())
