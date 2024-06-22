package storage.services

import zio.*
import zio.http.*
import zio.schema.{ DeriveSchema, Schema }
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.stream.{ Stream, ZStream }
import zio.nio.file.Files
import zio.nio.file.Path

import java.util.UUID

import core.NotFoundError

trait FileStorage:

  def uploadFile(file: Chunk[Byte], fileName: String): Task[String]

  def downloadFile(fileName: String): Task[Stream[Throwable, Byte]]

object FileStorage:
  def uploadFile(file: Chunk[Byte], fileName: String): RIO[FileStorage, String] =
    ZIO.serviceWithZIO[FileStorage](_.uploadFile(file, fileName))

  def downloadFile(fileName: String): RIO[FileStorage, Stream[Throwable, Byte]] =
    ZIO.serviceWithZIO[FileStorage](_.downloadFile(fileName))

class LocalFileStorage(folderPath: Path) extends FileStorage:
  def uploadFile(file: Chunk[Byte], fileName: String): Task[String] =
    val fullPath = folderPath / fileName
    for _ <- Files.writeBytes(fullPath, file)
    yield fullPath.toString

  def downloadFile(fileName: String): Task[Stream[Throwable, Byte]] =
    val filePath = folderPath / fileName
    ZIO.ifZIO(Files.exists(filePath))(
      onTrue = ZIO.succeed(ZStream.fromPath(filePath.toFile.toPath)),
      onFalse = ZIO.fail(NotFoundError(s"There is not file with name=$fileName")),
    )
