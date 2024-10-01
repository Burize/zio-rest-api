package core

import zio.*
import zio.config.magnolia.deriveConfig
import zio.json.{ CustomCase, jsonExclude, jsonMemberNames }
import zio.config.typesafe.TypesafeConfigProvider

@jsonMemberNames(CustomCase(_.toLowerCase))
final case class AppConfig(
    session: Session,
    database: Database,
  )

object AppConfig:
  def configProvider: ConfigProvider = TypesafeConfigProvider.fromResourcePath()
  lazy val config: Config[AppConfig] = deriveConfig[AppConfig]
  def layer: TaskLayer[AppConfig]    = ZLayer(ZIO.config(AppConfig.config))

final case class Session(jwt_secret_key: String)

final case class Database(
    dataSourceClassName: String,
    connectionTimeout: Int,
    dataSource: DataSource,
  )

final case class DataSource(
    user: String,
    databaseName: String,
    portNumber: Int,
    serverName: String,
    @jsonExclude password: String,
  )
