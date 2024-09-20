package utils

import zio.*
import zio.nio.file.{Files, Path}
import zio.test.*

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import ujson.*

import java.nio.file.NoSuchFileException


object unzipFileSpec extends ZIOSpecDefault {
  def spec = suite("unzipFile")(
    test("Should extract all files and folders from an archive to specified folder. Empty folders should not be extracted.") {
      for {
        tempDirectory <- Files.createTempDirectoryScoped(prefix = None, fileAttributes = Nil)
        folderToUnzip = tempDirectory / "unzip_file_test"
        _ <- Files.createDirectories(folderToUnzip)
        pathToZipFile = Path(URLDecoder.decode(getClass.getResource("/utils/zipped_folder.zip").getPath, "UTF-8"))
        _ <- unzipFile(pathToFile = pathToZipFile, pathToUnzip = folderToUnzip)
        emptyFolderIsExtracted <- Files.exists(folderToUnzip / "zipped_folder" / "empty_folder")
        fileAJson <- Files.readAllBytes(folderToUnzip / "zipped_folder" / "file_a.json").map(b => String(b.toArray, StandardCharsets.UTF_8))
        fileBText <- Files.readAllBytes(folderToUnzip / "zipped_folder" / "sub_folder" /"file_b.txt").map(b => String(b.toArray, StandardCharsets.UTF_8))
        fileAField = ujson.read(fileAJson)("field").arr.toSeq.map(_.num)
      } yield assertTrue(!emptyFolderIsExtracted && fileAField == Seq(1, 2, 3) && fileBText == "File from subfolder.")
    },
    test("Should unzip the archive at specified path even if it does not exist") {
      for {
        tempDirectory <- Files.createTempDirectoryScoped(prefix = None, fileAttributes = Nil)
        folderToUnzip = tempDirectory / "unzip_file_test"
        pathToZipFile = Path(URLDecoder.decode(getClass.getResource("/utils/zipped_folder.zip").getPath, "UTF-8"))
        _ <- unzipFile(pathToFile = pathToZipFile, pathToUnzip = folderToUnzip)
        extractedFileExists <- Files.exists(folderToUnzip / "zipped_folder" / "file_a.json")
      } yield assertTrue(extractedFileExists)
    },
    test("Should unzip the archive at specified path even if there is already extracted file at specified path") {
      for {
        tempDirectory <- Files.createTempDirectoryScoped(prefix = None, fileAttributes = Nil)
        folderToUnzip = tempDirectory / "unzip_file_test"
        _ <- Files.createDirectories(folderToUnzip)
        pathToZipFile = Path(URLDecoder.decode(getClass.getResource("/utils/zipped_folder.zip").getPath, "UTF-8"))
        _ <- unzipFile(pathToFile = pathToZipFile, pathToUnzip = folderToUnzip)
        error <- unzipFile(pathToFile = pathToZipFile, pathToUnzip = folderToUnzip).catchAll(error => ZIO.succeed(error))
        extractedFileExists <- Files.exists(folderToUnzip / "zipped_folder" / "file_a.json")

      } yield assertTrue(error == () && extractedFileExists)
    },
    test("Should fail if there is no the archive at specified path") {
      for {
        tempDirectory <- Files.createTempDirectoryScoped(prefix = None, fileAttributes = Nil)
        folderToUnzip = tempDirectory / "unzip_file_test"
        _ <- Files.createDirectories(folderToUnzip)
        pathToZipFile = Path("not_existent_folder")
        error <- unzipFile(pathToFile = pathToZipFile, pathToUnzip = folderToUnzip).catchAll(error => ZIO.succeed(error))
      } yield assertTrue(error.isInstanceOf[NoSuchFileException])
    },
  )
}
