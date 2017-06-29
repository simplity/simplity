/*
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.service;

import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageBox;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;

/**
 * @author simplity.org
 *
 */
public abstract class AbstractService implements ServiceInterface {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return this.getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		return this.getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getReady()
	 */
	@Override
	public void getReady() {
		// this class is ever-ready
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.comp.Component#validate(org.simplity.kernel.comp.
	 * ValidationContext)
	 */
	@Override
	public int validate(ValidationContext ctx) {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getComponentType()
	 */
	@Override
	public ComponentType getComponentType() {
		return ComponentType.SERVICE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.service.ServiceInterface#executeAsAction(org.simplity.
	 * service
	 * .ServiceContext, org.simplity.kernel.db.DbDriver)
	 */
	@Override
	public Value executeAsAction(ServiceContext ctx, DbDriver driver, boolean useOwnDriverForTransaction) {
		Tracer.trace("Service " + this.getQualifiedName() + " is run as sub-service action..");
		return Value.VALUE_TRUE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ServiceInterface#respond(org.simplity.service.
	 * ServiceData)
	 */
	@Override
	public ServiceData respond(ServiceData inputData, PayloadType payloadType) {
		/*
		 * concrete classes should over-ride this. Instead of making this an
		 * abstract method, we have given a default implementation that does
		 * input-output. but no processing
		 */
		ServiceContext ctx = this.createDefaultContext(inputData, true);
		/*
		 * No dbDriver
		 */
		DbDriver driver = null;
		/*
		 * we can put our logic in executeAsAction so that this service is
		 * available as service as well as sub-service
		 */
		this.executeAsAction(ctx, driver, false);

		ServiceData outData = this.createDefaultOutput(ctx);
		return outData;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ServiceInterface#toBeRunInBackground()
	 */
	@Override
	public boolean toBeRunInBackground() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ServiceInterface#okToCache()
	 */
	@Override
	public boolean okToCache(ServiceData inData) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ServiceInterface#getDataAccessType()
	 */
	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.NONE;
	}

	/**
	 * extract all data coming in as they are..
	 *
	 * @param inData
	 * @param extractPayloadAsWell
	 * @return a service context with all default values loaded into it
	 */
	protected ServiceContext createDefaultContext(ServiceData inData, boolean extractPayloadAsWell) {
		ServiceContext ctx = new ServiceContext(this.getQualifiedName(), inData.getUserId());
		/*
		 * copy values and data sheets sent by the client agent.
		 * These are typically session-stored, but not necessarily that
		 */
		for (String key : inData.getFieldNames()) {
			Object val = inData.get(key);
			if (val instanceof Value) {
				ctx.setValue(key, (Value) val);
			} else if (val instanceof DataSheet) {
				ctx.putDataSheet(key, (DataSheet) val);
			} else {
				ctx.setObject(key, val);
			}
		}
		MessageBox box = inData.getMessageBox();
		if(box != null){
			ctx.setMessageBox(box);
		}
		if (extractPayloadAsWell == false) {
			return ctx;
		}

		String payload = inData.getPayLoad();
		if (payload == null) {
			Tracer.trace("No input from client");
			return ctx;
		}

		JsonUtil.extractAll(payload, ctx);
		Tracer.trace(ctx.getAllFields().size() + " fields extracted ");
		return ctx;
	}

	/**
	 * default response is created with all data available in context
	 *
	 * @param ctx
	 * @return output service data
	 */
	protected ServiceData createDefaultOutput(ServiceContext ctx) {
		ServiceData outData = new ServiceData(ctx.getUserId(), this.getQualifiedName());
		for (FormattedMessage msg : ctx.getMessages()) {
			outData.addMessage(msg);
		}
		outData.setPayLoad(JsonUtil.outputAll(ctx));
		return outData;
	}
}
