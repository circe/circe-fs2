package io.circe.fs2

import fs2.{Pipe, Stream}
import io.circe.Encoder
import io.circe.syntax._
import shapeless._
import shapeless.labelled.FieldType

object encoding {
  def jsonArrayString[F[_], T: Encoder]: Pipe[F, T, String] =
    stream => Stream.emit("[") ++ stream.map(t => t.asJson.noSpaces).intersperse(",") ++ Stream.emit("]")

  trait StreamEncoder[F[_], A] {
    def encode: A => Stream[F, String]
  }

  object StreamEncoder extends LowPriorityImplicits {

    def instance[F[_], A](f: A => Stream[F, String]): StreamEncoder[F, A] =
      new StreamEncoder[F, A] { def encode: A => Stream[F, String] = f }

    def apply[F[_], A](implicit enc: StreamEncoder[F, A]): StreamEncoder[F, A] = enc

    implicit def stream[F[_], A: Encoder]: StreamEncoder[F, Stream[F, A]] = StreamEncoder.instance(jsonArrayString)

    implicit def fromOption[F[_], A](implicit enc: StreamEncoder[F, A]): StreamEncoder[F, Option[A]] =
      StreamEncoder.instance(_.fold[Stream[F, String]](Stream("null"))(enc.encode))

    implicit def fromEncoder[F[_], A: Encoder]: StreamEncoder[F, A] = StreamEncoder.instance(a => Stream.emit(a.asJson.noSpaces))
  }

  trait LowPriorityImplicits {

    //    TODO: make coproducts work
    //    implicit def cnilEncoder[F[_]]: StreamEncoder[F, CNil] =
    //      StreamEncoder.instance(_ => throw new Exception("Inconceivable!"))
    //
    //    implicit def coproductEncoder[F[_], H, T <: Coproduct](
    //      implicit
    //      hEncoder: Lazy[StreamEncoder[F, H]],
    //      tEncoder: StreamEncoder[F, T]
    //    ): StreamEncoder[F, H :+: T] = StreamEncoder.instance {
    //      case Inl(h) => hEncoder.value.encode(h)
    //      case Inr(t) => tEncoder.encode(t)
    //    }

    implicit def hnilEncoder[F[_]]: StreamEncoder[F, HNil] =
      StreamEncoder.instance(_ => Stream.empty)

    implicit def hlistObjectEncoder[F[_], K <: Symbol, H, T <: HList](
                                                                       implicit
                                                                       witness: Witness.Aux[K],
                                                                       hEncoder: Lazy[StreamEncoder[F, H]],
                                                                       tEncoder: StreamEncoder[F, T]
                                                                     ): StreamEncoder[F, FieldType[K, H] :: T] = {
      val fieldName = witness.value.name
      StreamEncoder.instance {
        case h :: t =>
          val head = hEncoder.value.encode(h)
          val tail = tEncoder.encode(t)
          val comma = t match {
            case HNil => Stream.empty
            case _    => Stream.emit(",")
          }
          Stream.emit(s""""$fieldName":""") ++ head ++ comma ++ tail
      }
    }

    implicit def genericObjectEncoder[F[_], A, H](
                                                   implicit
                                                   generic: LabelledGeneric.Aux[A, H],
                                                   hEncoder: Lazy[StreamEncoder[F, H]]
                                                 ): StreamEncoder[F, A] =
      StreamEncoder.instance { value =>
        hEncoder.value.encode(generic.to(value)).cons1("{") ++ Stream("}")
      }

  }

  object syntax {
    implicit class StreamEncoderSyntax[A](self: A) {
      def asJsonStream[F[_]](implicit enc: StreamEncoder[F, A]): Stream[F, String] = enc.encode(self)
    }
  }
}
