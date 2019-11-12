package slowikps

import cats.Applicative
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

case class SomeDataClass(name: String, age: Int)

case object SomeDataClass {
  implicit val encoder: Encoder[SomeDataClass] = deriveEncoder[SomeDataClass]
  implicit def entityEncoder[F[_]: Applicative]: EntityEncoder[F, SomeDataClass] =
    jsonEncoderOf
}