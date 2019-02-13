CREATE DATABASE IF NOT EXISTS petclinic;

ALTER DATABASE petclinic
  DEFAULT CHARACTER SET utf8
  DEFAULT COLLATE utf8_general_ci;

GRANT ALL PRIVILEGES ON petclinic.* TO pc@localhost IDENTIFIED BY 'pc';

USE petclinic;

CREATE TABLE IF NOT EXISTS vets (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(30),
  last_name VARCHAR(30),
  INDEX(last_name)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS specialties (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80),
  INDEX(name)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS vet_specialties (
  vet_id INT(4) UNSIGNED NOT NULL,
  specialty_id INT(4) UNSIGNED NOT NULL,
  FOREIGN KEY (vet_id) REFERENCES vets(id),
  FOREIGN KEY (specialty_id) REFERENCES specialties(id),
  UNIQUE (vet_id,specialty_id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS types (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80),
  INDEX(name)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS owners (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(30),
  last_name VARCHAR(30),
  address VARCHAR(255),
  city VARCHAR(80),
  telephone VARCHAR(20),
  INDEX(last_name)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS pets (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(30),
  birth_date DATE,
  type_id INT(4) UNSIGNED NOT NULL,
  owner_id INT(4) UNSIGNED NOT NULL,
  INDEX(name),
  FOREIGN KEY (owner_id) REFERENCES owners(id),
  FOREIGN KEY (type_id) REFERENCES types(id)
) engine=InnoDB;

CREATE TABLE IF NOT EXISTS visits (
  id INT(4) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  pet_id INT(4) UNSIGNED NOT NULL,
  visit_date DATE,
  description VARCHAR(255),
  FOREIGN KEY (pet_id) REFERENCES pets(id)
) engine=InnoDB;

USE `petclinic`;

DROP VIEW IF EXISTS `petclinic`.`pet_details` ;

USE `petclinic`;
CREATE VIEW `petclinic`.`pet_details` AS
    SELECT 
        `a`.`id` AS `pet_id`,
        `a`.`name` AS `pet_name`,
        `a`.`birth_date` AS `birth_date`,
        `a`.`type_id` AS `type_id`,
        `a`.`owner_id` AS `owner_id`,
        `b`.`name` AS `pet_type`,
        `c`.`first_name` AS `first_name`,
        `c`.`last_name` AS `last_name`
    FROM
        `petclinic`.`pets` `a`,
        `petclinic`.`types` `b`,
        `petclinic`.`owners` `c`
    WHERE
        `a`.`type_id` = `b`.`id` AND a.owner_id = c.id;

DROP VIEW IF EXISTS `petclinic`.`vet_specialties_details` ;
CREATE VIEW `petclinic`.`vet_specialties_details` AS
    SELECT 
        `a`.`vet_id` AS `vet_id`,
        `a`.`specialty_id` AS `specialty_id`,
        `b`.`name` AS `specialty`
    FROM
        (`petclinic`.`vet_specialties` `a`
        JOIN `petclinic`.`specialties` `b`)
    WHERE
        (`a`.`specialty_id` = `b`.`id`);

