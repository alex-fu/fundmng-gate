package com.heqiying.fundmng.gate.directives

import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, MediaTypes, Multipart }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.FileIO
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

import scala.concurrent.Future
import scala.concurrent.duration._

object MultipartFormDirective {
  // form field data types
  type Name = String
  type PlainValue = String
  type HashCodeClass = String
  type HashCode = String

  case class FileInfo(fileName: String, targetFile: String, length: Long, contentHashes: Map[HashCodeClass, HashCode], createTime: String)

  object FileInfoJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
    implicit val fileInfoJsonFormat: RootJsonFormat[FileInfo] = jsonFormat5(FileInfo.apply)
  }

  def collectFormData: Directive1[Future[Map[Name, Either[PlainValue, FileInfo]]]] = {
    extractMaterializer.flatMap { implicit mat =>
      extractExecutionContext.flatMap { implicit ec =>
        entity(as[Multipart.FormData]).flatMap { (formdata: Multipart.FormData) =>
          val dataMap = formdata.parts.mapAsync(1) { p =>
            if (p.filename.isDefined) {
              val targetPath = File.createTempFile(s"userfile_${p.name}_${p.filename.getOrElse("")}", "")
              val written = p.entity.dataBytes.runWith(FileIO.toPath(Paths.get(targetPath.getAbsolutePath)))
              written.map(written =>
                p.name -> Right(FileInfo(p.filename.get, targetPath.getAbsolutePath, written.getCount, Map("hash1" -> "", "hash2" -> ""), LocalDateTime.now.toString)))
            } else {
              p.entity.toStrict(1.seconds).map { strict =>
                p.name -> Left(strict.data.utf8String)
              }
            }
          }.runFold(Map.empty[Name, Either[PlainValue, FileInfo]])((set, value) => set + value)
          provide(dataMap)
        }
      }
    }
  }

  def downloadFile(file: String): Route = {
    val f = new File(file)
    val responseEntity = HttpEntity(
      MediaTypes.`application/octet-stream`,
      f.length,
      FileIO.fromPath(f.toPath, chunkSize = 262144)
    )
    complete(HttpResponse(entity = responseEntity))
  }
}