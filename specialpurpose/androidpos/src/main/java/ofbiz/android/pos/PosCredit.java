package ofbiz.android.pos;

import java.util.Map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;


public class PosCredit extends BaseCreditAndCreditTxn
{
	public static final String module = PosCredit.class.getName();
	
	public static Map<String, Object> findPosCredit(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();

            Map<String, String> fields = new HashMap<>();
            if (context.containsKey("creditId")) {
                fields.put("userId", (String) context.get("userId"));
            }

            List<GenericValue> posCredit = delegator.findByAnd("posCredit", fields, new ArrayList<>(), false);

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("posCredit", posCredit);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in finding posCredit in " + module);
        }
    }
	
	public static Map<String, Object> createPosRetailorCredit(DispatchContext dispatchContext,
			Map<String, ? extends Object> context)
	{
		//delegator object(Stores all CRUD / Dao in this object)
		Delegator delegator = dispatchContext.getDelegator();
		//get input parameters
		String customerMobileNum = (String) context.get("customerMobileNum");
		String customerId 		= (String) context.get("customerId");
		String creditAmount 	= (String) context.get("creditAmount");
		String productStoreId 	= (String) context.get("productStoreId");
		String billId 			= (String) context.get("billId");
		String type 			= (String) context.get("type");
		String retailer 		= (String) context.get("retailer");
		String customer 		= (String) context.get("customer");
		
		String creditId = "";
		Timestamp now = UtilDateTime.nowTimestamp();
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			if (UtilValidate.isNotEmpty(customerMobileNum)) {
				customerId = getCustomerIdByMobileNum(delegator, customerMobileNum);
			}
			//code for create
			//customer date type
			//Since retailor issues credit to customer(retailor-1 & customer-0)
			creditId = delegator.getNextSeqId("PosCredit");						//PosCredit is Table Name
			GenericValue createPosCredit = delegator.makeValue("PosCredit", UtilMisc.toMap("creditId", creditId, 
																			"customerId", customerId, 
																			"billId", billId,
																			"productStoreId",productStoreId,
																			"creditAmount", new BigDecimal(creditAmount),
																			"statusId", "CREATED",
																			"retailer", Long.parseLong(retailer),
																			"customer", Long.parseLong(customer), 
																			"type", type, 
																			"date", now));
			createPosCredit.create();
			
		} catch (GenericEntityException e) {
			result.put("ERROR_MSG", "ERROR");
			return ServiceUtil.returnError("Error in crating posCredit in " + module);
		}
		result.put("SUCCESS_MSG", "SUCCESS");
		result.put("creditId", creditId);
		return result;
	}
	
	public static Map<String, Object> createPosCustomerCredit(DispatchContext dispatchContext,
					Map<String, ? extends Object> context)
	{
		Delegator delegator = dispatchContext.getDelegator();
		
		String customerMobileNum 	= (String) context.get("customerMobileNum");
		String productStoreId 	= (String) context.get("productStoreId");
		String customerId 		= (String) context.get("customerId");
		String billId 			= (String) context.get("billId");
		String paidAmount 		= (String) context.get("paidAmount");
		String dayId 			= (String) context.get("dayId");
		String customer 		= (String) context.get("customer");
		String retailer 		= (String) context.get("retailer");
		String type 			= (String) context.get("type");
		
		String creditId = "";
		Timestamp now = UtilDateTime.nowTimestamp();
		BigDecimal totRetailorGivenCreditAmt = BigDecimal.ZERO;
		BigDecimal totCustomerPaidCreditAmt = BigDecimal.ZERO;
		BigDecimal balanceAmt = BigDecimal.ZERO;
		BigDecimal creditPaidAmt = BigDecimal.ZERO;
		
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			if (UtilValidate.isNotEmpty(customerMobileNum)) {
				customerId = getCustomerIdByMobileNum(delegator, customerMobileNum);
			}
			//Get totalRetalorCreditAmt given to Customer
			totRetailorGivenCreditAmt = getTotalRetailorCreditAmtToCustomer(delegator, productStoreId, customerId);
			//Get totalCustomerCredit PaidAmt to Retailor
			totCustomerPaidCreditAmt = getTotalCustomerCreditPaidAmtToRetailor(delegator, productStoreId, customerId);
			if(UtilValidate.isNotEmpty(paidAmount)) {
				creditPaidAmt = new BigDecimal(paidAmount);
				totCustomerPaidCreditAmt = totCustomerPaidCreditAmt.add(creditPaidAmt);
			}
			//balanceAmt = SUM(retailorCreditAmt) - SUM(customer paidAmt)
			balanceAmt = totRetailorGivenCreditAmt.subtract(totCustomerPaidCreditAmt);
			
			creditId = delegator.getNextSeqId("PosCredit");
			GenericValue createPosCredit = delegator.makeValue("PosCredit", UtilMisc.toMap("creditId", creditId, 
																			"productStoreId", productStoreId,
																			"customerId", customerId,
																			"billId", billId,
																			"paidAmount", new BigDecimal(paidAmount),
																			"dayId",dayId,
																			"balanceAmount", balanceAmt,
																			"retailer", Long.parseLong(retailer),
																			"customer", Long.parseLong(customer),
																			"type", type, 
																			"date", now, 
																			"statusId", "PAID"));
												createPosCredit.create();
			
		} catch (GenericEntityException e) {
			result.put("ERROR_MSG", "ERROR");
			return ServiceUtil.returnError("Error in crating posCustomerCredit in " + module);
		}
		result.put("SUCCESS_MSG", "SUCCESS");
		result.put("creditId", creditId);
		result.put("totRetailorGivenCreditAmt", totRetailorGivenCreditAmt);
		result.put("totCustomerPaidCreditAmt", totCustomerPaidCreditAmt);
		result.put("balanceAmt", balanceAmt);
		return result;
		
	}
	
	//		Starting of "CreateCreditLimit" to the Customer
	public static Map<String, Object> updateCreditLimit(DispatchContext dispatchContext,
			Map<String, ? extends Object> context)
	{
		
		Delegator delegator = dispatchContext.getDelegator();
		//get input parameters
	
		String creditLimit 	= (String) context.get("creditLimit");
		String partyId = (String) context.get("partyId");
		BigDecimal creditLimitAmt = BigDecimal.ZERO;
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			
			GenericValue partyVal = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
			
			if(UtilValidate.isNotEmpty(partyVal)) {
				creditLimitAmt = new BigDecimal(creditLimit);
				partyVal.put("creditLimit", creditLimitAmt);
				partyVal.store();
			}
			
		} catch (GenericEntityException e) {
			result.put("ERROR_MSG", "ERROR");
			return ServiceUtil.returnError("Error in crating UpdateCreditLimit in " + module);
		}
		result.put("SUCCESS_MSG", "SUCCESS");
		result.put("partyId", partyId);
		return result;
	}
	
