/**
 * 
 */
package org.simplity.kernel.util.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.test.Customer;

public class SheetUtilTest {

	@Test
	public final void columnToArray(){
		MultiRowsSheet dataSheet = getSheet();
		Integer[] intArray = new Integer[dataSheet.length()];
		intArray = dataSheet.columnAsArray("customerNumber",intArray);
		assertNotNull(intArray);
		Integer[] expectedResult = {103,112,114,119,121};
		assertEquals(Arrays.asList(expectedResult).toString(), Arrays.asList(intArray).toString());
	}
	
	@Test
	public final void columnToListInteger(){
		MultiRowsSheet dataSheet = getSheet();
		List<Integer> actualList = new ArrayList<Integer>();
		actualList = (List<Integer>) dataSheet.columnAsCollection("customerNumber",actualList,Integer.class);
		List<Integer> expectedList = new ArrayList<Integer>();
		expectedList.add(103);
		expectedList.add(112);
		expectedList.add(114);
		expectedList.add(119);
		expectedList.add(121);
		assertEquals(expectedList,actualList);
	}
	
	@Test
	public final void columnToListString(){
		MultiRowsSheet dataSheet = getSheet();
		List<String> actualList = new ArrayList<String>();
		actualList = (List<String>) dataSheet.columnAsCollection("customerName",actualList,String.class);
		List<String> expectedList = new ArrayList<String>();
		expectedList.add("Atelier graphique");
		expectedList.add("Signal Gift Stores");
		expectedList.add("Australian Collectors");
		expectedList.add("La Rochelle Gifts");
		expectedList.add("Baane Mini Imports");
		assertEquals(expectedList,actualList);
	}
	
	@Test
	public final void columnToSet(){
		MultiRowsSheet dataSheet = getSheet();
		Set<String> actualSet = new HashSet<String>();
		actualSet = (Set<String>) dataSheet.columnAsCollection("city",actualSet,String.class);
		Set<String> expectedSet = new HashSet<String>();
		expectedSet.add("Nantes");
		expectedSet.add("Las Vegas");
		expectedSet.add("Melbourne");
		expectedSet.add("Nantes");
		expectedSet.add("San Rafael");
		assertEquals(expectedSet,actualSet);
	}
	
	@Test
	public final void columnsToMap(){
		MultiRowsSheet dataSheet = getSheet();
		Map<String,String> actualResult = new HashMap<String,String>();
		actualResult = dataSheet.columnsAsMap("city","country",actualResult,String.class,String.class);
		Map<String,String> expectedResult = new HashMap<String,String>();
		expectedResult.put("Nantes", "France");
		expectedResult.put("Las Vegas", "USA");
		expectedResult.put("Melbourne", "Australia");
		expectedResult.put("Nantes", "France");
		expectedResult.put("San Rafael", "USA");
		assertEquals(expectedResult,actualResult);
	}
	
	@Test
	public final void sheetToList(){
		MultiRowsSheet dataSheet = getSheet();
		List<Customer> actualResult = new ArrayList<Customer>();
		actualResult = dataSheet.toList(actualResult,Customer.class);
		Customer cust = actualResult.get(0);
		assertEquals(cust.getCustomerName(), "Atelier graphique");
	}
	
	@Test
	public final void sheetToSet(){
		MultiRowsSheet dataSheet = getSheet();
		Set<Customer> actualResult = new HashSet<Customer>();
		actualResult = dataSheet.toSet(actualResult,Customer.class);
		assertEquals(5, actualResult.size());
	}
	
