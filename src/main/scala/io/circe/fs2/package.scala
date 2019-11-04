package io.circe

import _root_.fs2.{ Chunk, Pipe, Stream }
import cats.effect.Sync
import io.circe.jawn.CirceSupportParser
import org.typelevel.jawn.{ AsyncParser, ParseException }
import scala.collection.Seq

package object fs2 {
  private[this] val supportParser: CirceSupportParser = new CirceSupportParser(None, true)

  final def stringArrayParser[F[_]: Sync]: Pipe[F, String, Json] = stringParser(AsyncParser.UnwrapArray)

  final def stringStreamParser[F[_]: Sync]: Pipe[F, String, Json] = stringParser(AsyncParser.ValueStream)

  final def byteArrayParser[F[_]: Sync]: Pipe[F, Byte, Json] = byteParser(AsyncParser.UnwrapArray)

  final def byteStreamParser[F[_]: Sync]: Pipe[F, Byte, Json] = byteParser(AsyncParser.ValueStream)

  final def byteArrayParserC[F[_]: Sync]: Pipe[F, Chunk[Byte], Json] = byteParserC(AsyncParser.UnwrapArray)

  final def byteStreamParserC[F[_]: Sync]: Pipe[F, Chunk[Byte], Json] = byteParserC(AsyncParser.ValueStream)

  final def stringParser[F[_]: Sync](mode: AsyncParser.Mode): Pipe[F, String, Json] =
    new ParsingPipe[F, String](supportParser) {
      protected[this] final def parseWith(p: AsyncParser[Json])(in: String): Either[ParseException, Seq[Json]] =
        p.absorb(in)(supportParser.facade)

      protected[this] val parsingMode: AsyncParser.Mode = mode
    }

  final def byteParserC[F[_]: Sync](mode: AsyncParser.Mode): Pipe[F, Chunk[Byte], Json] =
    new ParsingPipe[F, Chunk[Byte]](supportParser) {
      protected[this] final def parseWith(p: AsyncParser[Json])(in: Chunk[Byte]): Either[ParseException, Seq[Json]] =
        p.absorb(in.toArray)(supportParser.facade)

      protected[this] val parsingMode: AsyncParser.Mode = mode
    }

  final def byteParser[F[_]: Sync](mode: AsyncParser.Mode): Pipe[F, Byte, Json] = _.chunks.through(byteParserC(mode))

  final def decoder[F[_]: Sync, A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(df) => Stream.raiseError(df)
        case Right(a) => Stream.emit(a)
      }
    }
}
