# Auth Service Fix - Demo Script Login Errors Resolved

## Date: June 10, 2026

## Problem Summary
The demo.sh script was failing with login errors for all users (admin, teacher, student):
```
[ERROR] Admin login failed
[ERROR] Teacher login failed
[ERROR] Student login failed
[ERROR] Token validation failed
```

## Root Causes Identified

1. **Missing UserRepository Implementation**: The AuthServer had a stub UserRepository with `???` placeholders
2. **Password Mismatch**: Demo script used "password123" but database had "password"
3. **Database Schema Mismatch**: demo-data.sql column names didn't match actual schema
4. **Type Compatibility**: Skunk codec strict type checking required explicit casts

## Solutions Implemented

### 1. Created UserRepositoryImpl
**File**: `modules/auth-service/src/main/scala/com/education/grades/auth/repository/UserRepositoryImpl.scala`

- Implemented proper database-backed UserRepository using Skunk
- Created custom codecs for UserId, StudentId, TeacherId, and UserRoles
- Used SQL type casts (::text) to handle varchar to text conversion
- Implemented findByUsername, findById, and create methods

### 2. Updated AuthServer
**File**: `modules/auth-service/src/main/scala/com/education/grades/auth/AuthServer.scala`

- Replaced placeholder UserRepository with UserRepositoryImpl
- Added import for UserRepositoryImpl

### 3. Fixed demo.sh Password
**File**: `demo.sh`

Changed all login passwords from "password123" to "password" to match database:
- Admin: password
- Prof. Smith: password  
- John Doe: password

### 4. Fixed demo-data.sql Schema
**File**: `database/demo-data.sql`

Updated SQL to match actual database schema:
- Teachers: `first_name, last_name` instead of `name`
- Students: `first_name, last_name` instead of `name`, removed `major, gpa`
- Courses: removed `description, semester, year`
- Assignments: `assignment_type` instead of `type`, removed `description`

## Test Results

After fixes, the demo script runs successfully:

```
✅ Admin login successful
✅ Teacher login successful  
✅ Student login successful
✅ Token validation successful
```

### Demo Data Loaded
- 6 users (1 admin, 2 teachers, 3 students)
- 3 students
- 2 teachers
- 3 courses (CS101, CS201, MATH101)
- 15 assignments
- 16 grades

## Files Modified

1. `modules/auth-service/src/main/scala/com/education/grades/auth/repository/UserRepositoryImpl.scala` (NEW)
2. `modules/auth-service/src/main/scala/com/education/grades/auth/AuthServer.scala`
3. `demo.sh`
4. `database/demo-data.sql`

## Technical Details

### Skunk Type Casting
Skunk requires exact type matches. The solution was to cast varchar columns to text:
```sql
SELECT id, username::text, email::text, password_hash::text, roles, student_id, teacher_id
FROM users
WHERE username = $text
```

### User Roles Codec
Handled PostgreSQL text[] arrays for roles:
```scala
private val userRolesCodec: Codec[Set[UserRole]] = 
  _text.imap[Set[UserRole]](
    arr => arr.toList.flatMap {
      case "ADMIN" => Some(UserRole.Admin)
      case "TEACHER" => Some(UserRole.Teacher)
      case "STUDENT" => Some(UserRole.Student)
      case _ => None
    }.toSet
  )(roles => Arr.fromFoldable(roles.map(_.toString).toList))
```

## Verification

Run the demo script:
```bash
bash demo.sh
```

Or test individual login:
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq .
```

## Status: ✅ COMPLETE

All login errors in demo.sh have been resolved. The auth service now properly authenticates users against the database.

