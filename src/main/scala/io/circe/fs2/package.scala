package io.circe

import _root_.fs2.{Chunk, Pipe, RaiseThrowable, Stream}
import io.circe.jawn.CirceSupportParser
import org.typelevel.jawn.{AsyncParser, ParseException}

package object fs2 {
  final def stringArrayParser[F[_] : RaiseThrowable]: Pipe[F, String, Json] = stringParser(AsyncParser.UnwrapArray)

  final def stringStreamParser[F[_] : RaiseThrowable]: Pipe[F, String, Json] = stringParser(AsyncParser.ValueStream)

  final def byteArrayParser[F[_] : RaiseThrowable]: Pipe[F, Byte, Json] = byteParser(AsyncParser.UnwrapArray)

  final def byteStreamParser[F[_] : RaiseThrowable]: Pipe[F, Byte, Json] = byteParser(AsyncParser.ValueStream)

  final def byteArrayParserC[F[_] : RaiseThrowable]: Pipe[F, Chunk[Byte], Json] = byteParserC(AsyncParser.UnwrapArray)

  final def byteStreamParserC[F[_] : RaiseThrowable]: Pipe[F, Chunk[Byte], Json] = byteParserC(AsyncParser.ValueStream)

  final def stringParser[F[_] : RaiseThrowable](mode: AsyncParser.Mode): Pipe[F, String, Json] = new ParsingPipe[F, String] {
    override protected[this] val raiseThrowable: RaiseThrowable[F] = implicitly[RaiseThrowable[F]]

    protected[this] final def parseWith(p: AsyncParser[Json])(in: String): Either[ParseException, Seq[Json]] =
      p.absorb(in)(CirceSupportParser.facade)

    protected[this] val parsingMode: AsyncParser.Mode = mode
  }

  final def byteParserC[F[_] : RaiseThrowable](mode: AsyncParser.Mode): Pipe[F, Chunk[Byte], Json] =
    new ParsingPipe[F, Chunk[Byte]] {
      override protected[this] val raiseThrowable: RaiseThrowable[F] = implicitly[RaiseThrowable[F]]

      protected[this] final def parseWith(p: AsyncParser[Json])(in: Chunk[Byte]): Either[ParseException, Seq[Json]] =
        p.absorb(in.toArray)(CirceSupportParser.facade)

      protected[this] val parsingMode: AsyncParser.Mode = mode
    }

  final def byteParser[F[_] : RaiseThrowable](mode: AsyncParser.Mode): Pipe[F, Byte, Json] = _.chunks.through(byteParserC(mode))

  final def decoder[F[_] : RaiseThrowable, A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(df) => Stream.raiseError(df)
        case Right(a) => Stream.emit(a)
      }
    }
}
