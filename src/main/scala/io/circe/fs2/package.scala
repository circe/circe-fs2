package io.circe

import _root_.fs2.{ Chunk, Pipe, Stream }
import _root_.fs2.pipe
import _root_.jawn.{ AsyncParser, ParseException }
import io.circe.jawn.CirceSupportParser

package object fs2 {
  final def stringArrayParser[F[_]]: Pipe[F, String, Json] = stringParser(AsyncParser.UnwrapArray)

  final def stringStreamParser[F[_]]: Pipe[F, String, Json] = stringParser(AsyncParser.ValueStream)

  final def byteArrayParser[F[_]]: Pipe[F, Byte, Json] = byteParser(AsyncParser.UnwrapArray)

  final def byteStreamParser[F[_]]: Pipe[F, Byte, Json] = byteParser(AsyncParser.ValueStream)

  final def byteArrayParserC[F[_]]: Pipe[F, Chunk[Byte], Json] = byteParserC(AsyncParser.UnwrapArray)

  final def byteStreamParserC[F[_]]: Pipe[F, Chunk[Byte], Json] = byteParserC(AsyncParser.ValueStream)
  
  final def stringParser[F[_]](mode: AsyncParser.Mode): Pipe[F, String, Json] = new ParsingPipe[F, String] {
    protected[this] final def parseWith(p: AsyncParser[Json])(in: String): Either[ParseException, Seq[Json]] =
      p.absorb(in)(CirceSupportParser.facade)

    protected[this] val parsingMode: AsyncParser.Mode = mode
  }

  final def byteParserC[F[_]](mode: AsyncParser.Mode): Pipe[F, Chunk[Byte], Json] = new ParsingPipe[F, Chunk[Byte]] {
    protected[this] final def parseWith(p: AsyncParser[Json])(in: Chunk[Byte]): Either[ParseException, Seq[Json]] =
      p.absorb(in.toArray)(CirceSupportParser.facade)

    protected[this] val parsingMode: AsyncParser.Mode = mode
  }

  final def byteParser[F[_]](mode: AsyncParser.Mode): Pipe[F, Byte, Json] = _.through(pipe.chunks).through(byteParserC(mode))

  final def decoder[F[_], A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(df) => Stream.fail(df)
        case Right(a) => Stream.emit(a)
      }
    }
}
