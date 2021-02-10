package io.circe.fs2

import fs2.{Pure, Stream}
import io.circe.Codec
import io.circe.generic.semiauto._
import org.scalatest.matchers.should.Matchers
import encoding.syntax._
import io.circe.jawn.parse
import org.scalatest.flatspec.AnyFlatSpec

class EncodingSuite extends AnyFlatSpec with Matchers {
  case class Simple(s: String)

  object Simple {
    implicit val enc: Codec[Simple] = deriveCodec
  }

  case class Streamed[F[_], A](a: Int, b: Stream[Pure, A], c: Simple)

  case class Listed[A](a: Int, b: List[A], c: Simple)

  object Listed {
    def fromStreamed[F[_], A](s: Streamed[F, A]): Listed[A] = Listed(s.a, s.b.compile.toList, s.c)
    implicit def codec[A: Codec]: Codec[Listed[A]]          = deriveCodec
  }

  it should "encode a case class containing a stream" in {
    val streamed = Streamed(1, Stream(Simple("a"), Simple("b"), Simple("c")), Simple("2"))
    parse(streamed.asJsonStream[Pure].compile.string).flatMap(_.as[Listed[Simple]]) shouldBe Right(Listed.fromStreamed(streamed))
  }

  it should "encode a nested case class containing a stream" in {
    case class Nested(a: Stream[Pure, Int], b: Option[Nested])
    case class NestedL(a: List[Int], b: Option[NestedL])
    object NestedL {
      def fromStreamed(s: Nested): NestedL = NestedL(s.a.compile.toList, s.b.map(fromStreamed))
      implicit val codec: Codec[NestedL]   = deriveCodec
    }

    val streamed = Nested(Stream(1), Some(Nested(Stream(2), None)))
    val string = streamed.asJsonStream[Pure].compile.string

    parse(string).flatMap(_.as[NestedL]) shouldBe Right(NestedL.fromStreamed(streamed))
  }
}
