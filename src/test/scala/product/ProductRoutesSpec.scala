package product

import zio.*
import zio.http.*
import zio.http.netty.NettyConfig
import zio.http.netty.server.NettyDriver
import zio.json.*
import zio.nio.file.{Files, Path}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase
import java.util.UUID
import java.nio.charset.StandardCharsets

import core.AppConfig
import api.RouteErrorHandler
import utils.jwtEncode

given ProductDTOJsonDecoder: JsonDecoder[ProductDTO] = DeriveJsonDecoder.gen[ProductDTO]

given ProductTypeJsonDecoder: JsonDecoder[ProductType] = DeriveJsonDecoder.gen[ProductType]

object ProductRoutesSpec extends ZIOSpecDefault {
  val apiRoutes = ProductRoutes().handleError(RouteErrorHandler)

  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    Runtime.setConfigProvider(AppConfig.configProvider) ++ testEnvironment

  def spec = suite("Product routes") (
    suite("Create new products") (
      test("Should add a batch of products") {
        for {
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)
          products = List(
            ProductDTO(productType = ProductType.Apparel, title="A"),
            ProductDTO(productType= ProductType.ConsumerProducts, title="B"),
            ProductDTO(productType= ProductType.HomeAppliance, title="C"),
            ProductDTO(productType= ProductType.Furniture, title="D")
          )
          request = Request.put(url = URL.root.port(port) / "product" / "batch", body = Body.from(products))
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
          products <- ProductRepository.getAll()
          productTitles = products.map(_.title)
          _ <- ProductRepository.deleteAll() // Can not be isolated via rollbackDataBase, so delete them explicitly
        } yield assertTrue(
          response.status == Status.Ok,
          responseBody == "Products have been created",
          products.length == 4,
          productTitles == Seq("A", "B", "C", "D")
        )
      },
    ),
    suite("Get list of products")(
      test("Get list of products filtered by title") {
        for {
          _ <- ProductRepository.upsertMany(
            Seq(
              CreateProduct(ProductType.Apparel, "A CCC", Some(Map(Language.English -> "English A", Language.German -> "German B"))),
              CreateProduct(ProductType.ConsumerProducts, "B CCC", None),
              CreateProduct(ProductType.ConsumerProducts, "FILTER_OUT", None)
            )
          )
          client <- ZIO.service[Client]
          port <- ZIO.serviceWithZIO[Server](_.port)

          request = Request.get(url = URL.root.port(port) / "product" / "all").addQueryParam("title", "ccc")
          _ <- TestServer.addRoutes(apiRoutes)
          response <- client.batched(request)
          responseBody <- response.body.asString
          productDTOs <- ZIO.fromEither(responseBody.fromJson[List[ProductDTO]])
          expectedProducts = List(
            ProductDTO(productType = ProductType.Apparel, title = "A CCC", Some(Map(Language.English -> "English A", Language.German -> "German B"))),
            ProductDTO(productType = ProductType.ConsumerProducts, title = "B CCC", Some(Map.empty)),
          )
          _ <- ProductRepository.deleteAll() // Can not be isolated via rollbackDataBase, so delete them explicitly
        } yield assertTrue(
          response.status == Status.Ok,
          productDTOs.head == expectedProducts.head,
        )
      },
    ),
  ).provide(
    Client.default,
    NettyDriver.customized,
    ZLayer.succeed(NettyConfig.defaultWithFastShutdown),
    ZLayer.succeed(Server.Config.default.onAnyOpenPort),
    TestServer.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("database"),
    ProductRepositoryImpl.layer,
  ) @@ TestAspect.sequential
}