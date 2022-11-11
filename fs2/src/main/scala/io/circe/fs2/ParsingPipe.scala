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

package io.circe.fs2

import _root_.fs2.{ Chunk, Pipe, Pull, RaiseThrowable, Stream }
import cats.effect.Sync
import io.circe.{ Json, ParsingFailure }
import io.circe.jawn.CirceSupportParser
import org.typelevel.jawn.{ AsyncParser, ParseException }
import scala.collection.Seq

private[fs2] abstract class ParsingPipe[F[_]: Sync, S](supportParser: CirceSupportParser) extends Pipe[F, S, Json] {
  private[this] val raiseThrowable: RaiseThrowable[F] = RaiseThrowable.fromApplicativeError

  protected[this] def parsingMode: AsyncParser.Mode

  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: F[AsyncParser[Json]] = Sync[F].delay(supportParser.async(mode = parsingMode))

  private[this] final def doneOrLoop[A](p: AsyncParser[Json])(s: Stream[F, S]): Pull[F, Json, Unit] =
    s.pull.uncons1.flatMap {
      case Some((s, str)) =>
        parseWith(p)(s) match {
          case Left(error) =>
            Pull.raiseError(ParsingFailure(error.getMessage, error))(raiseThrowable)
          case Right(js) =>
            Pull.output(Chunk.seq(js)) >> doneOrLoop(p)(str)
        }
      case None => Pull.done
    }

  final def apply(s: Stream[F, S]): Stream[F, Json] =
    Stream.eval(makeParser).flatMap(parser => doneOrLoop(parser)(s).stream)
}
