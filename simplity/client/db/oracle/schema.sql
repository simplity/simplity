
CREATE SEQUENCE   "ALL_SEQ"  MINVALUE 100 
	MAXVALUE 9999999999999999999999999999 
	INCREMENT BY 1 START WITH 100 CACHE 20 NOORDER  NOCYCLE ;

/

CREATE TABLE  "VETS" 
   ("ID" NUMBER NOT NULL ENABLE, 
	"FIRST_NAME" VARCHAR2(30) NOT NULL ENABLE, 
	"LAST_NAME" VARCHAR2(30) NOT NULL ENABLE, 
	 CONSTRAINT "VETS_PK" PRIMARY KEY ("ID") ENABLE
   ) ;
/
   
CREATE  TRIGGER "BI_VETS"
  before insert on "VETS"               
  for each row  
begin   
  if :NEW."ID" is null then 
    select "ALL_SEQ".nextval into :NEW."ID" from dual; 
  end if; 
end; 
/
ALTER TRIGGER  "BI_VETS" ENABLE;

/

create table specialties 
(
  id number not null enable,
  name varchar2(80) not null,
  constraint specialties_pk primary key (id) enable
) ;

/

create trigger bi_specialties
  before insert on specialties               
  for each row  
begin   
  if :new.id is null then 
    select all_seq.nextval into :new.id from dual; 
  end if; 
end; 

/

alter trigger bi_specialties enable;

/

create table vet_specialties 
(
  vet_id number not null enable,
  specialty_id number not null enable,
  constraint vet_specialties_pk primary key (vet_id, specialty_id ) enable
) ;

/

create table types 
(
  id number not null enable,
  name varchar2(80) not null enable,
  constraint types_pk primary key (id ) enable
) ;

/

create table owners (
  id number not null enable,
  first_name varchar2(30) not null enable,
  last_name varchar2(30) not null enable,
  address varchar2(255),
  city varchar2(80),
  telephone varchar2(20),
  constraint owners_pk primary key (id ) enable
) ;

/

create trigger bi_owners
  before insert on owners               
  for each row  
begin   
  if :new.id is null then 
    select all_seq.nextval into :new.id from dual; 
  end if; 
end; 

/

alter trigger bi_owners enable;

/

create table pets (
  id number not null enable,
  name varchar2(30) not null enable,
  birth_date date ,
  type_id number not null enable,
  owner_id number not null enable,
  constraint pets_pk primary key (id ) enable
) ;

/

create trigger bi_pets
  before insert on pets              
  for each row  
begin   
  if :new.id is null then 
    select all_seq.nextval into :new.id from dual; 
  end if; 
end; 

/

alter trigger bi_pets enable;

/

create table visits (
  id number not null enable,
  pet_id number not null enable,
  visit_date date ,
  description varchar2(255),
  constraint visits_pk primary key (id ) enable
) ;

create trigger bi_visits
  before insert on visits               
  for each row  
begin   
  if :new.id is null then 
    select all_seq.nextval into :new.id from dual; 
  end if; 
end; 

/

alter trigger bi_visits enable;

/
/
CREATE VIEW pet_details AS
    SELECT 
        a.id AS pet_id,
        a.name AS pet_name,
        a.birth_date AS birth_date,
        a.type_id AS type_id,
        a.owner_id AS owner_id,
        b.name AS pet_type,
        c.first_name AS first_name,
        c.last_name AS last_name
    FROM
        pets a,
        types b,
        owners c
    WHERE
        a.type_id = b.id AND a.owner_id = c.id;

/

CREATE VIEW vet_specialties_details AS
    SELECT 
        a.vet_id AS vet_id,
        a.specialty_id AS specialty_id,
        b.name AS specialty
    FROM
        vet_specialties a left join
        specialties b on
        a.specialty_id = b.id;

