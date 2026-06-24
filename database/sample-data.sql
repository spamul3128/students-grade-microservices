-- Sample data for testing

-- Insert sample students
INSERT INTO students (id, first_name, last_name, email, enrollment_date) VALUES
('550e8400-e29b-41d4-a716-446655440001'::uuid, 'John', 'Doe', 'john.doe@university.edu', '2023-09-01 00:00:00'),
('550e8400-e29b-41d4-a716-446655440002'::uuid, 'Jane', 'Smith', 'jane.smith@university.edu', '2023-09-01 00:00:00'),
('550e8400-e29b-41d4-a716-446655440003'::uuid, 'Alice', 'Johnson', 'alice.j@university.edu', '2023-09-01 00:00:00');

-- Insert sample teachers
INSERT INTO teachers (id, first_name, last_name, email, department) VALUES
('660e8400-e29b-41d4-a716-446655440001'::uuid, 'Dr. Robert', 'Williams', 'r.williams@university.edu', 'Computer Science'),
('660e8400-e29b-41d4-a716-446655440002'::uuid, 'Prof. Sarah', 'Davis', 's.davis@university.edu', 'Mathematics');

-- Insert sample courses
INSERT INTO courses (id, name, code, credits, teacher_id) VALUES
('770e8400-e29b-41d4-a716-446655440001'::uuid, 'Introduction to Computer Science', 'CS101', 3, '660e8400-e29b-41d4-a716-446655440001'::uuid),
('770e8400-e29b-41d4-a716-446655440002'::uuid, 'Calculus I', 'MATH101', 4, '660e8400-e29b-41d4-a716-446655440002'::uuid);

-- Insert sample assignments
INSERT INTO assignments (id, course_id, name, max_score, weight, due_date, assignment_type) VALUES
('880e8400-e29b-41d4-a716-446655440001'::uuid, '770e8400-e29b-41d4-a716-446655440001'::uuid, 'Assignment 1', 100, 0.2, '2024-01-15 23:59:59', 'Homework'),
('880e8400-e29b-41d4-a716-446655440002'::uuid, '770e8400-e29b-41d4-a716-446655440001'::uuid, 'Midterm Exam', 100, 0.3, '2024-02-20 23:59:59', 'Exam'),
('880e8400-e29b-41d4-a716-446655440003'::uuid, '770e8400-e29b-41d4-a716-446655440001'::uuid, 'Final Project', 100, 0.5, '2024-04-30 23:59:59', 'Project');

-- Insert sample grades
INSERT INTO grades (id, student_id, assignment_id, score, submitted_at, graded_at, graded_by) VALUES
('990e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, 95, '2024-01-14 20:30:00', '2024-01-15 10:00:00', '660e8400-e29b-41d4-a716-446655440001'::uuid),
('990e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440002'::uuid, 87, '2024-02-20 19:00:00', '2024-02-21 14:00:00', '660e8400-e29b-41d4-a716-446655440001'::uuid);

-- Insert sample users (passwords are 'password123' bcrypt hashed)
INSERT INTO users (id, username, email, password_hash, roles, student_id) VALUES
('aa0e8400-e29b-41d4-a716-446655440001'::uuid, 'johndoe', 'john.doe@university.edu', '$2a$10$YourBcryptHashHere', ARRAY['Student'], '550e8400-e29b-41d4-a716-446655440001'::uuid);

INSERT INTO users (id, username, email, password_hash, roles, teacher_id) VALUES
('aa0e8400-e29b-41d4-a716-446655440002'::uuid, 'profwilliams', 'r.williams@university.edu', '$2a$10$YourBcryptHashHere', ARRAY['Teacher'], '660e8400-e29b-41d4-a716-446655440001'::uuid);

