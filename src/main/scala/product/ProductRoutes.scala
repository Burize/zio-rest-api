package product

import scala.util.*
import zio.*
import zio.http.*
import zio.json.{ SnakeCase, jsonField, jsonMemberNames }
import zio.schema.annotation.fieldName
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.schema.{ DeriveSchema, Schema, StandardType }
import java.util.UUID

@jsonMemberNames(SnakeCase)
final case class ProductDTO(
    @fieldName("product_type") productType: ProductType,
    title: String,
    description: Option[Map[Language, String]] = None,
  )

object ProductDTO:
  def fromProduct(p: Product): ProductDTO =
    ProductDTO(productType = p.productType, title = p.title, description = Some(p.description))

  given Schema[Map[Language, String]] = Schema.map(
    Schema[String].transform(Language.valueOf, _.toString),
    Schema[String],
  )

  given Schema[ProductDTO] = DeriveSchema.gen[ProductDTO]

  given Schema[List[ProductDTO]] = Schema.list[ProductDTO]

object ProductRoutes:
  def apply() =
    Routes(
      Method.GET / "product" / "all"   -> handler { (request: Request) =>
        for response <- ProductRepository
                          .getAll(filterByTitle = request.queryParam("title"))
                          .map(products => products.map(ProductDTO.fromProduct))
                          .map(products => Response(body = Body.from(products)))
        yield response
      },
      Method.PUT / "product" / "batch" -> handler { (request: Request) =>
        for
          productDTOs <- request.body.to[List[ProductDTO]].mapError(error => Response.badRequest(error.getMessage))
          response    <-
            ProductRepository
              .upsertMany(
                productDTOs.map(product => CreateProduct(product.productType, product.title, product.description))
              )
              .map(_ => Response.text("Products have been created"))
        yield response
      },
    )
end ProductRoutes
