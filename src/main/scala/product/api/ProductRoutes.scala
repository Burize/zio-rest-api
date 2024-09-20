package product.api

import auth.repositories.UserRepository
import auth.services.AuthService
import product.entities.{Language, ProductType}
import product.repositories.{CreateProduct, ProductRepository}
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.schema.{DeriveSchema, Schema}

import java.util.UUID
import scala.util.*

case class CreateProductDTO(
    productType: ProductType,
    title: String,
    description: Option[Map[Language, String]] = None,
  )

given Schema[CreateProductDTO] = DeriveSchema.gen[CreateProductDTO]

object ProductRoutes:
  def apply(): Routes[ProductRepository, Response] =
    Routes(
      Method.PUT / "product" / "batch" -> handler { (request: Request) =>
        for
          dto      <- request.body.to[CreateProductDTO].orElseFail(Response.badRequest)
          response <-
            ProductRepository
              .upsertMany(
                Seq(dto).map(product => CreateProduct(product.productType, product.title, product.description))
              )
              .mapBoth(
                error => Response.internalServerError(s"Failed to register the user: $error"),
                _ => Response.text("OK"),
              )
        yield response
      }
    )