	@Test
	public final void arrayToDatasheet(){
		Integer[] intArray = {1,2,3};
		MultiRowsSheet sheet = MultiRowsSheet.toDatasheet(intArray, "arraydata");
		try {
			assertEquals(1,(int)sheet.getColumnValue("arraydata", 0).toInteger());
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public final void objectarrayToDatasheet(){
		Customer cust1 = new Customer(130,"Mike","ABC","AAA","VVV","xxx",123456);
		Customer cust2 = new Customer(140,"Mick","DEF","BBB","CCC","yyy",112233);
		Customer[] custArray = {cust1,cust2};
		MultiRowsSheet sheet = MultiRowsSheet.toDatasheet(custArray,null);
		int cust1Num = 0;
		try {
			cust1Num = (int)sheet.getColumnValue("customerNumber", 0).toInteger();
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
		assertEquals(130,cust1Num);
	}
	
	@Test
	public final void listToDatasheet(){
		List<String> list = new ArrayList<String>();
		list.add("jack");
		list.add("stephen");
		list.add("nick");
		MultiRowsSheet listtosheet = MultiRowsSheet.toDatasheet(list, "list");
		String actualResult = listtosheet.getColumnValue("list", 1).toString();
		assertEquals("stephen",actualResult);
	}
	
	@Test
	public final void objectListToDatasheet(){
		Customer cust1 = new Customer(130,"Mike","ABC","AAA","VVV","xxx",123456);
		Customer cust2 = new Customer(140,"Mick","DEF","BBB","CCC","yyy",112233);
		List<Customer> list = new ArrayList<Customer>();
		list.add(cust1);
		list.add(cust2);
		MultiRowsSheet sheet = MultiRowsSheet.toDatasheet(list,null);
		String cust2Name = null;
		cust2Name = sheet.getColumnValue("customerName",1).toString();
		assertEquals("Mick",cust2Name);
	}
	
	@Test
	public final void setToDatasheet(){
		Set<Double> set = new HashSet<Double>();
		set.add(3621.67);
		set.add(629.07);
		MultiRowsSheet settosheet = MultiRowsSheet.toDatasheet(set, "set");
		String actualResult = settosheet.getColumnValue("set", 1).toString();
		assertEquals("629.07",actualResult);
	}
	
	@Test
	public final void objectSetToDatasheet(){
		Customer cust1 = new Customer(130,"Mike","ABC","AAA","VVV","xxx",123456);
		Set<Customer> set = new HashSet<Customer>();
		set.add(cust1);
		MultiRowsSheet sheet = MultiRowsSheet.toDatasheet(set,null);
		int cust2postalCode = 0;
		try {
			cust2postalCode = (int)sheet.getColumnValue("postalCode",0).toInteger();
		} catch (InvalidValueException e) {
			e.printStackTrace();
		}
		assertEquals(123456,cust2postalCode);
	}
	
	@Test
	public final void mapSheetTranspose(){
		Map<String,String> map = new HashMap<String, String>();
		map.put("empNo", "235");
		map.put("empName", "Robert");
		map.put("empMail", "Robert@abc.com");
		DataSheet transposeSheet = MultiRowsSheet.toDatasheet(map, true);
		int noOfColumns = transposeSheet.width();
		int noOfRows = transposeSheet.length();
		assertEquals(3,noOfColumns);
		assertEquals(1,noOfRows);
	}
	
	@Test
	public final void mapToSheet(){
		Map<String,String> map = new HashMap<String, String>();
		map.put("empNo", "235");
		map.put("empName", "Robert");
		map.put("empMail", "Robert@abc.com");
		DataSheet transposeSheet = MultiRowsSheet.toDatasheet(map, false);
		String key1 = transposeSheet.getColumnValue("key", 0).toString();
		String value1 = transposeSheet.getColumnValue("value", 0).toString();
		assertEquals(map.get(key1).toString(),value1);
	}
	
	public final MultiRowsSheet getSheet(){
		String[] columnNames = {"customerNumber","customerName","address","city","state","country","postalCode"};
		ValueType[] columnValueTypes = {ValueType.INTEGER,ValueType.TEXT,ValueType.TEXT,ValueType.TEXT,ValueType.TEXT,ValueType.TEXT,ValueType.INTEGER};
		MultiRowsSheet sheet = new MultiRowsSheet(columnNames, columnValueTypes);

		Value[] row1 = {Value.newIntegerValue(103),Value.newTextValue("Atelier graphique"),Value.newTextValue("54, rue Royale"),Value.newTextValue("Nantes"),Value.newTextValue("NULL"),Value.newTextValue("France"),Value.newIntegerValue(44000)};
		Value[] row2 = {Value.newIntegerValue(112),Value.newTextValue("Signal Gift Stores"),Value.newTextValue("8489 Strong St."),Value.newTextValue("Las Vegas"),Value.newTextValue("NV"),Value.newTextValue("USA"),Value.newIntegerValue(83030)};
		Value[] row3 = {Value.newIntegerValue(114),Value.newTextValue("Australian Collectors"),Value.newTextValue("636 St Kilda Road"),Value.newTextValue("Melbourne"),Value.newTextValue("Victoria"),Value.newTextValue("Australia"),Value.newIntegerValue(3004)};
		Value[] row4 = {Value.newIntegerValue(119),Value.newTextValue("La Rochelle Gifts"),Value.newTextValue("67, rue des Cinquante Otages"),Value.newTextValue("Nantes"),Value.newTextValue("NULL"),Value.newTextValue("France"),Value.newIntegerValue(4110)};
		Value[] row5 = {Value.newIntegerValue(121),Value.newTextValue("Baane Mini Imports"),Value.newTextValue("Erling Skakkes gate 78"),Value.newTextValue("San Rafael"),Value.newTextValue("CA"),Value.newTextValue("USA"),Value.newIntegerValue(97562)};
	
		sheet.addRow(row1);
		sheet.addRow(row2);
		sheet.addRow(row3);
		sheet.addRow(row4);
		sheet.addRow(row5);
		
		return sheet;
	}
}
