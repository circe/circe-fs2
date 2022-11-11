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

import cats.effect.testing.scalatest.AssertingSyntax
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.testing.scalatest.EffectTestSupport
import cats.instances.AllInstances
import cats.syntax.AllSyntax
import cats.syntax.EitherOps
import io.circe.testing.ArbitraryInstances
import io.circe.testing.EqInstances
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatestplus.scalacheck.Checkers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.typelevel.discipline.Laws

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceSuite
    extends AsyncFlatSpec
    with AsyncIOSpec
    with AssertingSyntax
    with EffectTestSupport
    with ScalaCheckDrivenPropertyChecks
    with AllInstances
    with AllSyntax
    with ArbitraryInstances
    with EqInstances {
  override def convertToEqualizer[T](left: T): Equalizer[T] =
    sys.error("Intentionally ambiguous implicit for Equalizer")

  implicit def prioritizedCatsSyntaxEither[A, B](eab: Either[A, B]): EitherOps[A, B] = new EitherOps(eab)

  def checkLaws(name: String, ruleSet: Laws#RuleSet): Unit = ruleSet.all.properties.zipWithIndex.foreach {
    case ((id, prop), 0) => name should s"obey $id" in Checkers.check(prop)
    case ((id, prop), _) => it should s"obey $id" in Checkers.check(prop)
  }
}
