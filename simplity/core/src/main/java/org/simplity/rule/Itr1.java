package org.simplity.rule;

import org.simplity.rule.InvalidRuleException;
/**
 * generated class for rule set itr1
 * generated at 2019-04-09T19:18:40Z
 **/
public class Itr1 extends AbstractCalculator {
	private static final String[] GLOBALS = {"incomeFromHouseProperty", "houseValueAfterStandardDeduction", "assesseeCategory"};
	private static final int NBR_GLOBALS = 3;
	private static final long[] C = {0, 0, 0, 0, 0, 0, 150000, 140000, 0, 0, 0, 0, 0, 0, 0, 0, 50000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 300000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 200000, 0, 0, 0};
	
	private static final String[] CONSTANTS = {"deductedMeedicalExpense", "deducted80ddbType", "IncomeAfterDeduction", "educationalCess", "deducted80ee", "deducted80gg", "max80c", "max80ccd2", "lateFilingFee", "deducted80ccd1b", "deducted80tta", "balancePayable", "deducted80ddb", "deducted80rrb", "totalTaxPlusInterest", "totalInterest", "max80ccd1b", "deducted80ccdEmployer", "totalTaxPayable", "deductedPreventiveExpense", "deducted80g", "interest234c", "deducted80ccdEmployee", "taxPayableAfterRebate", "interest234b", "interest234a", "totalDeducted", "totalPaid", "maxHousingInterestForSenior", "deducted80ddType", "deducted80e", "taxPayableWithCess", "deducted80dd", "refundDue", "deductedHealthInsurance", "grossTotalIncome", "deducted80gga", "deducted80u", "deducted80ccc", "deducted80ggc", "maxHousingInterestForNonSenior", "deducted80qqb", "netTaxLiability", "deducted80ccg"};
	private static final String[] INPUTS = {"age", "gender", "employerCategory", "PortugeseSectionApplicable", "salary", "allowanceNonExempt", "perquisite", "profitDeemedAsSalary", "deduction16", "housePropertyType", "rentReceived", "proprtyTaxPaid", "annualRentalValue", "interestOnHousingLoan", "otherIncome", "deduction80c", "deduction80ccc", "deduction80ccdEmployee", "deduction80ccd1b", "deduction80ccdEmployer", "deductionHealthInsuranceType", "deductionHealthInsurance", "deductionMedicalExpenseType", "deductionMeedicalExpense", "deductionPreventiveExpenseType", "deductionPreventiveExpense", "deduction80ddType", "deduction80dd", "deduction80ddbType", "deduction80ddb", "deduction80e", "deduction80ee", "deduction80g", "deduction80gg", "deduction80gga", "deduction80ggc", "deduction80u", "deduction80rrb", "deduction80qqb", "deduction80ccg", "deduction80tta", "rebate87a", "section89", "tdsPaid", "tcsPaid", "selfAssessmentPaid"};
	private static final String[] OUTPUTS = {"grossTotalIncome", "houseValueAfterStandardDeduction", "incomeFromHouseProperty", "deducted80ccc", "deducted80ccdEmployee", "deducted80ccd1b", "deducted80ccdEmployer", "deductedHealthInsurance", "deductedMeedicalExpense", "deductedPreventiveExpense", "deducted80ddType", "deducted80dd", "deducted80ddbType", "deducted80ddb", "deducted80e", "deducted80ee", "deducted80g", "deducted80gg", "deducted80gga", "deducted80ggc", "deducted80u", "deducted80rrb", "deducted80qqb", "deducted80ccg", "deducted80tta", "totalDeducted", "IncomeAfterDeduction", "totalTaxPayable", "taxPayableAfterRebate", "educationalCess", "taxPayableWithCess", "netTaxLiability", "lateFilingFee", "interest234a", "interest234b", "interest234c", "totalInterest", "totalTaxPlusInterest", "totalPaid", "balancePayable", "refundDue"};
	@Override
	public String[] getInputParameters(){
		return INPUTS;
	}
	@Override
	public String[] getOutputParameters(){
		return OUTPUTS;
	}
	@Override
	public String[] getConstants(){
		return CONSTANTS;
	}
	@Override
	public String[] getGlobalParameters(){
		return GLOBALS;
	}
	@Override
	public long[] getConstantValues(){
		return C;
	}
	@Override
	protected void init(long[] constValues, long[]inputValues){
		this.cVal = constValues;
		this.iVal = inputValues;
		this.gVal = new long[NBR_GLOBALS];
		this.isReady = new boolean[NBR_GLOBALS];
		this.isLooping = new boolean[NBR_GLOBALS];
	}
	@Override
	public long[] doCalculate(long[] inp) throws InvalidRuleException{
		this.init(C, inp);
		long[] _r = {0, this.f1(), this.f0(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		return _r;
	}

	private  void calced(int idx){
		this.isReady[idx] = true;
		this.isLooping[idx] = false;
	}
	private  long f0() throws InvalidRuleException{
		if (this.isReady[0]){
			return this.gVal[0];
		}
		if (this.isLooping[0]){
			throw new InvalidRuleException("Rule for calculating incomeFromHouseProperty is recursively dependant on itself. This leads to infinite loop.");
		}
		this.isLooping[0] = true;
		long _r = 0;
		long _maxInterest = 0;
		if(this.f2() == 2 || this.f2() == 3){
			_maxInterest = this.cVal[28];
		} else {
			_maxInterest = this.cVal[40];
		}
		long _self = 0;
		if(this.iVal[9] != 0){
			_self = 0;
		} else if(this.iVal[13] > _maxInterest){
			_self = -_maxInterest;
		} else {
			_self = -this.iVal[13];
		}
		long _rented = 0;
		if(this.iVal[9] != 1){
			_rented = 0;
		} else if(this.iVal[12] <= this.iVal[11]){
			_rented = 0;
		} else {
			_rented = this.f1() - this.iVal[13];
		}
		if(this.iVal[9] != 0){
			_r = _self;
		} else {
			_r = _rented;
		}
		this.gVal[0] = _r;
		this.calced(0);
		return _r;
	}
	private  long f1() throws InvalidRuleException{
		if (this.isReady[1]){
			return this.gVal[1];
		}
		if (this.isLooping[1]){
			throw new InvalidRuleException("Rule for calculating houseValueAfterStandardDeduction is recursively dependant on itself. This leads to infinite loop.");
		}
		this.isLooping[1] = true;
		long _r = 0;
		if(this.iVal[9] != 1){
			_r = 0;
		} else if(this.iVal[12] <= this.iVal[11]){
			_r = 0;
		} else {
			_r = ((this.iVal[12] - this.iVal[11]) * 7) / 10;
		}
		this.gVal[1] = _r;
		this.calced(1);
		return _r;
	}
	private  long f2() throws InvalidRuleException{
		if (this.isReady[2]){
			return this.gVal[2];
		}
		if (this.isLooping[2]){
			throw new InvalidRuleException("Rule for calculating assesseeCategory is recursively dependant on itself. This leads to infinite loop.");
		}
		this.isLooping[2] = true;
		long _r = 0;
		if(this.iVal[0] > 79){
			_r = 3;
		} else if(this.iVal[0] > 59){
			_r = 2;
		} else if(this.iVal[1] == 0){
			_r = 0;
		} else {
			_r = 1;
		}
		this.gVal[2] = _r;
		this.calced(2);
		return _r;
	}
}
