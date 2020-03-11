package ofbiz.android.pos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ServiceUtil;

public class BaseCreditAndCreditTxn {
	
	//  <!---- Starting of Credit Logic ---->
	public static BigDecimal getTotalRetailorCreditAmtToCustomer(Delegator delegator,String productStoreId,
			String customerId) {
		
		Long retailor = 1l;
		Long customer = 0l;
		List<GenericValue> retailorGivenCreditList = new LinkedList<GenericValue>();
		BigDecimal retailorGivenCreditAmt = BigDecimal.ZERO;
		BigDecimal totRetailorGivenCreditAmt = BigDecimal.ZERO;
		
		try {
			//Get Retailor(productStoreId) total given creditAmount to customer
			//condition(productStoreId=productStoreId,customerId=customerId,retailer=1,customer=0)
			EntityCondition retailorGivenAmtCond = EntityCondition.makeCondition(EntityOperator.AND,
					EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS,productStoreId),
					EntityCondition.makeCondition("customerId", EntityOperator.EQUALS, customerId),
					EntityCondition.makeCondition("retailer", EntityOperator.EQUALS,retailor),
					EntityCondition.makeCondition("customer", EntityOperator.EQUALS,customer),
					EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CREATED"),
					EntityCondition.makeCondition("type", EntityOperator.EQUALS, "CREDIT"));
			retailorGivenCreditList = EntityQuery.use(delegator).from("PosCredit").where(retailorGivenAmtCond)
					.queryList();
			for (GenericValue retailorGivenCredit : retailorGivenCreditList) {
				retailorGivenCreditAmt = retailorGivenCredit.getBigDecimal("creditAmount");
				totRetailorGivenCreditAmt = totRetailorGivenCreditAmt.add(retailorGivenCreditAmt);
			}
			
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return totRetailorGivenCreditAmt;
	}
	
	public static BigDecimal getTotalCustomerCreditPaidAmtToRetailor(Delegator delegator,String productStoreId,
			String customerId) {
		
		Long retailor = 0l;
		Long customer = 1l;
		List<GenericValue> customerPaidCreditList = new LinkedList<GenericValue>();
		BigDecimal customerPaidCreditAmt = BigDecimal.ZERO;
		BigDecimal totCustomerPaidCreditAmt = BigDecimal.ZERO;
		
		try {
			//Get Customer(customerId) total paid creditAmount to retailor
			//condition(productStoreId=productStoreId,customerId=customerId,retailer=0,customer=1)
			EntityCondition customerPaidAmtCond = EntityCondition.makeCondition(EntityOperator.AND,
					EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS,productStoreId),
					EntityCondition.makeCondition("customerId", EntityOperator.EQUALS, customerId),
					EntityCondition.makeCondition("retailer", EntityOperator.EQUALS, retailor),
					EntityCondition.makeCondition("customer", EntityOperator.EQUALS, customer),
					EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAID"),
					EntityCondition.makeCondition("type", EntityOperator.EQUALS, "CREDI"));
			
			customerPaidCreditList = EntityQuery.use(delegator).from("PosCredit").where(customerPaidAmtCond)
					.queryList();
			for (GenericValue customerPaidAmt : customerPaidCreditList) {
				customerPaidCreditAmt = customerPaidAmt.getBigDecimal("paidAmount");
				totCustomerPaidCreditAmt = totCustomerPaidCreditAmt.add(customerPaidCreditAmt);
			}
			
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return totCustomerPaidCreditAmt;
	}

	//   <!---- Ending of Credit Logic ---->
	
