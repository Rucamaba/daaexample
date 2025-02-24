DROP DATABASE IF EXISTS `daaexample`;
CREATE DATABASE `daaexample`
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

CREATE TABLE `daaexample`.`people` (
    `id` int NOT NULL AUTO_INCREMENT,
    `name` varchar(50) NOT NULL,
    `surname` varchar(100) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `daaexample`.`users` (
    `login` varchar(100) NOT NULL,
    `password` varchar(64) NOT NULL,
    `role` varchar(10) NOT NULL,
    PRIMARY KEY (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `daaexample`.`types` (
    `id` int NOT NULL AUTO_INCREMENT,
    `name` varchar(50) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `daaexample`.`pets` (
    `id` int NOT NULL AUTO_INCREMENT,
    `name` varchar(50) NOT NULL,
    `owner_id` int NOT NULL,
    `type_id` int NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`owner_id`) REFERENCES `people`(`id`),
    FOREIGN KEY (`type_id`) REFERENCES `types`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE USER IF NOT EXISTS 'daa'@'localhost' IDENTIFIED WITH mysql_native_password BY 'daa';
GRANT ALL ON `daaexample`.* TO 'daa'@'localhost';

-- Insert some default pet types
INSERT INTO `daaexample`.`types` (`name`) VALUES 
('dog'),
('cat'),
('rabbit'),
('bird'),
('fish'),
('hamster'),
('turtle');
