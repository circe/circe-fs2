# circe-fs2

[![Build status](https://img.shields.io/travis/circe/circe-fs2/master.svg)](https://travis-ci.org/circe/circe-fs2)
[![Coverage status](https://img.shields.io/codecov/c/github/circe/circe-fs2/master.svg)](https://codecov.io/github/circe/circe-fs2)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/circe/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-fs2_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-fs2_2.12)

This project provides support for using [fs2][fs2] for streaming JSON parsing and decoding with
[circe][circe], a Scala library for encoding and decoding JSON to Scala types.

## Parsing

Circe-fs2 provides three pipes to parse your streams of JSONs:

### stringParser: `Stream[F[_], String] => Stream[F[_], Json]`

`stringParser` converts a stream of `String` to a stream of `Json`, Circe's representation of JSONs:

```scala
import io.circe.fs2._

val stringStream: Stream[Task, String] = ...

val parsedStream: Stream[Task, Json] = stringStream.through(stringParser)
```

### byteParser: `Stream[F[_], Byte] => Stream[F[_], Json]`

`byteParser` converts a stream of `Byte` to a stream of `Json`:

```scala
val byteStream: Stream[Task, Byte] = ...

val parsedStream: Stream[Task, Json] = byteStream.through(byteParser)
```

### byteParserC: `Stream[F[_], Chunk[Byte]] => Stream[F[_], Json]`

`byteParserC` converts a chunked stream of `Byte` to a stream of `Json`:

```scala
val chunkedByteStream: Stream[Task, Chunk[Byte]] = ...

val parsedStream: Stream[Task, Json] = chunkedByteStream.through(byteParserC)
```

## Decoding

Circe-fs2 comes with a `decoder` pipe which, given a `Decoder[A]`, produces a
`Stream[F[_], Json] => Stream[F[_], A]` pipe.

For example, using Circe's fully automatic derivation:

```scala
import io.circe.generic.auto._

case class Foo(a: Int, b: String)

val parsedStream: Stream[Task, Json] = ...

val decodedStream: Stream[Task, Foo] = parsedStream.through(decoder[Task, Foo])
```

## Encoding

It is also possible to go the other way around through the `encoder` pipe which, given an
`Encoder[A]`, produces a `Stream[F[_], A] => Stream[F[_], Json]` pipe.

Continuing with the previous example:

```scala
val encodedStream: Stream[Task, Json] = decodedStream.through(encoder[Task, Foo])
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
