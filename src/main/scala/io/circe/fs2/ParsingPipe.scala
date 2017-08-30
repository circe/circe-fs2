package io.circe.fs2

import _root_.fs2.{Pipe, Pull, Segment, Stream}
import _root_.jawn.{AsyncParser, ParseException}
import io.circe.jawn.CirceSupportParser
import io.circe.{Json, ParsingFailure}

private[fs2] abstract class ParsingPipe[F[_], S] extends Pipe[F, S, Json] {
  protected[this] def parsingMode: AsyncParser.Mode
  
  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: AsyncParser[Json] = CirceSupportParser.async(mode = parsingMode)

  private[this] final def doneOrLoop[A](p: AsyncParser[Json])(stream: Stream[F, S]): Pull[F, Json, Unit] =
    stream.pull.uncons1.flatMap {
      case Some((s, stream)) => parseWith(p)(s) match {
        case Left(error) =>
          Pull.fail(ParsingFailure(error.getMessage, error))
        case Right(js) =>
          Pull.output(Segment.seq(js)) >> doneOrLoop(p)(stream)
      }
      case None => Pull.done
    }

  final def apply(s: Stream[F, S]): Stream[F, Json] = doneOrLoop(makeParser)(s).stream
}
