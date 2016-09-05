#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import ${groupId}.kernel.comp.ComponentType;
import ${groupId}.kernel.comp.ValidationContext;
import ${groupId}.kernel.db.DbAccessType;
import ${groupId}.kernel.db.DbDriver;
import ${groupId}.kernel.value.Value;
import ${groupId}.service.ServiceContext;
import ${groupId}.service.ServiceData;
import ${groupId}.service.ServiceInterface;

/**
 * @author admin
 *
 */
public class HelloJavaDirect implements ServiceInterface {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.kernel.comp.Component${symbol_pound}getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.kernel.comp.Component${symbol_pound}getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.kernel.comp.Component${symbol_pound}getReady()
	 */
	@Override
	public void getReady() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ${groupId}.service.ServiceInterface${symbol_pound}executeAsAction(${groupId}.service
	 * .ServiceContext, ${groupId}.kernel.db.DbDriver)
	 */
	@Override
	public Value executeAsAction(ServiceContext ctx, DbDriver driver) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.service.ServiceInterface${symbol_pound}toBeRunInBackground()
	 */
	@Override
	public boolean toBeRunInBackground() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.service.ServiceInterface${symbol_pound}okToCache()
	 */
	@Override
	public boolean okToCache() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.service.ServiceInterface${symbol_pound}getDataAccessType()
	 */
	@Override
	public DbAccessType getDataAccessType() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.service.ServiceInterface${symbol_pound}respond(${groupId}.service.
	 * ServiceData)
	 */
	@Override
	public ServiceData respond(ServiceData inputData) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ${groupId}.kernel.comp.Component${symbol_pound}validate(${groupId}.kernel.comp.
	 * ValidationContext)
	 */
	@Override
	public int validate(ValidationContext ctx) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ${groupId}.kernel.comp.Component${symbol_pound}getComponentType()
	 */
	@Override
	public ComponentType getComponentType() {
		return ComponentType.SERVICE;
	}

}
