package product

import io.getquill.{JsonbValue, MappedEncoding}
import zio.json.{JsonDecoder, JsonEncoder}
import zio.schema.annotation.fieldName
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID

enum ProductType:
  case Apparel, ConsumerProducts, Furniture, HomeAppliance


object ProductType:
  given productTypeDbEncoder: MappedEncoding[ProductType, String] =
    MappedEncoding[ProductType, String](_.toString)

  given productTypeDbDecoder: MappedEncoding[String, ProductType] =
    MappedEncoding[String, ProductType](v => ProductType.valueOf(v))

enum Language:
  case English, German

object Language:
  given descriptionJsonEncoder: JsonEncoder[Map[Language, String]] =
    JsonEncoder[Map[String, String]].contramap((description: Map[Language, String]) =>
      description.map((k, v) => (k.toString, v))
    )

  given descriptionJsonDecoder: JsonDecoder[Map[Language, String]] =
    JsonDecoder[Map[String, String]].map((description: Map[String, String]) =>
      description.map((k, v) => (Language.valueOf(k), v))
    )

  given descriptionDbEncoder: MappedEncoding[Map[Language, String], JsonbValue[Map[Language, String]]] =
    MappedEncoding[Map[Language, String], JsonbValue[Map[Language, String]]](JsonbValue(_))

  given descriptionDbDecoder: MappedEncoding[JsonbValue[Map[Language, String]], Map[Language, String]] =
    MappedEncoding[JsonbValue[Map[Language, String]], Map[Language, String]](_.value)

final case class Product(
  id: UUID,
  productType: ProductType,
  title: String,
  description: Map[Language, String],
)

// https://github.com/zio/zio-protoquill/issues/283
// Can not use Option[JsonbValue[Map[Language, String]]], because Quill wrongly sets type to varchar instead of jsonb when value is None
// ERROR: column "description" is of type jsonb but expression is of type character varying
