package io.circe.fs2

import cats.syntax.{ AllSyntax, EitherOps }
import munit.{ CatsEffectSuite, ScalaCheckEffectSuite }

import scala.language.implicitConversions

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceSuite extends CatsEffectSuite with ScalaCheckEffectSuite with AllSyntax {
  implicit def prioritizedCatsSyntaxEither[A, B](eab: Either[A, B]): EitherOps[A, B] = new EitherOps(eab)
}
