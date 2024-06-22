package storage.api

import core.NotFoundError
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import storage.services.FileStorage

import java.nio.charset.StandardCharsets
import java.util.UUID


object StorageRoutes:
  def apply(): Routes[FileStorage & Client & Scope, Response] =
    Routes(
      Method.GET / "storage" /  string("fileName") -> handler { (fileName: String, _: Request) =>
        for {
          fileStream <- FileStorage.downloadFile(fileName = fileName)
        } yield Response(
          status = Status.Ok,
          headers = Headers(
            Header.ContentType(MediaType.application.`octet-stream`),
            Header.ContentDisposition.attachment(fileName)
          ),
          body = Body.fromStream(fileStream))
      },
      Method.POST / "storage" / string("fileName")  -> handler { (fileName: String, request: Request) =>
        for {
          file <- request.body.asChunk
          result <- FileStorage.uploadFile(file=file, fileName = fileName)
        } yield Response.text(result)
       },
    ).handleError(error =>
      error match
        case NotFoundError(message) => Response.notFound(message)
        case _ => Response.internalServerError("500 error")
    )
