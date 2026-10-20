-- Database Schema for Smart Placement and Internship Management System
-- Compatible with Oracle / MySQL / PostgreSQL (Standard SQL DDL)

-- 1. Students Table
CREATE TABLE students (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    roll_no VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    department VARCHAR(50),
    gpa DECIMAL(4,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Companies Table
CREATE TABLE companies (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    industry VARCHAR(100),
    website VARCHAR(200),
    access_granted_by INT, -- Admin ID who granted access
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Admins Table
CREATE TABLE admins (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Opportunities (Internships & Placements) Table
CREATE TABLE opportunities (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id INT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'INTERNSHIP' or 'PLACEMENT'
    duration VARCHAR(50),      -- e.g., '6 Months', 'Full-time'
    eligible_departments VARCHAR(255), -- e.g., 'CSE, IT, ECE'
    location VARCHAR(100),     -- e.g., 'Bangalore', 'Remote', 'Chennai'
    compensation VARCHAR(100), -- e.g., '₹50,000 / month' or '14 LPA'
    min_gpa DECIMAL(4,2) DEFAULT 0.0,
    deadline DATE NOT NULL,
    drive_date DATE,
    description CLOB,
    status VARCHAR(20) DEFAULT 'OPEN', -- 'OPEN', 'CLOSED', 'EXPIRED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_opp_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- 5. Applications Table
CREATE TABLE applications (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id INT NOT NULL,
    opportunity_id INT NOT NULL,
    applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) DEFAULT 'APPLIED', -- 'APPLIED', 'SHORTLISTED', 'REJECTED', 'OFFERED'
    CONSTRAINT fk_app_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_app_opp FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_opp UNIQUE (student_id, opportunity_id)
);

-- 6. Calendar Events Table
CREATE TABLE calendar_events (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id INT NOT NULL,
    opportunity_id INT,
    event_title VARCHAR(150) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- 'DEADLINE', 'ONLINE_TEST', 'TECHNICAL_INTERVIEW', 'HR_ROUND'
    event_date DATE NOT NULL,
    event_time VARCHAR(20),
    details VARCHAR(255),
    CONSTRAINT fk_cal_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

-- 7. Past Opportunities & Interview Experience Table
CREATE TABLE past_experiences (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    role VARCHAR(100) NOT NULL,
    year_hired INT NOT NULL,
    opportunity_type VARCHAR(20), -- 'INTERNSHIP' or 'PLACEMENT'
    package_offered VARCHAR(100),
    rounds_count INT DEFAULT 3,
    online_test_details CLOB, -- Topics, platforms, difficulty
    technical_interview_details CLOB, -- DSA, Core CS, System Design questions
    hr_round_details CLOB, -- Behavioral questions & tips
    preparation_tips CLOB,
    contributor_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sample Initial Admin
INSERT INTO admins (username, email, password, name) 
VALUES ('admin', 'admin@annauniv.edu', 'Admin@1234', 'Super Admin');
