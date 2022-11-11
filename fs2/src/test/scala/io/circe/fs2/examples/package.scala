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

import cats.Eq
import cats.instances.AllInstances
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.testing.ArbitraryInstances
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

package object examples extends AllInstances with ArbitraryInstances {
  implicit val eqThrowable: Eq[Throwable] = Eq.fromUniversalEquals
}

package examples {
  case class Box[A](a: A)

  object Box {
    implicit def eqBox[A: Eq]: Eq[Box[A]] = Eq.by(_.a)

    implicit def arbitraryBox[A](implicit A: Arbitrary[A]): Arbitrary[Box[A]] =
      Arbitrary(A.arbitrary.map(Box(_)))
  }

  case class Qux[A](i: Int, a: A, j: Int)

  object Qux {
    implicit def eqQux[A: Eq]: Eq[Qux[A]] = Eq.by(q => (q.i, q.a, q.j))

    implicit def arbitraryQux[A](implicit A: Arbitrary[A]): Arbitrary[Qux[A]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
          j <- Arbitrary.arbitrary[Int]
        } yield Qux(i, a, j)
      )
  }

  case class Wub(x: Long)

  object Wub {
    implicit val eqWub: Eq[Wub] = Eq.by(_.x)

    implicit val arbitraryWub: Arbitrary[Wub] =
      Arbitrary(Arbitrary.arbitrary[Long].map(Wub(_)))
  }

  sealed trait Foo
  case class Bar(i: Int, s: String) extends Foo
  case class Baz(xs: List[String]) extends Foo
  case class Bam(w: Wub, d: Double) extends Foo

  object Baz {
    implicit val decodeBaz: Decoder[Baz] = Decoder[List[String]].map(Baz(_))
    implicit val encodeBaz: Encoder[Baz] = Encoder.instance { case Baz(xs) =>
      Json.fromValues(xs.map(Json.fromString))
    }
  }

  object Foo {
    implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals

    implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(
      Gen.oneOf(
        for {
          i <- Arbitrary.arbitrary[Int]
          s <- Arbitrary.arbitrary[String]
        } yield Bar(i, s),
        Gen.listOf(Arbitrary.arbitrary[String]).map(Baz.apply),
        for {
          w <- Arbitrary.arbitrary[Wub]
          d <- Arbitrary.arbitrary[Double]
        } yield Bam(w, d)
      )
    )
  }
}
