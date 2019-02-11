var a = {
	"mandatoryTextWithDefault" : "default",
	"mandatoryTextWithDefaultAndValue" : "value specified overrides default",
	"mandatoryText" : "mandatory text value",
	"optionalTextWithValue" : "optional value",
	"optionalTextWithDefault" : "default for optional",
	"optionalTextWithDefaultAndValue" : "this optional value to show up",
	"dateTime" : "2017-01-31T23:11:23.456Z",
	"grandId" : 20,
	"grandIdTo" : 100,
	"grandIdOperator" : "><",
	"grandName" : "abcd",
	"grandNameOperator" : "~",
	"childName" : "childNameInFields",
	"specialSheet" : [ {
		"a" : "a1",
		"b" : 12,
		"c" : 23.4,
		"d" : true,
		"e" : "2017-12-31T12:12:01.234Z"
	}, {
		"a" : "a2",
		"b" : 23,
		"c" : 34.5,
		"d" : false,
		"e" : "2016-12-31T12:12:01.234Z"
	} ],
	"input" : [ {
		"id" : 1,
		"fromField" : 12,
		"toField" : 13,
		"leadField" : "lead1",
		"dependentField" : "depend1"
	}, {
		"id" : 2,
		"fromField" : 1,
		"toField" : 2,
		"leadField" : "",
		"dependentField" : "depend2"
	}, {
		"id" : 3,
		"fromField" : 340,
		"toField" : 346,
		"leadField" : "",
		"dependentField" : ""
	} ],
	"read" : [ {
		"id" : 100
	}, {
		"id" : 100
	} ],
	"save" : [ {
		"id" : 101,
		"fromField" : 10,
		"toField" : 11,
		"leadField" : "",
		"dependentField" : "dep"
	} ],
	"subset" : [ {
		"id" : 101,
		"fromField" : 10
	} ],
	"minMax" : [ {
		"childId" : 11
	}, {
		"childId" : 22
	} ],
	"parent" : [ {
		"parentId" : 1,
		"parentName" : "p1"
	}, {
		"parentId" : 2,
		"parentName" : "p2"
	}, {
		"parentId" : 3,
		"parentName" : "p3"
	} ],
	"child" : [ {
		"childId" : 11,
		"parentId" : 1,
		"childName" : "c11"
	}, {
		"childId" : 12,
		"parentId" : 1,
		"childName" : "c12"
	}, {
		"childId" : 21,
		"parentId" : 2,
		"childName" : "c21"
	} ],
	"child1" : [ {
		"childId" : 101,
		"parentId" : 10,
		"childName" : "c101"
	}, {
		"childId" : 102,
		"parentId" : 10,
		"childName" : "c102"
	} ],
	"array1" : [ 1, 2, 3, 4, 5 ],
	"array2" : [ 1, 2, 3, 4, 5 ],
	"array3" : [ 1 ]

};