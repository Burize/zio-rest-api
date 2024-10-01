package product

import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.jdbczio.Quill.Postgres
import zio.*
import zio.json.{ JsonDecoder, JsonEncoder }

import java.util.UUID

case class CreateProduct(
    productType: ProductType,
    title: String,
    description: Option[Map[Language, String]] = None,
  )

trait ProductRepository:
  def getAll(filterByTitle: Option[String] = None): ZIO[ProductRepository, Throwable, List[Product]]

  def upsertMany(products: Seq[CreateProduct]): ZIO[ProductRepository, Throwable, Unit]

  def deleteAll(): ZIO[ProductRepository, Throwable, Unit]

object ProductRepository:
  def getAll(filterByTitle: Option[String] = None): ZIO[ProductRepository, Throwable, List[Product]] =
    ZIO.serviceWithZIO[ProductRepository](_.getAll(filterByTitle = filterByTitle))

  def upsertMany(products: Seq[CreateProduct]): ZIO[ProductRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[ProductRepository](_.upsertMany(products))

  def deleteAll(): ZIO[ProductRepository, Throwable, Unit] =
    ZIO.serviceWithZIO[ProductRepository](_.deleteAll())

case class ProductRepositoryImpl(quill: Quill.Postgres[SnakeCase]) extends ProductRepository:
  import quill.*

  def getAll(filterByTitle: Option[String] = None): Task[List[Product]] =
    // TODO: think how we can construct the query with conditions,
    //  so that if filterByTitle is None then we don't specify the filter at all.

    run(
      quote(
        query[Product]
          .filter(p => p.title.toLowerCase() like lift("%" + filterByTitle.getOrElse("").toLowerCase() + "%"))
          .sortBy(p => p.title)
      )
    )

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

  def deleteAll(): Task[Unit] =
    run(quote(query[Product].delete)).unit

end ProductRepositoryImpl

object ProductRepositoryImpl:
  val layer: ZLayer[Quill.Postgres[SnakeCase], Any, ProductRepository] = ZLayer.fromFunction(ProductRepositoryImpl(_))
