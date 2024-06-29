package product.repositories

import java.util.UUID
import auth.entities.User
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import product.entities.*
import product.entities.descriptionEncoder
import zio.*
import zio.json.JsonEncoder

case class CreateProduct(
    productType: ProductType,
    title: String,
    description: Option[Map[Language, String]] = None,
  )

trait ProductRepository:
  def upsertMany(products: Seq[CreateProduct]): ZIO[ProductRepository, Throwable, Unit]

object ProductRepository:
  def upsertMany(products: Seq[CreateProduct]): ZIO[ProductRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[ProductRepository](_.upsertMany(products))

case class ProductRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends ProductRepository:
  import quill.*

  def upsertMany(products: Seq[CreateProduct]): Task[Unit] =
    val rows = products.map(order =>
      Product(
        orderId = UUID.randomUUID(),
        productType = order.productType,
        title = order.title,
        description = order.description.map(JsonValue(_)),
      )
    )
    run(
      liftQuery(rows)
        .foreach { row =>
          val q = query[Product].insertValue(row)
          sql"$q on conflict on constraint product_title_product_type_key do update set description = excluded.description"
            .as[Insert[Product]]
        }
    )
      .map(_ => ())

end ProductRepositoryImpl

object ProductRepositoryImpl:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Any, ProductRepository] = ZLayer.fromFunction(ProductRepositoryImpl(_))
