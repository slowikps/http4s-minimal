package slowikps

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  def run(args: List[String]) =
    Http4stestServer.stream.compile.drain.as(ExitCode.Success)
}
