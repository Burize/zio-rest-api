package product.repositories

import java.util.UUID
import auth.entities.User
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import product.entities.*
import zio.*
import zio.json.JsonDecoder
import zio.json.JsonEncoder

case class CreateProduct(
    productType: ProductType,
    title: String,
    description: Option[Map[Language, String]] = None,
  )

trait ProductRepository:
  def upsertMany(products: Seq[CreateProduct]): ZIO[ProductRepository, Throwable, Unit]

  def getAll(): ZIO[ProductRepository, Throwable, List[Product]]

object ProductRepository:
  def upsertMany(products: Seq[CreateProduct]): ZIO[ProductRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[ProductRepository](_.upsertMany(products))

  def getAll(): ZIO[ProductRepository, Throwable, List[Product]] =
    ZIO.serviceWithZIO[ProductRepository](_.getAll())

case class ProductRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends ProductRepository:
  import quill.*

  def upsertMany(products: Seq[CreateProduct]): Task[Unit] =
    val rows = products.map(product =>
      Product(
        id = UUID.randomUUID(),
        productType = product.productType,
        title = product.title,
        description = product.description.getOrElse(Map.empty),
      )
    )
    run(
      liftQuery(rows)
        .foreach { row =>
          val q = query[Product].insertValue(row)
          sql"$q on conflict on constraint product_title_product_type_key do update set description = excluded.description"
            .as[Insert[Product]]
        }
    ).unit

  def getAll(): Task[List[Product]] =
    run(quote(query[Product])).mapError(Exception(_))

end ProductRepositoryImpl

object ProductRepositoryImpl:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Any, ProductRepository] = ZLayer.fromFunction(ProductRepositoryImpl(_))