public static Map<String, Object> getCustomerCreditTxns (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
	    
		Delegator delegator = dispatchContext.getDelegator();
	        
	        String customerId = (String) context.get("customerId");
	        String productStoreGroupId = (String) context.get("productStoreGroupId");
	        String currencyUomId = (String) context.get("currencyUomId");
	        
	        String customerMobileNo = "";
	        String txnCustomerId = "", customerName = "";
	        BigDecimal totCustomerCreditDue = BigDecimal.ZERO;
	        BigDecimal totCustomerPaidAmt = BigDecimal.ZERO;
	        BigDecimal totCustomerCreditAmt = BigDecimal.ZERO;
	        List<String> customerIds = new ArrayList<String>();
	        BigDecimal creditLimit = BigDecimal.ZERO;
	        BigInteger customerCreditLimit = BigInteger.ZERO;
	        Integer size = 0;
	        List<Map<String, Object>> customerCreditTxnList = new ArrayList<Map<String,Object>>();
	        try {
	        	//get Distinct customers
	        	customerIds = EntityQuery.use(delegator).from("PosCredit").where("statusId", "CREATED", 
	        			"customer", Long.parseLong("0"), "retailer", Long.parseLong("1"),"type", "CREDIT").distinct(true).
	        			getFieldList("customerId");
	        			
	        	for (String customerIdEach : customerIds) {
	        		Map<String, Object> customerMap = new HashMap<String, Object>();
	        		customerCreditLimit = BigInteger.ZERO;
	        		//get customer Name
	        		customerName = getCustomerName(delegator, customerIdEach);
	        		//get customer Mobile No
	        		customerMobileNo = getCustomerMobileNumById(delegator, customerIdEach);
	        		//get customer creditDue
	        		totCustomerPaidAmt = getCustomerCreditPaidAmtById(delegator, customerIdEach);
	        		//get customer total credit
	        		totCustomerCreditAmt = getCustomerTotalCreditAmtById(delegator, customerIdEach);
	        		
	        		//get customer credit due
	        		totCustomerCreditDue = totCustomerCreditAmt.subtract(totCustomerPaidAmt);
	        		//get customer credit limit
	        		creditLimit = getCustomerCreditLimit(delegator, customerIdEach);
	        		if(UtilValidate.isNotEmpty(creditLimit)) {
	        			customerCreditLimit = creditLimit.toBigInteger();
	        		}

	        		customerMap.put("customerId", customerIdEach);
	        		customerMap.put("customerName", customerName);
	        		customerMap.put("customerMobileNo", customerMobileNo);
	        		customerMap.put("totCustomerCreditDue", totCustomerCreditDue);
	        		customerMap.put("totCustomerCreditAmt", totCustomerCreditAmt);
				    customerMap.put("customerCreditLimit", customerCreditLimit);
	        		
	        		customerCreditTxnList.add(customerMap);
	        	}
	        	
	        } catch (Exception e) {
				// TODO: handle exception
			}
	        size = customerCreditTxnList.size();
		System.out.println("size----------------"+size);
	        Map<String, Object> result = ServiceUtil.returnSuccess();
	        result.put("customerCreditTxnList", customerCreditTxnList);
	        return result;
	}
	
	public static Map<String, Object> getCustomerTotalCreditAmt (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
        String customerId = (String) context.get("customerId");
        String contactNumber = (String) context.get("contactNumber");
        BigDecimal totCreditAmt = BigDecimal.ZERO;
        
        if(UtilValidate.isEmpty(customerId)) {
        	customerId = getCustomerIdByMobileNum(delegator, contactNumber);
        	totCreditAmt = getCustomerTotalCreditAmtById(delegator, customerId);
        }else {
        	totCreditAmt = getCustomerTotalCreditAmtById(delegator, customerId);
        }
        
        result.put("totCreditAmt", totCreditAmt);
        return result;
	}
	
	public static Map<String, Object> getCustomerTotalCreditDue (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
        String customerId = (String) context.get("customerId");
        String contactNumber = (String) context.get("contactNumber");
        BigDecimal totCreditAmt = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        
        if(UtilValidate.isEmpty(customerId)) {
        	customerId = getCustomerIdByMobileNum(delegator, contactNumber);
        	totCreditAmt = getCustomerTotalCreditAmtById(delegator, customerId);
            paidAmount = getCustomerCreditPaidAmtById(delegator, customerId);
        }else {
        	totCreditAmt = getCustomerTotalCreditAmtById(delegator, customerId);
            paidAmount = getCustomerCreditPaidAmtById(delegator, customerId);
        }
        
        
        
        BigDecimal totCreditDueAmt = totCreditAmt.subtract(paidAmount);
        
        result.put("totCreditDueAmt", totCreditDueAmt);
        return result;
	}
	
	public static Map<String, Object> getCustomerCreditLimit (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
        String customerId = (String) context.get("customerId");
        String contactNumber = (String) context.get("contactNumber");
        BigDecimal creditLimitAmt = BigDecimal.ZERO;
        GenericValue partyVal = new GenericValue();
        
        try {
        	
        	if(UtilValidate.isEmpty(customerId)) {
            	customerId = getCustomerIdByMobileNum(delegator, contactNumber);
            	partyVal = EntityQuery.use(delegator).from("Party").where("partyId", customerId).queryOne();
            	if(UtilValidate.isNotEmpty(partyVal)) {
            		creditLimitAmt = partyVal.getBigDecimal("creditLimit");
            	}
            }else {
            	partyVal = EntityQuery.use(delegator).from("Party").where("partyId", customerId).queryOne();
            	if(UtilValidate.isNotEmpty(partyVal)) {
            		creditLimitAmt = partyVal.getBigDecimal("creditLimit");
            	}
            	creditLimitAmt = partyVal.getBigDecimal("creditLimit");
            }
        	
        } catch (GenericEntityException e) {
        	
        }
        
        result.put("creditLimitAmt", creditLimitAmt);
        return result;
	}
	
	public static Map<String, Object> getCustomerCreditNoteAmt (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
        String customerId = (String) context.get("customerId");
        String contactNumber = (String) context.get("contactNumber");
        BigDecimal totCreditNoteAmount = BigDecimal.ZERO;
        
    	if(UtilValidate.isEmpty(customerId)) {
        	customerId = getCustomerIdByMobileNum(delegator, contactNumber);
        	totCreditNoteAmount = getCustomerTotalCreditNoteAmtById(delegator, customerId);
        }else {
        	totCreditNoteAmount = getCustomerTotalCreditNoteAmtById(delegator, customerId);
        }
        	
        result.put("totCreditNoteAmount", totCreditNoteAmount);
        return result;
	}
	
	public static Map<String, Object> clearRetailorGivenCreditAmt (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
		String receiptId = (String) context.get("receiptId");
	    String customerId = (String) context.get("customerId");
	    String contactNumber = (String) context.get("contactNumber");
	    
		if(UtilValidate.isEmpty(customerId)) {
	    	customerId = getCustomerIdByMobileNum(delegator, contactNumber);
	    	//remove/delete customer paid credit txns
	    	//cond-(customerId=customerId && billId=receiptId && type="CREDIT" && retailor="1" && customer="0")
	    	try {
	    		List<GenericValue> retilorGivenCreditList = EntityQuery.use(delegator).from("PosCredit").
		    			where("customerId", customerId,"billId", receiptId,"type", "CREDIT", "retailer", Long.parseLong("1"),
		    					"customer", Long.parseLong("0")).queryList();
	    		if(UtilValidate.isNotEmpty(retilorGivenCreditList)) {
	    			for (GenericValue retilorGivenCredit : retilorGivenCreditList) {
	    				retilorGivenCredit.remove();
	    			}
	    		}
	    	} catch (GenericEntityException e) {
				// TODO: handle exception
	    		return result;
			}
	    }
	    	
	    return result;
	}
	public static Map<String, Object> clearCustomerRedeemedCreditNoteAmt (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
		String receiptId = (String) context.get("receiptId");
	    String customerId = (String) context.get("customerId");
	    String contactNumber = (String) context.get("contactNumber");
	    
		if(UtilValidate.isEmpty(customerId)) {
	    	customerId = getCustomerIdByMobileNum(delegator, contactNumber);
	    	//remove/delete customer redeemed credit note txns
	    	//cond-(customerId=customerId && billId=receiptId && type="CREDIT" && retailor="0" && customer="1")
	    	try {
	    		List<GenericValue> custoomerRedeemCreditNoteList = EntityQuery.use(delegator).from("PosCredit").
		    			where("customerId", customerId,"billId", receiptId,"type", "CREDIT_NOTE", "retailer", Long.parseLong("0"),
		    					"customer", Long.parseLong("1")).queryList();
	    		if(UtilValidate.isNotEmpty(custoomerRedeemCreditNoteList)) {
	    			for (GenericValue customerRedeemCredit : custoomerRedeemCreditNoteList) {
	    				customerRedeemCredit.remove();
	    			}
	    		}
	    	} catch (GenericEntityException e) {
				// TODO: handle exception
	    		return result;
			}
	    }
	    	
	    return result;
	}
	public static Map<String, Object> getCustomerCreditTakenTxns (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
	        
        String customerId = (String) context.get("customerId");
        String receiptId = "", billDate = "";
        BigDecimal billAmt = BigDecimal.ZERO;
        BigDecimal customerCreditAmt = BigDecimal.ZERO;
        List<GenericValue> customerCreditTxns = new LinkedList<GenericValue>();
        
        List<Map<String, Object>> customerCreditTakenTxnList = new ArrayList<Map<String,Object>>();
        try {
        	//get Retailor given Customer Credit Txns bill wise
        	customerCreditTxns = EntityQuery.use(delegator).from("PosCredit").where("customerId", customerId,
        			"statusId", "CREATED", "customer", Long.parseLong("0"), "retailer", Long.parseLong("1"),
        			"type", "CREDIT").distinct(true).queryList();
        	
        	for (GenericValue customerCreditTxnEach : customerCreditTxns) {
        		Map<String, Object> customerMap = new HashMap<String, Object>();
        		
        		receiptId = customerCreditTxnEach.getString("billId");
        		billDate = customerCreditTxnEach.getString("date");
        		billDate = convertStringToTmstp(billDate);
        		customerCreditAmt = customerCreditTxnEach.getBigDecimal("creditAmount");
        		customerCreditAmt = customerCreditAmt.setScale(0, BigDecimal.ROUND_HALF_UP);
        		customerCreditAmt = customerCreditAmt.setScale(2, BigDecimal.ROUND_HALF_UP);
        		//get Bill Amt
        		billAmt = getBillIdAmt(delegator, receiptId);
        		customerMap.put("receiptId", receiptId);
        		customerMap.put("billDate", billDate);
        		customerMap.put("creditAmt", customerCreditAmt);
        		customerMap.put("billAmt", billAmt);
        		
        		customerCreditTakenTxnList.add(customerMap);
        	}
        	
        } catch (Exception e) {
			// TODO: handle exception
		}
        
        result.put("customerCreditTakenTxnList", customerCreditTakenTxnList);
        return result;
	}
	public static Map<String, Object> getCustomerPrevBill(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
	        
        String customerId = (String) context.get("customerId");
        String contactNumber = (String) context.get("contactNumber");
        String receiptId = "";
        BigDecimal billAmt = BigDecimal.ZERO;
        BigDecimal billReturnAmt = BigDecimal.ZERO;
        BigDecimal creditNoteAmount = BigDecimal.ZERO;
        GenericValue posCartTxn = new GenericValue();
        List<GenericValue> billReturnTxn = new LinkedList<GenericValue>();
        Map<String, Object> billReturnMap = new HashMap<String, Object>();
        
        try {
        	//get Customer Cart Txns
        	posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("customerMobileNo", contactNumber,
        			"posStatus", "COMPLETE").orderBy("-createdStamp").queryFirst();
        	if(UtilValidate.isNotEmpty(posCartTxn)) {
        		receiptId = posCartTxn.getString("receiptId");
            	billAmt = posCartTxn.getBigDecimal("totalBillAmount");
            	billAmt = billAmt.setScale(0, BigDecimal.ROUND_HALF_UP);
            	billAmt = billAmt.setScale(2, BigDecimal.ROUND_HALF_UP);
            	
            	billReturnMap.put("billAmt", billAmt);
            	//Get Customer Bill return Amt
            	billReturnTxn = EntityQuery.use(delegator).from("PosCredit").where("billId", receiptId,"retailer", Long.parseLong("1")).queryList();
            	if(UtilValidate.isNotEmpty(billReturnTxn)) {
            		for (GenericValue billReturnEach : billReturnTxn) {
            			if(UtilValidate.isNotEmpty(billReturnEach.getBigDecimal("creditNoteAmount"))) {
            				creditNoteAmount = billReturnEach.getBigDecimal("creditNoteAmount");
            			}
            			billReturnAmt = billReturnAmt.add(creditNoteAmount);
            		}
            		billReturnMap.put("billReturnAmt", billReturnAmt);
            	}else {
            		billReturnMap.put("billReturnAmt", billReturnAmt);
            	}
        	}else {
        		billReturnMap.put("billAmt", BigDecimal.ZERO);
        		billReturnMap.put("billReturnAmt", BigDecimal.ZERO);
        	}
        	
        	
        } catch (Exception e) {
			// TODO: handle exception
		}
        
        result.put("billReturnMap", billReturnMap);
        return result;
	}
}
