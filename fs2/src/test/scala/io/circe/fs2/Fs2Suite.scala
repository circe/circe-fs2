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

import _root_.fs2.Pipe
import _root_.fs2.Stream
import _root_.fs2.text
import cats.effect.IO
import io.circe.DecodingFailure
import io.circe.Json
import io.circe.ParsingFailure
import io.circe.fs2.examples._
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalacheck.Prop
import org.scalacheck.effect.PropF
import org.scalatest.compatible.Assertion
import org.scalatest.enablers.WheneverAsserting
import org.scalatest.exceptions.DiscardedEvaluationException
import org.typelevel.jawn.AsyncParser
import org.typelevel.scalaccompat.annotation._

import scala.collection.immutable.{ Stream => StdStream }

@nowarn213
@nowarn3
class Fs2Suite extends CirceSuite {
  def fooStream(fooStdStream: StdStream[Foo], fooVector: Vector[Foo]): Stream[IO, Foo] =
    Stream.emits(fooStdStream).append(Stream.emits(fooVector))

  def serializeFoos(parsingMode: AsyncParser.Mode, foos: Stream[IO, Foo]): Stream[IO, String] =
    parsingMode match {
      case AsyncParser.ValueStream | AsyncParser.SingleValue =>
        foos.map((_: Foo).asJson.spaces2).intersperse("\n")
      case AsyncParser.UnwrapArray =>
        Stream("[").append(foos.map((_: Foo).asJson.spaces2).intersperse(", ")).append(Stream("]"))
    }

  def stringStream(stringStdStream: StdStream[String], stringVector: Vector[String]): Stream[IO, String] =
    Stream.emits(stringStdStream).append(Stream.emits(stringVector))

  "stringArrayParser" should "parse values wrapped in array" in {
    testParser(AsyncParser.UnwrapArray, stringArrayParser)
  }

  "stringStreamParser" should "parse values delimited by new lines" in {
    testParser(AsyncParser.ValueStream, stringStreamParser)
  }

  "stringParser" should "parse single value" in {
    PropF.forAllF { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      stream
        .through(stringParser(AsyncParser.SingleValue))
        .compile
        .toVector
        .attempt
        .map(r =>
          assert(
            r === Right(
              Vector(foo.asJson)
            )
          )
        )
    }.check().map(r => assert(r.passed))
  }

  "byteArrayParser" should "parse bytes wrapped in array" in {
    testParser(AsyncParser.UnwrapArray, _.through(text.utf8Encode).through(byteArrayParser))
  }

  "byteStreamParser" should "parse bytes delimited by new lines" in {
    testParser(AsyncParser.ValueStream, _.through(text.utf8Encode).through(byteStreamParser))
  }

  "byteParser" should "parse single value" in {
    PropF.forAllF { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      stream
        .through(text.utf8Encode)
        .through(byteParser(AsyncParser.SingleValue))
        .compile
        .toVector
        .attempt
        .map(r => assert(r === Right(Vector(foo.asJson))))
    }.check().map(r => assert(r.passed))
  }

  "byteParser" should "parse single value, when run twice" in {
    PropF.forAllF { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))

      val parseOnce =
        stream.through(text.utf8Encode).through(byteParser(AsyncParser.SingleValue)).compile.toVector