	//  <!---- Starting of Credit-Note Logic ---->
	public static BigDecimal getTotalRetailorCreditNoteAmtToCustomer(Delegator delegator,String productStoreId,
			String customerId) {
		
		Long retailor = 1l;
		Long customer = 0l;
		List<GenericValue> retailorGivenCreditList = new LinkedList<GenericValue>();
		BigDecimal retailorGivenCreditNoteAmt = BigDecimal.ZERO;
		BigDecimal totRetailorGivenCreditNoteAmt = BigDecimal.ZERO;
		
		try {
			//Get Retailor(productStoreId) total given creditAmount to customer
			//condition(productStoreId=productStoreId,customerId=customerId,retailer=1,customer=0)
			EntityCondition retailorGivenAmtCond = EntityCondition.makeCondition(EntityOperator.AND,
					EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS,productStoreId),
					EntityCondition.makeCondition("customerId", EntityOperator.EQUALS, customerId),
					EntityCondition.makeCondition("retailer", EntityOperator.EQUALS,retailor),
					EntityCondition.makeCondition("customer", EntityOperator.EQUALS,customer),
					EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CREATED"),
					EntityCondition.makeCondition("type", EntityOperator.EQUALS, "CREDIT_NOTE"));
			retailorGivenCreditList = EntityQuery.use(delegator).from("PosCredit").where(retailorGivenAmtCond)
					.queryList();
			for (GenericValue retailorGivenCredit : retailorGivenCreditList) {
				retailorGivenCreditNoteAmt = retailorGivenCredit.getBigDecimal("creditNoteAmount");
				totRetailorGivenCreditNoteAmt = totRetailorGivenCreditNoteAmt.add(retailorGivenCreditNoteAmt);
			}
			
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return totRetailorGivenCreditNoteAmt;
	}
	
	public static BigDecimal getTotalCustomerCreditNotePaidAmtToRetailor(Delegator delegator,String productStoreId,
			String customerId) {
		
		Long retailor = 0l;
		Long customer = 1l;
		List<GenericValue> customerPaidCreditList = new LinkedList<GenericValue>();
		BigDecimal customerPaidCreditNoteAmt = BigDecimal.ZERO;
		BigDecimal totCustomerPaidCreditNoteAmt = BigDecimal.ZERO;
		
		try {
			//Get Customer(customerId) total paid creditAmount to retailor
			//condition(productStoreId=productStoreId,customerId=customerId,retailer=0,customer=1)
			EntityCondition customerPaidAmtCond = EntityCondition.makeCondition(EntityOperator.AND,
					EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS,productStoreId),
					EntityCondition.makeCondition("customerId", EntityOperator.EQUALS, customerId),
					EntityCondition.makeCondition("retailer", EntityOperator.EQUALS, retailor),
					EntityCondition.makeCondition("customer", EntityOperator.EQUALS, customer),
					EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "PAID"),
					EntityCondition.makeCondition("type", EntityOperator.EQUALS, "CREDIT_NOTE"));
			
