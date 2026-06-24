package com.education.grades.common.infrastructure.db

import cats.effect.*
import cats.syntax.all.*
import cats.effect.std.Console
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import natchez.Trace.Implicits.noop
import com.education.grades.common.config.DatabaseConfig
import fs2.io.net.Network

object Database:

  def sessionResource[F[_]: Async: Network: Console](config: DatabaseConfig): Resource[F, Session[F]] =
    Session.single[F](
      host = config.host,
      port = config.port,
      user = config.user,
      database = config.database,
      password = Some(config.password)
    )

  def sessionPool[F[_]: Async: Network: Console](config: DatabaseConfig): Resource[F, Resource[F, Session[F]]] =
    Session.pooled[F](
      host = config.host,
      port = config.port,
      user = config.user,
      database = config.database,
      password = Some(config.password),
      max = config.maxConnections
    )

// Base repository trait
trait Repository[F[_], ID, Entity]:
  def findById(id: ID): F[Option[Entity]]
  def findAll: F[List[Entity]]
  def create(entity: Entity): F[Entity]
  def update(entity: Entity): F[Option[Entity]]
  def delete(id: ID): F[Boolean]