      (parseOnce.attempt >> parseOnce.attempt).map(r => assert(r == Right(Vector(foo.asJson))))
    }.check().map(r => assert(r.passed))
  }

  "byteArrayParserC" should "parse bytes wrapped in array" in {
    testParser(AsyncParser.UnwrapArray, _.through(text.utf8Encode).chunks.through(byteArrayParserC))
  }

  "byteStreamParserC" should "parse bytes delimited by new lines" in {
    testParser(AsyncParser.ValueStream, _.through(text.utf8Encode).chunks.through(byteStreamParserC))
  }

  "byteParserC" should "parse single value" in {
    PropF.forAllF { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      stream
        .through(text.utf8Encode)
        .chunks
        .through(byteParserC(AsyncParser.SingleValue))
        .compile
        .toVector
        .attempt
        .map(r => assert(r === Right(Vector(foo.asJson))))
    }.check().map(r => assert(r.passed))
  }

  "decoder" should "decode enumerated JSON values" in
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val stream = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
      val foos = fooStdStream ++ fooVector

      stream
        .through(stringArrayParser)
        .through(decoder[IO, Foo])
        .compile
        .toVector
        .attempt
        .map(r =>
          assert(
            r === Right(
              foos.toVector
            )
          )
        )
    }.check().map(r => assert(r.passed))

  "chunkDecoder" should "decode enumerated JSON values" in
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val stream = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
      val foos = fooStdStream ++ fooVector

      val result = stream.through(stringArrayParser).through(chunkDecoder[IO, Foo]).compile.toVector.attempt

      result.map(r => assert(r === Right(foos.toVector)))
    }.check().map(r => assert(r.passed))

  "chunkDecoder" should "maintain chunk size" in
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val chunkSize = 4
      val stream = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
      val x1 = stream.through(stringArrayParser).chunkMin(chunkSize).flatMap(c => Stream.chunk(c))
      x1.through(chunkDecoder[IO, Foo]).chunks.map(_.size).compile.toList.map { chunkSizes =>
        if (chunkSizes.sum >= chunkSize)
          assert(chunkSizes.head == chunkSize)
        else
          assert(chunkSizes.length <= 1)
      }
    }.check().map(r => assert(r.passed))

  "stringArrayParser" should "return ParsingFailure" in {
    testParsingFailure(_.through(stringArrayParser))
  }

  "stringStreamParser" should "return ParsingFailure" in {
    testParsingFailure(_.through(stringStreamParser))
  }

  "byteArrayParser" should "return ParsingFailure" in {
    testParsingFailure(_.through(text.utf8Encode).through(byteArrayParser))
  }

  "byteStreamParser" should "return ParsingFailure" in {
    testParsingFailure(_.through(text.utf8Encode).through(byteStreamParser))
  }

  "byteArrayParserC" should "return ParsingFailure" in {
    testParsingFailure(_.through(text.utf8Encode).chunks.through(byteArrayParserC))
  }

  "byteStreamParserC" should "return ParsingFailure" in {
    testParsingFailure(_.through(text.utf8Encode).chunks.through(byteStreamParserC))
  }

  "decoder" should "return DecodingFailure" in
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      sealed trait Foo2
      case class Bar2(x: String) extends Foo2

      whenever(fooStdStream.nonEmpty && fooVector.nonEmpty) {
        val result = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
          .through(stringArrayParser)
          .through(decoder[IO, Foo2])
          .compile
          .toVector
          .attempt

        result.map(r => assert(r.isLeft && r.left.get.isInstanceOf[DecodingFailure]))
      }
    }.check().map(r => assert(r.passed))

  "chunkDecoder" should "return DecodingFailure" in
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      sealed trait Foo2
      case class Bar2(x: String) extends Foo2

      whenever(fooStdStream.nonEmpty && fooVector.nonEmpty) {
        val result = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
          .through(stringArrayParser)
          .through(chunkDecoder[IO, Foo2])
          .compile
          .toVector
          .attempt
        result.map(r => assert(r.isLeft && r.left.get.isInstanceOf[DecodingFailure]))
      }
    }.check().map(r => assert(r.passed))

  private def testParser(mode: AsyncParser.Mode, through: Pipe[IO, String, Json]) =
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val stream = serializeFoos(mode, fooStream(fooStdStream, fooVector))
      val foos = (fooStdStream ++ fooVector).map(_.asJson)

      stream.through(through).compile.toVector.attempt.map(r => assert(r === Right(foos.toVector)))
    }.check().asserting(r => assert(r.passed))

  private def testParsingFailure(through: Pipe[IO, String, Json]) =
    PropF.forAllF { (stringStdStream: StdStream[String], stringVector: Vector[String]) =>
      val result =
        Stream("}").append(stringStream(stringStdStream, stringVector)).through(through).compile.toVector.attempt
      result.map(result => assert(result.isLeft && result.left.get.isInstanceOf[ParsingFailure]))
    }.check().asserting(r => assert(r.passed))

  private implicit def assertionToProp: IO[Assertion] => PropF[IO] = { assertion =>
    assertion.as(PropF.Result[IO](Prop.True, Nil, Set.empty, Set.empty): PropF[IO]).handleError {
      case _: DiscardedEvaluationException => PropF.Result[IO](Prop.Undecided, Nil, Set.empty, Set.empty)
      case t                               => PropF.Result[IO](Prop.Exception(t), Nil, Set.empty, Set.empty)
    }
  }

  private implicit def assertingNatureOfIO: WheneverAsserting[IO[Assertion]] { type Result = IO[Assertion] } =
    new WheneverAsserting[IO[Assertion]] {
      type Result = IO[Assertion]
      def whenever(condition: Boolean)(fun: => IO[Assertion]): IO[Assertion] =
        if (!condition)
          IO.raiseError[Assertion](new DiscardedEvaluationException)
        else
          fun
    }

}
