package org.simplity.ide;

import java.io.File;
import java.util.Scanner;

import org.simplity.json.JSONObject;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlUtil;

/**
 * Hello world!
 *
 */
public class OpenApiRecGenerator {
	
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.println("Provide the swagger json file path");
		String inputFile = sc.nextLine();
		String txt = FileManager.readFile(new File(inputFile));
		System.out.println("Provide path for output files");
		String outputPath = sc.nextLine();
		JSONObject swagger = new JSONObject(txt);
		JSONObject defs = swagger.optJSONObject("definitions");
		if (defs == null) {
			Tracer.trace("No defintions found");
			sc.close();
			return;
		}
		Tracer.trace("going to scan " + defs.length() + " schemas at the root level");
		Record[] recs = Record.fromSwaggerDefinitions(null, defs);
		for (Record rec : recs) {
			String text = XmlUtil.objectToXmlString(rec);
			FileManager.writeFile(new File(outputPath + "/" + rec.getSimpleName() + ".xml"), text);
		}
		sc.close();
	}
}
