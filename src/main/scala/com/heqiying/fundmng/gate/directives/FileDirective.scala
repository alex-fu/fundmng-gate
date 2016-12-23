package com.heqiying.fundmng.gate.directives

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, MediaTypes, Multipart }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO

import scala.concurrent.{ ExecutionContext, Future }

object FileDirective {

  //form field name
  type Name = String

  case class FileInfo(fileName: String, targetFile: String, length: Long)

  private def uploadFileImpl(implicit mat: Materializer, ec: ExecutionContext): Directive1[Future[Map[Name, FileInfo]]] = {
    Directive[Tuple1[Future[Map[Name, FileInfo]]]] { inner =>
      entity(as[Multipart.FormData]) { (formdata: Multipart.FormData) =>
        val fileNameMap = formdata.parts.mapAsync(1) { p =>
          if (p.filename.isDefined) {
            val targetPath = File.createTempFile(s"userfile_${p.name}_${p.filename.getOrElse("")}", "")
            val written = p.entity.dataBytes.runWith(FileIO.toPath(Paths.get(targetPath.getAbsolutePath)))
            written.map(written =>
              Map(p.name -> FileInfo(p.filename.get, targetPath.getAbsolutePath, written.getCount)))
          } else {
            Future(Map.empty[Name, FileInfo])
          }
        }.runFold(Map.empty[Name, FileInfo])((set, value) => set ++ value)
        inner(Tuple1(fileNameMap))
      }
    }
  }

  def uploadFile: Directive1[Map[Name, FileInfo]] = {
    Directive[Tuple1[Map[Name, FileInfo]]] { inner =>
      extractMaterializer { implicit mat =>
        extractExecutionContext { implicit ec =>
          uploadFileImpl(mat, ec) { filesFuture => ctx => {
            filesFuture.map(map => inner(Tuple1(map))).flatMap(route => route(ctx))
          }
          }
        }
      }
    }
  }

  def downloadFile(file: String): Route = {
    val f = new File(file)
    val path = Paths.get(file)
    val responseEntity = HttpEntity(
      MediaTypes.`application/octet-stream`,
      f.length,
      FileIO.fromPath(path, chunkSize = 262144)
    )
    complete(HttpResponse(entity = responseEntity))
  }
}