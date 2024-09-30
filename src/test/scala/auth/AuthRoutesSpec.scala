package auth

import zio.*
import zio.json.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.test.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.nio.file.Path
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import java.util.UUID

import core.AppConfig
import utils.{checkPassword, jwtDecode}
import api.RouteErrorHandler



object AuthRoutesSpec extends ZIOSpecDefault {
  val dsDelegate = new PostgresZioJdbcContext(SnakeCase)
  import dsDelegate.*

  val apiRoutes = AuthRoutes().handleError(RouteErrorHandler)
  
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    Runtime.setConfigProvider(AppConfig.configProvider) ++ testEnvironment

  def spec = suite("auth routes") (
    suite("Sign up route") (
      test("Should be signed up with specified username, password and name") {
        val username = UUID.randomUUID().toString
        val name = UUID.randomUUID().toString
        val password = "1234"
        val payload = SignUpDTO(username = username, password = password, name = Some(name))
        for {
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.post(url = URL.root.port(port) / "signup", body = Body.from(payload))
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
          ctx <- ZIO.service[Quill.Postgres[SnakeCase]]
          createdUserOpt <- ctx.run(quote(querySchema[User]("auth_user").filter(row => row.username == lift(username)))).map(_.headOption)
          createdUser <- ZIO.fromOption(createdUserOpt).orElseFail("User was not created")
          isPasswordRight <- ZIO.fromTry(checkPassword(password, createdUser.password))
        } yield assertTrue(
          response.status == Status.Ok,
          responseBody == createdUser.id.toString,
          createdUser.username == username,
          isPasswordRight,
        )
      },
      test("Should return 409 status if specified username is already taken") {
        val username = UUID.randomUUID().toString
        val password = "1234"
        val payload = SignUpDTO(username = username, password = password, name = None)
        for {
          userRepository <- ZIO.service[UserRepository]
          _ <- userRepository.create(username = username, password = password)
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.post(url = URL.root.port(port) / "signup", body = Body.from(payload))
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
        } yield assertTrue(
          response.status == Status.Conflict,
          responseBody == "Already taken",
        )
      },
    ),
    suite("Sign in route")(
      test("Should be signed in") {
        val username = UUID.randomUUID().toString
        val password = "1234"
        val payload = SignInDTO(username = username, password = password)
        for {
          authService <- ZIO.service[AuthService]
          user <- authService.signUp(username = username, password = password)
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.post(url = URL.root.port(port) / "signin", body = Body.from(payload))
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          authorizationHeader <- ZIO.fromOption(response.header(Header.Authorization))
          token <- authorizationHeader match
            case Header.Authorization.Bearer(token) => ZIO.succeed(token)
            case _ => ZIO.fail("Invalid Bearer token")
          config <- ZIO.service[AppConfig]
          claim <- ZIO.fromTry(jwtDecode(token.value.asString, config.session.jwt_secret_key))
          userSession <- ZIO.fromEither(claim.content.fromJson[UserSession])
        } yield assertTrue(
          response.status == Status.Ok,
          userSession.userId == user.id
        )
      },
      test("Should return unauthorized status if username or password is not correct") {
        val username = UUID.randomUUID().toString
        val payload = SignInDTO(username = username, password = "200")
        for {
          authService <- ZIO.service[AuthService]
          _ <- authService.signUp(username = username, password = "100")
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.post(url = URL.root.port(port) / "signin", body = Body.from(payload))
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
        } yield assertTrue(
          response.status == Status.Unauthorized,
          !response.hasHeader(Header.Authorization),
          responseBody == "Username or password is not correct",
        )
      },
    ),
  ).provide(
    Client.default,
    NettyDriver.customized,
    ZLayer.succeed(NettyConfig.defaultWithFastShutdown),
    ZLayer.succeed(Server.Config.default.onAnyOpenPort),
    AppConfig.layer,
    TestServer.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("database"),
    UserRepositoryImpl.layer,
    AuthServiceImpl.layer,
  )
}