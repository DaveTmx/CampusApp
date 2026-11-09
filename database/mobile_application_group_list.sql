-- MySQL import script generated from Mobile application Group List.xlsx
-- Review source-data inconsistencies before using this data in a live system.
CREATE DATABASE IF NOT EXISTS mobile_application_groups;
USE mobile_application_groups;

CREATE TABLE IF NOT EXISTS student_groups (
    group_id INT AUTO_INCREMENT PRIMARY KEY,
    group_number INT NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    list_number INT NOT NULL,
    first_name VARCHAR(120),
    last_name VARCHAR(120),
    student_number VARCHAR(30),
    gender VARCHAR(20),
    programme VARCHAR(120),
    CONSTRAINT fk_students_group FOREIGN KEY (group_id)
        REFERENCES student_groups(group_id),
    INDEX idx_student_number (student_number),
    INDEX idx_group_list (group_id, list_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Groups
INSERT IGNORE INTO student_groups (group_number) VALUES (1);
INSERT IGNORE INTO student_groups (group_number) VALUES (2);
INSERT IGNORE INTO student_groups (group_number) VALUES (3);
INSERT IGNORE INTO student_groups (group_number) VALUES (4);
INSERT IGNORE INTO student_groups (group_number) VALUES (5);
INSERT IGNORE INTO student_groups (group_number) VALUES (6);
INSERT IGNORE INTO student_groups (group_number) VALUES (7);
INSERT IGNORE INTO student_groups (group_number) VALUES (8);
INSERT IGNORE INTO student_groups (group_number) VALUES (9);
INSERT IGNORE INTO student_groups (group_number) VALUES (10);
INSERT IGNORE INTO student_groups (group_number) VALUES (11);
INSERT IGNORE INTO student_groups (group_number) VALUES (12);
INSERT IGNORE INTO student_groups (group_number) VALUES (13);
INSERT IGNORE INTO student_groups (group_number) VALUES (14);
INSERT IGNORE INTO student_groups (group_number) VALUES (15);

-- Student records
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 1, 'Mulemwa', 'Muchima', '202402387', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 2, 'Edwin', 'Kampamba', '202406643', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 3, 'Ivwananji', 'Chipasha', '202400442', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 4, 'Alinani', 'Sinyangwe', '202305208', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 5, 'Grace', 'Chilufya', '202408399', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 6, 'Charity', 'Namangolwa', '202402615', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 7, 'Memory', 'Mutemwa', '202404528', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 8, 'Mwila', 'Siaciti', '202409048', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 9, 'Hope', 'Nsana', '202402859', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 10, 'Judith', 'Mushabati', '202403063', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 11, 'Mcnaldo', 'Chilanga', '202406887', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 12, 'Collins', 'Kalela', '202403266', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 13, 'Nancy', 'Mailoni', '202409163', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 14, 'Onester', 'Kalunga', '202408720', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 15, 'Mayamiko', 'Phiri', '202409779', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 1), 16, 'Atupele', 'Andende', '202408882', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 1, 'JOSEPH', 'CHIWUSWA', '202400549', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 2, 'MALUBA', 'B AKAKANDELWA', '202400526', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 3, 'EMMANUEL', 'VIAZI', '202403134', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 4, 'MEMORY', 'TEMBO', '202408701', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 5, 'SALOME', 'SITWALA', '202401737', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 6, 'MWANSA', 'MUBANGA', '202405690', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 7, 'PAUL', 'MUNENE', '202404540', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 8, 'BLESSINGS', 'SINKAMBA', '202408808', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 9, 'JOSHUA', 'CHIBALE', '202308808', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 10, 'BLESSINGS', 'TEMBO', '202408439', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 11, 'MWAKA', 'HAMILEMBA', '202402633', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 12, 'LUUMBI', 'MALAMBO', '202408554', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 13, 'LISELI FAUSTINA', 'KALOWA', '202406948', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 14, 'LUTE WALUSUNGU', 'GONDWE', '202407170', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 2), 15, 'MATILDAH', 'YUSIKU', '202405590', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 1, 'GIFT', 'CHABUKA', '202404194', 'F', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 2, 'RUTH', 'KANGWA', '202307232', 'F', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 3, 'JASON', 'MBAO', '202406919', 'M', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 4, 'CHIKO', 'MUTALE', '202404164', 'M', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 5, 'FABIAN', 'MULOONGO', '202407862', 'M', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 6, 'RICHARD', 'MWANSA', '202402069', 'M', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 7, 'MEMORY', 'MWELA', '202410371', 'F', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 8, 'GILLIAN', 'MPOLA', '202406543', 'F', 'IT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 9, 'CHRISTABEL', 'CHIKOMONA', '201901101', 'F', 'ICT WITH EDUCATION');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 10, 'STEVEN', 'ZULU', '202301795', 'M', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 11, 'KAPUNZA', 'GWENANI', '202409282', 'F', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 12, 'LENGANJI', 'SIMWANZA', '202408164', 'F', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 13, 'EUSTANE', 'CHULU', '202403669', 'F', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 3), 14, 'ERASTAS', 'MICHELO', '202401965', 'M', 'COMPUTER SCIENCE');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 1, 'Manda', 'Dickson', '202405342', 'M', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 2, 'Nemar', 'kanyondwi', '202405228', 'F', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 3, 'Michael', 'Mutale', '202402427', 'M', 'IT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 4, 'Sikumbuzo', 'Monde', '202402839', 'F', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 5, 'Cleopatra', 'Ngulube', '202107598', 'F', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 6, 'Hellen', 'Mwanza', '202404130', 'F', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 7, 'Lloyd', 'Bulaya', '202306274', 'M', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 8, 'Gedan', 'Simbeye', '202407149', 'M', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 9, 'Muleba', 'Mwashi', '202408185', 'M', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 10, 'Nkonde R', 'Zimba', '202402132', 'F', 'IT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 11, 'Memory', 'Sokontwe', '202105977', 'F', 'BICTED');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 12, 'David', 'Kanyama', '202408941', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 13, 'Frackson', 'Mkandawire', '202302576', 'M', 'IT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 14, 'Andrew', 'moyo', '202203671', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 15, 'Thabo', 'Muliwana', '202404135', 'M', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 4), 16, 'Mwila', 'Stephen', '202402261', 'M', 'CS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 1, 'SIMAZUWO', 'BRIAN', '202406213', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 2, 'SANALUMBA', 'WAKUN’OLI', '202406222', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 3, 'DAVID', 'MBILIMA', '202406591', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 4, 'TEMBO', 'KUNDA', '202410059', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 5, 'CHABU', 'CHILELEKO', '202407895', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 6, 'NACHINGA', 'THELMA', '202400756', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 7, 'KAMWANA', 'MIRIAM', '202406797', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 8, 'MWELWA', 'MAGGIE', '202409524', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 9, 'MSHISHA', 'DADA', '202200670', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 10, 'MUKANGWA', 'BRIAN', '202409126', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 11, 'SHIKUPA', 'CHARLES', '202408328', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 12, 'MWAPE', 'PRISCA', '202307112', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 13, 'SHIKABANGA', 'CECILIA', '202400260', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 14, 'NEVERS', 'MWABA', '202403973', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 5), 15, 'FEBBY', 'NG''ANWE', '202403184', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 1, 'Muyoba', 'Mulako', '202407184', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 2, 'Owen M.', 'Mukela', '202400975', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 3, 'Previous', 'Mukosha', '202407403', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 4, 'Immaculate', 'Mwanangombe', '202400421', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 5, 'Houghton', 'Chibozyi', '202301165', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 6, 'Winnia', 'Mwanza', '202402071', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 7, 'Bosswell', 'Habenzu', '202400957', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 8, 'Kamwemba', 'Kulumuku', '202402043', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 9, 'Wilfred', 'Lungo', '202409550', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 10, 'Chakuletela', 'Musukwa', '202405691', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 11, 'Charles', 'Chisala', '202403830', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 12, 'Namasiku Beauty', 'Siemisa', '202407202', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 13, 'Fridah', 'Muzungu', '202401889', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 14, 'Mundia', 'Mundia', '202406520', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 6), 15, 'Marcus', 'Malulu', '202408897', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 1, 'Mwendalubi Mike', 'Manyika', '202303351', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 2, 'Bwalya', 'Manyando', '202406767', 'Female', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 3, 'Rachael', 'Shabolyo', '202404746', 'Female', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 4, 'Edwin', 'Makuya', '202408031', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 5, 'Cryvet', 'Simfukwe', '202408025', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 6, 'Yeshua', 'Owen', '202404317', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 7, 'Reagan', 'Chisanga', '202407565', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 8, 'Chanda', 'Mubanga', '202408138', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 9, 'Isaiah', 'Mbangweta', '202405883', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 10, 'Mapalo', 'Mwape', '202409469', 'Female', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 11, 'Ngosa', 'Florence', '202402434', 'Female', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 12, 'M''manga', 'Modester', '202403931', 'Female', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 13, 'Kazembe', 'Sing''ambwa', '202400114', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 14, 'Temwini', 'Phiri', '202404096', 'Male', 'Bsc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 7), 15, 'Caleb Mukelabai', 'Kalimina', '202401761', 'Male', 'BSc in Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 1, 'Bwalya', 'Marvelous', '202406501', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 2, 'Soko', 'Zilombo Choolwe', '202408157', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 3, 'Mbewe', 'Mathews', '202408295', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 4, 'Sakala', 'Blessings', '202404634', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 5, 'Moyo', 'Sibongile', '202409794', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 6, 'Gumbo', 'Dailess', '202406011', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 7, 'Shaba', 'Patience', '202409214', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 8, 'Chishala', 'Juliet', '202400397', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 9, 'Chileshe', 'Sampa', '202407247', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 10, 'Kabengele', 'Remmy', '201802185', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 11, 'Miyambo', 'Emmanuel', '202402295', 'M', 'Information Technology');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 8), 12, 'Kanjela', 'Musenge', '202106741', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 1, 'FLORENCE SHIMPA', 'MUSONDA', '202406031', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 2, 'Gerald', 'Gondwe', '202407879', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 3, 'Bernadette', 'Chanda', '202400653', 'F', 'IT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 4, 'William', 'Simbeye', '202403271', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 5, 'Derrick', 'Kabonso', '202402510', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 6, 'Miti', 'Steven', '202405283', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 7, 'Kampamba', 'Charity', '202409051', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 8, 'BENZU', 'HIMOONDE', '202403132', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 9, 'Frazer', 'Macha', '202405388', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 10, 'Timothy', 'Malasha', '202408282', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 11, 'Kalunga', 'Mervis', '202410672', 'F', 'IT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 12, 'Violet', 'Ngoma', '202407064', 'F', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 13, 'Mweene', 'Simon', '201701343', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 14, 'CHIMBALA', 'CHRISTOPHER', '202406408', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 15, 'Enock', 'Simumba', '202406593', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 9), 16, 'EUSTUCE', 'KASONGO', '202406805', 'M', 'BsCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 1, 'Beatrice', 'Chibesa', '202404114', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 2, 'Angel', 'Bwalya', '202400350', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 3, 'Evans', 'Saikolo', '202400550', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 4, 'Titus', 'Bwalya', '202402843', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 5, 'Ezron', 'Chiluba', '202404621', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 6, 'Juliet', 'Mulenga', '202410470', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 7, 'Ilukena', 'Mundia', '202402030', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 8, 'Cascious', 'Nyambe', '202409245', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 9, 'Aquirina', 'Mulauzi', '202410007', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 10, 'Tobias', 'Phiri', '202402824', 'M', 'Information Technology');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 11, 'Mary', 'Mula', '202410384', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 12, 'Weston', 'Mulenga', '202407468', 'M', 'Information Technology');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 13, 'Christopher', 'C.Chimfwembe', '202404891', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 14, 'Choolwe', 'Mweembe', '202405964', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 15, 'Oscar', 'Chipango', '202410253', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 10), 16, 'Gaidon', 'Sichone', '202109428', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 1, 'MITI', 'KATANGA', '202407015', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 2, 'MBASELA', 'FABIAN', '202404688', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 3, 'CHOLA', 'BEN', '202404172', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 4, 'HAMASUKWA', 'AGRIPPA C.', '202403019', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 5, 'MUUNDA', 'MAINZA', '202401150', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 6, 'MUMBI', 'CHIBESA', '202001699', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 7, 'SIMASIKU', 'ABEL', '202407015', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 8, 'CHANDA', 'COLLINS', '202403846', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 9, 'TRAORE', 'MORDECAI SALIM', '202403846', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 10, 'CHILUFYA', 'MAPALO', '202405943', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 11, 'MWAPE', 'KELVIN', '202203897', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 12, 'KANSAMBA', 'AUXIRIA', '202206607', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 13, 'MAKUYU', 'EDWIN', '202408031', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 14, 'BANDA', 'SALIMA', '202305732', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 15, 'TRAORE', 'LAMIN', '202403844', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 11), 16, 'DAKA', 'RACHEAL', '202303375', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 1, 'MOON', 'CLIVE', '202401321', 'M', 'BSIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 2, 'ZIMBA', 'EMMANUEL', '202404407', 'M', 'BSIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 3, 'KAONGA', 'MEMORY', '202405095', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 4, 'MUKUPA', 'FRANSCINE', '202405321', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 5, 'NAKANYIKA', 'NSUNGANI', '202409728', 'F', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 6, 'CHIPAMPE', 'STANSLOUS', '202406233', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 7, 'MUKONTA', 'DAVIS', '202401475', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 8, 'CHISHALA', 'ALFRED', '202406877', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 9, 'PHIRI', 'BRIAN', '202301116', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 10, 'CHOLA', 'SIMON', '202301173', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 11, 'NG''OMBA', 'KANGWA', '202300654', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 12, 'KABUTU', 'ETHEL', '202404608', 'F', 'BSIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 13, 'MWENDA HAMULAMBO', 'SOSTERN', '202409694', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 12), 14, 'MASAMBU', 'KALENGA', '202404703', 'M', 'BSCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 1, 'Silunji', 'Victor', '202408913', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 2, 'Sakala', 'Daniel', '202404454', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 3, 'Mawere', 'Saidi', '202410021', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 4, 'Mwanza', 'Taizya', '202408632', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 5, 'Mwitwa', 'Clement', '202406171', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 6, 'Banda', 'Agness', '202408752', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 7, 'N''Gandwe', 'Charles', '202400961', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 8, 'M''Soni', 'Deborah', '202409714', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 9, 'Ng''andu', 'Lwendo', '202402226', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 10, 'Chile', 'Bwalya', '202400014', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 11, 'Banda', 'Mphaso', '202404480', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 12, 'Tembo', 'Patience', '202406515', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 13, 'Mwambazi', 'Grace', '202407984', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 14, 'Ndhlovu', 'Godfrey', '202401433', 'M', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 13), 15, 'Lungu', 'Rebecca', '202406496', 'F', 'BCSC');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 1, 'JUMBE', 'THABO', '202406168', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 2, 'GONDWE', 'JOYCE', '202410082', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 3, 'KAPINGA', 'ANTONETTE', '202401120', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 4, 'LUNGU', 'JOSEPHAT', '202403000', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 5, 'NAMUKANZYE', 'FOSTER', '202407636', 'F', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 6, 'SIKUBEKA', 'EMMANUEL', '202308118', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 7, 'JUDGE', 'EZEKIEL', '202410125', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 8, 'NDUBA', 'JOEL', '202408068', 'M', 'Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 9, 'Maseba', 'Neo', '202407198', 'M', 'Bsc Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 10, 'Mapulanga', 'Henry', '202402976', 'M', 'Bsc Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 11, 'Kalengo', 'Andrew', '202406795', 'M', 'Bsc Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 12, 'Kamfwa', 'Nathan', '202408447', 'M', 'Bsc Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 13, 'Chansa', 'Sylvester', '202406183', 'M', 'Bsc Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 14), 14, 'Njunga', 'Emmanuel', '202301847', 'M', 'Bsc Computer Science');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 1, 'Chabota', 'Siyingwa', '202407073', 'M', 'BScIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 2, 'Reagan', 'Chikumbutso Mbao', '202404367', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 3, 'Fedness', 'Malilwe', '202406336', 'F', 'BScIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 4, 'Musonda', 'Amos', '202408460', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 5, 'Munesu', 'Lemba', '202409112', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 6, 'Arthur', 'sinyenga', '202408468', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 7, 'Libuku', 'Libuku', '202403876', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 8, 'Alicia', 'Kunda Muteba', '202402399', 'F', 'BScIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 9, 'Lutema', 'Jutine Lutangu', '202405510', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 10, 'Lushomo', 'Muteteete', '202407579', 'F', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 11, 'Chirwa', 'Janet', '202106055', 'F', 'BScIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 12, 'John', 'Kabemba', '202408026', 'M', 'BScIT');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 13, 'Victor', 'Lupenga', '202403212', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 14, 'George', 'Kunda', '202407907', 'M', 'BScCS');
INSERT INTO students (group_id, list_number, first_name, last_name, student_number, gender, programme)
VALUES ((SELECT group_id FROM student_groups WHERE group_number = 15), 15, 'Temwananji', 'Mutambo', '202404116', 'M', 'BScIT');

-- Note: student_number is indexed but deliberately not UNIQUE because the
-- source spreadsheet contains repeated student numbers.
-- Blank/incomplete rows were skipped; text values were trimmed at the edges.
