package slowikps

object IOTest extends App {
  import cats.effect.IO

  val ioa = IO { println("hey!") }

  val program: IO[Int] =
    for {
      _ <- ioa
      _ <- ioa
    } yield (1)

  println(
    program.unsafeRunSync()
  )

}
