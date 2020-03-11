package ofbiz.android.pos;

import java.util.Map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
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

public class PosCreditNote extends BaseCreditAndCreditTxn{
	
	public static final String module = PosCredit.class.getName();
	
	public static Map<String, Object> createPosCustomerCreditNote(DispatchContext dispatchContext,
			Map<String, ? extends Object> context)
	{
		//delegator object(Stores all CRUD / Dao in this object)
		Delegator delegator = dispatchContext.getDelegator();
		//get input parameters
		String creditId = (String) context.get("creditId");
		String customerMobileNum = (String) context.get("customerMobileNum");
		String customerId = (String) context.get("customerId");
		String creditAmount = (String) context.get("creditAmount");
		String productStoreId = (String) context.get("productStoreId");
		String paidAmount = (String) context.get("paidAmount");
		String dayId = (String) context.get("dayId");
		String date = (String) context.get("date");
        String billId = (String) context.get("billId");
		String type = (String) context.get("type");
		String retailer = (String) context.get("retailer");
		String customer = (String) context.get("customer");
		  
		Timestamp now = UtilDateTime.nowTimestamp();
		BigDecimal totRetailorGivenCreditNoteAmt = BigDecimal.ZERO;
		BigDecimal totCustomerPaidCreditNoteAmt = BigDecimal.ZERO;
		BigDecimal balanceAmt = BigDecimal.ZERO;
		BigDecimal prevPaidAmt = BigDecimal.ZERO;
		BigDecimal paidAmt = BigDecimal.ZERO;
		BigDecimal creditBalAmt = BigDecimal.ZERO;
		BigDecimal creditNoteAmount = BigDecimal.ZERO;
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			if (UtilValidate.isNotEmpty(customerMobileNum)) {
				customerId = getCustomerIdByMobileNum(delegator, customerMobileNum);
			}
			//code for create
			//customer date type
			//Since retailor issues credit to customer(retailor-1 & customer-0)
			//Get totalRetalorCreditAmt given to Customer
			totRetailorGivenCreditNoteAmt = getTotalRetailorCreditNoteAmtToCustomer(delegator, productStoreId, customerId);
			//Get totalCustomerCredit PaidAmt to Retailor
			totCustomerPaidCreditNoteAmt = getTotalCustomerCreditNotePaidAmtToRetailor(delegator, productStoreId, customerId);
			
			//balanceAmt = SUM(retailorCreditAmt) - SUM(customer paidAmt)
			balanceAmt = totRetailorGivenCreditNoteAmt.subtract(totCustomerPaidCreditNoteAmt);
			//update creditId 
			GenericValue creditNote = EntityQuery.use(delegator).from("PosCredit").where("creditId", creditId).queryOne();
			if(UtilValidate.isNotEmpty(creditNote)) {
				creditNoteAmount = creditNote.getBigDecimal("creditNoteAmount");
				if(UtilValidate.isNotEmpty(creditNote.getBigDecimal("paidAmount"))) {
					prevPaidAmt = creditNote.getBigDecimal("paidAmount");
				}
				paidAmt = prevPaidAmt.add(new BigDecimal(paidAmount));
				creditBalAmt = creditNoteAmount.subtract(paidAmt);
				creditNote.put("paidAmount", paidAmt);
				creditNote.put("balanceAmount", creditBalAmt);
				if(creditBalAmt.compareTo(BigDecimal.ZERO) == 0) {
					creditNote.put("statusId", "COMPLETED");
				}else {
					creditNote.put("statusId", "REDEEM");
				}
				creditNote.store();
			}
			creditId = delegator.getNextSeqId("PosCredit");
			GenericValue createPosCreditNote = delegator.makeValue("PosCredit", UtilMisc.toMap("creditId", creditId, 
					"customerId", customerId, "billId", billId,"productStoreId", productStoreId,
					"retailer", new Long(retailer),"customer", new Long(customer), 
					"paidAmount", new BigDecimal(paidAmount),
					"dayId", dayId,"balanceAmount", balanceAmt,
					"type", type, 
					"date", now));
			createPosCreditNote.create();
			
		} catch (GenericEntityException e) {
			result.put("ERROR_MSG", "ERROR");
			return ServiceUtil.returnError("Error in creating posCredit Note in " + module);
		}
		result.put("SUCCESS_MSG", "SUCCESS");
		result.put("creditId",creditId);
		return result;
	}
	public static Map<String, Object> createIssueCreditNote(DispatchContext dispatchContext,
			Map<String, ? extends Object> context)
	{
		//delegator object(Stores all CRUD / Dao in this object)
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = userLogin.getString("partyId");
		//get input parameters
		String facilityId = (String) context.get("facilityId");
		String customerMobileNum = (String) context.get("customerMobileNum");
		String customerId = (String) context.get("customerId");
		String productId = (String) context.get("productId");
		String billId = (String) context.get("billId");
		String dayId = (String) context.get("dayId");
		String type = (String) context.get("type");
		String productStoreId = (String) context.get("productStoreId");
		String customer = (String) context.get("customer");
		String retailer = (String) context.get("retailer");
		String returnQuantity = (String) context.get("returnQuantity");
		String productPrice = (String) context.get("productPrice");
		String creditNoteAmount = (String) context.get("creditNoteAmount");
		  
		String creditId = "", inventoryItemId = "";
		BigDecimal retQty = BigDecimal.ZERO;
		Timestamp now = UtilDateTime.nowTimestamp();
		Map<String, Object> result = new HashMap<String, Object>();
		try {
			if (UtilValidate.isNotEmpty(customerMobileNum)) {
				customerId = getCustomerIdByMobileNum(delegator, customerMobileNum);
			}
			//code for create
			//customer date type
			//Since retailor issues credit to customer(retailor-1 & customer-0)
			creditId = delegator.getNextSeqId("PosCredit");
			GenericValue createIssueCreditNote = delegator.makeValue("PosCredit",
					UtilMisc.toMap("creditId", creditId,"customerId", customerId,
					"productId", productId,"billId", billId,"productStoreId", productStoreId,
					"retailer", new Long(retailer),"customer", new Long(customer),
					"creditNoteAmount", new BigDecimal(creditNoteAmount), "returnQuantity", Long.parseLong(returnQuantity),
					 "productPrice", new BigDecimal(productPrice), "dayId", dayId, "type", type, "date", now, "statusId", "NEW"));
			createIssueCreditNote.create();
			//Code to Increase store Inventory
			if(UtilValidate.isNotEmpty(facilityId)) {
				retQty = new BigDecimal(returnQuantity);
				//get inventoryItem
				GenericValue invItemVal = EntityQuery.use(delegator).from("InventoryItem").where("facilityId", facilityId,
						"productId", productId.trim()).queryFirst();
				if(UtilValidate.isNotEmpty(invItemVal)) {
					//Create ItemInvemtory variance
    				inventoryItemId = invItemVal.getString("inventoryItemId");
					try {
    					Map<String,Object> physicalInvMap = dispatcher.runSync("createPhysicalInventory", 
    							UtilMisc.toMap("partyId", partyId, "physicalInventoryDate", UtilDateTime.nowTimestamp(),
    									"userLogin", userLogin));
    					
    					Map<String, Object> map = dispatcher.runSync("createInventoryItemVariance", 
    							UtilMisc.toMap("inventoryItemId",inventoryItemId, "comments", "", "status","APPROVED",
    									"quantityOnHandVar", retQty, 
    									"availableToPromiseVar", retQty,
    									"physicalInventoryId", physicalInvMap.get("physicalInventoryId"),
    									"userLogin", userLogin));
    				} catch (GenericServiceException e) {
    					
    				}
				}
			}
			//End of code to Increase Store Inv
		} catch (GenericEntityException e) {
			result.put("ERROR_MSG", "ERROR");
			return ServiceUtil.returnError("Error in creating posCredit Note in " + module);
		}
		result.put("SUCCESS_MSG", "SUCCESS");
		result.put("creditId",creditId);
		return result;
	}
	public static Map<String, Object> getCustomerCreditNoteTxns (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
			
			Map<String, Object> result = ServiceUtil.returnSuccess();
			Delegator delegator = dispatchContext.getDelegator();
	        
	        String customerId = (String) context.get("customerId");
	        String productStoreGroupId = (String) context.get("productStoreGroupId");
	        String currencyUomId = (String) context.get("currencyUomId");
	        
	        String customerMobileNo = "", issuedDate = "", statusId = "";
	        String txnCustomerId = "", customerName = "",billId = "",cnId = "";
	        BigDecimal totCustomerCreditDue = BigDecimal.ZERO;
	        BigDecimal creditNoteAmount = BigDecimal.ZERO;
	        BigDecimal totCustomerCreditNoteDue = BigDecimal.ZERO;
	        Map<String, Object> customerCreditNoteAmtMap = new HashMap<String, Object>();
	        List<GenericValue> creditNoteList = new LinkedList<GenericValue>();
	        
	        List<Map<String, Object>> customerCreditNoteTxnList = new ArrayList<Map<String,Object>>();
	        try {
	        	//get Distinct customers
	        	creditNoteList = EntityQuery.use(delegator).select("creditId","date", "customerId", 
	        			"billId", "type", "statusId", "customer", "retailer").from("PosCredit").where("type", "CREDIT_NOTE",
	        					"customer", Long.parseLong("0"),"retailer", Long.parseLong("1")).distinct(true).queryList();
	        			
	        	for (GenericValue creditNoteEach : creditNoteList) {
	        		Map<String, Object> customerMap = new HashMap<String, Object>();
	        		//get 
	        		//get customerId
	        		customerId = creditNoteEach.getString("customerId");
	        		billId = creditNoteEach.getString("billId");
	        		cnId = creditNoteEach.getString("creditId");
	        		issuedDate = creditNoteEach.getString("date");
	        		issuedDate = convertStringToTmstp(issuedDate);
	        		statusId = creditNoteEach.getString("statusId");
	        		//get customer Name
	        		customerName = getCustomerName(delegator, customerId);
	        		//get customer Mobile No
	        		customerMobileNo = getCustomerMobileNumById(delegator, customerId);
	        		//get customer total CreditNote amt by bill id
	        		customerCreditNoteAmtMap = getCustomerTotalCreditNoteAmtByBillId(delegator, customerId, billId, cnId);
	        		//get customer total credit note
	        		//creditNoteAmount = (BigDecimal) customerCreditNoteAmtMap.get("totCustomerCreditNoteAmt");
	        		creditNoteAmount = (BigDecimal) customerCreditNoteAmtMap.get("creditNoteAmount");
	        		//get customer creditDue
	        		totCustomerCreditNoteDue = (BigDecimal) customerCreditNoteAmtMap.get("creditNoteBalanceAmount");
	        		
	        		customerMap.put("customerId", customerId);
	        		customerMap.put("customerName", customerName);
	        		customerMap.put("billId", billId);
	        		customerMap.put("customerMobileNo", customerMobileNo);
	        		customerMap.put("cnId", cnId);
	        		customerMap.put("issuedDate", issuedDate);
	        		customerMap.put("statusId", statusId);
	        		customerMap.put("totCustomerCreditNoteAmt", creditNoteAmount);
	        		customerMap.put("totCustomerCreditNoteAmtDue", totCustomerCreditNoteDue);
	        		
	        		customerCreditNoteTxnList.add(customerMap);
	        	}
	        	
	        } catch (Exception e) {
				// TODO: handle exception
			}
	        
	        result.put("customerCreditNoteTxnList", customerCreditNoteTxnList);
	        return result;
	}
	public static Map<String, Object> getActiveCreditNoteList(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
		String customerId = (String) context.get("customerId");
		String contactNumber = (String) context.get("contactNumber");
		
		List<GenericValue> activeCreditNoteList = new ArrayList<GenericValue>();
		List<Map<String, Object>> creditNoteList = new ArrayList<Map<String,Object>>();
		BigDecimal balanceAmount = BigDecimal.ZERO;
		try {
			if (UtilValidate.isNotEmpty(contactNumber)) {
				customerId = getCustomerIdByMobileNum(delegator, contactNumber);
			}
			EntityCondition posCreditCond = EntityCondition.makeCondition(EntityOperator.AND,
					EntityCondition.makeCondition("customerId", EntityOperator.EQUALS, customerId),
					EntityCondition.makeCondition("customer", EntityOperator.EQUALS, Long.parseLong("0")),
					EntityCondition.makeCondition("retailer", EntityOperator.EQUALS, Long.parseLong("1")),
					EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "COMPLETED"),
					EntityCondition.makeCondition("type", EntityOperator.EQUALS, "CREDIT_NOTE"));
			activeCreditNoteList = EntityQuery.use(delegator).from("PosCredit").where(posCreditCond).queryList();
			for (GenericValue creditNoteVal : activeCreditNoteList) {
				Map<String, Object> creditNoteMap = new HashMap<String, Object>();
				balanceAmount = creditNoteVal.getBigDecimal("balanceAmount");
				if(UtilValidate.isEmpty(balanceAmount) || balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
					creditNoteMap.put("creditNoteAmount", creditNoteVal.getBigDecimal("creditNoteAmount").toBigInteger());
				}else {
					creditNoteMap.put("creditNoteAmount", creditNoteVal.getBigDecimal("balanceAmount").toBigInteger());
				}
				creditNoteMap.put("creditId", creditNoteVal.getString("creditId"));
				creditNoteList.add(creditNoteMap);
			}
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		result.put("creditNoteList", creditNoteList);
		return result;
	}
	public static Map<String, Object> getCreditNoteAmtByCreditId(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		Map<String, Object> result = ServiceUtil.returnSuccess();
		Delegator delegator = dispatchContext.getDelegator();
		String creditId = (String) context.get("creditId");
		BigDecimal creditNoteAmount = BigDecimal.ZERO;
		BigDecimal balanceAmount = BigDecimal.ZERO;
		try {
			GenericValue creditNote = EntityQuery.use(delegator).from("PosCredit").where("creditId", creditId).queryOne();
			
			balanceAmount = creditNote.getBigDecimal("balanceAmount");
			if(UtilValidate.isEmpty(balanceAmount) || balanceAmount.compareTo(BigDecimal.ZERO) == 0) {
				creditNoteAmount = creditNote.getBigDecimal("creditNoteAmount");
			}else {
				creditNoteAmount = creditNote.getBigDecimal("balanceAmount");
			}
			creditNoteAmount = creditNoteAmount.setScale(0, BigDecimal.ROUND_HALF_UP);
			creditNoteAmount = creditNoteAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
			
		}catch (GenericEntityException e) {
			// TODO: handle exception
		}
		result.put("creditNoteAmount", creditNoteAmount);
		return result;
	}
}
