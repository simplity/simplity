package org.simplity.ide;

import java.io.File;
import java.util.Scanner;

import org.simplity.json.JSONObject;
import org.simplity.kernel.Application;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.tp.Service;

public class OpenApiServicesGenerator {
	/**
	 * definitions object
	 */
	private static final String DEFS_ATTR = "definitions";

	/**
	 *
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner(System.in);
		System.out.println("Provide the swagger json file path");
		String inputFile = sc.nextLine();
		String txt = FileManager.readFile(new File(inputFile));
		System.out.println("Provide path for comp folder of your project");
		String compPath = sc.nextLine();
		System.out.println("Provide path for output files");
		String outPath = sc.nextLine();
		Application.bootStrap(compPath);
		JSONObject swagger = new JSONObject(txt);
		JSONObject paths = swagger.optJSONObject("paths");
		JSONObject defs = swagger.optJSONObject(DEFS_ATTR);
		if(paths == null){
			Tracer.trace("No paths found");
			sc.close();
			return;
		}

		Service[] services = Service.fromSwaggerPaths(paths,defs);
		for(Service service : services){
			String text = XmlUtil.objectToXmlString(service);
			FileManager.writeFile(new File( outPath+ "/" + service.getSimpleName() + ".xml"), text);		
		}
		sc.close();
	}
}
