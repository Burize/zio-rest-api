package product.repositories

import io.getquill.*
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import product.entities.*
import product.entities.given
import utils.DataBaseIsolation
import zio.*
import zio.test.*
import zio.json.JsonEncoder

import java.sql.Connection
import javax.sql.DataSource


object ProductRepositorySpec extends ZIOSpecDefault {
  def spec = suite("ProductRepository")(
    test("If the product already exists, then its description must be updated on upsert") {
        for {
          _ <- ProductRepository.upsertMany(
            Seq(CreateProduct(ProductType.Apparel, "Title A", Some(Map(Language.English -> "Description A"))))
          )
          _ <- ProductRepository.upsertMany(
            Seq(CreateProduct(ProductType.Apparel, "Title A", Some(Map(Language.English -> "Description B"))))
          )
          products <- ProductRepository.getAll()
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
