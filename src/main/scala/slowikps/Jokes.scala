package slowikps
import cats.Applicative
import cats.effect.{IO, Sync, Timer}
import cats.implicits._
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Method.{GET, _}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.implicits._
import org.http4s.{EntityDecoder, EntityEncoder}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
trait Jokes[F[_]] {
  def get(implicit timer: Timer[IO]): F[Jokes.Joke]
  def post: F[Jokes.Joke]
  def getWithParams(map: Map[String, Seq[String]]): F[Jokes.Joke]
}

object Jokes {
  def apply[F[_]](implicit ev: Jokes[F]): Jokes[F] = ev

  final case class Joke(joke: String) extends AnyVal
  object Joke {
    implicit val jokeDecoder: Decoder[Joke] = deriveDecoder[Joke]
    implicit def jokeEntityDecoder[F[_]: Sync]: EntityDecoder[F, Joke] =
      jsonOf
    implicit val jokeEncoder: Encoder[Joke] = deriveEncoder[Joke]
    implicit def seqJokeEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Seq[Joke]] =
      jsonEncoderOf
    implicit def jokeEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Joke] =
      jsonEncoderOf
  }

  final case class JokeError(e: Throwable) extends RuntimeException

  def impl(C: Client[IO]): Jokes[IO] = new Jokes[IO] {
    val dsl = new Http4sClientDsl[IO] {}
    import dsl._
    def get(implicit timer: Timer[IO]): IO[Jokes.Joke] = {
      for {
        _ <- IO.sleep(1.second)
        res <- C.expect[Joke](GET(uri"https://icanhazdadjoke.com/"))
                .adaptError { case t => JokeError(t) } // Prevent Client Json Decoding Failure Leaking
      } yield res
    }

    def post: IO[Jokes.Joke] = {
      {
        import io.circe.literal._
        Try {
          C.expect[Json](POST(json"""{"name": "Alice"}""", uri"https://postman-echo.com/post"))
            .unsafeRunSync()
        } match {
          case Success(value)     => println(value)
          case Failure(exception) => exception.printStackTrace()
        }
        C.expect[Json](POST(json"""{"my_joke": "very bad joke"}""", uri"https://postman-echo.com/post"))
          .map(_.hcursor.downField("data").downField("my_joke").as[String].getOrElse("ERROR"))
          .map(Joke(_))

      }
    }

    override def getWithParams(map: Map[String, Seq[String]]): IO[Joke] = {
      {
        C.expect[Json](GET(map.foldLeft(uri"https://postman-echo.com/get") {
            case (acc, (name, value)) => acc.withQueryParam(name, value)
          }))
          .map(_.hcursor.downField("args").as[Json].getOrElse("ERROR"))
          .map(json => Joke(json.toString))

      }
    }
  }
}
