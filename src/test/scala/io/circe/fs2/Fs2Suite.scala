package io.circe.fs2

import _root_.fs2.{ Pipe, Stream, text }
import cats.effect.IO
import io.circe.{ DecodingFailure, Json, ParsingFailure }
import io.circe.fs2.examples._
import io.circe.generic.auto._
import io.circe.syntax._
import jawn.AsyncParser
import scala.collection.immutable.{Stream => StdStream}

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
    forAll { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      assert(stream.through(stringParser(AsyncParser.SingleValue))
        .runLog.attempt.unsafeRunSync() === Right(Vector(foo.asJson)))
    }
  }
  
  "byteArrayParser" should "parse bytes wrapped in array" in {
    testParser(AsyncParser.UnwrapArray, _.through(text.utf8Encode).through(byteArrayParser))
  }

  "byteStreamParser" should "parse bytes delimited by new lines" in {
    testParser(AsyncParser.ValueStream, _.through(text.utf8Encode).through(byteStreamParser))
  }

  "byteParser" should "parse single value" in {
    forAll { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      assert(stream.through(text.utf8Encode).through(byteParser(AsyncParser.SingleValue))
        .runLog.attempt.unsafeRunSync() === Right(Vector(foo.asJson)))
    }
  }

  "byteArrayParserS" should "parse bytes wrapped in array" in {
    testParser(AsyncParser.UnwrapArray,
      _.through(text.utf8Encode).segments.through(byteArrayParserS))
  }

  "byteStreamParserS" should "parse bytes delimited by new lines" in {
    testParser(AsyncParser.ValueStream,
      _.through(text.utf8Encode).segments.through(byteStreamParserS))
  }


  "byteParserS" should "parse single value" in {
    forAll { (foo: Foo) =>
      val stream = serializeFoos(AsyncParser.SingleValue, Stream.emit(foo))
      assert(stream
        .through(text.utf8Encode)
        .segments
        .through(byteParserS(AsyncParser.SingleValue))
        .runLog.attempt.unsafeRunSync() === Right(Vector(foo.asJson)))
    }
  }

  "decoder" should "decode enumerated JSON values" in 
    forAll { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val stream = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
      val foos = fooStdStream ++ fooVector
  
      assert(stream.through(stringArrayParser).through(decoder[IO, Foo])
        .runLog.attempt.unsafeRunSync() === Right(foos.toVector))
    }

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

  "byteArrayParserS" should "return ParsingFailure" in {
    testParsingFailure(_.through(text.utf8Encode).segments.through(byteArrayParserS))
  }

  "byteStreamParserS" should "return ParsingFailure" in {
    testParsingFailure(_.through(text.utf8Encode).segments.through(byteStreamParserS))
  }

  "decoder" should "return DecodingFailure" in
    forAll { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      sealed trait Foo2
      case class Bar2(x: String) extends Foo2
  
      whenever(fooStdStream.nonEmpty && fooVector.nonEmpty) {
        val result = serializeFoos(AsyncParser.UnwrapArray, fooStream(fooStdStream, fooVector))
          .through(stringArrayParser).through(decoder[IO, Foo2])
          .runLog.attempt.unsafeRunSync()
  
        assert(result.isLeft && result.left.get.isInstanceOf[DecodingFailure])
      }
    }

  private def testParser(mode: AsyncParser.Mode, through: Pipe[IO, String, Json]) = {
    forAll { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
      val stream = serializeFoos(mode, fooStream(fooStdStream, fooVector))
      val foos = (fooStdStream ++ fooVector).map(_.asJson)

      assert(stream.through(through).runLog.attempt.unsafeRunSync() === Right(foos.toVector))
    }
  }
  
  private def testParsingFailure(through: Pipe[IO, String, Json]) = {
    forAll { (stringStdStream: StdStream[String], stringVector: Vector[String]) =>
      val result = Stream("}").append(stringStream(stringStdStream, stringVector))
        .through(through)
        .runLog.attempt.unsafeRunSync()
      assert(result.isLeft && result.left.get.isInstanceOf[ParsingFailure])
    }
  }
}
