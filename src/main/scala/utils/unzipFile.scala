package utils

import zio.nio.file.{Files, Path}
import zio.stream.ZStream
import zio.{Task, ZIO}

import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.*
import scala.util.Try

def unzipFile(pathToFile: Path, pathToUnzip: Path): Task[Unit] =
  for
    zipFile <- ZIO.fromTry(Try(new ZipFile(pathToFile.toFile)))
    files    = zipFile.entries.asScala.filter(!_.isDirectory)
    _       <- ZIO.foreach(files.to(Iterable)) { zipEntry =>
                 val path = pathToUnzip / zipEntry.getName
                 for
                   subFoldersPath <-
                     ZIO.fromOption(path.parent).orElseFail(Exception(s"Could not get parent folder for path: $path"))
                   _              <- Files.createDirectories(subFoldersPath)
                   _              <- Files.deleteIfExists(path)
                   _              <- Files.copy(ZStream.fromInputStream(zipFile.getInputStream(zipEntry)), path)
                 yield ()
               }
  yield ()
end unzipFile
