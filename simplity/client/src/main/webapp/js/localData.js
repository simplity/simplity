/**
 * local data contains responses and tables response contains ready JSONs
 * indexed by service names. tables has either table or view as its members.
 * table has key (name of key field, and we assume it to be one-up number at
 * this point) and children (an array of views/tables to be read saved along
 * with this table view has baseTable,
 */
var _localData = {
	responses : {},
	tables : {
		vets : {
			key : 'vetId',
			children : [ 'vetSpecialtiesDetails' ],
			data : [ {
				vetId : 1,
				firstName : 'James',
				lastName : 'Carter'
			}, {
				vetId : 2,
				firstName : 'Helen',
				lastName : 'Leary'
			}, {
				vetId : 3,
				firstName : 'Linda',
				lastName : 'Douglas'
			}, {
				vetId : 4,
				firstName : 'Rafael',
				lastName : 'Ortega'
			}, {
				vetId : 5,
				firstName : 'Henry',
				lastName : 'Stevens'
			}, {
				vetId : 6,
				firstName : 'Sharon',
				lastName : 'Jenkins'
			} ]
		},

		specialties : {
			key : 'specialtyId',
			valueFieldName : 'specialty',
			data : [ {
				specialtyId : 1,
				specialty : 'radiology'
			}, {
				specialtyId : 2,
				specialty : 'surgery'
			}, {
				specialtyId : 3,
				specialty : 'dentistry'
			} ]
		},
		vetSpecialties : {
			data : [ {
				vetId : 2,
				specialtyId : 1
			}, {
				vetId : 3,
				specialtyId : 2
			}, {
				vetId : 3,
				specialtyId : 3
			}, {
				vetId : 4,
				specialtyId : 2
			}, {
				vetId : 5,
				specialtyId : 1
			} ]
		},

		petTypes : {
			key : 'petTypeId',
			valueFieldName : 'petType',
			data : [ {
				petTypeId : 1,
				petType : 'cat'
			}, {
				petTypeId : 2,
				petType : 'dog'
			}, {
				petTypeId : 3,
				petType : 'lizard'
			}, {
				petTypeId : 4,
				petType : 'snake'
			}, {
				petTypeId : 5,
				petType : 'bird'
			}, {
				petTypeId : 6,
				petType : 'hamster'
			} ]
		},

		owners : {
			key : 'ownerId',
			children : [ 'petDetails' ],
			data : [ {
				ownerId : 1,
				firstName : 'George',
				lastName : 'Franklin',
				address : '110 W. Liberty St.',
				city : 'Madison',
				telephone : '6085551023'
			}, {
				ownerId : 2,
				firstName : 'Betty',
				lastName : 'Davis',
				address : '638 Cardinal Ave.',
				city : 'Sun Prairie',
				telephone : '6085551749'
			}, {
				ownerId : 3,
				firstName : 'Eduardo',
				lastName : 'Rodriquez',
				address : '2693 Commerce St.',
				city : 'McFarland',
				telephone : '6085558763'
			}, {
				ownerId : 4,
				firstName : 'Harold',
				lastName : 'Davis',
				address : '563 Friendly St.',
				city : 'Windsor',
				telephone : '6085553198'
			}, {
				ownerId : 5,
				firstName : 'Peter',
				lastName : 'McTavish',
				address : '2387 S. Fair Way',
				city : 'Madison',
				telephone : '6085552765'
			}, {
				ownerId : 6,
				firstName : 'Jean',
				lastName : 'Coleman',
				address : '105 N. Lake St.',
				city : 'Monona',
				telephone : '6085552654'
			}, {
				ownerId : 7,
				firstName : 'Jeff',
				lastName : 'Black',
				address : '1450 Oak Blvd.',
				city : 'Monona',
				telephone : '6085555387'
			}, {
				ownerId : 8,
				firstName : 'Maria',
				lastName : 'Escobito',
				address : '345 Maple St.',
				city : 'Madison',
				telephone : '6085557683'
			}, {
				ownerId : 9,
				firstName : 'David',
				lastName : 'Schroeder',
				address : '2749 Blackhawk Trail',
				city : 'Madison',
				telephone : '6085559435'
			}, {
				ownerId : 10,
				firstName : 'Carlos',
				lastName : 'Estaban',
				address : '2335 Independence La.',
				city : 'Waunakee',
				telephone : '6085555487'
			} ]
		},

		pets : {
			key : 'petId',
			data : [ {
				petId : 1,
				petName : 'Leo',
				petDob : '2010-09-07',
				petTypeId : 1,
				ownerId : 1
			}, {
				petId : 2,
				petName : 'Basil',
				petDob : '2012-08-06',
				petTypeId : 6,
				ownerId : 2
			}, {
				petId : 3,
				petName : 'Rosy',
				petDob : '2011-04-17',
				petTypeId : 2,
				ownerId : 3
			}, {
				petId : 4,
				petName : 'Jewel',
				petDob : '2010-03-07',
				petTypeId : 2,
				ownerId : 3
			}, {
				petId : 5,
				petName : 'Iggy',
				petDob : '2010-11-30',
				petTypeId : 3,
				ownerId : 4
			}, {
				petId : 6,
				petName : 'George',
				petDob : '2010-01-20',
				petTypeId : 4,
				ownerId : 5
			}, {
				petId : 7,
				petName : 'Samantha',
				petDob : '2012-09-04',
				petTypeId : 1,
				ownerId : 6
			}, {
				petId : 8,
				petName : 'Max',
				petDob : '2012-09-04',
				petTypeId : 1,
				ownerId : 6
			}, {
				petId : 9,
				petName : 'Lucky',
				petDob : '2011-08-06',
				petTypeId : 5,
				ownerId : 7
			}, {
				petId : 10,
				petName : 'Mulligan',
				petDob : '2007-02-24',
				petTypeId : 2,
				ownerId : 8
			}, {
				petId : 11,
				petName : 'Freddy',
				petDob : '2010-03-09',
				petTypeId : 5,
				ownerId : 9
			}, {
				petId : 12,
				petName : 'Lucky',
				petDob : '2010-06-24',
				petTypeId : 2,
				ownerId : 10
			}, {
				petId : 13,
				petName : 'Sly',
				petDob : '2012-06-08',
				petTypeId : 1,
				ownerId : 10
			} ]
		},

		visits : {
			data : [ {
				petId : 7,
				visitDate : '2013-01-01',
				visitDescription : 'rabies shot'
			}, {
				petId : 8,
				visitDate : '2013-01-02',
				visitDescription : 'rabies shot'
			}, {
				petId : 8,
				visitDate : '2013-01-03',
				visitDescription : 'neutered'
			}, {
				petId : 7,
				visitDate : '2013-01-04',
				visitDescription : 'spayed'
			} ]
		},
		vetSpecialtiesDetails : {
			baseTable : 'vetSpecialties',
			joins : [ {
				joinedTable : 'specialties',
				baseField : 'specialtyId',
				joinedField : 'specialtyId'
			} ]
		},
		petDetails : {
			key : 'petId',
			baseTable : 'pets',
			children : [ 'visits' ],
			joins : [ {
				joinedTable : 'petTypes',
				baseField : 'petTypeId',
				joinedField : 'petTypeId',
			}, {
				joinedTable : 'owners',
				baseField : 'ownerId',
				joinedField : 'ownerId',
				fields : [ 'firstName', 'lastName' ]
			} ]
		}
	}
};
