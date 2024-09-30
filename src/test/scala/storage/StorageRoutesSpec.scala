package storage

import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.json.*
import zio.nio.file.{Files, Path}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*

import java.util.UUID
import core.AppConfig
import api.RouteErrorHandler
import utils.jwtEncode

import java.nio.charset.StandardCharsets


val fileStorageFolder: Path = Path("/Users/burize/Desktop/rest_api_storage")

object StorageRoutesSpec extends ZIOSpecDefault {
  val apiRoutes = StorageRoutes().handleError(RouteErrorHandler)

  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    Runtime.setConfigProvider(AppConfig.configProvider) ++ testEnvironment

  def spec = suite("storage routes") (
    suite("Download a file by a name") (
      test("Should download a file by a name") {
        for {
          _ <- Files.createDirectories(fileStorageFolder)
          fileContent = "Test download"
          fileName = UUID.randomUUID().toString + ".txt"
          _ <- Files.writeBytes(fileStorageFolder / fileName, Chunk.fromArray(fileContent.getBytes()))
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.get(url = URL.root.port(port) / "storage" / fileName)
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
        } yield assertTrue(
          response.status == Status.Ok,
          responseBody == fileContent,
        )
      },
      test("Should return 404 status if there is no a file with specified name") {
        for {
          _ <- Files.createDirectories(fileStorageFolder)
          fileName = UUID.randomUUID().toString + ".txt"
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.get(url = URL.root.port(port) / "storage" / fileName)
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
        } yield assertTrue(
          response.status == Status.NotFound,
          responseBody == f"There is no file with name=$fileName",
        )
      },
    ),
    suite("Upload a file")(
      test("Should upload a file") {
        for {
          _ <- Files.createDirectories(fileStorageFolder)
          fileContent = "Uploaded file"
          fileName = UUID.randomUUID().toString + ".txt"
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          request = Request.post(url = URL.root.port(port) / "storage" / fileName, body = Body.fromArray(fileContent.getBytes()))
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
          uploadedFile <- Files.readAllBytes(fileStorageFolder / fileName)
        } yield assertTrue(
          response.status == Status.Ok,
          responseBody == "The file has been uploaded",
          uploadedFile.asString == fileContent,
        )
      },
    ),
  ).provide(
    Client.default,
    NettyDriver.customized,
    ZLayer.succeed(NettyConfig.defaultWithFastShutdown),
    ZLayer.succeed(Server.Config.default.onAnyOpenPort),
    TestServer.layer,
    ZLayer.succeed(LocalFileStorage(fileStorageFolder)),
  )
}