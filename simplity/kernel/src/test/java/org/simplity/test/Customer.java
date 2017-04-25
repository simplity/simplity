package org.simplity.test;

public class Customer {
	private int customerNumber;
	private String customerName;
	private String address;
	private String city;
	private String state;
	private String country;
	private int postalCode;	
	
	public Customer() {
		super();
	}
	
	public Customer(int customerNumber, String customerName, String address, String city, String state, String country,
			int postalCode) {
		super();
		this.customerNumber = customerNumber;
		this.customerName = customerName;
		this.address = address;
		this.city = city;
		this.state = state;
		this.country = country;
		this.postalCode = postalCode;
	}
	public int getCustomerNumber() {
		return customerNumber;
	}
	public void setCustomerNumber(int customerNumber) {
		this.customerNumber = customerNumber;
	}
	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	
	
}
