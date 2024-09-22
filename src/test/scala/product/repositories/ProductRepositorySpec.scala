package product.repositories

import io.getquill.*
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import product.entities.*
import testUtils.DataBaseIsolation
import zio.*
import zio.test.*
import zio.json.JsonDecoder
import zio.json.JsonEncoder

import java.sql.Connection
import javax.sql.DataSource


object ProductRepositorySpec extends ZIOSpecDefault {
  val dsDelegate = new PostgresZioJdbcContext(SnakeCase)
  import dsDelegate.*

  def spec = suite("ProductRepository")(
    test("If the product already exists, then its description must be updated on upsert") {
        for {
          _ <- ProductRepository.upsertMany(
            Seq(CreateProduct(ProductType.Apparel, "Title A", Some(Map(Language.English -> "Description A"))))
          )
          _ <- ProductRepository.upsertMany(
            Seq(CreateProduct(ProductType.Apparel, "Title A", Some(Map(Language.English -> "Description B"))))
          )
          ctx <- ZIO.service[Quill.Postgres[SnakeCase]]
          products <- ctx.run(quote(query[Product]))
        } yield assertTrue(
          products.length == 1 && products.head.description == Map(Language.English -> "Description B")
        )
    }
  ).provide(
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("database"),
    DataBaseIsolation.layer,
    ProductRepositoryImpl.layer,
  )
}
