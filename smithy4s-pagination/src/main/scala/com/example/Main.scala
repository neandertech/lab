package com.example

import com.example.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import fs2.Stream
import org.http4s.ember.client.EmberClientBuilder

object HelloWorldImpl extends HelloWorldService[IO] {
  def hello(name: String, town: Option[String]): IO[Greeting] = IO.pure {
    town match {
      case None    => Greeting(s"Hello " + name + "!")
      case Some(t) => Greeting(s"Hello " + name + " from " + t + "!")
    }
  }
  def pages(book: BookID, page: Option[String]): IO[PagesOutput] = page match {
    case None =>
      PagesOutput(nextPage = Some("1"), data = List(s"${book}0")).pure[IO]
    case Some(num(pageNumber)) if pageNumber > 0 && pageNumber < 100 =>
      PagesOutput(
        nextPage = Some(s"${pageNumber + 1}"),
        data = List(s"${book}$pageNumber")
      ).pure[IO]
    case Some(_) => PagesOutput(data = List.empty).pure[IO]
  }
}

object num {
  def unapply(string: String): Option[Int] = string.toIntOption
}

object Routes {
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {
  val run = Routes.all.flatMap { routes =>
    val thePort = port"9000"
    val theHost = host"localhost"
    EmberServerBuilder
      .default[IO]
      .withPort(thePort)
      .withHost(theHost)
      .withHttpApp(routes.orNotFound)
      .build <*
      Resource.eval(IO.println(s"Server started on: $theHost:$thePort"))
  }.useForever

}

object ClientMain extends IOApp.Simple {

  def run: IO[Unit] =
    EmberClientBuilder
      .default[IO]
      .build
      .flatMap { httpClient =>
        SimpleRestJsonBuilder(HelloWorldService)
          .client(httpClient)
          .uri(uri"http://localhost:9000")
          .resource
      }
      .use { helloWorld =>
        Pagination(helloWorld)
          .pages("myBook")
          .take(10)
          .flatMap { output =>
            Stream.foldable(output.data)
          }
          .evalMap(IO.println)
          .compile
          .drain
      }

}
