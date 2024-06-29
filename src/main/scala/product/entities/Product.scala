package product.entities

import io.getquill.{ JsonValue, MappedEncoding }
import zio.json.JsonEncoder
import zio.schema.{ DeriveSchema, Schema }

import java.util.UUID

enum ProductType:
  case Apparel, ConsumerProducts, Furniture, HomeAppliance

enum Language:
  case English, German

final case class Product(
    orderId: UUID,
    productType: ProductType,
    title: String,
    description: Option[JsonValue[Map[Language, String]]] = None,
  )

given Schema[Product] = DeriveSchema.gen[Product]

object ProductType:
  given dbProductTypeEncoder: MappedEncoding[ProductType, String] =
    MappedEncoding[ProductType, String](_.toString.toLowerCase())

  given dbProductTypeDecoder: MappedEncoding[String, ProductType] =
    MappedEncoding[String, ProductType](v => ProductType.valueOf(v.capitalize))

given descriptionEncoder: JsonEncoder[Map[Language, String]] =
  JsonEncoder[Map[String, String]].contramap((description: Map[Language, String]) =>
    description.map((k, v) => (k.toString, v))
  )
