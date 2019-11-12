package slowikps

import cats.effect._
import cats.implicits._
import cats._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object Http4stestRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F])(implicit timer: Timer[IO], par: cats.Parallel[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" / IntVar(number) =>
        for {
          joke: Seq[Jokes.Joke] <- Vector.from(1 to number).parTraverse(_ => J.get)
          resp <- Ok(joke)
        } yield resp
      case GET -> Root / "joke-post" =>
        for {
          joke: Seq[Jokes.Joke] <- Vector(1).parTraverse(_ => J.post)
          resp <- Ok(joke)
        } yield resp
      case GET -> Root / "joke-param" :? map =>
        for {
          joke <- J.getWithParams(map)
          _ = println(map)
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  def pawelRoutes[F[_]: Sync]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "pawel"  =>
        Ok(SomeDataClass("pawel", 34))
    }
  }
}
