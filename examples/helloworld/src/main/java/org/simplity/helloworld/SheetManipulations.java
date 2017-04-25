package org.simplity.helloworld;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.helloworld.entity.Orders;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class SheetManipulations implements LogicInterface{

	@Override
	public Value execute(ServiceContext ctx) {
		MultiRowsSheet sheet = (MultiRowsSheet) ctx.getDataSheet("orders");
		
		Date[] columnArray = new Date[sheet.length()];
		columnArray = sheet.columnAsArray("ordDate",columnArray);
		List<Integer> columnList = new ArrayList<Integer>();
		columnList = (List<Integer>) sheet.columnAsCollection("ordNum",columnList,Integer.class);		
		Set<Double> columnSet = new HashSet<Double>();
		columnSet = (Set<Double>) sheet.columnAsCollection("ordAmount",columnSet,Double.class);
		Map<Integer, String> columnsMap = new HashMap<Integer, String>();
		columnsMap = sheet.columnsAsMap("ordNum", "ordDescription",columnsMap,Integer.class,String.class);
		
		ctx.setObject("columnArray",columnArray);
		ctx.setObject("columnList",columnList);
		ctx.setObject("columnSet",columnSet);
		ctx.setObject("columnsMap",columnsMap);
		
		Set<Orders> sheetSet = new HashSet<Orders>();
		sheetSet = sheet.toSet(sheetSet,Orders.class);
		List<Orders> sheetList = new ArrayList<Orders>();
		sheetList = sheet.toList(sheetList,Orders.class);
		
		for(Orders obj:sheetList){
			System.out.println("Order Number: "+obj.getOrdNum());
		}
			
		Integer[] array = {12,34,26,78,39};
		DataSheet arraytosheet = MultiRowsSheet.toDatasheet(array);		
		List<String> list = new ArrayList<String>();
		list.add("jack");
		list.add("mike");
		list.add("stephen");
		list.add("nick");
		DataSheet listtosheet = MultiRowsSheet.toDatasheet(list);
		DataSheet settosheet = MultiRowsSheet.toDatasheet(sheetSet);
		
		ctx.putDataSheet("listtosheet", listtosheet);
		ctx.putDataSheet("arraytosheet", arraytosheet);
		ctx.putDataSheet("settosheet", settosheet);
		return null;
	}

}
