-- Demo Test Data for Student Grades Microservices
-- This provides a complete dataset for testing all services

-- Clear existing data (for clean demo)
TRUNCATE TABLE grades CASCADE;
TRUNCATE TABLE assignments CASCADE;
TRUNCATE TABLE courses CASCADE;
TRUNCATE TABLE teachers CASCADE;
TRUNCATE TABLE students CASCADE;
TRUNCATE TABLE users CASCADE;

-- Insert Users (for authentication)
INSERT INTO users (id, username, email, password_hash, roles, created_at) VALUES
  ('a1111111-1111-1111-1111-111111111111', 'admin', 'admin@school.edu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', ARRAY['ADMIN']::text[], NOW()),
  ('b2222222-2222-2222-2222-222222222222', 'prof.smith', 'smith@school.edu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', ARRAY['TEACHER']::text[], NOW()),
  ('b3333333-3333-3333-3333-333333333333', 'prof.jones', 'jones@school.edu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', ARRAY['TEACHER']::text[], NOW()),
  ('c4444444-4444-4444-4444-444444444444', 'john.doe', 'john.doe@school.edu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', ARRAY['STUDENT']::text[], NOW()),
  ('c5555555-5555-5555-5555-555555555555', 'jane.smith', 'jane.smith@school.edu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', ARRAY['STUDENT']::text[], NOW()),
  ('c6666666-6666-6666-6666-666666666666', 'bob.johnson', 'bob.johnson@school.edu', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', ARRAY['STUDENT']::text[], NOW());

-- Insert Teachers
INSERT INTO teachers (id, first_name, last_name, email, department, created_at) VALUES
  ('b2222222-2222-2222-2222-222222222222', 'Sarah', 'Smith', 'smith@school.edu', 'Computer Science', NOW()),
  ('b3333333-3333-3333-3333-333333333333', 'Michael', 'Jones', 'jones@school.edu', 'Mathematics', NOW());

-- Update users with teacher_id
UPDATE users SET teacher_id = 'b2222222-2222-2222-2222-222222222222' WHERE username = 'prof.smith';
UPDATE users SET teacher_id = 'b3333333-3333-3333-3333-333333333333' WHERE username = 'prof.jones';

-- Insert Students
INSERT INTO students (id, first_name, last_name, email, enrollment_date, created_at) VALUES
  ('c4444444-4444-4444-4444-444444444444', 'John', 'Doe', 'john.doe@school.edu', '2024-09-01', NOW()),
  ('c5555555-5555-5555-5555-555555555555', 'Jane', 'Smith', 'jane.smith@school.edu', '2024-09-01', NOW()),
  ('c6666666-6666-6666-6666-666666666666', 'Bob', 'Johnson', 'bob.johnson@school.edu', '2024-09-01', NOW());

-- Update users with student_id
UPDATE users SET student_id = 'c4444444-4444-4444-4444-444444444444' WHERE username = 'john.doe';
UPDATE users SET student_id = 'c5555555-5555-5555-5555-555555555555' WHERE username = 'jane.smith';
UPDATE users SET student_id = 'c6666666-6666-6666-6666-666666666666' WHERE username = 'bob.johnson';

-- Insert Courses
INSERT INTO courses (id, code, name, teacher_id, credits, created_at) VALUES
  ('d1111111-1111-1111-1111-111111111111', 'CS101', 'Introduction to Programming', 'b2222222-2222-2222-2222-222222222222', 3, NOW()),
  ('d2222222-2222-2222-2222-222222222222', 'CS201', 'Data Structures', 'b2222222-2222-2222-2222-222222222222', 4, NOW()),
  ('d3333333-3333-3333-3333-333333333333', 'MATH101', 'Calculus I', 'b3333333-3333-3333-3333-333333333333', 4, NOW());

-- Insert Assignments
INSERT INTO assignments (id, course_id, name, assignment_type, max_score, weight, due_date, created_at) VALUES
  -- CS101 Assignments
  ('e1111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'Homework 1', 'HOMEWORK', 100, 0.10, '2024-09-15 23:59:00', NOW()),
  ('e1111112-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'Quiz 1', 'QUIZ', 50, 0.15, '2024-09-20 14:00:00', NOW()),
  ('e1111113-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'Midterm Exam', 'EXAM', 100, 0.30, '2024-10-15 10:00:00', NOW()),
  ('e1111114-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'Final Project', 'PROJECT', 100, 0.35, '2024-12-10 23:59:00', NOW()),
  ('e1111115-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'Participation', 'PARTICIPATION', 100, 0.10, '2024-12-15 23:59:00', NOW()),

  -- CS201 Assignments
  ('e2222221-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'Array Assignment', 'HOMEWORK', 100, 0.15, '2024-09-22 23:59:00', NOW()),
  ('e2222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'Linked List Quiz', 'QUIZ', 50, 0.10, '2024-09-29 14:00:00', NOW()),
  ('e2222223-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'Tree Algorithms', 'HOMEWORK', 100, 0.20, '2024-10-20 23:59:00', NOW()),
  ('e2222224-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'Final Exam', 'EXAM', 100, 0.35, '2024-12-12 10:00:00', NOW()),
  ('e2222225-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'Graph Project', 'PROJECT', 100, 0.20, '2024-12-08 23:59:00', NOW()),

  -- MATH101 Assignments
  ('e3333331-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'Homework 1', 'HOMEWORK', 100, 0.15, '2024-09-18 23:59:00', NOW()),
  ('e3333332-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'Quiz 1', 'QUIZ', 50, 0.10, '2024-09-25 14:00:00', NOW()),
  ('e3333333-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'Midterm', 'EXAM', 100, 0.30, '2024-10-18 10:00:00', NOW()),
  ('e3333334-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'Integration HW', 'HOMEWORK', 100, 0.15, '2024-11-15 23:59:00', NOW()),
  ('e3333335-3333-3333-3333-333333333333', 'd3333333-3333-3333-3333-333333333333', 'Final Exam', 'EXAM', 100, 0.30, '2024-12-13 10:00:00', NOW());

-- Insert Grades for Students

-- John Doe's grades (CS101)
INSERT INTO grades (id, student_id, assignment_id, score, submitted_at, graded_at, graded_by, comments) VALUES
  (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'e1111111-1111-1111-1111-111111111111', 85, '2024-09-14 18:30:00', '2024-09-16 10:00:00', 'b2222222-2222-2222-2222-222222222222', 'Good work!'),
  (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'e1111112-1111-1111-1111-111111111111', 42, '2024-09-20 13:45:00', '2024-09-21 09:00:00', 'b2222222-2222-2222-2222-222222222222', 'Decent understanding'),
  (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'e1111113-1111-1111-1111-111111111111', 78, '2024-10-15 10:00:00', '2024-10-17 14:00:00', 'b2222222-2222-2222-2222-222222222222', 'Good exam performance');

-- John Doe's grades (CS201)
INSERT INTO grades (id, student_id, assignment_id, score, submitted_at, graded_at, graded_by, comments) VALUES
  (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'e2222221-2222-2222-2222-222222222222', 90, '2024-09-21 20:00:00', '2024-09-23 11:00:00', 'b2222222-2222-2222-2222-222222222222', 'Excellent arrays work'),
  (gen_random_uuid(), 'c4444444-4444-4444-4444-444444444444', 'e2222222-2222-2222-2222-222222222222', 45, '2024-09-29 13:50:00', '2024-09-30 09:00:00', 'b2222222-2222-2222-2222-222222222222', 'Good quiz');

-- Jane Smith's grades (CS101) - Top student
INSERT INTO grades (id, student_id, assignment_id, score, submitted_at, graded_at, graded_by, comments) VALUES
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'e1111111-1111-1111-1111-111111111111', 95, '2024-09-13 15:20:00', '2024-09-16 10:30:00', 'b2222222-2222-2222-2222-222222222222', 'Excellent work!'),
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'e1111112-1111-1111-1111-111111111111', 48, '2024-09-20 13:30:00', '2024-09-21 09:15:00', 'b2222222-2222-2222-2222-222222222222', 'Perfect understanding'),
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'e1111113-1111-1111-1111-111111111111', 92, '2024-10-15 10:00:00', '2024-10-17 14:30:00', 'b2222222-2222-2222-2222-222222222222', 'Outstanding!');

-- Jane Smith's grades (MATH101)
INSERT INTO grades (id, student_id, assignment_id, score, submitted_at, graded_at, graded_by, comments) VALUES
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'e3333331-3333-3333-3333-333333333333', 88, '2024-09-17 19:00:00', '2024-09-19 10:00:00', 'b3333333-3333-3333-3333-333333333333', 'Strong math skills'),
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'e3333332-3333-3333-3333-333333333333', 46, '2024-09-25 13:45:00', '2024-09-26 09:00:00', 'b3333333-3333-3333-3333-333333333333', 'Excellent'),
  (gen_random_uuid(), 'c5555555-5555-5555-5555-555555555555', 'e3333333-3333-3333-3333-333333333333', 90, '2024-10-18 10:00:00', '2024-10-20 11:00:00', 'b3333333-3333-3333-3333-333333333333', 'Great midterm!');

-- Bob Johnson's grades (CS101)
INSERT INTO grades (id, student_id, assignment_id, score, submitted_at, graded_at, graded_by, comments) VALUES
  (gen_random_uuid(), 'c6666666-6666-6666-6666-666666666666', 'e1111111-1111-1111-1111-111111111111', 72, '2024-09-15 22:45:00', '2024-09-16 11:00:00', 'b2222222-2222-2222-2222-222222222222', 'Keep practicing'),
  (gen_random_uuid(), 'c6666666-6666-6666-6666-666666666666', 'e1111112-1111-1111-1111-111111111111', 35, '2024-09-20 14:00:00', '2024-09-21 09:30:00', 'b2222222-2222-2222-2222-222222222222', 'Needs improvement'),
  (gen_random_uuid(), 'c6666666-6666-6666-6666-666666666666', 'e1111113-1111-1111-1111-111111111111', 68, '2024-10-15 10:00:00', '2024-10-17 15:00:00', 'b2222222-2222-2222-2222-222222222222', 'Satisfactory');

-- Bob Johnson's grades (MATH101)
INSERT INTO grades (id, student_id, assignment_id, score, submitted_at, graded_at, graded_by, comments) VALUES
  (gen_random_uuid(), 'c6666666-6666-6666-6666-666666666666', 'e3333331-3333-3333-3333-333333333333', 75, '2024-09-18 20:00:00', '2024-09-19 10:30:00', 'b3333333-3333-3333-3333-333333333333', 'Good effort'),
  (gen_random_uuid(), 'c6666666-6666-6666-6666-666666666666', 'e3333332-3333-3333-3333-333333333333', 38, '2024-09-25 13:55:00', '2024-09-26 09:15:00', 'b3333333-3333-3333-3333-333333333333', 'Average performance');

-- Summary
SELECT 'Demo data loaded successfully!' as message;
SELECT COUNT(*) as user_count FROM users;
SELECT COUNT(*) as student_count FROM students;
SELECT COUNT(*) as teacher_count FROM teachers;
SELECT COUNT(*) as course_count FROM courses;
SELECT COUNT(*) as assignment_count FROM assignments;
SELECT COUNT(*) as grade_count FROM grades;

