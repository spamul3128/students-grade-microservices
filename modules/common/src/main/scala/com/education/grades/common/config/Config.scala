package com.education.grades.common.config

import scala.concurrent.duration.FiniteDuration

case class AppConfig(
  server: ServerConfig,
  database: DatabaseConfig,
  redis: RedisConfig,
  auth: AuthConfig
)

case class ServerConfig(
  host: String,
  port: Int
)

case class DatabaseConfig(
  host: String,
  port: Int,
  database: String,
  user: String,
  password: String,
  maxConnections: Int
)

case class RedisConfig(
  host: String,
  port: Int,
  password: Option[String]
)

case class AuthConfig(
  jwtSecret: String,
  tokenExpiration: FiniteDuration
)

