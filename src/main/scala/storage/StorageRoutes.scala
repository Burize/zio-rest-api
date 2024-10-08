package storage

import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

import java.nio.charset.StandardCharsets
import java.util.UUID

object StorageRoutes:
  def apply() =
    Routes(
      Method.GET / "storage" / string("fileName")  -> handler { (fileName: String, _: Request) =>
        for fileStream <- FileStorage.downloadFile(fileName = fileName)
        yield Response(
          status = Status.Ok,
          headers = Headers(
            Header.ContentType(MediaType.application.`octet-stream`),
            Header.ContentDisposition.attachment(fileName),
          ),
          body = Body.fromStreamChunked(fileStream),
        )
      },
      Method.POST / "storage" / string("fileName") -> handler { (fileName: String, request: Request) =>
        for
          file <- request.body.asChunk
          _    <- FileStorage.uploadFile(file = file, fileName = fileName)
        yield Response.text("The file has been uploaded")
      },
    )
end StorageRoutes
