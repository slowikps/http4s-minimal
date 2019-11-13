package slowikps

import cats.data.Kleisli
import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.{Header, HttpRoutes, Request, Status}

import scala.concurrent.ExecutionContext.global

object Http4stestServer {

  def stream(implicit T: Timer[IO], C: ContextShift[IO]): Stream[IO, Nothing] = {
    for {
      client: Client[IO] <- BlazeClientBuilder[IO](global).stream
      helloWorldAlg      = HelloWorld.impl[IO]
      jokeAlg            = Jokes.impl(client)
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      allRoutes: HttpRoutes[IO] = Http4stestRoutes.helloWorldRoutes[IO](helloWorldAlg) <+>
        Http4stestRoutes.jokeRoutes[IO](jokeAlg) <+>
        Http4stestRoutes.pawelRoutes

      wrapped = myMiddle(allRoutes, Header("SomeKey", "SomeValue"))
      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(wrapped.orNotFound)

      exitCode <- BlazeServerBuilder[IO]
                   .bindHttp(8080, "0.0.0.0")
                   .withHttpApp(finalHttpApp)
                   .serve
    } yield exitCode
  }.drain

  def myMiddle(service: HttpRoutes[IO], header: Header): HttpRoutes[IO] = Kleisli { req: Request[IO] =>
    service(req).map {
      case Status.Successful(resp) =>
        resp.putHeaders(header)
      case resp =>
        resp
    }
  }
}
