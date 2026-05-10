CREATE DATABASE equipment_booking_system;

USE equipment_booking_system;

-- Users Table
CREATE TABLE users (
    user_id      INT IDENTITY(1,1) PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    full_name    VARCHAR(150) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('TEACHER', 'LAB_MANAGER', 'TECHNICIAN')),
    created_at   DATETIME DEFAULT GETDATE()
);

-- Equipment Table
CREATE TABLE equipment (
    equipment_id   INT IDENTITY(1,1) PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    category       VARCHAR(100),
    description    VARCHAR(MAX),
    status         VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
                   CHECK (status IN ('AVAILABLE', 'RESERVED', 'FAULTY', 'UNDER_REPAIR', 'RETIRED')),
    location       VARCHAR(150),
    created_at     DATETIME DEFAULT GETDATE(),
    updated_at     DATETIME DEFAULT GETDATE()
);

-- Bookings Table
CREATE TABLE bookings (
    booking_id     INT IDENTITY(1,1) PRIMARY KEY,
    teacher_id     INT NOT NULL,
    equipment_id   INT NOT NULL,
    booking_date   DATE NOT NULL,
    start_time     TIME NOT NULL,
    end_time       TIME NOT NULL,
    purpose        VARCHAR(MAX),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    manager_id     INT,
    created_at     DATETIME DEFAULT GETDATE(),
    updated_at     DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (teacher_id)   REFERENCES users(user_id),
    FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id),
    FOREIGN KEY (manager_id)   REFERENCES users(user_id)
);
-- Fault Reports Table
CREATE TABLE fault_reports (
    fault_id          INT IDENTITY(1,1) PRIMARY KEY,
    equipment_id      INT NOT NULL,
    reported_by       INT NOT NULL,
    fault_description VARCHAR(MAX) NOT NULL,
    severity          VARCHAR(10) NOT NULL DEFAULT 'LOW'
                      CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    status            VARCHAR(20) NOT NULL DEFAULT 'REPORTED'
                      CHECK (status IN ('REPORTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED')),
    reported_date     DATETIME DEFAULT GETDATE(),
    resolved_date     DATETIME NULL,
    FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id),
    FOREIGN KEY (reported_by)  REFERENCES users(user_id)
);

-- Maintenance Tasks Table
CREATE TABLE maintenance_tasks (
    task_id          INT IDENTITY(1,1) PRIMARY KEY,
    fault_id         INT NOT NULL,
    technician_id    INT NOT NULL,
    assigned_by      INT NOT NULL,
    priority         VARCHAR(10) NOT NULL DEFAULT 'LOW'
                     CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    notes            VARCHAR(MAX),
    assigned_at      DATETIME DEFAULT GETDATE(),
    completed_at     DATETIME NULL,
    FOREIGN KEY (fault_id)      REFERENCES fault_reports(fault_id),
    FOREIGN KEY (technician_id) REFERENCES users(user_id),
    FOREIGN KEY (assigned_by)   REFERENCES users(user_id)
);

-- Audit Log Table
CREATE TABLE audit_log (
    log_id       INT IDENTITY(1,1) PRIMARY KEY,
    user_id      INT,
    action       VARCHAR(255) NOT NULL,
    entity_type  VARCHAR(100),
    entity_id    INT,
    details      VARCHAR(MAX),
    logged_at    DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Sample Data
INSERT INTO users (username, password, full_name, email, role) VALUES
('teacher1', 'hashed_pass1', 'Alice Johnson',   'alice@university.edu',  'TEACHER'),
('teacher2', 'hashed_pass2', 'Bob Smith',       'bob@university.edu',    'TEACHER'),
('manager1', 'hashed_pass3', 'Carol Manager',   'carol@university.edu',  'LAB_MANAGER'),
('tech1',    'hashed_pass4', 'Dave Technician', 'dave@university.edu',   'TECHNICIAN'),
('tech2',    'hashed_pass5', 'Eve Technician',  'eve@university.edu',    'TECHNICIAN');

INSERT INTO equipment (name, category, description, status, location) VALUES
('Projector A',  'Projector',    'HD Projector 4K',           'AVAILABLE',    'Room 101'),
('Projector B',  'Projector',    'Standard Projector',        'AVAILABLE',    'Room 102'),
('Laptop X',     'Laptop',       'Dell Latitude i7',          'AVAILABLE',    'Lab 1'),
('Laptop Y',     'Laptop',       'HP ProBook i5',             'RESERVED',     'Lab 2'),
('Microscope',   'Lab Equipment','High-power Microscope',     'FAULTY',       'Lab 3'),
('Oscilloscope', 'Lab Equipment','Digital Oscilloscope',      'AVAILABLE',    'Lab 4'),
('Whiteboard',   'Classroom',    'Smart Whiteboard 75"',      'AVAILABLE',    'Room 201'),
('Speaker Set',  'Audio',        'Portable Bluetooth Set',    'UNDER_REPAIR', 'Storage');