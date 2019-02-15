INSERT INTO vets VALUES (1, 'James', 'Carter');
INSERT INTO vets VALUES (2, 'Helen', 'Leary');
INSERT INTO vets VALUES (3, 'Linda', 'Douglas');
INSERT INTO vets VALUES (4, 'Rafael', 'Ortega');
INSERT INTO vets VALUES (5, 'Henry', 'Stevens');
INSERT INTO vets VALUES (6, 'Sharon', 'Jenkins');

INSERT INTO specialties VALUES (1, 'radiology');
INSERT INTO specialties VALUES (2, 'surgery');
INSERT INTO specialties VALUES (3, 'dentistry');

INSERT INTO vet_specialties VALUES (2, 1);
INSERT INTO vet_specialties VALUES (3, 2);
INSERT INTO vet_specialties VALUES (3, 3);
INSERT INTO vet_specialties VALUES (4, 2);
INSERT INTO vet_specialties VALUES (5, 1);

INSERT INTO types VALUES (1, 'cat');
INSERT INTO types VALUES (2, 'dog');
INSERT INTO types VALUES (3, 'lizard');
INSERT INTO types VALUES (4, 'snake');
INSERT INTO types VALUES (5, 'bird');
INSERT INTO types VALUES (6, 'hamster');

INSERT INTO owners VALUES (1, 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023');
INSERT INTO owners VALUES (2, 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749');
INSERT INTO owners VALUES (3, 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763');
INSERT INTO owners VALUES (4, 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198');
INSERT INTO owners VALUES (5, 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765');
INSERT INTO owners VALUES (6, 'Jean', 'Coleman', '105 N. Lake St.', 'Monona', '6085552654');
INSERT INTO owners VALUES (7, 'Jeff', 'Black', '1450 Oak Blvd.', 'Monona', '6085555387');
INSERT INTO owners VALUES (8, 'Maria', 'Escobito', '345 Maple St.', 'Madison', '6085557683');
INSERT INTO owners VALUES (9, 'David', 'Schroeder', '2749 Blackhawk Trail', 'Madison', '6085559435');
INSERT INTO owners VALUES (10, 'Carlos', 'Estaban', '2335 Independence La.', 'Waunakee', '6085555487');

INSERT INTO pets VALUES (1, 'Leo', to_date('2000-09-07','yyyy-mm-dd'), 1, 1);
INSERT INTO pets VALUES (2, 'Basil', to_date('2002-08-06','yyyy-mm-dd'), 6, 2);
INSERT INTO pets VALUES (3, 'Rosy', to_date('2001-04-17','yyyy-mm-dd'), 2, 3);
INSERT INTO pets VALUES (4, 'Jewel', to_date('2000-03-07','yyyy-mm-dd'), 2, 3);
INSERT INTO pets VALUES (5, 'Iggy', to_date('2000-11-30','yyyy-mm-dd'), 3, 4);
INSERT INTO pets VALUES (6, 'George', to_date('2000-01-20','yyyy-mm-dd'), 4, 5);
INSERT INTO pets VALUES (7, 'Samantha', to_date('1995-09-04','yyyy-mm-dd'), 1, 6);
INSERT INTO pets VALUES (8, 'Max', to_date('1995-09-04', 'yyyy-mm-dd'), 1, 6);
INSERT INTO pets VALUES (9, 'Lucky', to_date('1999-08-06','yyyy-mm-dd'), 5, 7);
INSERT INTO pets VALUES (10, 'Mulligan', to_date('1997-02-24','yyyy-mm-dd'), 2, 8);
INSERT INTO pets VALUES (11, 'Freddy', to_date('2000-03-09','yyyy-mm-dd'), 5, 9);
INSERT INTO pets VALUES (12, 'Lucky', to_date('2000-06-24','yyyy-mm-dd'), 2, 10);
INSERT INTO pets VALUES (13, 'Sly', to_date('2002-06-08','yyyy-mm-dd'), 1, 10);

INSERT INTO visits VALUES (1, 7, to_date('2010-03-04','yyyy-mm-dd'), 'rabies shot');
INSERT INTO visits VALUES (2, 8, to_date('2011-03-04','yyyy-mm-dd'), 'rabies shot');
INSERT INTO visits VALUES (3, 8, to_date('2009-06-04','yyyy-mm-dd'), 'neutered');
INSERT INTO visits VALUES (4, 7, to_date('2008-09-04','yyyy-mm-dd'), 'spayed');