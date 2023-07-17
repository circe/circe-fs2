### Deprecated

This library is now in maintenance-mode. We recommend to use [fs2-data-json-circe](https://fs2-data.gnieh.org/documentation/json/libraries/#circe) which provides a [migration guide](https://fs2-data.gnieh.org/documentation/json/libraries/#migrating-from-circe-fs2).

# circe-fs2

[![Build](https://github.com/circe/circe-fs2/workflows/Continuous%20Integration/badge.svg)](https://github.com/circe/circe-fs2/actions)
[![Coverage status](https://img.shields.io/codecov/c/github/circe/circe-fs2/master.svg)](https://codecov.io/github/circe/circe-fs2)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/circe/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-fs2_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-fs2_2.12)

This project provides support for using [fs2][fs2] for streaming JSON parsing and decoding with
[circe][circe], a Scala library for encoding and decoding JSON to Scala types.

## Parsing

Circe-fs2 provides different pipes to parse your streams of JSONs depending on whether your stream
is:

- a \n-separated stream of JSON values or value stream:

```json
{ "repo": "circe-fs2", "stars": 14 }
{ "repo": "circe-config", "stars": 5 }
```

- or a JSON array:

```json
[
  { "repo": "circe-fs2", "stars": 14 },
  { "repo": "circe-config", "stars": 5 }
]
```

The appropriate pipe for the job also depends on your input stream value type (i.e. `String`, `Byte`
or `Chunk[Byte]`).

The following table sums up every pipe available as a function of the input stream value type as
well as the JSON structure:

|                |String              |Byte              |Chunk[Byte]      |
|----------------|--------------------|------------------|-------------------|
|__Value stream__|`stringStreamParser`|`byteStreamParser`|`byteStreamParserC`|
|__Array__       |`stringArrayParser` |`byteArrayParser` |`byteArrayParserC` |

As an example, let's say we have a stream of strings representing a JSON array, we'll
pick the `stringArrayParser` pipe which converts a stream of `String` to a stream of `Json`, Circe's
representation of JSONs:

```scala
import io.circe.fs2._
val stringStream: Stream[IO, String] = ...
val parsedStream: Stream[IO, Json] = stringStream.through(stringArrayParser)
```

## Decoding

Circe-fs2 also comes with a `decoder` pipe which, given a `Decoder[A]`, produces a
`Stream[F[_], Json] => Stream[F[_], A]` pipe.

For example, using Circe's fully automatic derivation:

```scala
import io.circe.generic.auto._
case class Foo(a: Int, b: String)
val parsedStream: Stream[IO, Json] = ...
val decodedStream: Stream[IO, Foo] = parsedStream.through(decoder[IO, Foo])
```

## Contributors and participation

All circe projects support the [Typelevel][typelevel] [code of conduct][code-of-conduct] and we want
all of their channels (Gitter, GitHub, etc.) to be welcoming environments for everyone.

Please see the [circe contributors' guide][contributing] for details on how to submit a pull
request.

## License

circe-fs2 is licensed under the **[Apache License, Version 2.0][apache]**
(the "License"); you may not use this software except in compliance with the
License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
[circe]: https://github.com/circe/circe
[code-of-conduct]: http://typelevel.org/conduct.html
[contributing]: https://circe.github.io/circe/contributing.html
[fs2]: https://github.com/functional-streams-for-scala/fs2
[typelevel]: http://typelevel.org/