			customerPaidCreditList = EntityQuery.use(delegator).from("PosCredit").where(customerPaidAmtCond)
					.queryList();
			for (GenericValue customerPaidAmt : customerPaidCreditList) {
				customerPaidCreditNoteAmt = customerPaidAmt.getBigDecimal("paidAmount");
				totCustomerPaidCreditNoteAmt = totCustomerPaidCreditNoteAmt.add(customerPaidCreditNoteAmt);
			}
			
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return totCustomerPaidCreditNoteAmt;
	}
	
	public static String getCustomerIdByMobileNum(Delegator delegator,String customerMobileNo) {
		
		String customerId = "";
		try {
        	if(UtilValidate.isNotEmpty(customerMobileNo)) {
        		customerId = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("contactNumber", customerMobileNo).
        				orderBy("contactNumber").queryFirst().getString("partyId");
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return customerId;
	}
	
	public static String getCustomerName(Delegator delegator,String customerId) {
		
	String customerName = "";
		try {
        	if(UtilValidate.isNotEmpty(customerId)) {
        		GenericValue supplierPerson = EntityQuery.use(delegator).from("Person").where("partyId",customerId).queryOne();
        		if(UtilValidate.isNotEmpty(supplierPerson.getString("firstName"))) {
        			customerName = supplierPerson.getString("firstName");
        		}
        		if(UtilValidate.isNotEmpty(supplierPerson.getString("lastName"))) {
        			customerName = customerName +" "+ supplierPerson.getString("lastName");
        		}
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return customerName;
	}
	
	public static String getCustomerMobileNumById(Delegator delegator,String customerId) {
		
		String contactNumber = "";
		try {
        	if(UtilValidate.isNotEmpty(customerId)) {
        		contactNumber = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("partyId", customerId).
        				orderBy("contactNumber").queryFirst().getString("contactNumber");
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return contactNumber;
	}
	
	public static BigDecimal getCustomerTotalCreditAmtById(Delegator delegator,String customerId) {
		
		List<GenericValue> customerCreditTxns = new ArrayList<GenericValue>();
		BigDecimal totCreditAmt = BigDecimal.ZERO;
		BigDecimal creditAmt = BigDecimal.ZERO;
		try {
        	if(UtilValidate.isNotEmpty(customerId)) {
        		customerCreditTxns = EntityQuery.use(delegator).from("PosCredit").where("customerId", customerId, "customer", 
        				Long.parseLong("0"), "retailer", Long.parseLong("1"),"type", "CREDIT").queryList();
        		for (GenericValue customerCredit : customerCreditTxns) {
        			if(UtilValidate.isNotEmpty(customerCredit.getBigDecimal("creditAmount"))) {
        				creditAmt = customerCredit.getBigDecimal("creditAmount");
        			}
        			totCreditAmt = totCreditAmt.add(creditAmt);
        		}
        				
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return totCreditAmt;
	}

	public static BigDecimal getCustomerCreditPaidAmtById(Delegator delegator,String customerId) {
		
		List<GenericValue> customerCreditTxns = new ArrayList<GenericValue>();
		BigDecimal totPaidAmount = BigDecimal.ZERO;
		BigDecimal paidAmount = BigDecimal.ZERO;
		try {
        	if(UtilValidate.isNotEmpty(customerId)) {
        		customerCreditTxns = EntityQuery.use(delegator).from("PosCredit").where("customerId", customerId, "customer", Long.parseLong("1"),
        				"retailer", Long.parseLong("0"),"type", "CREDIT").queryList();
        		for (GenericValue customerCredit : customerCreditTxns) {
        			paidAmount = customerCredit.getBigDecimal("paidAmount");
        			totPaidAmount = totPaidAmount.add(paidAmount);
        		}
        				
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return totPaidAmount;
	}
	
	public static BigDecimal getCustomerTotalCreditNoteAmtById(Delegator delegator,String customerId) {
		
		List<GenericValue> customerCreditTxns = new ArrayList<GenericValue>();
		BigDecimal creditNoteAmount = BigDecimal.ZERO;
		BigDecimal totCreditNoteAmt = BigDecimal.ZERO;
		
		BigDecimal creditNoteRedeemAmount = BigDecimal.ZERO;
		BigDecimal totCreditNoteRedeemAmount = BigDecimal.ZERO;
		
		BigDecimal totCreditNoteAmount = BigDecimal.ZERO;
		try {
        	if(UtilValidate.isNotEmpty(customerId)) {
        		//get Total Credit Note Amt
        		customerCreditTxns = EntityQuery.use(delegator).from("PosCredit").where("customerId", customerId, "customer", 
        				Long.parseLong("0"), "retailer", Long.parseLong("1"),"type", "CREDIT_NOTE").queryList();
        		for (GenericValue customerCredit : customerCreditTxns) {
        			if(UtilValidate.isNotEmpty(customerCredit.getBigDecimal("creditNoteAmount"))) {
        				creditNoteAmount = customerCredit.getBigDecimal("creditNoteAmount");
        			}
        			totCreditNoteAmt = totCreditNoteAmt.add(creditNoteAmount);
        		}
        		//Get Total Customer redeem Amt
        		customerCreditTxns = EntityQuery.use(delegator).from("PosCredit").where("customerId", customerId, "customer", 
        				Long.parseLong("1"), "retailer", Long.parseLong("0"),"type", "CREDIT_NOTE").queryList();
        		for (GenericValue customerCredit : customerCreditTxns) {
        			if(UtilValidate.isNotEmpty(customerCredit.getBigDecimal("paidAmount"))) {
        				creditNoteRedeemAmount = customerCredit.getBigDecimal("paidAmount");
        			}
        			totCreditNoteRedeemAmount = totCreditNoteRedeemAmount.add(creditNoteRedeemAmount);
        		}
        		//CreditNote amt = totCreditNoteAmt - 
        		totCreditNoteAmount = totCreditNoteAmt.subtract(totCreditNoteRedeemAmount);
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return totCreditNoteAmount;
	}
	public static Map<String, Object> getCustomerTotalCreditNoteAmtByBillId(Delegator delegator,String customerId, String billId, String cnId) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue customerCreditTxns = new GenericValue();
		GenericValue cusCreditNotePaidTxns = new GenericValue();
		BigDecimal creditNoteAmount = BigDecimal.ZERO;
		BigDecimal totCreditNoteAmt = BigDecimal.ZERO;
		
		BigDecimal creditNoteRedeemAmount = BigDecimal.ZERO;
		BigDecimal totCreditNoteRedeemAmount = BigDecimal.ZERO;
		BigDecimal creditNoteBalanceAmount = BigDecimal.ZERO;
		
		BigDecimal creditNoteDueAmount = BigDecimal.ZERO;
		try {
        	if(UtilValidate.isNotEmpty(customerId)) {
        		//get Total Credit Note Amt
        		
        		customerCreditTxns = EntityQuery.use(delegator).from("PosCredit").where("creditId", cnId).queryOne();
        		
    			if(UtilValidate.isNotEmpty(customerCreditTxns.getBigDecimal("creditNoteAmount"))) {
    				creditNoteAmount = customerCreditTxns.getBigDecimal("creditNoteAmount");
    			}
        		//Get Total Customer redeem Amt
    			cusCreditNotePaidTxns = EntityQuery.use(delegator).from("PosCredit").where("creditId", cnId).queryOne();
    			if(UtilValidate.isNotEmpty(cusCreditNotePaidTxns.getBigDecimal("balanceAmount"))) {
    				creditNoteBalanceAmount = cusCreditNotePaidTxns.getBigDecimal("balanceAmount");
    			}
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		result.put("creditNoteAmount", creditNoteAmount);
		result.put("creditNoteBalanceAmount", creditNoteBalanceAmount);
		return result;
	}
	public static BigDecimal getBillIdAmt(Delegator delegator,String receiptId) {
		
		BigDecimal billAmt = BigDecimal.ZERO;
		GenericValue billTxn = new GenericValue();
		try {
        	if(UtilValidate.isNotEmpty(receiptId)) {
        		billTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).
        				orderBy("receiptId").queryOne();
        		if(UtilValidate.isNotEmpty(billTxn)) {
        			billAmt = billTxn.getBigDecimal("totalBillAmount");
        			billAmt = billAmt.setScale(0, BigDecimal.ROUND_HALF_UP);
            		billAmt = billAmt.setScale(2, BigDecimal.ROUND_HALF_UP);
        		}
        		
        	}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		return billAmt;
	}
	public static List<String> sortedList (List<String> toBeSortList) {
		
		List<Long> stringToIntList = new ArrayList<Long>();
		List<String> sortedList = new ArrayList<String>();
		Long parseVal = 0l;
		try {
			for(String listEach : toBeSortList) {
				parseVal = Long.parseLong(listEach.trim());
				stringToIntList.add(parseVal);
			}
			Collections.sort(stringToIntList, Collections.reverseOrder());
			for(Long intListEach : stringToIntList) {
				sortedList.add(intListEach.toString());
			}
		}catch (NumberFormatException e) {
			// TODO: handle exception
		}
		return sortedList;
	}
	public static BigDecimal getCustomerCreditLimit(Delegator delegator,String customerId) {
		
		BigDecimal creditLimit = BigDecimal.ZERO;
			try {
	        	if(UtilValidate.isNotEmpty(customerId)) {
	        		GenericValue partyVal = EntityQuery.use(delegator).from("Party").where("partyId",customerId).queryOne();
	        		if(UtilValidate.isNotEmpty(partyVal)) {
	        			creditLimit = partyVal.getBigDecimal("creditLimit");
	        		}
	        	}
			}catch (GenericEntityException e) {
				// TODO: handle exception
			}
			return creditLimit;
		}
/**
 * Method Name: convertStringToTimestamp
 * Method Description: Method to convert String date val to Timestamp
 * @return
 */
public static Timestamp convertStringToTimestamp(String strDate) {
	try {
      DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
       // you can change format of date
      Date date = formatter.parse(strDate);
      Timestamp timeStampDate = new Timestamp(date.getTime());

      return timeStampDate;
    } catch (ParseException e) {
      System.out.println("Exception :" + e);
      return null;
    }
}
public static String convertStringToTmstp(String strDate) {
	String convertDate = "";
    try {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat formatter1 = new SimpleDateFormat("dd/MM/yyyy");
        Date date = formatter.parse(strDate);
        convertDate = formatter1.format(date);

        return convertDate;
    } catch (ParseException e) {
        System.out.println("Exception :" + e);
        return null;
    }
}
	
}
