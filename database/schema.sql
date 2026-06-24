-- Students Grade Management Database Schema

-- Students table
CREATE TABLE students (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    enrollment_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Teachers table
CREATE TABLE teachers (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    department VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Courses table
CREATE TABLE courses (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(20) UNIQUE NOT NULL,
    credits INT NOT NULL CHECK (credits > 0),
    teacher_id UUID NOT NULL REFERENCES teachers(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Assignments table
CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id),
    name VARCHAR(200) NOT NULL,
    max_score DOUBLE PRECISION NOT NULL CHECK (max_score > 0),
    weight DOUBLE PRECISION NOT NULL CHECK (weight >= 0 AND weight <= 1),
    due_date TIMESTAMP NOT NULL,
    assignment_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Grades table
CREATE TABLE grades (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id),
    assignment_id UUID NOT NULL REFERENCES assignments(id),
    score DOUBLE PRECISION NOT NULL CHECK (score >= 0),
    submitted_at TIMESTAMP NOT NULL,
    graded_at TIMESTAMP,
    graded_by UUID REFERENCES teachers(id),
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, assignment_id)
);

-- Course grades table
CREATE TABLE course_grades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id),
    course_id UUID NOT NULL REFERENCES courses(id),
    numeric_grade DOUBLE PRECISION NOT NULL CHECK (numeric_grade >= 0 AND numeric_grade <= 100),
    letter_grade VARCHAR(3) NOT NULL,
    gpa DOUBLE PRECISION NOT NULL CHECK (gpa >= 0 AND gpa <= 4.0),
    semester VARCHAR(20) NOT NULL,
    academic_year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, course_id, semester, academic_year)
);

-- Users table (for authentication)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    roles TEXT[] NOT NULL,
    student_id UUID REFERENCES students(id),
    teacher_id UUID REFERENCES teachers(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_grades_student_id ON grades(student_id);
CREATE INDEX idx_grades_assignment_id ON grades(assignment_id);
CREATE INDEX idx_assignments_course_id ON assignments(course_id);
CREATE INDEX idx_course_grades_student_id ON course_grades(student_id);
CREATE INDEX idx_course_grades_course_id ON course_grades(course_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

