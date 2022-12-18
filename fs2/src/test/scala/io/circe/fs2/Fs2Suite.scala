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

import _root_.fs2.{ Pipe, Stream, text }
import cats.effect.IO
import io.circe.fs2.examples._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{ DecodingFailure, Json, ParsingFailure }
import org.scalacheck.effect.PropF
import org.typelevel.jawn.AsyncParser

import scala.annotation.nowarn
import scala.collection.immutable.{ Stream => StdStream }

@nowarn
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

  test("stringArrayParser should parse values wrapped in array") {
    testParser(AsyncParser.UnwrapArray, stringArrayParser)
  }

  test("stringStreamParser should parse values delimited by new lines") {
    testParser(AsyncParser.ValueStream, stringStreamParser)
  }

  test("stringParser should parse single value") {
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

  test("byteArrayParser should parse bytes wrapped in array") {
    testParser(AsyncParser.UnwrapArray, _.through(text.utf8.encode).through(byteArrayParser))
  }

  test("byteStreamParser should parse bytes delimited by new lines") {
    testParser(AsyncParser.ValueStream, _.through(text.utf8.encode).through(byteStreamParser))
  }

  test("byteParser should parse single value") {
    PropF.forAllF { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      stream
        .through(text.utf8.encode)
        .through(byteParser(AsyncParser.SingleValue))
        .compile
        .toVector
        .attempt
        .map(r => assert(r === Right(Vector(foo.asJson))))
    }.check().map(r => assert(r.passed))
  }

  test("byteParser should parse single value, when run twice") {
    PropF.forAllF { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))

      val parseOnce =
        stream.through(text.utf8.encode).through(byteParser(AsyncParser.SingleValue)).compile.toVector

      (parseOnce.attempt >> parseOnce.attempt).map(r => assert(r == Right(Vector(foo.asJson))))
    }.check().map(r => assert(r.passed))
  }

  test("byteArrayParserC should parse bytes wrapped in array") {
    testParser(AsyncParser.UnwrapArray, _.through(text.utf8.encode).chunks.through(byteArrayParserC))
  }

  test("byteStreamParserC should parse bytes delimited by new lines") {
    testParser(AsyncParser.ValueStream, _.through(text.utf8.encode).chunks.through(byteStreamParserC))
  }

  test("byteParserC should parse single value") {
    PropF.forAllF { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      stream
        .through(text.utf8.encode)
        .chunks
        .through(byteParserC(AsyncParser.SingleValue))
        .compile
        .toVector
        .attempt
        .map(r => assert(r === Right(Vector(foo.asJson))))
    }
  }

  test("decoder should decode enumerated JSON values") {
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
  }

  test("chunkDecoder should decode enumerated JSON values") {
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val stream = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
      val foos = fooStdStream ++ fooVector

      val result = stream.through(stringArrayParser).through(chunkDecoder[IO, Foo]).compile.toVector.attempt

      result.map(r => assert(r === Right(foos.toVector)))
    }.check().map(r => assert(r.passed))
  }

  test("chunkDecoder should maintain chunk size") {
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
  }

  test("stringArrayParser should return ParsingFailure") {
    testParsingFailure(_.through(stringArrayParser))
  }

  test("stringStreamParser should return ParsingFailure") {
    testParsingFailure(_.through(stringStreamParser))
  }

  test("byteArrayParser should return ParsingFailure") {
    testParsingFailure(_.through(text.utf8.encode).through(byteArrayParser))
  }

  test("byteStreamParser should return ParsingFailure") {
    testParsingFailure(_.through(text.utf8.encode).through(byteStreamParser))
  }

  test("byteArrayParserC should return ParsingFailure") {
    testParsingFailure(_.through(text.utf8.encode).chunks.through(byteArrayParserC))
  }

  test("byteStreamParserC should return ParsingFailure") {
    testParsingFailure(_.through(text.utf8.encode).chunks.through(byteStreamParserC))
  }

  test("decoder should return DecodingFailure") {
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      sealed trait Foo2
      case class Bar2(x: String) extends Foo2

      if (fooStdStream.nonEmpty && fooVector.nonEmpty) {
        val result = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
          .through(stringArrayParser)
          .through(decoder[IO, Foo2])
          .compile
          .toVector
          .attempt

        result.map(r => assert(r.isLeft && r.left.get.isInstanceOf[DecodingFailure]))
      } else IO.pure(())
    }
  }

  test("chunkDecoder should return DecodingFailure") {
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      sealed trait Foo2
      case class Bar2(x: String) extends Foo2

      if (fooStdStream.nonEmpty && fooVector.nonEmpty) {
        val result = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
          .through(stringArrayParser)
          .through(chunkDecoder[IO, Foo2])
          .compile
          .toVector
          .attempt
        result.map(r => assert(r.isLeft && r.left.get.isInstanceOf[DecodingFailure]))
      } else {
        IO.pure(())
      }
    }
  }

  private def testParser(mode: AsyncParser.Mode, through: Pipe[IO, String, Json]) =
    PropF.forAllF { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val stream = serializeFoos(mode, fooStream(fooStdStream, fooVector))
      val foos = (fooStdStream ++ fooVector).map(_.asJson)

      stream.through(through).compile.toVector.attempt.map(r => assert(r === Right(foos.toVector)))
    }

  private def testParsingFailure(through: Pipe[IO, String, Json]) =
    PropF.forAllF { (stringStdStream: StdStream[String], stringVector: Vector[String]) =>
      val result =
        Stream("}").append(stringStream(stringStdStream, stringVector)).through(through).compile.toVector.attempt
      result.map(result => assert(result.isLeft && result.left.get.isInstanceOf[ParsingFailure]))
    }
}
