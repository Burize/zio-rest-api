package auth.api

import auth.api.{SignInDTO, SignUpDTO, UserRoutes}
import auth.entities.{User, UserSession}
import auth.repositories.{UserRepository, UserRepositoryImpl}
import auth.services.{AuthService, AuthServiceImpl}
import core.AppConfig
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import io.getquill.{SnakeCase, *}
import middlewares.AuthBearer
import product.repositories.ProductRepositoryImpl
import testUtils.DataBaseIsolation
import utils.{checkPassword, jwtDecode, jwtEncode}
import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.json.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*

import java.util.UUID

val routes = UserRoutes() @@ AuthBearer

given userDTODecoder: JsonDecoder[UserDTO] = DeriveJsonDecoder.gen[UserDTO]


object UserRoutesSpec extends ZIOSpecDefault {
  val dsDelegate = new PostgresZioJdbcContext(SnakeCase)
  import dsDelegate.*

  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    Runtime.setConfigProvider(AppConfig.configProvider) ++ testEnvironment

  def spec = suite("auth routes") (
    suite("Get own user info") (
      test("Should be able retrieve own info") {
        val username = UUID.randomUUID().toString
        val name = UUID.randomUUID().toString
        for {
          config <- ZIO.service[AppConfig]
          authService <- ZIO.service[AuthService]
          user <- authService.signUp(username = username, password = "1234", name = Some(name))
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          userSession = UserSession(user.id).toJson
          sessionHeader = Header.Authorization.Bearer(jwtEncode(userSession, config.session.jwt_secret_key))
          request = Request.get(url = URL.root.port(port) / "user" / "my").addHeader(sessionHeader)
          _ <- TestServer.addRoutes(routes)
          response <- client.batched(request)
          responseBody <- response.body.asString
        } yield assertTrue(
          response.status == Status.Ok,
          responseBody == s"Welcome $name!",
        )
      },
      test("Should return 401 status if user is not authorized") {
        val username = UUID.randomUUID().toString
        val password = "1234"
        val payload = SignUpDTO(username = username, password = password, name = None)
        for {
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.get(url = URL.root.port(port) / "user" / "my")
          _ <- TestServer.addRoutes(routes)
          response <- client.batched(request)
        } yield assertTrue(
          response.status == Status.Unauthorized,
        )
      },
    ),
    suite("Get info of another user by admin") (
      test("Admin should be able retrieve user info by id") {
        val name = UUID.randomUUID().toString
        for {
          config <- ZIO.service[AppConfig]
          authService <- ZIO.service[AuthService]
          admin <- authService.signUp(username = UUID.randomUUID().toString, password = "1234", admin = true)
          user <- authService.signUp(username = UUID.randomUUID().toString, password = "1234", name = Some(name))
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          adminSession = UserSession(admin.id).toJson
          sessionHeader = Header.Authorization.Bearer(jwtEncode(adminSession, config.session.jwt_secret_key))
          request = Request.get(url = URL.root.port(port) / "user" / user.id.toString).addHeader(sessionHeader)
          _ <- TestServer.addRoutes(routes)
          response <- client.batched(request)
          responseBody <- response.body.asString
          userDTO <- ZIO.fromEither(responseBody.fromJson[UserDTO])
        } yield assertTrue(
          response.status == Status.Ok,
          userDTO.id == user.id,
          userDTO.name.contains(name),
        )
      },
      test("Should return unauthorized status if current user is not admin") {
        val name = UUID.randomUUID().toString
        for {
          config <- ZIO.service[AppConfig]
          authService <- ZIO.service[AuthService]
          notAdmin <- authService.signUp(username = UUID.randomUUID().toString, password = "1234", admin = false)
          user <- authService.signUp(username = UUID.randomUUID().toString, password = "1234", name = Some(name))
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          adminSession = UserSession(notAdmin.id).toJson
          sessionHeader = Header.Authorization.Bearer(jwtEncode(adminSession, config.session.jwt_secret_key))
          request = Request.get(url = URL.root.port(port) / "user" / user.id.toString).addHeader(sessionHeader)
          _ <- TestServer.addRoutes(routes)
          response <- client.batched(request)
          responseBody <- response.body.asString
        } yield assertTrue(
          response.status == Status.Unauthorized,
          responseBody == "It is available only for admin",
        )
      },
      test("Should return not found if there is no an user with specified id") {
        val name = UUID.randomUUID().toString
        for {
          config <- ZIO.service[AppConfig]
          authService <- ZIO.service[AuthService]
          admin <- authService.signUp(username = UUID.randomUUID().toString, password = "1234", admin = true)
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          adminSession = UserSession(admin.id).toJson
          sessionHeader = Header.Authorization.Bearer(jwtEncode(adminSession, config.session.jwt_secret_key))
          request = Request.get(url = URL.root.port(port) / "user" / UUID.randomUUID().toString).addHeader(sessionHeader)
          _ <- TestServer.addRoutes(routes)
          response <- client.batched(request)
          responseBody <- response.body.asString
        } yield assertTrue(
          response.status == Status.NotFound,
          responseBody == "User is not found!"
        )
      },
    ),
  ).provide(
    ZLayer.succeed(Server.Config.default.onAnyOpenPort),
    Client.default,
    NettyDriver.customized,
    ZLayer.succeed(NettyConfig.defaultWithFastShutdown),
    AppConfig.layer,
    TestServer.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("database"),
    UserRepositoryImpl.layer,
    AuthServiceImpl.layer,
    //DataBaseIsolation.layer, // TODO: does not work. Probably, Isolation and Route handler use different db connection
  )
}