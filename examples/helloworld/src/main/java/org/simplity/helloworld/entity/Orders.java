package org.simplity.helloworld.entity;

import java.util.Date;

public class Orders {	
	private double ordNum;
	double ordAmount;
	double advanceAmount;
	Date ordDate;
	String custCode;
	String agentCode;
	String ordDescription;
	public double getOrdNum() {
		return ordNum;
	}
	public void setOrdNum(float ordNum) {
		this.ordNum = ordNum;
	}
	public double getOrdAmount() {
		return ordAmount;
	}
	public void setOrdAmount(float ordAmount) {
		this.ordAmount = ordAmount;
	}
	public double getAdvanceAmount() {
		return advanceAmount;
	}
	public void setAdvanceAmount(float advanceAmount) {
		this.advanceAmount = advanceAmount;
	}
	public Date getOrdDate() {
		return ordDate;
	}
	public void setOrdDate(Date ordDate) {
		this.ordDate = ordDate;
	}
	public String getCustCode() {
		return custCode;
	}
	public void setCustCode(String custCode) {
		this.custCode = custCode;
	}
	public String getAgentCode() {
		return agentCode;
	}
	public void setAgentCode(String agentCode) {
		this.agentCode = agentCode;
	}
	public String getOrdDescription() {
		return ordDescription;
	}
	public void setOrdDescription(String ordDescription) {
		this.ordDescription = ordDescription;
	}	

}
