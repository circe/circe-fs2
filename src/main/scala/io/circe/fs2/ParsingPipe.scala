package io.circe.fs2

import _root_.fs2.{ Chunk, Pipe, Pull, RaiseThrowable, Stream }
import io.circe.{ Json, ParsingFailure }
import io.circe.jawn.CirceSupportParser
import org.typelevel.jawn.{ AsyncParser, ParseException }

private[fs2] abstract class ParsingPipe[F[_], S] extends Pipe[F, S, Json] {
  protected[this] val raiseThrowable: RaiseThrowable[F]

  protected[this] def parsingMode: AsyncParser.Mode

  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: AsyncParser[Json] = CirceSupportParser.async(mode = parsingMode)

  private[this] final def doneOrLoop[A](p: AsyncParser[Json])(s: Stream[F, S]): Pull[F, Json, Unit] =
    s.pull.uncons1.flatMap {
      case Some((s, str)) => parseWith(p)(s) match {
        case Left(error) =>
          Pull.raiseError(ParsingFailure(error.getMessage, error))(raiseThrowable)
        case Right(js) =>
          Pull.output(Chunk.seq(js)) >> doneOrLoop(p)(str)
      }
      case None => Pull.done
    }

  final def apply(s: Stream[F, S]): Stream[F, Json] = doneOrLoop(makeParser)(s).stream
}
