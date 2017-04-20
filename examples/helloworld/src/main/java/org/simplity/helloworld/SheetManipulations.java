package org.simplity.helloworld;

import java.util.ArrayList;
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
		
		Object[] columnArray = sheet.columnAsArray("ordDate");
		 List<Object> columnList = sheet.columnAsList("ordNum");		
		Set<Object> columnSet = sheet.columnAsSet("ordAmount");
		Map<String, Object> columnsMap = sheet.columnsAsMap("ordNum", "ordDescription");
		
		ctx.setObject("columnArray",columnArray);
		ctx.setObject("columnList",columnList);
		ctx.setObject("columnSet",columnSet);
		ctx.setObject("columnsMap",columnsMap);
		
		Set<Object> sheetSet = sheet.toSet("org.simplity.helloworld.entity.Orders");
		List<Object> sheetList = sheet.toList("org.simplity.helloworld.entity.Orders");
		
		for(Object obj:sheetList){
			Orders orders = (Orders)obj;
			System.out.println("Order Number: "+orders.getOrdNum());
		}
			
		Integer[] array = {12,34,26,78,39};
		DataSheet arraytosheet = MultiRowsSheet.toDatasheet(array, "name");		
		List<String> list = new ArrayList<String>();
		list.add("jack");
		list.add("mike");
		list.add("stephen");
		list.add("nick");
		DataSheet listtosheet = MultiRowsSheet.toDatasheet(list, "name");
		DataSheet settosheet = MultiRowsSheet.toDatasheet(sheetSet, null);
		
		ctx.putDataSheet("listtosheet", listtosheet);
		ctx.putDataSheet("arraytosheet", arraytosheet);
		ctx.putDataSheet("settosheet", settosheet);
		return null;
	}

}
