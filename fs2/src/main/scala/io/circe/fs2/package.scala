/*
 * Copyright 2017 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import _root_.fs2.Chunk
import _root_.fs2.Pipe
import _root_.fs2.RaiseThrowable
import _root_.fs2.Stream
import cats.ApplicativeError
import cats.effect.Sync
import io.circe.jawn.CirceSupportParser
import org.typelevel.jawn.AsyncParser
import org.typelevel.jawn.ParseException

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

  /* Decode json stream to a stream of `A`
   *
   * Lazily decodes elements and emits each resulting `A` as a singleton chunk. This stream "de-chunking" can have
   * performance implications. As an alternative, `chunkDecoder` is available, with some caveats.
   */
  final def decoder[F[_]: RaiseThrowable, A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    _.flatMap { json =>
      decode(json.hcursor) match {
        case Left(df) => Stream.raiseError(df)
        case Right(a) => Stream.emit(a)
      }
    }

  /* Like `decoder` but operates on the original chunks in the stream.
   *
   * Preserving the chunk structure of the stream is more performant. However, this means that this pipe is not
   * lazy on elements, but rather on the chunks. For example, `stream.chunkN(199).through(chunkDecoder[F, A]).take(400)`
   * would decode 597 elements (3 chunks worth) in order to accumulate 400 `A`
   */
  final def chunkDecoder[F[_], A](implicit decode: Decoder[A], ev: ApplicativeError[F, Throwable]): Pipe[F, Json, A] =
    _.evalMapChunk { json =>
      decode(json.hcursor) match {
        case Left(df) => ev.raiseError[A](df)
        case Right(a) => ev.pure(a)
      }
    }
}
