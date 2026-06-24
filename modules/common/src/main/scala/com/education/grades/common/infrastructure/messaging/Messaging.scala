package com.education.grades.common.infrastructure.messaging

import cats.effect.*
import cats.syntax.all.*
import dev.profunktor.redis4cats.*
import dev.profunktor.redis4cats.effect.Log.Stdout.given
import dev.profunktor.redis4cats.pubsub.{PubSub, PubSubCommands}
import dev.profunktor.redis4cats.data.RedisChannel
import fs2.Stream
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax.*
import com.education.grades.common.config.RedisConfig

trait MessagePublisher[F[_]]:
  def publish(channel: String, message: String): F[Unit]

trait MessageSubscriber[F[_]]:
  def subscribe(channel: String): Stream[F, String]

object RedisMessaging:

  def create[F[_]: Async](config: RedisConfig): Resource[F, (MessagePublisher[F], MessageSubscriber[F])] =
    val uri = config.password match
      case Some(pwd) => s"redis://:$pwd@${config.host}:${config.port}"
      case None => s"redis://${config.host}:${config.port}"

    import dev.profunktor.redis4cats.connection.RedisClient
    import dev.profunktor.redis4cats.data.RedisCodec

    for {
      client <- RedisClient[F].from(uri)
      codec = RedisCodec.Utf8
      pubSubStats <- PubSub.mkPubSubConnection[F, String, String](client, codec)
    } yield {
      val publisher = new MessagePublisher[F]:
        def publish(channel: String, message: String): F[Unit] =
          Stream.emit(message)
            .through(pubSubStats.publish(RedisChannel(channel)))
            .compile
            .drain

      val subscriber = new MessageSubscriber[F]:
        def subscribe(channel: String): Stream[F, String] =
          pubSubStats.subscribe(RedisChannel(channel))

      (publisher, subscriber)
    }

// Event publisher with JSON serialization
class EventPublisher[F[_]: Async](publisher: MessagePublisher[F]):

  def publishEvent[E: Encoder](channel: String, event: E): F[Unit] =
    val json = event.asJson.noSpaces
    publisher.publish(channel, json)

// Event subscriber with JSON deserialization
class EventSubscriber[F[_]: Async](subscriber: MessageSubscriber[F]):

  def subscribeToEvents[E: Decoder](channel: String): Stream[F, Either[io.circe.Error, E]] =
    subscriber.subscribe(channel).map(decode[E])

