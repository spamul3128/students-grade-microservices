package com.education.grades.auth.repository

import cats.effect.*
import cats.syntax.all.*
import skunk.*
import skunk.implicits.*
import skunk.codec.all.*
import skunk.data.Arr
import com.education.grades.auth.service.{User, UserRepository}
import com.education.grades.common.auth.{UserId, UserRole}
import com.education.grades.common.domain.{StudentId, TeacherId}
import java.util.UUID

class UserRepositoryImpl[F[_]: Concurrent](sessionPool: Resource[F, Session[F]])
  extends UserRepository[F]:

  // Codecs for custom types
  private val userIdCodec: Codec[UserId] = uuid.imap[UserId](id => UserId(id))(_.value)
  private val studentIdCodec: Codec[StudentId] = uuid.imap[StudentId](id => StudentId(id))(_.value)
  private val teacherIdCodec: Codec[TeacherId] = uuid.imap[TeacherId](id => TeacherId(id))(_.value)

  private val userRolesCodec: Codec[Set[UserRole]] =
    _text.imap[Set[UserRole]](
      arr => arr.toList.flatMap {
        case "ADMIN" => Some(UserRole.Admin)
        case "TEACHER" => Some(UserRole.Teacher)
        case "STUDENT" => Some(UserRole.Student)
        case _ => None
      }.toSet
    )(roles => Arr.fromFoldable(roles.map(_.toString).toList))

  // SQL Queries
  private val findByUsernameQuery: Query[String, User] =
    sql"""
      SELECT id, username::text, email::text, password_hash::text, roles, student_id, teacher_id
      FROM users
      WHERE username = $text
    """.query(
      userIdCodec *: text *: text *: text *: userRolesCodec *:
      studentIdCodec.opt *: teacherIdCodec.opt
    ).to[User]

  private val findByIdQuery: Query[UserId, User] =
    sql"""
      SELECT id, username::text, email::text, password_hash::text, roles, student_id, teacher_id
      FROM users
      WHERE id = $userIdCodec
    """.query(
      userIdCodec *: text *: text *: text *: userRolesCodec *:
      studentIdCodec.opt *: teacherIdCodec.opt
    ).to[User]

  private val insertUserCommand: Command[User] =
    sql"""
      INSERT INTO users (id, username, email, password_hash, roles, student_id, teacher_id, created_at)
      VALUES ($userIdCodec, $text, $text, $text, $userRolesCodec, ${studentIdCodec.opt}, ${teacherIdCodec.opt}, NOW())
    """.command.contramap { (user: User) =>
      (user.id, user.username, user.email, user.passwordHash, user.roles, user.studentId, user.teacherId)
    }

  override def findByUsername(username: String): F[Option[User]] =
    sessionPool.use { session =>
      session.prepare(findByUsernameQuery).flatMap { ps =>
        ps.option(username)
      }
    }

  override def findById(id: UserId): F[Option[User]] =
    sessionPool.use { session =>
      session.prepare(findByIdQuery).flatMap { ps =>
        ps.option(id)
      }
    }

  override def create(user: User): F[User] =
    sessionPool.use { session =>
      session.prepare(insertUserCommand).flatMap { cmd =>
        cmd.execute(user).map(_ => user)
      }
    }

