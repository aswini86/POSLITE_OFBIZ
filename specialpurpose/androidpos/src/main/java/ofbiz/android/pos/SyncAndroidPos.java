package ofbiz.android.pos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.rmi.CORBA.Util;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class SyncAndroidPos {

	public static final String module = SyncAndroidPos.class.getName();
	
	public static Map<String, Object> createSyncCustomer(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            String partyId = (String) context.get("partyId");

            GenericValue createSyncCustomer = delegator.makeValue("SyncCustomer", 
        			UtilMisc.toMap("partyId",partyId, "isSync", "N"));
            createSyncCustomer.create();
            
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("partyId", partyId);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in Syncing Customer " + module);
        }
    }
	
	public static Map<String, Object> createSyncProduct(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            String productId = (String) context.get("productId");

            GenericValue createSyncCustomer = delegator.makeValue("SyncProduct", 
        			UtilMisc.toMap("productId",productId, "isSync", "N"));
            createSyncCustomer.create();
            
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("productId", productId);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in product creation " + module);
        }
    }
	
	public static Map<String, Object> createCartTransaction(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            String receiptId = (String) context.get("receiptId");

            GenericValue createSyncCustomer = delegator.makeValue("SyncCartTransaction", 
        			UtilMisc.toMap("receiptId",receiptId, "isSync", "N"));
            createSyncCustomer.create();
            
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("receiptId", receiptId);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in Cart creation " + module);
        }
    }
	
	public static Map<String, Object> getCustomerSyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            List<HashMap<String, Object>> customerList = new ArrayList<HashMap<String,Object>>();
            String firstName = "", lastName = "", contactNumber = "";
            String address1 = "", city = "", postalCode = "", stateProvinceGeoId = "";
            
            List<String> partyIds = EntityQuery.use(delegator).from("SyncCustomer").where("isSync", "N").orderBy("partyId").
        			distinct(true).getFieldList("partyId");
            if(UtilValidate.isNotEmpty(partyIds)) {
            	for (String partyId : partyIds) {
            		HashMap<String, Object> customermap = new HashMap<String, Object>();
            		//Get Person details
            		GenericValue person = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
            		if(UtilValidate.isNotEmpty(person)) {
            			firstName = person.getString("firstName");
            			lastName = person.getString("lastName");
            			customermap.put("partyId", partyId);
            			customermap.put("firstName", firstName);
            			customermap.put("lastName", lastName);
            			customermap.put("login.username", "admin");
            			customermap.put("login.password", "ofbiz");
            		}
            		//Get postal address details
            		GenericValue postalAddress = EntityQuery.use(delegator).from("PartyAndPostalAddress").where("partyId", partyId).queryFirst();
            		if(UtilValidate.isNotEmpty(postalAddress)) {
            			address1 = postalAddress.getString("address1");
            			city = postalAddress.getString("city");
            			postalCode = postalAddress.getString("postalCode");
            			stateProvinceGeoId = postalAddress.getString("stateProvinceGeoId");
            			customermap.put("address1", address1);
            			customermap.put("city", city);
            			customermap.put("postalCode", postalCode);
            			customermap.put("stateProvinceGeoId", stateProvinceGeoId);
            		}
            		//get contact number details
            		GenericValue telecomNumber = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("partyId", partyId).queryFirst();
            		if(UtilValidate.isNotEmpty(telecomNumber)) {
            			contactNumber = telecomNumber.getString("contactNumber");
            			customermap.put("contactNumber", contactNumber);
            		}
            		customerList.add(customermap);
            	}
            }
            
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("customerList", customerList);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in Syncing Customer " + module);
        }
    }
	public static Map<String, Object> updateSyncCustomer(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		String customerPartyIds = (String) context.get("partyIds");
		String[] partyIds = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();
		try {
			customerPartyIds = customerPartyIds.substring(1, customerPartyIds.length() -1);
			if(UtilValidate.isNotEmpty(customerPartyIds)) {
				partyIds = customerPartyIds.split(",");
			}
			if(UtilValidate.isNotEmpty(partyIds)) {
				for (String partyId : partyIds) {
					GenericValue syncCustomer = EntityQuery.use(delegator).from("SyncCustomer").where("partyId", partyId.trim()).queryOne();
					syncCustomer.set("isSync", "Y");
					syncCustomer.store();
				}
			}
		} catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in updating Customer " + module);
        }
		return result;
	}
	
	public static Map<String, Object> getProductSyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            List<HashMap<String, Object>> productList = new ArrayList<HashMap<String,Object>>();
            String barcode = "", productName = "";
            BigDecimal mrp = BigDecimal.ZERO;
            BigDecimal sp = BigDecimal.ZERO;
            List<String> productIds = EntityQuery.use(delegator).from("SyncProduct").where("isSync", "N").orderBy("productId").
        			distinct(true).getFieldList("productId");
            if(UtilValidate.isNotEmpty(productIds)) {
            	for (String productId : productIds) {
            		HashMap<String, Object> productMap = new HashMap<String, Object>();
            		//Get Product name
            		GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            		if(UtilValidate.isNotEmpty(product)) {
            			productName = product.getString("productName");
            			productMap.put("productId", productId);
            			productMap.put("productName", productName);
            		}
            		//Get mrp sp
            		GenericValue productMrp = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId,
            				"productPriceTypeId", "LIST_PRICE").queryFirst();
            		if(UtilValidate.isNotEmpty(productMrp)) {
            			mrp = productMrp.getBigDecimal("price");
            			productMap.put("mrp", mrp);
            		}
            		GenericValue productSp = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId,
            				"productPriceTypeId", "DEFAULT_PRICE").queryFirst();
            		if(UtilValidate.isNotEmpty(productSp)) {
            			sp = productSp.getBigDecimal("price");
            			productMap.put("sp", sp);
            		}
            		//get contact number details
            		GenericValue barCodeDetails = EntityQuery.use(delegator).from("GoodIdentification").where("productId", productId,
            				"goodIdentificationTypeId", "EAN").queryFirst();
            		if(UtilValidate.isNotEmpty(barCodeDetails)) {
            			barcode = barCodeDetails.getString("idValue");
            			productMap.put("barcode", barcode);
            		}
            		productList.add(productMap);
            	}
            }
            
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("productList", productList);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in Syncing Customer " + module);
        }
    }
	
	public static Map<String, Object> pushProductServer(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		
        String barcode = (String) context.get("barcode");
        String productName = (String) context.get("productName");
        String mrp = (String) context.get("mrp");
        String sp = (String) context.get("sp");
        String productId = "";
        
		Map<String, Object> result = new HashMap<String, Object>();
        try {
        	Map<String, Object> createPrdRes = dispatcher.runSync("createProduct", UtilMisc.toMap("internalName", productName,
        			"productName", productName, "description", productName, "productTypeId", "FINISHED_GOOD", "userLogin", userLogin));
        	if(ServiceUtil.isSuccess(createPrdRes)) {
        		productId = (String) createPrdRes.get("productId");
        		//create SP
            	Map<String, Object> createSellingPrice = dispatcher.runSync("createProductPrice", UtilMisc.toMap("productId", productId,
            			"price",new BigDecimal(sp), "productPriceTypeId", "DEFAULT_PRICE", "currencyUomId", "INR",
            			"productStoreGroupId", "_NA_", "productPricePurposeId","PURCHASE", 
            			"fromDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin));
            	//create mrp
            	Map<String, Object> createMrp = dispatcher.runSync("createProductPrice", UtilMisc.toMap("productId", productId,
            			"price",new BigDecimal(mrp), "productPriceTypeId", "LIST_PRICE", "currencyUomId", "INR",
            			"productStoreGroupId", "_NA_", "productPricePurposeId","PURCHASE", 
            			"fromDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin));
            	
            	GenericValue barCode = delegator.makeValue("GoodIdentification", UtilMisc.toMap("goodIdentificationTypeId","EAN",
            								"productId", productId,"idValue", barcode));
            	barCode.create();
            	
        	}
        	result.put("productId", productId);
            return result;
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in creating Global Product " + module);
        }
    }
	public static Map<String, Object> updateSyncProduct(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		String productIds = (String) context.get("productIds");
		String[] added_productIds = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();
		try {
			productIds = productIds.substring(1, productIds.length() -1);
			if(UtilValidate.isNotEmpty(productIds)) {
				added_productIds = productIds.split(",");
			}
			if(UtilValidate.isNotEmpty(added_productIds)) {
				for (String productId : added_productIds) {
					GenericValue syncProduct = EntityQuery.use(delegator).from("SyncProduct").where("productId", productId.trim()).queryOne();
					if(UtilValidate.isNotEmpty(syncProduct)) {
						syncProduct.set("isSync", "Y");
						syncProduct.store();
					}
				}
			}
		} catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in updating Product " + module);
        }
		return result;
	}
	public static Map<String, Object> getFacilitySyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		List<HashMap<String, Object>> productFacilityList = new ArrayList<HashMap<String,Object>>();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		try {
			List<GenericValue> facilityList = EntityQuery.use(delegator).from("ProductStoreFacility").queryList();
			for (GenericValue facilityRow : facilityList) {
				HashMap<String, Object> productFacilityMap = new HashMap<String, Object>();
				productFacilityMap.put("productStoreId", facilityRow.getString("productStoreId"));
				productFacilityMap.put("facilityId", facilityRow.getString("facilityId"));
				productFacilityList.add(productFacilityMap);
			}
			
		} catch (GenericEntityException e) {
			
		}
		result.put("productFacilityList", productFacilityList);
		return result;
	}
	public static Map<String, Object> getPosTerminalSyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		List<HashMap<String, Object>> posTerminalList = new ArrayList<HashMap<String,Object>>();
		try {
			List<GenericValue> posTermList = EntityQuery.use(delegator).from("PosTerminal").queryList();
			for (GenericValue posTermRow : posTermList) {
				HashMap<String, Object> posTerminalMap = new HashMap<String, Object>();
				posTerminalMap.put("posTerminalId", posTermRow.getString("posTerminalId"));
				
				posTerminalList.add(posTerminalMap);
			}
		} catch (GenericEntityException e) {
			
		}
		result.put("posTerminalList", posTerminalList);
		return result;
	}
	public static Map<String, Object> getPSGSyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		List<HashMap<String, Object>> prdStoreGrpList = new ArrayList<HashMap<String,Object>>();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		try {
			List<GenericValue> prdStoreGroupList = EntityQuery.use(delegator).from("ProductStoreGroup").queryList();
			for (GenericValue prdStoreGrprow : prdStoreGroupList) {
				HashMap<String, Object> psgMap = new HashMap<String, Object>();
				psgMap.put("description", prdStoreGrprow.getString("description"));
				prdStoreGrpList.add(psgMap);
			}
			
		} catch (GenericEntityException e) {
			
		}
		result.put("prdStoreGrpList", prdStoreGrpList);
		return result;
	}
	public static Map<String, Object> getPosFacilitySyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		List<HashMap<String, Object>> posFacilityList = new ArrayList<HashMap<String,Object>>();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		try {
			List<GenericValue> facilityList = EntityQuery.use(delegator).from("Facility").queryList();
			for (GenericValue posFacilityRow : facilityList) {
				HashMap<String, Object> facilityMap = new HashMap<String, Object>();
				facilityMap.put("facilityTypeId", posFacilityRow.getString("facilityTypeId"));
				facilityMap.put("facilityName", posFacilityRow.getString("facilityName"));
				facilityMap.put("defaultInventoryItemTypeId", posFacilityRow.getString("defaultInventoryItemTypeId"));
				facilityMap.put("ownerPartyId", posFacilityRow.getString("ownerPartyId"));
				posFacilityList.add(facilityMap);
			}
			
		} catch (GenericEntityException e) {
			
		}
		result.put("posFacilityList", posFacilityList);
		return result;
	}
	public static Map<String, Object> getProductStoreSyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		List<HashMap<String, Object>> productStoreList = new ArrayList<HashMap<String,Object>>();
		try {
			List<GenericValue> prdStoreList = EntityQuery.use(delegator).from("ProductStore").queryList();
			for (GenericValue prdStoreRow : prdStoreList) {
				HashMap<String, Object> productStoreMap = new HashMap<String, Object>();
				productStoreMap.put("storeName", prdStoreRow.getString("storeName"));
				productStoreMap.put("companyName", prdStoreRow.getString("companyName"));
				//productStoreMap.put("primaryStoreGroupId", prdStoreRow.getString("primaryStoreGroupId"));
				
				productStoreList.add(productStoreMap);
			}
			
		} catch (GenericEntityException e) {
			
		}
		result.put("productStoreList", productStoreList);
		return result;
	}
	public static Map<String, Object> getCartSyncData(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            List<HashMap<String, Object>> cartTxnList = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String, Object>> cartTxnItemList = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String, Object>> cartTxnPaymentList = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String, Object>> posOrderHeaderList = new ArrayList<HashMap<String,Object>>();
            List<HashMap<String, Object>> posOrderItemList = new ArrayList<HashMap<String,Object>>();
            String posReceiptId = "", dayId = "", terminalId = "", posStatus = "", customerMobileNo = "",facilityId = "";
            String productName = "", productId = "";
            
            List<String> receiptIds = EntityQuery.use(delegator).from("SyncCartTransaction").where("isSync", "N").orderBy("receiptId").
        			distinct(true).getFieldList("receiptId");
            if(UtilValidate.isNotEmpty(receiptIds)) {
            	for (String receiptId : receiptIds) {
            		HashMap<String, Object> posCartTxnMap = new HashMap<String, Object>();
            		HashMap<String, Object> posCartItemMap = new HashMap<String, Object>();
            		HashMap<String, Object> posCartPaymentMap = new HashMap<String, Object>();
            		HashMap<String, Object> orderHeaderMap = new HashMap<String, Object>();
            		HashMap<String, Object> orderItemMap = new HashMap<String, Object>();
            		//Get Pos Car txn
            		GenericValue posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId,
            				"posStatus", "COMPLETE").queryFirst();
            		if(UtilValidate.isNotEmpty(posCartTxn)) {
            			posReceiptId = posCartTxn.getString("receiptId");
            			dayId = posCartTxn.getString("dayId");
            			terminalId = posCartTxn.getString("terminalId");
            			posStatus = posCartTxn.getString("posStatus");
            			customerMobileNo = posCartTxn.getString("customerMobileNo");
            			
            			posCartTxnMap.put("posReceiptId", posReceiptId);
            			posCartTxnMap.put("dayId", dayId);
            			posCartTxnMap.put("terminalId", terminalId);
            			posCartTxnMap.put("posStatus", posStatus);
            			posCartTxnMap.put("customerMobileNo", customerMobileNo);
            			cartTxnList.add(posCartTxnMap);
            		}
            		//Get posCartItemMap
            		List<GenericValue> posCartItem = EntityQuery.use(delegator).from("PosCartItem").where("receiptId", receiptId,
            				"posStatus", "COMPLETE").queryList();
            		if(UtilValidate.isNotEmpty(posCartItem)) {
            			for (GenericValue row : posCartItem) {
            				productId = row.getString("productId");
            				productName = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne().getString("productName");
            				posCartItemMap.put("receiptId", row.getString("receiptId"));
            				posCartItemMap.put("productId", row.getString("productId"));
            				posCartItemMap.put("productName", productName);
            				posCartItemMap.put("dayId", row.getString("dayId"));
            				posCartItemMap.put("quantity", row.getString("quantity"));
            				posCartItemMap.put("productPrice", row.getString("productPrice"));
            				cartTxnItemList.add(posCartItemMap);
            			}
            		}
            		
            		List<GenericValue> posCartReceivePayment = EntityQuery.use(delegator).from("PosCartReceivePayment").where("receiptId", receiptId).queryList();
            		if(UtilValidate.isNotEmpty(posCartReceivePayment)) {
            			for (GenericValue posCartPayment : posCartReceivePayment) {
            				posCartPaymentMap.put("posCartPaymentId", posCartPayment.getString("posCartPaymentId"));
            				posCartPaymentMap.put("receiptId", posCartPayment.getString("receiptId"));
            				posCartPaymentMap.put("dayId", posCartPayment.getString("dayId"));
            				posCartPaymentMap.put("terminalId", posCartPayment.getString("terminalId"));
            				posCartPaymentMap.put("paymentType", posCartPayment.getString("paymentType"));
            				posCartPaymentMap.put("receivedPayment", posCartPayment.getString("receivedPayment"));
            				cartTxnPaymentList.add(posCartPaymentMap);
            			}
            			
            		}
            		
            		List<GenericValue> orderHeaderList = EntityQuery.use(delegator).from("OrderHeader").where("orderId", receiptId).queryList();
            		if(UtilValidate.isNotEmpty(orderHeaderList)) {
            			for (GenericValue orderHeaderEach : orderHeaderList) {
            				orderHeaderMap.put("orderId", orderHeaderEach.getString("orderId"));
            				orderHeaderMap.put("orderTypeId", orderHeaderEach.getString("orderTypeId"));
            				orderHeaderMap.put("terminalId", orderHeaderEach.getString("terminalId"));
            				orderHeaderMap.put("grandTotal", orderHeaderEach.getString("grandTotal"));
            				orderHeaderMap.put("remainingSubTotal", orderHeaderEach.getString("remainingSubTotal"));
            				posOrderHeaderList.add(orderHeaderMap);
            			}
            			
            		}
            		List<GenericValue> orderItemList = EntityQuery.use(delegator).from("OrderItem").where("orderId", receiptId).queryList();
            		if(UtilValidate.isNotEmpty(orderItemList)) {
            			for (GenericValue orderItemEach : orderItemList) {
            				productId = orderItemEach.getString("productId");
            				productName = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne().getString("productName");
            				orderItemMap.put("orderId", orderItemEach.getString("orderId"));
            				orderItemMap.put("orderItemSeqId", orderItemEach.getString("orderItemSeqId"));
            				orderItemMap.put("productName", productName);
            				orderItemMap.put("quantity", orderItemEach.getString("quantity"));
            				orderItemMap.put("unitPrice", orderItemEach.getString("unitPrice"));
            				orderItemMap.put("unitListPrice", orderItemEach.getString("unitListPrice"));
            				posOrderItemList.add(orderItemMap);
            			}
            			
            		}
            	}
            }
            
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("cartTxnList", cartTxnList);
            result.put("cartTxnItemList", cartTxnItemList);
            result.put("cartTxnPaymentList", cartTxnPaymentList);
            result.put("posOrderHeaderList", posOrderHeaderList);
            result.put("posOrderItemList", posOrderItemList);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in Syncing Pos cart Txn " + module);
        }
    }
	
	public static Map<String, Object> syncPosMetaData (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String partyId = userLogin.getString("partyId");
		//Facility details
		String facilityName = (String) context.get("facilityName");
		String facilityTypeId = (String) context.get("facilityTypeId");
		String ownerPartyId = (String) context.get("ownerPartyId");
		//Store details
		String storeName = (String) context.get("storeName");
		String companyName = (String) context.get("companyName");
		//Store group details
		String description = (String) context.get("description");
		//Product store facility details
		String posTerminalId = (String) context.get("posTerminalId");
		String facilityId = "", productStoreId = "";
		GenericValue posTerminal = new GenericValue();
		try {
			//check facilityName exists or not
			List<GenericValue> facilityList = EntityQuery.use(delegator).from("Facility").where("facilityName", facilityName).queryList();
			if(!(facilityList.size() > 0)) {
				//createFacility
				Map<String, Object> facilityMap = dispatcher.runSync("createFacility", UtilMisc.toMap("facilityName", facilityName,
						"facilityTypeId", facilityTypeId, "ownerPartyId", ownerPartyId, "userLogin", userLogin));
				facilityId = (String) facilityMap.get("facilityId");
				//create facility party
				
				//create facility postal address
				//create store
				Map<String, Object> storeMap = dispatcher.runSync("createProductStore", UtilMisc.toMap("storeName", storeName,
						"companyName", companyName, "payToPartyId", partyId, "userLogin", userLogin));
				productStoreId = (String) storeMap.get("productStoreId");
				//create product store group
				Map<String, Object> prdStoreGroupMap = dispatcher.runSync("createProductStoreGroup", UtilMisc.toMap("description", description,
						"productStoreGroupName", description, "userLogin", userLogin));
				//create createProductStoreFacility service
				Map<String, Object> prdStoreFacilityMap = dispatcher.runSync("createProductStoreFacility", UtilMisc.toMap("facilityId", facilityId,
						"productStoreId", productStoreId, "userLogin", userLogin));
				//create pos terminal
				/*posTerminal = delegator.makeValue("PosTerminal", UtilMisc.toMap("posTerminalId", posTerminalId,
						"facilityId", facilityId, "terminalName", posTerminalId));
				posTerminal.create();*/
			}
			
		} catch (GenericServiceException | GenericEntityException e) {
			
		}
		
		return result;
	}
	public static Map<String, Object> syncPosTerminalData (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String partyId = userLogin.getString("partyId");
		//Facility details
		String facilityName = (String) context.get("facilityName");
		//Store details
		String posTerminalId = (String) context.get("posTerminalId");
		String facilityId = "", productStoreId = "";
		GenericValue posTerminal = new GenericValue();
		try {
			//check facilityName exists or not
			List<GenericValue> facilityList = EntityQuery.use(delegator).from("Facility").where("facilityName", facilityName).queryList();
			if(!(facilityList.size() > 0)) {
				//create pos terminal
				posTerminal = delegator.makeValue("PosTerminal", UtilMisc.toMap("posTerminalId", posTerminalId,
						"facilityId", facilityId, "terminalName", posTerminalId));
				posTerminal.create();
			}
			
		} catch (GenericEntityException e) {
			
		}
		
		return result;
	}
	public static Map<String, Object> pushCartTxnServer (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String partyId = userLogin.getString("partyId");
		//Facility details
		String posReceiptId = (String) context.get("posReceiptId");
		String dayId = (String) context.get("dayId");
		String terminalId = (String) context.get("terminalId");
		String posStatus = (String) context.get("posStatus");
		String customerMobileNo = (String) context.get("customerMobileNo");
		GenericValue PosCartTransaction = new GenericValue();
		GenericValue PosCartTransVal = new GenericValue();
		String facilityId = "", receiptId = "";
		Map<String, String> receiptMap = new HashMap<String, String>();
		List<Map<String,String>> receiptList = new ArrayList<Map<String,String>>();
		
		try {
			//get facilityId
			if(UtilValidate.isNotEmpty(terminalId)) {
				facilityId = EntityQuery.use(delegator).from("PosTerminal").where("posTerminalId", terminalId).queryOne().getString("facilityId");
			}
			if(UtilValidate.isNotEmpty(posReceiptId)) {
				//check receiptId exists or not.
				PosCartTransVal = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", posReceiptId).queryOne();
				if(UtilValidate.isNotEmpty(PosCartTransVal)) {
					//Code to generate receiptId
					Map<String, Object> generateReceiptmap = dispatcher.runSync("generateReceiptId", UtilMisc.toMap("facilityId",facilityId, 
																"dayId",dayId));
					
					if(ServiceUtil.isSuccess(generateReceiptmap)) {
						//Code to create Pos transaction.
						receiptId = (String) generateReceiptmap.get("receiptId");
						PosCartTransaction = delegator.makeValue("PosCartTransaction", UtilMisc.toMap("receiptId", receiptId,
								"childReceiptId", posReceiptId,"dayId", dayId, "terminalId", terminalId,
								"posStatus", posStatus, "customerMobileNo", customerMobileNo));
						PosCartTransaction.create();
					}
				}
			}
		} catch (GenericEntityException | GenericServiceException e) {
			
		}
		return result;
	}
	public static Map<String, Object> pushCartItemServer (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String partyId = userLogin.getString("partyId");
		//Facility details
		String receiptId = (String) context.get("receiptId");
		String productId = (String) context.get("productId");
		String dayId = (String) context.get("dayId");
		String quantity = (String) context.get("quantity");
		String productPrice = (String) context.get("productPrice");
		GenericValue PosCartItem = new GenericValue();
		String newReceiptId = "";
		try {
			//get new ReceiptId
			newReceiptId = EntityQuery.use(delegator).from("PosCartTransaction").where("childReceiptId", receiptId)
					.queryFirst().getString("childReceiptId");
			PosCartItem = delegator.makeValue("PosCartItem", UtilMisc.toMap("receiptId", newReceiptId,
					"productId", productId, "dayId", dayId, "quantity", quantity, "productPrice", productPrice));
			PosCartItem.create();
		} catch (GenericEntityException e) {
			
		}
		return result;
	}
	
	public static Map<String, Object> pushCartPaymentServer (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String partyId = userLogin.getString("partyId");
		//Facility details
		String posCartPaymentId = (String) context.get("posCartPaymentId");
		String receiptId = (String) context.get("receiptId");
		String dayId = (String) context.get("dayId");
		String terminalId = (String) context.get("terminalId");
		String paymentType = (String) context.get("paymentType");
		String receivedPayment = (String) context.get("receivedPayment");
		GenericValue PosCartPayment = new GenericValue();
		String newReceiptId = "";
		try {
			//get new ReceiptId
			newReceiptId = EntityQuery.use(delegator).from("PosCartTransaction").where("childReceiptId", receiptId)
					.queryFirst().getString("childReceiptId");
			
			PosCartPayment = delegator.makeValue("PosCartReceivePayment", UtilMisc.toMap("posCartPaymentId", posCartPaymentId,
					"receiptId", newReceiptId, "dayId", dayId, "terminalId", terminalId, "paymentType", paymentType,
					"receivedPayment", receivedPayment));
					PosCartPayment.create();
		} catch (GenericEntityException e) {
			
		}
		return result;
	}
	public static Map<String, Object> updateSyncCartTxn(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		String cartReceiptIds = (String) context.get("receiptIds");
		String[] receiptIds = null;
		Map<String, Object> result = ServiceUtil.returnSuccess();
		try {
			cartReceiptIds = cartReceiptIds.substring(1, cartReceiptIds.length() -1);
			if(UtilValidate.isNotEmpty(cartReceiptIds)) {
				receiptIds = cartReceiptIds.split(",");
			}
			if(UtilValidate.isNotEmpty(receiptIds)) {
				for (String receiptId : receiptIds) {
					GenericValue SyncCartTransaction = EntityQuery.use(delegator).from("SyncCartTransaction").where("receiptId", receiptId.trim()).queryOne();
					if(UtilValidate.isNotEmpty(SyncCartTransaction)) {
						SyncCartTransaction.set("isSync", "Y");
						SyncCartTransaction.store();
					}
				}
			}
		} catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in updating Sync Cart Txn " + module);
        }
		return result;
	}
	
	public static Map<String, Object> pushCartOrderHeaderServer (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String partyId = userLogin.getString("partyId");
		//Facility details
		String orderId = (String) context.get("orderId");
		String orderTypeId = (String) context.get("orderTypeId");
		String terminalId = (String) context.get("terminalId");
		String grandTotal = (String) context.get("grandTotal");
		String remainingSubTotal = (String) context.get("remainingSubTotal");
		GenericValue posOrderHeader = new GenericValue();
		String newOrderId = "", originFacilityId = "";
		BigDecimal remSubTotal = BigDecimal.ZERO;
		BigDecimal grandTot = BigDecimal.ZERO;
		try {
			//get new ReceiptId
			GenericValue PosCartTransaction = EntityQuery.use(delegator).from("PosCartTransaction").where("childReceiptId", orderId)
					.queryFirst();
			if(UtilValidate.isNotEmpty(PosCartTransaction)) {
				newOrderId = PosCartTransaction.getString("childReceiptId");
			}
			GenericValue posTerminal = EntityQuery.use(delegator).from("PosTerminal").where("posTerminalId", terminalId).queryFirst();
			if(UtilValidate.isNotEmpty(posTerminal)) {
				originFacilityId = posTerminal.getString("facilityId");
			}
			if(UtilValidate.isNotEmpty(grandTotal)) {
				grandTot = new BigDecimal("grandTotal");
			}
			if(UtilValidate.isNotEmpty(remainingSubTotal)) {
				remSubTotal = new BigDecimal("remainingSubTotal");
			}
			
			posOrderHeader = delegator.makeValue("OrderHeader", UtilMisc.toMap("orderId", newOrderId,
					"orderTypeId", "SALES_ORDER", "salesChannelEnumId", "POS_SALES_CHANNEL", "orderDate", UtilDateTime.nowTimestamp(),
					"entryDate", UtilDateTime.nowTimestamp(),"statusId", "ORDER_CREATED","createdBy",partyId,"currencyUom", "INR",
					"terminalId",terminalId, "originFacilityId",originFacilityId,
					"grandTot", grandTot,"remSubTotal", remSubTotal));
			posOrderHeader.create();
		} catch (GenericEntityException e) {
			
		}
		return result;
	}
	
	public static Map<String, Object> pushCartOrderItemServer (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String partyId = userLogin.getString("partyId");
		//Facility details
		String orderId = (String) context.get("orderId");
		String orderItemSeqId = (String) context.get("orderItemSeqId");
		String productId = (String) context.get("productId");
		String quantity = (String) context.get("quantity");
		String unitPrice = (String) context.get("unitPrice");
		String unitListPrice = (String) context.get("unitListPrice");
		
		GenericValue posOrderItem = new GenericValue();
		String newOrderId = "", originFacilityId = "";
		BigDecimal qty = BigDecimal.ZERO;
		BigDecimal prdUnitPrice = BigDecimal.ZERO;
		BigDecimal prdUnitListPrice = BigDecimal.ZERO;
		
		try {
			//get new ReceiptId
			GenericValue PosCartTransaction = EntityQuery.use(delegator).from("PosCartTransaction").where("childReceiptId", orderId).queryFirst();
			if(UtilValidate.isNotEmpty(PosCartTransaction)) {
				newOrderId = PosCartTransaction.getString("childReceiptId");
			}
			
			if(UtilValidate.isNotEmpty(quantity)) {
				qty = new BigDecimal("quantity");
			}
			if(UtilValidate.isNotEmpty(unitPrice)) {
				prdUnitPrice = new BigDecimal("unitPrice");
			}
			if(UtilValidate.isNotEmpty(unitListPrice)) {
				prdUnitListPrice = new BigDecimal("unitListPrice");
			}
			
			posOrderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderId", newOrderId,
					"orderItemSeqId", orderItemSeqId, "orderItemTypeId", "PRODUCT_ORDER_ITEM", "productId", productId,
					"isPromo", "N","quantity", qty,"unitPrice", prdUnitPrice,"unitListPrice", prdUnitListPrice,
					"statusId", "ITEM_CREATED"));
			posOrderItem.create();
			
		} catch (GenericEntityException e) {
			
		}
		return result;
	}
}
