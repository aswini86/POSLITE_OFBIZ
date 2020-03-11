package ofbiz.android.pos;

import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.spi.DirStateFactory.Result;
import javax.rmi.CORBA.Util;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.apache.ofbiz.unmapproduct.UnmappedItemService;

public class AndroidPos extends BaseCreditAndCreditTxn{

    public static final String module = AndroidPos.class.getName();
    
    
    public static Map<String, Object> findPOSTerminals(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();

            Map<String, String> fields = new HashMap<>();
            if (context.containsKey("userId")) {
                fields.put("userId", (String) context.get("userId"));
            }

            List<GenericValue> posTerminals = delegator.findByAnd("POSTerminal", fields, new ArrayList<>(), false);

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("posTerminals", posTerminals);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in finding POSTerminals in " + module);
        }
    }

    public static Map<String, Object> findPOSTerminal(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();

            String terminalId = (String) context.get("terminalId");
            GenericValue posTerminal = EntityQuery.use(delegator).from("POSTerminal").where("terminalId", terminalId).queryOne();

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("posTerminal", posTerminal);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in getting POSTerminal in " + module);
        }
    }
    
    public static Map<String, Object> createDay(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();

            GenericValue dayDetail = (GenericValue) delegator.makeValue("DayDetail");
            dayDetail.setNextSeqId();
            dayDetail.setNonPKFields(context);
            dayDetail = delegator.create(dayDetail);

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("dayDetail", dayDetail);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in creating DayDetail in " + module);
        }
    }

    public static Map<String, Object> findDays(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            List<GenericValue> finalDayDetails = new ArrayList<GenericValue>();
            
            Map<String, String> fields = new HashMap<>();
            if (context.containsKey("day") && UtilValidate.isNotEmpty(context.get("day"))) {
                fields.put("day", (String) context.get("day"));
            }
            if (context.containsKey("terminalId") && UtilValidate.isNotEmpty(context.get("terminalId"))) {
                fields.put("terminalId", (String) context.get("terminalId"));
            }
            if (context.containsKey("userId") && UtilValidate.isNotEmpty(context.get("userId"))) {
                fields.put("userId", (String) context.get("userId"));
            }
            if (context.containsKey("dayStatus") && UtilValidate.isNotEmpty(context.get("dayStatus"))) {
                fields.put("dayStatus", (String) context.get("dayStatus"));
            }
            if (context.containsKey("adminOpeningDayStatus") && UtilValidate.isNotEmpty(context.get("adminOpeningDayStatus"))) {
                fields.put("adminOpeningDayStatus", (String) context.get("adminOpeningDayStatus"));
            }
            if (context.containsKey("adminClosingDayStatus") && UtilValidate.isNotEmpty(context.get("adminClosingDayStatus"))) {
                fields.put("adminClosingDayStatus", (String) context.get("adminClosingDayStatus"));
            }
            //if posAdmin
            //get list of posTerminals by storeId
            if (context.containsKey("storeId") && UtilValidate.isNotEmpty(context.get("storeId"))) {
            	List<GenericValue> posTerminals = delegator.findByAnd("PosTerminal", UtilMisc.toMap("facilityId",
                		(String) context.get("storeId")), new ArrayList<>(), false);
                List<String> storePosTerminals = EntityUtil.getFieldListFromEntityList(posTerminals, "posTerminalId", true);
                
                
                if (UtilValidate.isNotEmpty(storePosTerminals)) {
                	for (String posTerminalId : storePosTerminals) {
                		fields.put("terminalId", posTerminalId);
                		List<GenericValue> dayDetails = delegator.findByAnd("DayDetail", fields, new ArrayList<>(), false);
                		finalDayDetails.addAll(dayDetails);
                	}
                }
            } else {
            	finalDayDetails = delegator.findByAnd("DayDetail", fields, new ArrayList<>(), false);
            }
            
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("dayDetails", finalDayDetails);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in finding DayDetail in " + module);
        }
    }
    
    public static Map<String, Object> findDay(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();

            String dayId = (String) context.get("dayId");
            GenericValue dayDetail = EntityQuery.use(delegator).from("DayDetail").where("dayId", dayId).queryOne();

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("dayDetail", dayDetail);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in getting DayDetail in " + module);
        }
    }
    
    public static Map<String, Object> getDayId(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
        	GenericValue userLogin = (GenericValue) context.get("userLogin");
            Delegator delegator = dispatchContext.getDelegator();
            String userLoginId = userLogin.getString("userLoginId");
            
            String posTerminalId = (String) context.get("posTerminalId");
            String facilityId = (String) context.get("facilityId");
            String dayId = "", receiptId = "";
            Long nxtDayId = 0l;
            GenericValue posTerminal = new GenericValue();
            GenericValue dayDetail = EntityQuery.use(delegator).from("DayDetail").where("terminalId", posTerminalId).orderBy("-createdStamp").queryFirst();
            if(UtilValidate.isEmpty(dayDetail)) {
            	//create Day details
            	String day = UtilDateTime.nowDateString("dd-MM-yyyy");
            	String date = UtilDateTime.nowDateString("dd");
            	String month = UtilDateTime.nowDateString("MM");
            	String year = UtilDateTime.nowDateString("yyyy");
            	nxtDayId = delegator.getNextSeqIdLong("DayDetail", 1);
        	    //receiptId = String.valueOf(UtilDateTime.nowTimestamp().getTime());
            	receiptId = facilityId+date+month+year+nxtDayId;
            	GenericValue createDayDetail = delegator.makeValue("DayDetail", 
            			UtilMisc.toMap("dayId",nxtDayId.toString(), "day", day,"terminalId", posTerminalId, "userId", userLoginId,
            					"receiptId",receiptId,
            					"dayStatus", "OPENED","adminOpeningDayStatus", "APPROVE"));
            	
            	createDayDetail.create();
				posTerminal = EntityQuery.use(delegator).from("PosTerminal").where("posTerminalId", posTerminalId).queryFirst();
				if(UtilValidate.isEmpty(dayDetail)) {
					GenericValue createPosTerminal = delegator.makeValue("PosTerminal",
							UtilMisc.toMap("posTerminalId", posTerminalId, "facilityId", facilityId, "terminalName", posTerminalId));
					createPosTerminal.create();
				}
            } else {
            	dayId = dayDetail.getString("dayId");
            	receiptId = dayDetail.getString("receiptId");
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("dayId", dayId);
            result.put("receiptId", receiptId);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in creating Day Id " + module);
        }
    }
    
    public static Map<String, Object> updateDay(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();

            GenericValue dayDetail = (GenericValue) delegator.makeValue("DayDetail");
            dayDetail.setPKFields(context);
            dayDetail.setNonPKFields(context);
            dayDetail.store();

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("dayDetail", dayDetail);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in updating DayDetail in " + module);
        }
    }

    public Map<String, Object> findPOSProductPrice(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            LocalDispatcher dispatcher = dispatchContext.getDispatcher();
            String productId = (String) context.get("productId");
            String productStoreGroupId = (String) context.get("productStoreGroupId");
            String currencyUomId = (String) context.get("currencyUomId");
            
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            System.out.println("productId------------&&&&&----------------"+productId);
            System.out.println("product------------&&&&&----------------"+product);
            Map<String, Object> result = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product,"productStoreGroupId", productStoreGroupId, "currencyUomId",currencyUomId));

            return result;
        } catch (GenericServiceException genericServiceException) {
            Debug.logError(genericServiceException, module);
            return ServiceUtil.returnError("Error in closing day in " + module);
        }catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in closing day in " + module);
        }
    }
    public Map<String, Object> findPOSProducts(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        Delegator delegator = dispatchContext.getDelegator();
        String searchBy = (String) context.get("searchBy");
        String searchValue = (String) context.get("searchValue");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
        List<EntityCondition> entityConditions = new LinkedList<EntityCondition>();
        String partyId = "";
        searchValue = searchValue.toUpperCase().trim();
        entityConditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(searchBy), EntityOperator.LIKE, EntityFunction.UPPER("%" + searchValue + "%")));
        List<GenericValue> products = null;
        GenericValue productPrice = new GenericValue();
        String productId = "", mrp = "";
        Map<String,Object> productMap = new HashMap<String, Object>();
        List<Map<String,Object>> productListMap = new ArrayList<Map<String,Object>>();
        try {
        	GenericValue facilityParty = new GenericValue();
        	List<GenericValue> facilityList = new ArrayList<GenericValue>();
        	List<String> productIds = new ArrayList<String>();
        	//get User Facility
        	if(UtilValidate.isNotEmpty(userLogin)) {
        		partyId = userLogin.getString("partyId");
        		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
    					.orderBy("productId").orderBy("createdStamp").queryOne();

        	}
        	//get Facility Inventory Items
        	if (UtilValidate.isNotEmpty(facilityParty)) {
        		String facilityId = facilityParty.getString("facilityId");
        		productIds = EntityQuery.use(delegator).from("InventoryItem").where("facilityId",facilityId)
    					.orderBy("productId").distinct(true).getFieldList("productId");
        		//productIds = EntityUtil.getFieldListFromEntityList(facilityList, "productId", true);
        	}
        	EntityCondition faciliyPrdCond = EntityCondition.makeCondition("productId",EntityOperator.IN,productIds);
        	entityConditions.add(faciliyPrdCond);
        	EntityCondition entityCondition = EntityCondition.makeCondition(entityConditions, EntityOperator.AND);
            products = EntityQuery.use(delegator).from("Product").where(entityCondition).orderBy("productName", "description").queryList();
            for (GenericValue product : products) {
            	productId = product.getString("productId");
            	productPrice = EntityQuery.use(delegator).from("ProductPrice").
            			where(UtilMisc.toMap("productId",productId,"productPriceTypeId","DEFAULT_PRICE")).queryFirst();
            	if (UtilValidate.isNotEmpty(productPrice)) {
            		mrp = productPrice.getBigDecimal("price").toString();
            	}
            	productMap = product.getAllFields();
            	productMap.put("mrp", mrp);
            	productListMap.add(productMap);
            }
            //get price
            
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        result.put("products", productListMap);
        return result;
    }
    
    public Map<String, Object> getStockProducts(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        Delegator delegator = dispatchContext.getDelegator();
        String searchBy = (String) context.get("searchBy");
        String searchValue = (String) context.get("searchValue");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
        List<EntityCondition> entityConditions = new LinkedList<EntityCondition>();
        String partyId = "";
        searchValue = searchValue.toUpperCase().trim();
        entityConditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(searchBy), EntityOperator.LIKE, EntityFunction.UPPER("%" + searchValue + "%")));
        entityConditions.add(EntityCondition.makeCondition(EntityOperator.OR,
        		EntityCondition.makeCondition("isPosVirtual",EntityOperator.EQUALS,null),
        		EntityCondition.makeCondition("isPosVirtual",EntityOperator.NOT_EQUAL,"Y")));
        List<GenericValue> products = null;
        GenericValue productPrice = new GenericValue();
        GenericValue productEan = new GenericValue();
        String productId = "", mrp = "";
        Map<String,Object> productMap = new HashMap<String, Object>();
        List<Map<String,Object>> productListMap = new ArrayList<Map<String,Object>>();
        try {
        	GenericValue facilityParty = new GenericValue();
        	List<GenericValue> facilityList = new ArrayList<GenericValue>();
        	List<String> productIds = new ArrayList<String>();
        	//get User Facility
        	if(UtilValidate.isNotEmpty(userLogin)) {
        		partyId = userLogin.getString("partyId");
        		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
    					.orderBy("productId").orderBy("createdStamp").queryOne();

        	}
        	//get Facility Inventory Items
        	if (UtilValidate.isNotEmpty(facilityParty)) {
        		String facilityId = facilityParty.getString("facilityId");
        		productIds = EntityQuery.use(delegator).from("InventoryItem").where("facilityId",facilityId)
    					.orderBy("productId").distinct(true).getFieldList("productId");
        		//productIds = EntityUtil.getFieldListFromEntityList(facilityList, "productId", true);
        	}
        	//EntityCondition faciliyPrdCond = EntityCondition.makeCondition("productId",EntityOperator.IN,productIds);
        	//entityConditions.add(faciliyPrdCond);
        	EntityCondition entityCondition = EntityCondition.makeCondition(entityConditions, EntityOperator.AND);
            products = EntityQuery.use(delegator).from("Product").where(entityCondition).orderBy("productName", "description").queryList();
            for (GenericValue product : products) {
            	productId = product.getString("productId");
            	productPrice = EntityQuery.use(delegator).from("ProductPrice").
            			where(UtilMisc.toMap("productId",productId,"productPriceTypeId","DEFAULT_PRICE")).queryFirst();
            	if (UtilValidate.isNotEmpty(productPrice)) {
            		mrp = productPrice.getBigDecimal("price").toString();
            	}
            	productMap = product.getAllFields();
            	productMap.put("mrp", mrp);
            	//Get product Ean
            	productEan = EntityQuery.use(delegator).from("GoodIdentification").
            			where(UtilMisc.toMap("productId",productId,"goodIdentificationTypeId","EAN")).queryFirst();
            	if(UtilValidate.isNotEmpty(productEan)) {
            		productMap.put("ean", productEan.getString("idValue"));
            	} else {
            		productMap.put("ean","");
            	}
            	productListMap.add(productMap);
            }
            
            //get price
            
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        result.put("products", productListMap);
        return result;
    }
    public static Map<String, Object> findStorePOSTerminals(Delegator delegator,String storeId) {
        try {

            Map<String, String> fields = new HashMap<>();
            if (UtilValidate.isNotEmpty(storeId)) {
                fields.put("facilityId", storeId);
            }

            List<GenericValue> posTerminals = delegator.findByAnd("PosTerminal", fields, new ArrayList<>(), false);

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("posTerminals", posTerminals);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in finding Store POSTerminals in " + module);
        }
    }
    	
    	public Map<String, Object> posCheckout(HttpServletRequest request, HttpServletResponse response) {
    	
		/*
		 * Delegator delegator = dispatchContext.getDelegator(); LocalDispatcher
		 * dispatcher = dispatchContext.getDispatcher(); HttpServletRequest request =
		 * (HttpServletRequest) context.get("request");
		 */
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
    	String terminalId = (String) request.getParameter("posTerminalId");
    	/*String productId = "", quantity = "";
    	List<Map<String, Object>> productList = (ArrayList<Map<String,Object>>) context.get("productList");
        try {
        	if(UtilValidate.isNotEmpty(productList)) {
        		for (Map<String, Object> productMap : productList) {
        			productId = (String) productMap.get("productId");
        			quantity = (String) productMap.get("quantity");
        			
        			request.setAttribute("add_product_id", productId);
        			request.setAttribute("quantity", quantity);
        			Map<String, Object> addCartItem = dispatcher.runSync("addCartItem", UtilMisc.toMap("posTerminalId", terminalId));
        		}
        	}*/
        	result.put("terminalId", "PS_01");

            return result;
        /*} catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in finding Store POSTerminals in " + module);
        }*/
    }
    	
	public Map<String, Object> derivedKey(DispatchContext ctx, Map<String, ? extends Object> context) {
    	
		HashMap<String, Object> result = new HashMap<String, Object>();
		
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Double cartAmt = (Double) context.get("cartAmt");//From mobile
		String currencyCode = "INR";//to be get from store
		//Ids
		String merchantId = "T_78600";//To be get from store merchant data config
		String aggId = "J_00022";//One application + 
		//Secret keys
		String aggSKey = "0a0e9179649b4e1c9d62912875736eec";
		String mSKey = "1ef0a85b53cb48559c8bc1679e368baf";
		String paymentTxnId = "CMFPK1000234";//newly created payment trxn id from good entity(AlphaNumeric min 10 digit)
		String billId = (String) context.get("billId");//From mobile
		String derivedKey = "";
		try {
			
			 //1) Concatenate all the above 4 fields (Order cartAmt+currencyCode+merchantId+paymentTxnId) 
			 //2)Generate a HMAC-256 value using the secret key corresponding to the above
			 // merchantId. In case of aggregator, the aggregator key should be used. 
			 //3)Encode the output to Hex (lower case) 
			 //4) This forms the derived key.
			String encodeData = cartAmt.toString()+currencyCode+merchantId+paymentTxnId;
			derivedKey = encode(mSKey, encodeData);
			result.put("derivedKey", derivedKey);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return result;
    }
	
	public static String encode(String key, String data) throws Exception {
	  Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
	  SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
	  sha256_HMAC.init(secret_key);
	  //convert sha256_HMAC to lower case Hexa
	  String encodeHexa = Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
	  
	  return encodeHexa.toLowerCase();
	}

	public Map<String, Object> findDayDetails(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        Delegator delegator = dispatchContext.getDelegator();
        
        String dayId = (String) context.get("dayId");
        String day = (String) context.get("day");
        String terminalId = (String) context.get("terminalId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
        List<Map<String,Object>> productListMap = new ArrayList<Map<String,Object>>();
        List<GenericValue> dayDetailList = new ArrayList<GenericValue>();
        List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
        EntityCondition dayDetailCond = null;
        try {
        	if(UtilValidate.isNotEmpty(dayId)) {
        		andExprs.add(EntityCondition.makeCondition("dayId",EntityOperator.EQUALS,dayId));
        	}
        	if(UtilValidate.isNotEmpty(day)) {
        		andExprs.add(EntityCondition.makeCondition("day",EntityOperator.EQUALS,day));
        	}
        	if(UtilValidate.isNotEmpty(terminalId)) {
        		andExprs.add(EntityCondition.makeCondition("terminalId",EntityOperator.EQUALS,terminalId));
        	}
        	//andExprs.add(EntityCondition.makeCondition("dayStatus",EntityOperator.EQUALS,"OPENED"));
        	//andExprs(EntityCondition.makeCondition("adminOpeningDayStatus",EntityOperator.EQUALS,"APPROVE"));
        	
        	dayDetailCond = EntityCondition.makeCondition(andExprs,EntityOperator.AND);
        	dayDetailList = EntityQuery.use(delegator).from("DayDetail").where(dayDetailCond).queryList();
        	
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        result.put("DayDetails", dayDetailList);
        return result;
    }
	
	public Map<String, Object> updateProductEAN(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        Delegator delegator = dispatchContext.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        GenericValue GoodIdentification = new GenericValue();
        List<GenericValue> eanGoodIdentification = new ArrayList<GenericValue>();
        try {
        	List<GenericValue> goodIdentificationList = EntityQuery.use(delegator).from("GoodIdentification").queryList();
        	
        	for (GenericValue row : goodIdentificationList) {
        		if(row.getString("goodIdentificationTypeId").equalsIgnoreCase("ISBN")) {
        			eanGoodIdentification = EntityQuery.use(delegator).from("GoodIdentification").
        					where("goodIdentificationTypeId","EAN","productId",row.getString("productId"),
        							"idValue",row.getString("idValue")).queryList();
        			if (eanGoodIdentification.isEmpty()) {
        				GoodIdentification = delegator.makeValue("GoodIdentification",
            					UtilMisc.toMap("goodIdentificationTypeId","EAN","productId",row.getString("productId"),"idValue",row.getString("idValue")));
            			GoodIdentification.create();
        			}
        		}
        	}
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        result.put("Sucess", "Update Successfully");
        return result;
    }
	
	public Map<String, Object> updateProductPriceTax(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        Delegator delegator = dispatchContext.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<GenericValue> productPriceList = new ArrayList<GenericValue>();
        BigDecimal taxPercentage = BigDecimal.ZERO;
        try {
        	List<GenericValue> globalProductPriceList = EntityQuery.use(delegator).from("ProductPrice").
        													where("productStoreGroupId","_NA_").queryList();
        	
        	for (GenericValue row : globalProductPriceList) {
        		EntityCondition prdPriceCond = EntityCondition.makeCondition(EntityOperator.AND,
        				EntityCondition.makeCondition("productId",EntityOperator.EQUALS,row.getString("productId")),
        						EntityCondition.makeCondition("productStoreGroupId",EntityOperator.NOT_EQUAL,"_NA_"));
        		
        		productPriceList = EntityQuery.use(delegator).from("ProductPrice").where(prdPriceCond).queryList();
        		if(UtilValidate.isNotEmpty(row.getBigDecimal("taxPercentage"))) {
        			taxPercentage = row.getBigDecimal("taxPercentage");
        		}
				if (UtilValidate.isNotEmpty(productPriceList)) {
					//update PrdPrice data
					for (GenericValue prdPriceRow : productPriceList) {
						
						prdPriceRow.set("taxPercentage", taxPercentage);
						prdPriceRow.set("taxAuthPartyId", "GST");
						prdPriceRow.set("taxAuthGeoId", "IN-MH");
						prdPriceRow.set("taxInPrice", "Y");
						prdPriceRow.store();
					}
				}
        	}
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        result.put("Sucess", "Update Successfully");
        return result;
    }
	public static Map<String, Object> findSuppliers(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Map<String, Object> result = new HashMap<String, Object>();
        try {
            Delegator delegator = dispatchContext.getDelegator();

            String roleTypeId = (String) context.get("roleTypeId");
            List<GenericValue> partyRoleList = EntityQuery.use(delegator).from("PartyRoleAndPartyDetail").
            										where("roleTypeId", roleTypeId).queryList();

            result.put("partyRoleList", partyRoleList);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in Getting Suppliers list " + module);
        }
    }
	public static Map<String, Object> findVendorProducts(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        System.out.println("came into getProductPriceInfo-------");
        String productId = (String) context.get("productId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");
        String productStoreId = (String) context.get("productStoreId");
        String currencyUomId = (String) context.get("currencyUomId");
        String categoryId = (String) context.get("categoryId");
        String supplierId = (String) context.get("supplierId");
        String prdId = new String(), partyId = "";
        Map<String, Double> productMap = new HashMap<String, Double>();
        Map<String, Double> productSaleMap = new HashMap<String, Double>();
        Map<String, Object> productTaxMap = new HashMap<String, Object>();
        List<GenericValue> productPriceData = new LinkedList<GenericValue>();

        Map<String, Object> productPriceInfoMap = new HashMap<String, Object>();
        List<Map<String, Object>> finalProductPriceInfoList = new LinkedList<Map<String, Object>>();
        ArrayList<String> productIdArray = new ArrayList<String>();
        List<GenericValue> InventoryItemList = new ArrayList<GenericValue>();
        if(UtilValidate.isNotEmpty(userLogin)) {
        	partyId = userLogin.getString("partyId");
        }

        try {
            if (UtilValidate.isNotEmpty(productId)) {
            	productPriceData = getSupplierProducts(delegator, supplierId, productId);
                if (UtilValidate.isNotEmpty(productPriceData)) {
                    for (GenericValue productDataEach : productPriceData) {
                        prdId = productDataEach.getString("productId");
                        productMap = UnmappedItemService.getProductMrp(delegator,dispatcher, prdId, productStoreId, currencyUomId);
                        productSaleMap = UnmappedItemService.getProductSaleRate(delegator,dispatcher, prdId,productStoreId, currencyUomId);
                        //get Tax
                        productTaxMap = UnmappedItemService.getProductTaxRate(delegator,dispatcher, prdId, productStoreId, supplierId, 
                        		partyId, currencyUomId, productMap.get("productPrice"));
                        productPriceInfoMap = productDataEach.getAllFields();

                        productPriceInfoMap.put("productPrice", productMap.get("productPrice"));
                        productPriceInfoMap.put("currencyUomId", productMap.get("currencyUomId"));
                        productPriceInfoMap.put("productSalePrice", productSaleMap.get("productSalePrice"));
                        productPriceInfoMap.put("productNewSalePrice", productSaleMap.get("newProductSalePrice"));
                        productPriceInfoMap.put("productTaxMap", productTaxMap.get("taxMap"));
                        productPriceInfoMap.put("currencyUomId", currencyUomId);
                        productPriceInfoMap.put("productIdEan", UnmappedItemService.getProductEan(delegator,prdId));
                        finalProductPriceInfoList.add(productPriceInfoMap);
                        productIdArray.add(prdId);
                    }
                }
            } else {
            	//Get user loggedin facility products
                //productPriceData = EntityQuery.use(delegator).from("Product").queryList();
            	productPriceData = getUserloginSupplierProducts(delegator, supplierId);
                if (UtilValidate.isNotEmpty(productPriceData)) {
                    for (GenericValue productDataEach : productPriceData) {
                        prdId = productDataEach.getString("productId");
                        productMap = UnmappedItemService.getProductMrp(delegator,dispatcher, prdId, productStoreId, currencyUomId);
                        productSaleMap = UnmappedItemService.getProductSaleRate(delegator,dispatcher, prdId,productStoreId, currencyUomId);
                        productPriceInfoMap = productDataEach.getAllFields();

                        productPriceInfoMap.put("productPrice", productMap.get("productPrice"));
                        productPriceInfoMap.put("currencyUomId", productMap.get("currencyUomId"));
                        productPriceInfoMap.put("productSalePrice", productSaleMap.get("productSalePrice"));
                        productPriceInfoMap.put("productNewSalePrice", productSaleMap.get("newProductSalePrice"));
                        productPriceInfoMap.put("currencyUomId", currencyUomId);
                        productPriceInfoMap.put("productIdEan", UnmappedItemService.getProductEan(delegator,prdId));
                        finalProductPriceInfoList.add(productPriceInfoMap);
                        productIdArray.add(prdId);
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "UnmapProductService.groovy");
        }

        result.put("finalProductPriceInfoList", finalProductPriceInfoList);
        result.put("productIdArray", productIdArray);
        return result;
    }
	private static List<GenericValue> getSupplierProducts(Delegator delegator, String supplierId, String productId) throws GenericEntityException, GenericServiceException {
    	List<GenericValue> productList = new ArrayList<GenericValue>();
    	List<GenericValue> productNameList = new ArrayList<GenericValue>();
    	List<GenericValue> productEanList = new ArrayList<GenericValue>();
    	List<GenericValue> finalPrdList = new ArrayList<GenericValue>();
    	//get User Facility
    	try {
        	List<String> productIds = new ArrayList<String>();
        	
    		if(UtilValidate.isNotEmpty(supplierId)) {
        		
        		//get Facility Inventory Items
            	if (UtilValidate.isNotEmpty(supplierId)) {
            		productIds = EntityQuery.use(delegator).from("SupplierProduct").where("partyId",supplierId)
        					.orderBy("productId").distinct(true).getFieldList("productId");
            		
            		//EntityCondition to check by productId
            		EntityCondition faciliyPrdCond = EntityCondition.makeCondition(EntityOperator.AND,
            				EntityCondition.makeCondition("productId",EntityOperator.EQUALS,productId),
            				EntityCondition.makeCondition("productId",EntityOperator.IN,productIds));
            		productList = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where(faciliyPrdCond).
                    		orderBy("productName", "description").queryList();
            		//EntityCondition to check by productName
            		faciliyPrdCond = EntityCondition.makeCondition(EntityOperator.AND,
            				EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productName"), 
                    				EntityOperator.LIKE, EntityFunction.UPPER("%" + productId + "%")),
            				EntityCondition.makeCondition("productId",EntityOperator.IN,productIds));
            		productNameList = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where(faciliyPrdCond).
                    		orderBy("productName", "description").queryList();
            		//EntityCondition to check by productEanCode
            		faciliyPrdCond = EntityCondition.makeCondition(EntityOperator.AND,
            				EntityCondition.makeCondition("idValue",EntityOperator.EQUALS,productId),
            				EntityCondition.makeCondition("productId",EntityOperator.IN,productIds));
            		productEanList = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where(faciliyPrdCond).
                    		orderBy("productName", "description").queryList();
                    if(UtilValidate.isNotEmpty(productList)) {
                    	finalPrdList.addAll(productList);
                    }
					if(UtilValidate.isNotEmpty(productNameList)) {
						finalPrdList.addAll(productNameList);
					}
					if(UtilValidate.isNotEmpty(productEanList)) {
						finalPrdList.addAll(productEanList);
					}
            	}
        	}
    	} catch (GenericEntityException e) {
    		
    	}
    	
        return finalPrdList;
    }
	private static List<GenericValue> getUserloginSupplierProducts(Delegator delegator, String supplierId) throws GenericEntityException, GenericServiceException {
    	List<GenericValue> products = null;
    	//get User Facility
    	try {
        	List<String> productIds = new ArrayList<String>();
        	
    		if(UtilValidate.isNotEmpty(supplierId)) {
        		//get Facility Inventory Items
            	if (UtilValidate.isNotEmpty(supplierId)) {
            		productIds = EntityQuery.use(delegator).from("SupplierProduct").where("partyId",supplierId)
        					.orderBy("productId").distinct(true).getFieldList("productId");
            		
            		EntityCondition faciliyPrdCond = EntityCondition.makeCondition("productId",EntityOperator.IN,productIds);
                    products = EntityQuery.use(delegator).from("Product").where(faciliyPrdCond).orderBy("productName", "description").queryList();
            	}
        	}
    	} catch (GenericEntityException e) {
    		
    	}
    	
        return products;
    }
	
	public static Map<String, Object> createPurchaseOrderTxn(DispatchContext dctx, Map<String, ? extends Object> context) {
		
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = userLogin.getString("partyId");
        Map<String, Object> result = new HashMap<String, Object>();
        
        String productStoreId = (String) context.get("productStoreId");
        String currencyUomId = (String) context.get("currencyUomId");
        String supplierId = (String) context.get("supplierId");
        String poDate = (String) context.get("poDate");
        String validBy = (String) context.get("validBy");
        String statusId = (String) context.get("statusId");
        
        String orderItemArray = (String) context.get("orderItemArray");
        String orderQuantityArray = (String) context.get("orderQuantityArray");
        String orderItemMRPArray = (String) context.get("orderItemMRPArray");
        String oiOfferRateArray = (String) context.get("oiOfferRateArray");
        String oiTaxRateArray = (String) context.get("oiTaxRateArray");
        String sgstAmtArray = (String) context.get("sgstAmtArray");
        String cgstAmtArray = (String) context.get("cgstAmtArray");
        String igstAmtArray = (String) context.get("igstAmtArray");
        
        String [] orderItemIds = null;
        String [] orderQuantitys = null;
        String [] orderItemMrps = null;
        String [] oiOfferRates = null;
        String [] oiTaxRates = null;
        String [] sgstAmts = null;
        String [] cgstAmts = null;
        String [] igstAmts = null;
        
        String orderId = "", orderItemID = "", orderAdjustmentId = "", supplierProductId = "";
        String productName = "";
        String iorderAdjustmentId = "", corderAdjustmentId = "" , sorderAdjustmentId = "";
        String createdOrderId = "";
        String estimatedDeliveryDate = null;
        
        Timestamp estimatedDeliveryTimestamp = null;
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal itemTotal = BigDecimal.ZERO;
        GenericValue orderItems = new GenericValue();
        GenericValue sorderItems = new GenericValue();
        GenericValue OrderAdjustments = new GenericValue();
        List<GenericValue> finalorderItems = new ArrayList<GenericValue>();
        List<GenericValue> finalOrderAdjustments = new ArrayList<GenericValue>();
        try {
        	//create OrderItem
        	if(UtilValidate.isNotEmpty(validBy)) {
        		estimatedDeliveryDate = validBy.trim();
        		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        	    Date parsedDate = (Date) dateFormat.parse(estimatedDeliveryDate);
        	    estimatedDeliveryTimestamp = new java.sql.Timestamp(parsedDate.getTime());
        	}
        	if(UtilValidate.isNotEmpty(orderItemArray)) {
        		orderItemIds = orderItemArray.split(",");
        		orderQuantitys = orderQuantityArray.split(",");
        		orderItemMrps = orderItemMRPArray.split(",");
        		oiOfferRates = oiOfferRateArray.split(",");
        		oiTaxRates = oiTaxRateArray.split(",");
        		sgstAmts = sgstAmtArray.split(",");
        		cgstAmts = cgstAmtArray.split(",");
        		igstAmts = igstAmtArray.split(",");
        		
        		int i = 0;
        		for (String orderItemId : orderItemIds) {
        			
        			orderItemID = delegator.getNextSeqId("OrderItemTxn");
        			
        			BigDecimal orderQuantity = new BigDecimal(orderQuantitys[i]);
        			BigDecimal unitPrice = new BigDecimal(orderItemMrps[i]);
        			BigDecimal unitOfferPrice = new BigDecimal(oiOfferRates[i]);
        			BigDecimal sgstAmt = new BigDecimal(sgstAmts[i]);
        			BigDecimal cgstAmt = new BigDecimal(cgstAmts[i]);
        			BigDecimal igstAmt = new BigDecimal(igstAmts[i]);
        			GenericValue supplierProduct = EntityQuery.use(delegator).from("SupplierProduct").
        					where("productId", orderItemId).queryFirst();
        			GenericValue products = EntityQuery.use(delegator).from("Product").
        					where("productId", orderItemId).queryFirst();
        			
        			if(UtilValidate.isNotEmpty(products)) {
        				productName = products.getString("productName");
        			}
            		GenericValue createOrderItemTxn = delegator.makeValue("OrderItem", 
                			UtilMisc.toMap("orderId",orderItemID, "orderItemSeqId", String.valueOf(i),
                					"externalId", "", "orderItemTypeId", "PRODUCT_ORDER_ITEM",
                					"productId", orderItemId, "supplierProductId", supplierProduct.getString("supplierProductId"),
                					"quantity", orderQuantity, "unitPrice", unitOfferPrice, "unitListPrice", unitPrice,
                					"isModifiedPrice", "Y", "itemDescription",productName,"statusId", statusId,
                					"estimatedShipDate", estimatedDeliveryTimestamp,"estimatedDeliveryDate", estimatedDeliveryTimestamp,
                					"shipBeforeDate", estimatedDeliveryTimestamp));
            		//createOrderItemTxn.create();
            		/*sorderItems = EntityQuery.use(delegator).from("OrderItemTxn").
            				where("orderId",orderItemID,"orderItemSeqId",String.valueOf(i)).queryOne();
            		orderItems = (GenericValue) orderItems.getAllFields();*/
            		finalorderItems.add(createOrderItemTxn);
            		//create Order Adjustment
            		if(igstAmt.compareTo(BigDecimal.ZERO) != 0) {
            			//create IGST
            			iorderAdjustmentId = delegator.getNextSeqId("OrderAdjustmentTxn");
            			GenericValue createOrderIGSTAdjTxn = delegator.makeValue("OrderAdjustmentTxn", 
                    			UtilMisc.toMap("orderAdjustmentId",iorderAdjustmentId, "orderAdjustmentTypeId", "SALES_TAX",
                    					"orderItemSeqId", String.valueOf(i), "comments", "Integrated Goods and Service Tax","orderId", orderItemID,
                    					"description", "SALES_TAX", "amount", igstAmt,"sourcePercentage", new BigDecimal(oiTaxRates[i]),
                    					"taxAuthPartyId", "GST", "taxAuthGeoId", "IND"));
            			//createOrderIGSTAdjTxn.create();
            			finalOrderAdjustments.add(createOrderIGSTAdjTxn);
            		} else {
            			//create CGST
            			corderAdjustmentId = delegator.getNextSeqId("OrderAdjustment");
            			GenericValue createOrderCGSTAdjTxn = delegator.makeValue("OrderAdjustment", 
                    			UtilMisc.toMap("orderAdjustmentId",corderAdjustmentId, "orderAdjustmentTypeId", "SALES_TAX",
                    					"orderItemSeqId", String.valueOf(i), "comments", "Central Goods and Service Tax","orderId", orderItemID,
                    					"description", "SALES_TAX", "amount", cgstAmt,"sourcePercentage", new BigDecimal(oiTaxRates[i]),
                    					"taxAuthPartyId", "GST", "taxAuthGeoId", "IN-MH"));
            			//createOrderCGSTAdjTxn.create();
            			/*OrderAdjustments = EntityQuery.use(delegator).from("OrderAdjustmentTxn").
                				where("orderAdjustmentId",corderAdjustmentId).queryOne();*/
            			finalOrderAdjustments.add(createOrderCGSTAdjTxn);
                		//create SGST
            			sorderAdjustmentId = delegator.getNextSeqId("OrderAdjustment");
            			GenericValue createOrderSGSTAdjTxn = delegator.makeValue("OrderAdjustment", 
                    			UtilMisc.toMap("orderAdjustmentId",sorderAdjustmentId, "orderAdjustmentTypeId", "SALES_TAX",
                    					"orderItemSeqId", String.valueOf(i), "comments", "State Goods and Service Tax","orderId", orderItemID,
                    					"description", "SALES_TAX", "amount", sgstAmt,"sourcePercentage", new BigDecimal(oiTaxRates[i]),
                    					"taxAuthPartyId", "GST", "taxAuthGeoId", "IN-MH"));
            			/*createOrderSGSTAdjTxn.create();
            			OrderAdjustments = EntityQuery.use(delegator).from("OrderAdjustmentTxn").
                				where("orderAdjustmentId",sorderAdjustmentId).queryOne();*/
            			finalOrderAdjustments.add(createOrderSGSTAdjTxn);
            		}
        			i++;
        			itemTotal = orderQuantity.multiply(unitPrice.add(sgstAmt).add(cgstAmt).add(igstAmt));
        			grandTotal = grandTotal.add(itemTotal);
        		}
        		//Create Order
    			Map<String, Object> orderMap = UtilMisc.<String, Object>toMap("userLogin", userLogin);
    			orderMap.put("initialStatus", statusId);
    			orderMap.put("orderTypeId", "PURCHASE_ORDER");
    			orderMap.put("partyId", partyId);
                orderMap.put("productStoreId", productStoreId);
                orderMap.put("currencyUom", currencyUomId);
                orderMap.put("grandTotal",  grandTotal);
                orderMap.put("orderDate",  UtilDateTime.nowTimestamp());
                orderMap.put("orderItems",  finalorderItems);
                orderMap.put("orderAdjustments",  finalOrderAdjustments);
                
                Map<String, Object> orderResult = dispatcher.runSync("storeOrder", orderMap);
                if (orderResult != null) {
                    createdOrderId = (String) orderResult.get("orderId");
                    GenericValue createOrderRole = delegator.makeValue("OrderRole", 
                			UtilMisc.toMap("orderId",createdOrderId, "partyId", supplierId,
                					"roleTypeId", "SUPPLIER_AGENT"));
                    createOrderRole.create();
                }
        	}
        	
        	result.put("orderId",createdOrderId);
        } catch(GenericEntityException | ParseException | GenericServiceException e) {
        	
        }
        return result;
    }
	public static Map<String, Object> findPurchaseOrder(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Delegator delegator = dispatchContext.getDelegator();
		Map<String, Object> result = new HashMap<String, Object>();
		String orderId = (String) context.get("orderId");
		String orderStatusId = (String) context.get("orderStatusId");
		String orderFromDate = (String) context.get("orderFromDate");
		String orderThruDate = (String) context.get("orderThruDate");
		String orderFlowType = (String) context.get("orderFlowType");
		List<Map<String, Object>> finalOrderList = new ArrayList<Map<String, Object>>();
		String supplierId = "";
		EntityCondition statusCond = null;
        try {
            List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
            if(orderFlowType.equals("PURCHASEORDER")) {
            	statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_PRT_INITIATED"),
                		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_INITIATED"));
            }
            
            if(orderFlowType.equals("INWARD")) {
            	statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_CREATED"));
            }
            
            andExprs.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"));
            andExprs.add(statusCond);
            if (UtilValidate.isNotEmpty(orderId)) {
            	andExprs.add(EntityCondition.makeCondition("orderId",EntityOperator.EQUALS,orderId));
            }
            if (UtilValidate.isNotEmpty(orderStatusId)) {
            	andExprs.add(EntityCondition.makeCondition("statusId",EntityOperator.EQUALS,orderStatusId));
            }
            EntityCondition mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
            List<GenericValue> orderHeaderList = EntityQuery.use(delegator).from("OrderHeader").where(mainCond).queryList();
            
            for (GenericValue orderHeaderRow : orderHeaderList) {
            	Map<String, Object> orderMap = new HashMap<String, Object>();
            	String orderHeaderId = orderHeaderRow.getString("orderId");
            	orderMap.put("orderId", orderHeaderId);
            	//get supplierName
            	GenericValue orderRole = EntityQuery.use(delegator).from("OrderRole").where("orderId",orderHeaderId,
            			"roleTypeId", "SUPPLIER_AGENT").queryFirst();
            	if(UtilValidate.isNotEmpty(orderRole)) {
            		supplierId = orderRole.getString("partyId");
            	}
            	if(UtilValidate.isNotEmpty(supplierId)) {
            		GenericValue supplierPerson = EntityQuery.use(delegator).from("Person").where("partyId",supplierId).queryOne();
            		orderMap.put("vendorName", supplierPerson.getString("firstName"));
            	}
            	SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
            	Date poDate = (Date) sf.parse(orderHeaderRow.getTimestamp("orderDate").toString());
            	SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
            	String orderDate = newFormat.format(poDate);
            	//Date orderDate = new Date(Long.parseLong(orderHeaderRow.getTimestamp("orderDate").toString()));
            	orderMap.put("orderDate", orderDate);
            	orderMap.put("orderStatus", orderHeaderRow.getString("statusId"));
            	orderMap.put("orderCreatedBy", orderHeaderRow.getString("createdBy"));
            	finalOrderList.add(orderMap);
            }
            result.put("orderList", finalOrderList);

            return result;
        } catch (GenericEntityException | ParseException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in Getting Suppliers list " + module);
        }
    }
	
	
	public static Map<String, Object> findPurchaseOrderItems(DispatchContext dispatchContext, Map<String, ? extends Object> context)  {
		
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = new HashMap<String, Object>();
		String orderId = (String) context.get("orderId");
		String productStoreId = (String) context.get("productStoreId");
		String currencyUomId = (String) context.get("currencyUomId");
		String orderStatusId = (String) context.get("orderStatusId");
		String orderFromDate = (String) context.get("orderFromDate");
		String orderThruDate = (String) context.get("orderThruDate");
		String orderFlowType = (String) context.get("orderFlowType");
		List<Map<String, Object>> finalOrderList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> finalOrderItemList = new ArrayList<Map<String, Object>>();
		Map<String, Double> productMap = new HashMap<String, Double>();
		String supplierId = "";
		ArrayList<String> orderItemArray = new ArrayList<String>();
		ArrayList<String> orderQuantityArray = new ArrayList<String>();
		ArrayList<String> orderItemMRPArray = new ArrayList<String>();
		ArrayList<String> oiOfferRateArray = new ArrayList<String>();
		ArrayList<String> oiTaxRateArray = new ArrayList<String>();
		ArrayList<String> sgstAmtArray = new ArrayList<String>();
		ArrayList<String> cgstAmtArray = new ArrayList<String>();
		ArrayList<String> igstAmtArray = new ArrayList<String>();
		
		EntityCondition statusCond = null;
        try {
            List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
            List<EntityCondition> orderItemExprs = new ArrayList<EntityCondition>();
            
            if(orderFlowType.equals("PURCHASEORDER")) {
            	statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_PRT_INITIATED"),
                		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_INITIATED"));
            }
            
            if(orderFlowType.equals("INWARD")) {
            	statusCond = EntityCondition.makeCondition(EntityOperator.OR,
                		EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_CREATED"));
            }
            
            andExprs.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "PURCHASE_ORDER"));
            andExprs.add(statusCond);
            if (UtilValidate.isNotEmpty(orderId)) {
            	andExprs.add(EntityCondition.makeCondition("orderId",EntityOperator.EQUALS,orderId));
            }
            if (UtilValidate.isNotEmpty(orderStatusId)) {
            	andExprs.add(EntityCondition.makeCondition("statusId",EntityOperator.EQUALS,orderStatusId));
            }
            EntityCondition mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
            List<GenericValue> orderHeaderList = EntityQuery.use(delegator).from("OrderHeader").where(mainCond).queryList();
            
            for (GenericValue orderHeaderRow : orderHeaderList) {
            	Map<String, Object> orderMap = new HashMap<String, Object>();
            	String orderHeaderId = orderHeaderRow.getString("orderId");
            	orderMap.put("orderId", orderHeaderId);
            	//get supplierName
            	GenericValue orderRole = EntityQuery.use(delegator).from("OrderRole").where("orderId",orderHeaderId,
            			"roleTypeId", "SUPPLIER_AGENT").queryFirst();
            	if(UtilValidate.isNotEmpty(orderRole)) {
            		supplierId = orderRole.getString("partyId");
            	}
            	if(UtilValidate.isNotEmpty(supplierId)) {
            		GenericValue supplierPerson = EntityQuery.use(delegator).from("Person").where("partyId",supplierId).queryOne();
            		orderMap.put("vendorName", supplierPerson.getString("firstName"));
            	}
            	SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
            	Date poDate = (Date) sf.parse(orderHeaderRow.getTimestamp("orderDate").toString());
            	SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
            	String orderDate = newFormat.format(poDate);
            	//Date orderDate = new Date(Long.parseLong(orderHeaderRow.getTimestamp("orderDate").toString()));
            	orderMap.put("orderDate", orderDate);
            	orderMap.put("orderStatus", orderHeaderRow.getString("statusId"));
            	orderMap.put("orderCreatedBy", orderHeaderRow.getString("createdBy"));
            	finalOrderList.add(orderMap);
            }
            //get OrderItem List
            orderItemExprs.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
            orderItemExprs.add(statusCond);
            EntityCondition orderItemMainCond = EntityCondition.makeCondition(orderItemExprs, EntityOperator.AND);
            
            List<GenericValue> orderItemList = EntityQuery.use(delegator).from("OrderItem").where(orderItemMainCond).queryList();
            if(UtilValidate.isNotEmpty(orderItemList)) {
            	//get OrderItemList
            	String productId = "";
            	String productEan = "", productName = "",orderItemSeqId = "";
            	Double unitPrice = 0.0;
            	Double productPrice = 0.0;
            	BigDecimal orderAmt = BigDecimal.ZERO;
            	BigDecimal orderSgst = BigDecimal.ZERO;
            	BigDecimal taxPercentage = BigDecimal.ZERO;
            	BigDecimal orderCgst = BigDecimal.ZERO;
            	BigDecimal orderQty = BigDecimal.ZERO;
            	for (GenericValue orderItemRow : orderItemList) {
            		Map<String, Object> orderItemMap = new HashMap<String, Object>();
					productId = orderItemRow.getString("productId");
					orderQty = orderItemRow.getBigDecimal("quantity");
            		unitPrice = orderItemRow.getBigDecimal("unitPrice").doubleValue();
            		orderItemSeqId = orderItemRow.getString("orderItemSeqId");
            		//get product EAN
            		if(UtilValidate.isNotEmpty(productId)) {
            			GenericValue eanList = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").
            					where("productId", productId,"goodIdentificationTypeId", "EAN").queryOne();
            			if(UtilValidate.isNotEmpty(eanList)) {
            				productEan = eanList.getString("idValue");
            				productName = eanList.getString("productName");
            				orderItemMap.put("productEan", productEan);
            				//get productName
            				orderItemMap.put("productName", productName);
            			}
            		}
            		
            		//get productUom
            		orderItemMap.put("currencyUomId", currencyUomId);
            		//get orderQty
            		orderItemMap.put("orderQty", orderQty.toString());
            		//get productMrp
            		productMap = UnmappedItemService.getProductMrp(delegator,dispatcher, productId, productStoreId, currencyUomId);
            		if(UtilValidate.isNotEmpty(productMap)) {
            			productPrice = productMap.get("productPrice");
            			orderItemMap.put("productPrice", productPrice.toString());
            		}
            		//get Product Offer Rate
            		if (unitPrice <= productPrice) {
            			orderItemMap.put("offerPrice", unitPrice.toString());
            		}
            		List<GenericValue> orderAdjCgstList = EntityQuery.use(delegator).from("OrderAdjustment").
        					where("orderId", orderId,"orderItemSeqId", orderItemSeqId,"comments", "Central Goods and Service Tax").queryList();
            		if(UtilValidate.isNotEmpty(orderAdjCgstList)) {
            			for (GenericValue orderAdjCgstRow : orderAdjCgstList) {
            				//get product Tax
            				taxPercentage = orderAdjCgstRow.getBigDecimal("sourcePercentage");
            				orderItemMap.put("taxPercentage", taxPercentage);
            				//get product cgst
            				orderCgst = orderAdjCgstRow.getBigDecimal("amount");
            				orderItemMap.put("orderCgst", orderCgst.toString());
            			}
            		}
            		List<GenericValue> orderAdjSgstList = EntityQuery.use(delegator).from("OrderAdjustment").
        					where("orderId", orderId,"orderItemSeqId", orderItemSeqId,"comments", "State Goods and Service Tax").queryList();
            		if(UtilValidate.isNotEmpty(orderAdjSgstList)) {
            			for (GenericValue orderAdjSgstRow : orderAdjSgstList) {
            				//get product sgst
            				orderSgst = orderAdjSgstRow.getBigDecimal("amount");
            				orderItemMap.put("orderSgst", orderSgst.toString());
            			}
            		}
            		//get product igst
            		orderItemMap.put("orderIgst", BigDecimal.ZERO.toString());
            		//get product netRate
            		orderItemMap.put("productNetRate", productMap.get("productPrice").toString());
            		//get product Amount
            		orderAmt = orderQty.multiply(new BigDecimal(unitPrice).add(orderCgst.add(orderSgst)));
            		orderItemMap.put("productAmount", orderAmt.toString());
            		finalOrderItemList.add(orderItemMap);
            		
            		orderItemArray.add(productId);
            		orderQuantityArray.add(orderQty.toString());
            		orderItemMRPArray.add(productPrice.toString());
            		oiOfferRateArray.add(unitPrice.toString());
            		oiTaxRateArray.add(taxPercentage.toString());
            		sgstAmtArray.add(orderSgst.toString());
            		cgstAmtArray.add(orderCgst.toString());
            		igstAmtArray.add(BigDecimal.ZERO.toString());
            	}
            }
            
            result.put("orderList", finalOrderList);
            result.put("orderItemList", finalOrderItemList);
            
            result.put("orderItemArray", orderItemArray);
            result.put("orderQuantityArray", orderQuantityArray);
            result.put("orderItemMRPArray", orderItemMRPArray);
            result.put("oiOfferRateArray", oiOfferRateArray);
            result.put("oiTaxRateArray", oiTaxRateArray);
            result.put("sgstAmtArray", sgstAmtArray);
            result.put("cgstAmtArray", cgstAmtArray);
            result.put("igstAmtArray", igstAmtArray);
            
            return result;
        } catch (GenericEntityException | GenericServiceException | ParseException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in Getting Suppliers list " + module);
        }
    }
	
public static Map<String, Object> editPurchaseOrderTxn(DispatchContext dctx, Map<String, ? extends Object> context) {
		//Need to workj
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = userLogin.getString("partyId");
        Map<String, Object> result = new HashMap<String, Object>();
        
        String orderId = (String) context.get("orderId");
        String productStoreId = (String) context.get("productStoreId");
        String currencyUomId = (String) context.get("currencyUomId");
        String supplierId = (String) context.get("supplierId");
        String poDate = (String) context.get("poDate");
        String validBy = (String) context.get("validBy");
        String statusId = (String) context.get("statusId");
        
        String orderItemArray = (String) context.get("orderItemArray");
        String orderQuantityArray = (String) context.get("orderQuantityArray");
        String orderItemMRPArray = (String) context.get("orderItemMRPArray");
        String oiOfferRateArray = (String) context.get("oiOfferRateArray");
        String oiTaxRateArray = (String) context.get("oiTaxRateArray");
        String sgstAmtArray = (String) context.get("sgstAmtArray");
        String cgstAmtArray = (String) context.get("cgstAmtArray");
        String igstAmtArray = (String) context.get("igstAmtArray");
        
        String [] orderItemIds = null;
        String [] orderQuantitys = null;
        String [] orderItemMrps = null;
        String [] oiOfferRates = null;
        String [] oiTaxRates = null;
        String [] sgstAmts = null;
        String [] cgstAmts = null;
        String [] igstAmts = null;
        
        String orderItemID = "", orderAdjustmentId = "", supplierProductId = "";
        String productName = "";
        String iorderAdjustmentId = "", corderAdjustmentId = "" , sorderAdjustmentId = "";
        String createdOrderId = "";
        String estimatedDeliveryDate = null;
        
        Timestamp estimatedDeliveryTimestamp = null;
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal itemTotal = BigDecimal.ZERO;
        GenericValue orderItems = new GenericValue();
        GenericValue sorderItems = new GenericValue();
        GenericValue OrderAdjustments = new GenericValue();
        List<GenericValue> finalorderItems = new ArrayList<GenericValue>();
        List<GenericValue> finalOrderAdjustments = new ArrayList<GenericValue>();
        GenericValue orderHeader = new GenericValue();
        try {
        	//Update OrderHeader
        	if (UtilValidate.isNotEmpty(orderId)) {
        		orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
        		if(UtilValidate.isNotEmpty(orderHeader)) {
        			orderHeader.put("statusId", statusId);
        		}
        	}
        	if(UtilValidate.isNotEmpty(validBy)) {
        		estimatedDeliveryDate = validBy.trim();
        		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        	    Date parsedDate = (Date) dateFormat.parse(estimatedDeliveryDate);
        	    estimatedDeliveryTimestamp = new java.sql.Timestamp(parsedDate.getTime());
        	}
        	if(UtilValidate.isNotEmpty(orderItemArray)) {
        		orderItemIds = orderItemArray.split(",");
        		orderQuantitys = orderQuantityArray.split(",");
        		orderItemMrps = orderItemMRPArray.split(",");
        		oiOfferRates = oiOfferRateArray.split(",");
        		oiTaxRates = oiTaxRateArray.split(",");
        		sgstAmts = sgstAmtArray.split(",");
        		cgstAmts = cgstAmtArray.split(",");
        		igstAmts = igstAmtArray.split(",");
        		
        		int i = 0;
        		for (String orderItemId : orderItemIds) {
        			
        			BigDecimal orderQuantity = new BigDecimal(orderQuantitys[i].trim());
        			BigDecimal unitPrice = new BigDecimal(orderItemMrps[i].trim());
        			BigDecimal unitOfferPrice = new BigDecimal(oiOfferRates[i].trim());
        			BigDecimal sgstAmt = new BigDecimal(sgstAmts[i].trim());
        			BigDecimal cgstAmt = new BigDecimal(cgstAmts[i].trim());
        			BigDecimal igstAmt = new BigDecimal(igstAmts[i].trim());
        			
        			//Update OrderItem
        			//check order productId exists or not in OrderItem entity.
        			GenericValue orderItemList = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId,
        					"productId", orderItemId.trim()).queryFirst();
        			if(UtilValidate.isNotEmpty(orderItemList)) {
        				orderItemList.put("quantity", orderQuantity);
        				orderItemList.put("unitPrice", unitOfferPrice);
        				orderItemList.put("unitListPrice", unitPrice);
        				orderItemList.put("statusId", statusId);
        				orderItemList.store();
        			} else {
        				GenericValue supplierProduct = EntityQuery.use(delegator).from("SupplierProduct").
            					where("productId", orderItemId.trim()).queryFirst();
            			GenericValue products = EntityQuery.use(delegator).from("Product").
            					where("productId", orderItemId.trim()).queryFirst();
            			
            			if(UtilValidate.isNotEmpty(products)) {
            				productName = products.getString("productName");
            			}
            			
        				GenericValue createOrderItemTxn = delegator.makeValue("OrderItem", 
                    			UtilMisc.toMap("orderId",orderId, "orderItemSeqId", String.valueOf(i),
                    					"externalId", "", "orderItemTypeId", "PRODUCT_ORDER_ITEM",
                    					"productId", orderItemId, "supplierProductId", supplierProduct.getString("supplierProductId"),
                    					"quantity", orderQuantity, "unitPrice", unitOfferPrice, "unitListPrice", unitPrice,
                    					"isModifiedPrice", "Y", "itemDescription",productName,"statusId", statusId,
                    					"estimatedShipDate", estimatedDeliveryTimestamp,"estimatedDeliveryDate", estimatedDeliveryTimestamp,
                    					"shipBeforeDate", estimatedDeliveryTimestamp));
                		
        				createOrderItemTxn.create();
                		
                		//create Order Adjustment
                		if(igstAmt.compareTo(BigDecimal.ZERO) != 0) {
                			//create IGST
                			iorderAdjustmentId = delegator.getNextSeqId("OrderAdjustmentTxn");
                			GenericValue createOrderIGSTAdjTxn = delegator.makeValue("OrderAdjustmentTxn", 
                        			UtilMisc.toMap("orderAdjustmentId",iorderAdjustmentId, "orderAdjustmentTypeId", "SALES_TAX",
                        					"orderItemSeqId", String.valueOf(i), "comments", "Integrated Goods and Service Tax","orderId", orderId,
                        					"description", "SALES_TAX", "amount", igstAmt,"sourcePercentage", new BigDecimal(oiTaxRates[i]),
                        					"taxAuthPartyId", "GST", "taxAuthGeoId", "IND"));
                			//createOrderIGSTAdjTxn.create();
                			createOrderIGSTAdjTxn.create();
                			//finalOrderAdjustments.add(createOrderIGSTAdjTxn);
                		} else {
                			//create CGST
                			corderAdjustmentId = delegator.getNextSeqId("OrderAdjustment");
                			GenericValue createOrderCGSTAdjTxn = delegator.makeValue("OrderAdjustment", 
                        			UtilMisc.toMap("orderAdjustmentId",corderAdjustmentId, "orderAdjustmentTypeId", "SALES_TAX",
                        					"orderItemSeqId", String.valueOf(i), "comments", "Central Goods and Service Tax","orderId", orderId,
                        					"description", "SALES_TAX", "amount", cgstAmt,"sourcePercentage", new BigDecimal(oiTaxRates[i]),
                        					"taxAuthPartyId", "GST", "taxAuthGeoId", "IN-MH"));
                			//createOrderCGSTAdjTxn.create();
                			/*OrderAdjustments = EntityQuery.use(delegator).from("OrderAdjustmentTxn").
                    				where("orderAdjustmentId",corderAdjustmentId).queryOne();*/
                    		createOrderCGSTAdjTxn.create();
                			finalOrderAdjustments.add(createOrderCGSTAdjTxn);
                    		//create SGST
                			sorderAdjustmentId = delegator.getNextSeqId("OrderAdjustment");
                			GenericValue createOrderSGSTAdjTxn = delegator.makeValue("OrderAdjustment", 
                        			UtilMisc.toMap("orderAdjustmentId",sorderAdjustmentId, "orderAdjustmentTypeId", "SALES_TAX",
                        					"orderItemSeqId", String.valueOf(i), "comments", "State Goods and Service Tax","orderId", orderId,
                        					"description", "SALES_TAX", "amount", sgstAmt,"sourcePercentage", new BigDecimal(oiTaxRates[i]),
                        					"taxAuthPartyId", "GST", "taxAuthGeoId", "IN-MH"));
                			createOrderSGSTAdjTxn.create();
                			//finalOrderAdjustments.add(createOrderSGSTAdjTxn);
                		}
        			}
        			i++;
        			itemTotal = orderQuantity.multiply(unitPrice.add(sgstAmt).add(cgstAmt).add(igstAmt));
        			grandTotal = grandTotal.add(itemTotal);
        		}
        		orderHeader.put("grandTotal", grandTotal);
    			orderHeader.store();
        	}
        	
        	result.put("orderId",orderId);
        } catch(GenericEntityException | ParseException e) {
        	
        }
        return result;
    }

	public static Map<String, Object> generateReceiptId(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
	    try {
	        Delegator delegator = dispatchContext.getDelegator();
	        String dayId = (String) context.get("dayId");
	        String facilityId = (String) context.get("facilityId");;
	        GenericValue dayDetail = new GenericValue();
	        String receiptId = "";
	        //code to generate receiptId
	        Date d = new Date();
        	String day = UtilDateTime.nowDateString("dd-MM-yyyy");
        	String date = UtilDateTime.nowDateString("dd");
        	String month = UtilDateTime.nowDateString("MM");
        	String year = UtilDateTime.nowDateString("yyyy");
            Long nxtDayId = delegator.getNextSeqIdLong("DayDetail", 1);
            dayDetail = EntityQuery.use(delegator).from("DayDetail").where("dayId", dayId).orderBy("-createdStamp").queryOne();
            if(UtilValidate.isNotEmpty(dayDetail)) {
            	receiptId = facilityId+date+month+year+nxtDayId;
            	dayDetail.set("receiptId", receiptId);
            	dayDetail.store();
            }
	
	        Map<String, Object> result = ServiceUtil.returnSuccess();
	        result.put("receiptId", receiptId);
	
	        return result;
	    } catch (GenericEntityException genericEntityException) {
	        Debug.logError(genericEntityException, module);
	        return ServiceUtil.returnError("Error in updating DayDetail in " + module);
	    }
	}
	
	public static Map<String, Object> getStoreAddress(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
	    try {
	    	Map<String, Object> result = ServiceUtil.returnSuccess();
	        Delegator delegator = dispatchContext.getDelegator();
	        String posTerminalId = (String) context.get("posTerminalId");
	        String facilityId = "", contactMechId = "", gstnno = "";
	        GenericValue posTerminalVal = new GenericValue();
	        GenericValue postalAddress = new GenericValue();
	        List<GenericValue> facilityContactMechs = new ArrayList<GenericValue>();
	        String address1 = "", city = "", postalCode = "", facilityName = "", address2 = "";
	        String directions = "";
	        //get facilityId from posTerminalId
	        if(UtilValidate.isNotEmpty(posTerminalId)) {
	        	posTerminalVal = EntityQuery.use(delegator).from("PosTerminal").where("posTerminalId", posTerminalId).queryOne();
	        	facilityId = posTerminalVal.getString("facilityId");
	        	//get facility gst
	        	GenericValue facilityAttr = EntityQuery.use(delegator).from("FacilityAttribute").where("facilityId", facilityId,
	        			"attrName", "gstnno").queryOne();
	        	if(UtilValidate.isNotEmpty(facilityAttr)) {
	        		gstnno = facilityAttr.getString("attrValue");
	        	}
	        	//get facility contactMech
	        	facilityContactMechs = EntityQuery.use(delegator).from("FacilityAndContactMech").where("facilityId", facilityId,
	        			"contactMechTypeId", "POSTAL_ADDRESS").queryList();
	        	if(UtilValidate.isNotEmpty(facilityContactMechs)) {
	        		facilityName = facilityContactMechs.get(0).getString("facilityName");
	        		contactMechId = facilityContactMechs.get(0).getString("contactMechId");
	        		//get Postal Address
	        		postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", contactMechId).queryOne();
	        		if (UtilValidate.isNotEmpty(postalAddress)) {
	        			if(UtilValidate.isNotEmpty(postalAddress.getString("address1"))) {
	        				address1 = postalAddress.getString("address1");
	        			}
	        			if(UtilValidate.isNotEmpty(postalAddress.getString("address2"))) {
	        				address2 = postalAddress.getString("address2");
	        			}
	        			if(UtilValidate.isNotEmpty(postalAddress.getString("city"))) {
	        				city = postalAddress.getString("city");
	        			}
	        			if(UtilValidate.isNotEmpty(postalAddress.getString("postalCode"))) {
	        				postalCode = postalAddress.getString("postalCode");
	        			}
	        			if(UtilValidate.isNotEmpty(postalAddress.getString("directions"))) {
	        				directions = postalAddress.getString("directions");
	        			}
	        		}
	        		
	        	}
	        	
	        }
	        
	        result.put("facilityName", facilityName);
	        result.put("directions", directions);
	        result.put("postalCode", postalCode);
	        result.put("city", city);
	        result.put("address1", address1);
	        result.put("address2", address2);
	        result.put("gstnno", gstnno);
	        
	        return result;
	    } catch (GenericEntityException e) {
	        Debug.logError(e, module);
	        return ServiceUtil.returnError("Error in updating DayDetail in " + module);
	    }
	}
	public static Map<String, Object> addGlobalProduct(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		
        String barcode = (String) context.get("barcode");
        String productName = (String) context.get("productName");
        String mrp = (String) context.get("mrp");
        String sp = (String) context.get("sp");
        String productId = "", partyId = "", facilityId = "";
        GenericValue createUnMap = new GenericValue();
        GenericValue facilityParty = new GenericValue();
        
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
            	
            	Map<String, Object> createSyncProduct = dispatcher.runSync("createSyncProduct", UtilMisc.toMap("productId", productId,
            												"userLogin", userLogin));
            
        	}
        	result.put("productId", productId);
            return result;
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in creating Global Product " + module);
        }
    }
	public static Map<String, Object> createGlobalProduct(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		
        String barcode = (String) context.get("barcode");
        String productName = (String) context.get("productName");
        String mrp = (String) context.get("mrp");
        String sp = (String) context.get("sp");
        String productId = "", partyId = "", facilityId = "";
        GenericValue createUnMap = new GenericValue();
        GenericValue facilityParty = new GenericValue();
        
		Map<String, Object> result = new HashMap<String, Object>();
        try {
        	Map<String, Object> createPrdRes = dispatcher.runSync("createProduct", UtilMisc.toMap("internalName", productName,
        			"productName", productName, "description", productName, "productTypeId", "FINISHED_GOOD", 
        			"isPosVirtual", "Y", "userLogin", userLogin));
        	
        	if(ServiceUtil.isSuccess(createPrdRes)) {
        		productId = (String) createPrdRes.get("productId");
        		if(UtilValidate.isNotEmpty(userLogin)) {
            		partyId = userLogin.getString("partyId");
            		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
        					.orderBy("productId").orderBy("createdStamp").queryOne();
            		if (UtilValidate.isNotEmpty(facilityParty)) {
            			facilityId = facilityParty.getString("facilityId");
            		}
            	}
        		createUnMap = delegator.makeValue("UnmappedItem", UtilMisc.toMap("itemId", productId,
        				"productName", productName,"description", productName, "quantity", new Double("1"), "mrp", new Double(mrp),
            			"sp", new Double(sp),"cp", new Double(sp), "ean", barcode, "facilityId", facilityId));
        		createUnMap.create();
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
            	
				Map<String, Object> createSyncProduct = dispatcher.runSync("createSyncProduct", UtilMisc.toMap("productId", productId,
            												"userLogin", userLogin));
        	}
        	result.put("productId", productId);
            return result;
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in creating Global Product " + module);
        }
    }
	
	public static Map<String, Object> posmartCashReport(DispatchContext dispatchContext, Map<String, ? extends Object> context) throws ParseException {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            Map<String,Object> cartCashMap = new HashMap<String, Object>();
            
            String posTerminalId = (String) context.get("posTerminalId");
            String productStoreId = (String) context.get("productStoreId");
            String fromDate = (String) context.get("fromDate");
            String thruDate = (String) context.get("thruDate");
            
            String facilityId = "", customerName = "", customerMobileNo = "";
            int orderQty = 0;
            BigDecimal orderAmtTotal = BigDecimal.ZERO;
            BigDecimal totalAmtPaid = BigDecimal.ZERO;
            BigDecimal totalAmtPaidByCash = BigDecimal.ZERO;
            BigDecimal totalAmtPaidByCard = BigDecimal.ZERO;
            BigDecimal totalAmtPaidByCreditnote = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidByCredit = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidByGiftcoupon = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidBySodexo = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidByPaytm = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidByUpi = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidByBhim = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidByTicket = BigDecimal.ZERO;
        	BigDecimal totalAmtPaidByPhonepe = BigDecimal.ZERO;
            List<String> orderIds = new ArrayList<String>();
            List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
            List<GenericValue> posCartPaymentList = new ArrayList<GenericValue>();
            List<GenericValue> posCustomerList = new ArrayList<GenericValue>();
            List<String> receiptIds = new ArrayList<String>();
            List<Map<String, Object>> posCartSummaryList = new ArrayList<Map<String, Object>>();
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            GenericValue posCartTxnList = new GenericValue();
            //Code for getting payments
            if(UtilValidate.isNotEmpty(posTerminalId)) {
            	andExprs.add(EntityCondition.makeCondition("terminalId",EntityOperator.EQUALS,posTerminalId));
            }
            if(UtilValidate.isNotEmpty(productStoreId)) {
            	//get store terminals
            	List<GenericValue> storeFacilityList = EntityQuery.use(delegator).from("ProductStoreFacility").
            			where("productStoreId",productStoreId).queryList();
            	if(UtilValidate.isNotEmpty(storeFacilityList)) {
            		facilityId = storeFacilityList.get(0).getString("facilityId");
            	}
            	List<String> posTerminals = EntityQuery.use(delegator).from("PosTerminal").where("facilityId",facilityId).orderBy("posTerminalId").
            			distinct(true).getFieldList("posTerminalId");
            	andExprs.add(EntityCondition.makeCondition("terminalId",EntityOperator.IN,posTerminals));
            }
            if(UtilValidate.isNotEmpty(fromDate)) {
            	//convert to timestamp
            	Timestamp startDate = convertStringToTimestamp(fromDate);
            	andExprs.add(EntityCondition.makeCondition("createdStamp",EntityOperator.GREATER_THAN_EQUAL_TO,startDate));
            }
            if(UtilValidate.isNotEmpty(thruDate)) {
            	//convert to timestamp
            	Timestamp endDate = convertStringToTimestamp(thruDate);
            	endDate = UtilDateTime.getDayEnd(endDate);
            	andExprs.add(EntityCondition.makeCondition("createdStamp",EntityOperator.LESS_THAN_EQUAL_TO,endDate));
            }
            andExprs.add(EntityCondition.makeCondition("posStatus",EntityOperator.EQUALS,"COMPLETE"));
            EntityCondition posTrxCond = EntityCondition.makeCondition(andExprs,EntityOperator.AND);
            
            receiptIds = EntityQuery.use(delegator).from("PosCartTransaction").where(posTrxCond).orderBy("receiptId").
        			distinct(true).getFieldList("receiptId");
            		
            EntityCondition posPaymentCond = EntityCondition.makeCondition("receiptId",EntityOperator.IN,receiptIds);
            posCartPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").where(posPaymentCond).orderBy("-createdStamp").queryList();
            if (UtilValidate.isNotEmpty(posCartPaymentList)) {
            	for (GenericValue cartPaymentrow : posCartPaymentList) {
            		Map<String,Object> cartCashDetailsMap = new HashMap<String, Object>();
            		totalAmtPaid = totalAmtPaid.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		if(cartPaymentrow.getString("paymentType").equals("CASH")) {
            			totalAmtPaidByCash = totalAmtPaidByCash.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		} else if (cartPaymentrow.getString("paymentType").equals("CARD")){
            			totalAmtPaidByCard = totalAmtPaidByCard.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("CREDIT_NOTE")){
            			totalAmtPaidByCreditnote = totalAmtPaidByCreditnote.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("CREDIT")) {
            			totalAmtPaidByCredit = totalAmtPaidByCredit.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("GIFT_COUPON")) {
            			totalAmtPaidByGiftcoupon = totalAmtPaidByGiftcoupon.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("SODEXO")){
            			 totalAmtPaidBySodexo = totalAmtPaidBySodexo.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("PAYTM")){
            			totalAmtPaidByPaytm = totalAmtPaidByPaytm.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("UPI")) {
            			totalAmtPaidByUpi = totalAmtPaidByUpi.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("BHIM")){
            			totalAmtPaidByBhim = totalAmtPaidByBhim.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("TICKET"))  {
            			totalAmtPaidByTicket = totalAmtPaidByTicket.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}
            		else if(cartPaymentrow.getString("paymentType").equals("PHONE_PE"))  {
            			totalAmtPaidByPhonepe = totalAmtPaidByPhonepe.add(cartPaymentrow.getBigDecimal("receivedPayment"));
            		}

            		cartCashDetailsMap.put("receiptId", cartPaymentrow.getString("receiptId"));
            		cartCashDetailsMap.put("createdStamp", cartPaymentrow.getString("createdStamp"));
            		
            		cartCashMap.put("totalAmtPaidByCash", totalAmtPaidByCash);
            		cartCashMap.put("totalAmtPaidByCard", totalAmtPaidByCard);
            		cartCashMap.put("totalAmtPaidByCreditnote", totalAmtPaidByCreditnote);
            		cartCashMap.put("totalAmtPaidByCredit", totalAmtPaidByCredit);
            		cartCashMap.put("totalAmtPaidByGiftcoupon", totalAmtPaidByGiftcoupon);
            		cartCashMap.put("totalAmtPaidBySodexo", totalAmtPaidBySodexo);
            		cartCashMap.put("totalAmtPaidByPaytm",totalAmtPaidByPaytm);
            		cartCashMap.put("totalAmtPaidByUpi", totalAmtPaidByUpi);
            		cartCashMap.put("totalAmtPaidByBhim", totalAmtPaidByBhim);
            		cartCashMap.put("totalAmtPaidByTicket",totalAmtPaidByTicket);
            		cartCashMap.put("totalAmtPaidByPhonepe",totalAmtPaidByPhonepe);
            	}
            	orderIds = EntityUtil.getFieldListFromEntityList(posCartPaymentList, "receiptId", true);
            }else {
            	cartCashMap.put("totalAmtPaidByCash", "");
        		cartCashMap.put("totalAmtPaidByCard", "");
        		cartCashMap.put("totalAmtPaidByCreditnote", "");
        		cartCashMap.put("totalAmtPaidByCredit", "");
        		cartCashMap.put("totalAmtPaidByGiftcoupon", "");
        		cartCashMap.put("totalAmtPaidBySodexo", "");
        		cartCashMap.put("totalAmtPaidByPaytm","");
        		cartCashMap.put("totalAmtPaidByUpi", "");
        		cartCashMap.put("totalAmtPaidByBhim", "");
        		cartCashMap.put("totalAmtPaidByTicket","");
        		cartCashMap.put("totalAmtPaidByPhonepe","");
            }
            
            //End of code for getting payments
            //Code for getting #of ordered Qty
            if(UtilValidate.isNotEmpty(orderIds)) {
            	
            	EntityCondition orderCond = EntityCondition.makeCondition("orderId",EntityOperator.IN,orderIds);
            	EntityCondition posTxnCond = EntityCondition.makeCondition("receiptId",EntityOperator.IN,orderIds);
            	//List<GenericValue> orderHeaderList = EntityQuery.use(delegator).from("OrderHeader").where(orderCond).queryList();
            	List<GenericValue> orderHeaderList = EntityQuery.use(delegator).from("PosCartTransaction").where(posTxnCond).queryList();
            	for(GenericValue ohRrow : orderHeaderList) {
            		orderAmtTotal = orderAmtTotal.add(ohRrow.getBigDecimal("totalBillAmount"));
            	}
            	orderQty = orderHeaderList.size();
            	List<GenericValue> orderHeaderItemList = EntityQuery.use(delegator).from("OrderHeaderAndItems").where(orderCond).queryList();
            	for(GenericValue row : orderHeaderItemList) {
            		Map<String, Object> posCartSummary = new HashMap<String, Object>();
            		Date date = formatter.parse(row.getTimestamp("orderDate").toString());
            		String orderDate = formatter.format(date).toString();
            		//String orderDate = new SimpleDateFormat("dd/MM/yyyy").format((Date) row.get("expireDate"));
            		posCartSummary.put("receiptId", row.getString("orderId"));
            		posCartSummary.put("productName", row.getString("itemDescription"));
            		posCartSummary.put("quantity", row.getBigDecimal("quantity"));
            		posCartSummary.put("productPrice", row.getString("unitPrice"));
            		posCartSummary.put("orderDate", row.getTimestamp("orderDate"));
            		posCartSummary.put("orderDate", orderDate);
            		//posCartSummaryList.add(posCartSummary);
            	}
            	//List<GenericValue> ohList = EntityQuery.use(delegator).from("OrderHeader").where(orderCond).queryList();
            	for(String orderId : orderIds) {
            		GenericValue ohRow = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

            		BigDecimal orderItemQty = BigDecimal.ZERO;
                    	BigDecimal orderItemGrandTot = BigDecimal.ZERO;
                    	BigDecimal amtPaidByCash = BigDecimal.ZERO;
                    	BigDecimal amtPaidByCard = BigDecimal.ZERO;
                    	BigDecimal amtPaidByCreditnote = BigDecimal.ZERO;
                    	BigDecimal amtPaidByCredit = BigDecimal.ZERO;
                    	BigDecimal amtPaidByGiftcoupon = BigDecimal.ZERO;
                    	BigDecimal amtPaidBySodexo = BigDecimal.ZERO;
                    	BigDecimal amtPaidByPaytm = BigDecimal.ZERO;
                    	BigDecimal amtPaidByUpi = BigDecimal.ZERO;
                    	BigDecimal amtPaidByTicket = BigDecimal.ZERO;
                    	BigDecimal amtPaidByBhim = BigDecimal.ZERO;
                    	BigDecimal amtPaidByPhonepe = BigDecimal.ZERO;
                    	
            		Map<String, Object> posCartSummary = new HashMap<String, Object>();
            		Date date = formatter.parse(ohRow.getTimestamp("orderDate").toString());
            		String orderDate = formatter.format(date).toString();
            		//orderQty = orderQty.add(row.getBigDecimal("quantity"));
            		posCartSummary.put("receiptId", ohRow.getString("orderId"));
            		posCartSummary.put("orderDate", orderDate);
            		
            		orderHeaderItemList = EntityQuery.use(delegator).from("OrderHeaderAndItems").where("orderId", ohRow.getString("orderId")).
                			queryList();
            		posCartTxnList = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", ohRow.getString("orderId")).
                			queryOne();
            		for(GenericValue orderHeaderItemRow : orderHeaderItemList) {
            			orderItemQty = orderItemQty.add(orderHeaderItemRow.getBigDecimal("quantity"));
            			//orderItemGrandTot = orderItemGrandTot.add(orderHeaderItemRow.getBigDecimal("grandTotal"));
            		}
            		//orderItemGrandTot = orderHeaderItemList.get(0).getBigDecimal("grandTotal");
            		orderItemGrandTot = posCartTxnList.getBigDecimal("totalBillAmount");
            		posCartPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").where("receiptId", ohRow.getString("orderId")).
            				queryList();
            		for(GenericValue paymentRow : posCartPaymentList) {
            		  	if(paymentRow.getString("paymentType").equals("CASH")) {
            				amtPaidByCash = amtPaidByCash.add(paymentRow.getBigDecimal("receivedPayment"));
                		} else if(paymentRow.getString("paymentType").equals("CARD")) {
                			amtPaidByCard = amtPaidByCard.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if (paymentRow.getString("paymentType").equals("CREDIT_NOTE")){
                			amtPaidByCreditnote = amtPaidByCreditnote.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if (paymentRow.getString("paymentType").equals("CREDIT")) {
                			amtPaidByCredit = amtPaidByCredit.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if(paymentRow.getString("paymentType").equals("GIFT_COUPON")) {
                			amtPaidByGiftcoupon = amtPaidByGiftcoupon.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if (paymentRow.getString("paymentType").equals("SODEXO")){
                			amtPaidBySodexo = amtPaidBySodexo.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if (paymentRow.getString("paymentType").equals("PAYTM")){
                			amtPaidByPaytm = amtPaidByPaytm.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if(paymentRow.getString("paymentType").equals("UPI")) {
                			amtPaidByUpi = amtPaidByUpi.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if (paymentRow.getString("paymentType").equals("BHIM")){
                			amtPaidByBhim = amtPaidByBhim.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if(paymentRow.getString("paymentType").equals("TICKET"))  {
                			amtPaidByTicket = amtPaidByTicket.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
                		else if(paymentRow.getString("paymentType").equals("PHONE_PE"))  {
                			amtPaidByPhonepe = amtPaidByPhonepe.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
            		}

            		customerMobileNo = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId",ohRow.getString("orderId")).
            				queryOne().getString("customerMobileNo");
            		posCustomerList = EntityQuery.use(delegator).from("PartyAndTelecomNumberAndPerson").where("contactNumber",
            				customerMobileNo, "roleTypeId", "CUSTOMER").queryList();
            		if(UtilValidate.isNotEmpty(posCustomerList)) {
            			customerName = posCustomerList.get(0).getString("firstName");
            		}else {
            			customerName = "";
            		}
            		posCartSummary.put("totalSales", orderItemQty.intValue());
            		posCartSummary.put("orderItemAmt", orderItemGrandTot);
            		posCartSummary.put("amtPaidByCash", amtPaidByCash);
            		posCartSummary.put("amtPaidByCard", amtPaidByCard);
            		posCartSummary.put("amtPaidByCreditnote", amtPaidByCreditnote);
            		posCartSummary.put("amtPaidByCredit", amtPaidByCredit);
            		posCartSummary.put("amtPaidByGiftcoupon",amtPaidByGiftcoupon);
            		posCartSummary.put("amtPaidBySodexo", amtPaidBySodexo);
            		posCartSummary.put("amtPaidByPaytm", amtPaidByPaytm);
            		posCartSummary.put("amtPaidByUpi", amtPaidByUpi);
            		posCartSummary.put("amtPaidByBhim", amtPaidByBhim);
            		posCartSummary.put("amtPaidByTicket",amtPaidByTicket);
            		posCartSummary.put("amtPaidByPhonepe", amtPaidByPhonepe);
            		posCartSummary.put("customerName", customerName);
            		posCartSummaryList.add(posCartSummary);
            	}
            	cartCashMap.put("orderQty", orderQty);
            	cartCashMap.put("orderAmtTotal", orderAmtTotal);
            } else {
            	cartCashMap.put("orderQty", "");
            	cartCashMap.put("orderAmtTotal", "");
            }
            //End of code for getting #of ordered qty

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("posCartSummaryList", posCartSummaryList);
            result.put("posCartSummary", cartCashMap);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in getting POSTerminal in " + module);
        }
    }
	public static Map<String, Object> getPosmartSummaryCashReport(DispatchContext dispatchContext, Map<String, ? extends Object> context) throws ParseException {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            
            String billId = (String) context.get("billId");
            String customerName = (String) context.get("customerName");
            String customerMobileNo = (String) context.get("customerMobileNo");
            
            List<String> orderIds = new ArrayList<String>();
            List<String> contactNumbers = new ArrayList<String>();
            List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
            List<GenericValue> posCartPaymentList = new ArrayList<GenericValue>();
            List<GenericValue> posCustomerList = new ArrayList<GenericValue>();
            List<Map<String, Object>> posCartSummaryList = new ArrayList<Map<String, Object>>();
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            List<GenericValue> orderHeaderItemList = new LinkedList<GenericValue>();
            GenericValue ohRow = new GenericValue();
            //Code for getting #of ordered Qty
            if(UtilValidate.isNotEmpty(billId)) {
            	andExprs.add(EntityCondition.makeCondition("receiptId",EntityOperator.EQUALS,billId));
            }
            if(UtilValidate.isNotEmpty(customerName)) {
            	contactNumbers = EntityQuery.use(delegator).from("PartyAndTelecomNumberAndPerson").where(
            			EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), 
        						EntityOperator.LIKE, EntityFunction.UPPER("%" + customerName + "%"))).getFieldList("contactNumber");
            	
            	andExprs.add(EntityCondition.makeCondition("customerMobileNo",EntityOperator.IN,contactNumbers));
            }
            if(UtilValidate.isNotEmpty(customerMobileNo)) {
            	andExprs.add(EntityCondition.makeCondition("customerMobileNo",EntityOperator.EQUALS,customerMobileNo));
            }
            
            andExprs.add(EntityCondition.makeCondition("posStatus",EntityOperator.EQUALS,"COMPLETE"));
            EntityCondition posTrxCond = EntityCondition.makeCondition(andExprs,EntityOperator.AND);
            orderIds = EntityQuery.use(delegator).from("PosCartTransaction").where(posTrxCond).orderBy("receiptId").
        			distinct(true).getFieldList("receiptId");

	    EntityCondition posPaymentCond = EntityCondition.makeCondition("receiptId",EntityOperator.IN,orderIds);
            posCartPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").where(posPaymentCond).queryList();
            if (UtilValidate.isNotEmpty(posCartPaymentList)) {
             orderIds = EntityUtil.getFieldListFromEntityList(posCartPaymentList, "receiptId", true);
            }

            if(UtilValidate.isNotEmpty(orderIds)) {
            	//get sorted order list
            	List<String> sortOrderList = sortedList(orderIds);
            	
            	EntityCondition orderCond = EntityCondition.makeCondition("orderId",EntityOperator.IN,orderIds);
            	/*List<GenericValue> ohList = EntityQuery.use(delegator).from("OrderHeader").where(orderCond).
            			orderBy("-orderDate").queryList();*/
            	for(String orderId : sortOrderList) {
            		 ohRow = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
            		 
            		BigDecimal orderItemQty = BigDecimal.ZERO;
                	BigDecimal orderItemGrandTot = BigDecimal.ZERO;
                	BigDecimal amtPaidByCash = BigDecimal.ZERO;
                	BigDecimal amtPaidByCard = BigDecimal.ZERO;
            		Map<String, Object> posCartSummary = new HashMap<String, Object>();
            		Date date = formatter.parse(ohRow.getTimestamp("orderDate").toString());
            		String orderDate = formatter.format(date).toString();
            		posCartSummary.put("receiptId", ohRow.getString("orderId"));
            		posCartSummary.put("orderDate", orderDate);
            		
            		orderHeaderItemList = EntityQuery.use(delegator).from("OrderHeaderAndItems").where("orderId", ohRow.getString("orderId")).
                			orderBy("quantity").queryList();
            		for(GenericValue orderHeaderItemRow : orderHeaderItemList) {
            			orderItemQty = orderItemQty.add(orderHeaderItemRow.getBigDecimal("quantity"));
            			orderItemGrandTot = orderHeaderItemRow.getBigDecimal("grandTotal");
            		}
            		posCartPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").where("receiptId", ohRow.getString("orderId")).
            				queryList();
            		for(GenericValue paymentRow : posCartPaymentList) {
            			if(paymentRow.getString("paymentType").equals("CASH")) {
            				amtPaidByCash = amtPaidByCash.add(paymentRow.getBigDecimal("receivedPayment"));
                		} else {
                			amtPaidByCard = amtPaidByCard.add(paymentRow.getBigDecimal("receivedPayment"));
                		}
            		}
            		customerMobileNo = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId",ohRow.getString("orderId")).
            				orderBy("createdStamp").queryOne().getString("customerMobileNo");
            		posCustomerList = EntityQuery.use(delegator).from("PartyAndTelecomNumberAndPerson").where("contactNumber",
            				customerMobileNo, "roleTypeId", "CUSTOMER").queryList();
            		if(UtilValidate.isNotEmpty(posCustomerList)) {
            			customerName = posCustomerList.get(0).getString("firstName");
            		}
            		posCartSummary.put("totalSales", orderItemQty.intValue());
            		posCartSummary.put("orderItemAmt", orderItemGrandTot);
            		posCartSummary.put("amtPaidByCash", amtPaidByCash);
            		posCartSummary.put("amtPaidByCard", amtPaidByCard);
            		posCartSummary.put("customerName", customerName);
            		posCartSummaryList.add(posCartSummary);
            	}
            }
            //End of code for getting #of ordered qty
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("posCartSummaryList", posCartSummaryList);

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in getting POSTerminal in " + module);
        }
    }
	
	public static Map<String, Object> billSummary(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
	    try {
	        Delegator delegator = dispatchContext.getDelegator();
	        
	        String customerMobileNo = (String) context.get("customerMobileNo");
	        String productStoreId = (String) context.get("productStoreId");
	        String fromDate = (String) context.get("fromDate");
	        String thruDate = (String) context.get("thruDate");
	        
	        String facilityId = "";
	        BigDecimal orderQty = BigDecimal.ZERO;
	        BigDecimal orderAmtTotal = BigDecimal.ZERO;
	        BigDecimal totalAmtPaid = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByCash = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByCard = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByCreditnote = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByCredit = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByGiftcoupon = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidBySodexo = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByPaytm = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByUpi= BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByBhim = BigDecimal.ZERO;	    
	        BigDecimal totalAmtPaidByTicket = BigDecimal.ZERO;
	        BigDecimal totalAmtPaidByPhonepe = BigDecimal.ZERO;


	        List<String> orderIds = new ArrayList<String>();
	        List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
	        List<GenericValue> posCartPaymentList = new ArrayList<GenericValue>();
	        List<String> receiptIds = new ArrayList<String>();
	        List<String> customerMobileNums = new ArrayList<String>();
	        List<Map<String, Object>> posCartSummaryList = new ArrayList<Map<String, Object>>();
	        String customerName = "", receiptId = "";
	        String billCreatedDate = null;
	        boolean checkIssued = false;
	        List<GenericValue> checkBill = new LinkedList<GenericValue>();
	        
	        //Code for getting payments
	        if(UtilValidate.isNotEmpty(customerMobileNo)) {
	        	andExprs.add(EntityCondition.makeCondition("customerMobileNo",EntityOperator.EQUALS,customerMobileNo));
	        }
	        if(UtilValidate.isNotEmpty(productStoreId)) {
	        	//get store terminals
	        	List<GenericValue> storeFacilityList = EntityQuery.use(delegator).from("ProductStoreFacility").
	        			where("productStoreId",productStoreId).queryList();
	        	if(UtilValidate.isNotEmpty(storeFacilityList)) {
	        		facilityId = storeFacilityList.get(0).getString("facilityId");
	        	}
	        	List<String> posTerminals = EntityQuery.use(delegator).from("PosTerminal").where("facilityId",facilityId).orderBy("posTerminalId").
	        			distinct(true).getFieldList("posTerminalId");
	        	andExprs.add(EntityCondition.makeCondition("terminalId",EntityOperator.IN,posTerminals));
	        }
	        if(UtilValidate.isNotEmpty(fromDate)) {
	        	//convert to timestamp
	        	Timestamp startDate = convertStringToTimestamp(fromDate);
	        	andExprs.add(EntityCondition.makeCondition("createdStamp",EntityOperator.GREATER_THAN_EQUAL_TO,startDate));
	        }
	        if(UtilValidate.isNotEmpty(thruDate)) {
	        	//convert to timestamp
	        	Timestamp endDate = convertStringToTimestamp(thruDate);
	        	andExprs.add(EntityCondition.makeCondition("createdStamp",EntityOperator.LESS_THAN_EQUAL_TO,endDate));
	        }
	        andExprs.add(EntityCondition.makeCondition("posStatus",EntityOperator.EQUALS,"COMPLETE"));
	        EntityCondition posTrxCond = EntityCondition.makeCondition(andExprs,EntityOperator.AND);
	        
	        List<GenericValue> customerTxns = EntityQuery.use(delegator).from("PosCartTransaction").where(posTrxCond).orderBy("customerMobileNo").
	    			distinct(true).orderBy("-createdStamp").queryList();
	        customerMobileNums = EntityUtil.getFieldListFromEntityList(customerTxns, "customerMobileNo", true);
	        //get distinct customer trasaction list
	        for (String customerMobileNum : customerMobileNums) {
	        	//get customername
	        	GenericValue customerVal = EntityQuery.use(delegator).from("PartyAndTelecomNumberAndPerson").where("contactNumber", customerMobileNum)
	        			.orderBy("-createdDate").queryFirst();
	        	if(UtilValidate.isNotEmpty(customerVal)) {
	        		if(UtilValidate.isNotEmpty(customerVal.getString("firstName"))) {
	        			customerName = customerVal.getString("firstName");
	        		}
	        		if(UtilValidate.isNotEmpty(customerVal.getString("lastName"))) {
	        			customerName = customerName +" "+ customerVal.getString("lastName");
	        		}
	        	}
				
	        	List<GenericValue> posCartTransactionList = EntityQuery.use(delegator).from("PosCartTransaction").
	        			where("customerMobileNo", customerMobileNum,"posStatus", "COMPLETE").orderBy("-createdStamp").queryList();
	        	if(UtilValidate.isNotEmpty(posCartTransactionList)) {
	        		for (GenericValue posCartTransaction : posCartTransactionList) {
	        			totalAmtPaid = BigDecimal.ZERO;
	                	totalAmtPaidByCash = BigDecimal.ZERO;
	        	        totalAmtPaidByCard = BigDecimal.ZERO;
	        			Map<String,Object> cartCashMap = new HashMap<String, Object>();
	        			cartCashMap.put("customerName", customerName);
	        			cartCashMap.put("customerMobileNo", customerMobileNum);
	        			
	        			receiptId = posCartTransaction.getString("receiptId");
	        			cartCashMap.put("receiptId", receiptId);
	        			//check for bill already credit issued or not
	        			checkBill = EntityQuery.use(delegator).from("PosCredit").where("billId", receiptId,"type", 
	        					"CREDIT_NOTE","retailer", Long.parseLong("1"),"customer", Long.parseLong("0")).queryList();
	        			if(UtilValidate.isNotEmpty(checkBill)) {
	        				checkIssued = true;
	        			}
	        			billCreatedDate = posCartTransaction.getString("createdStamp");
	        			orderAmtTotal = posCartTransaction.getBigDecimal("totalBillAmount");
	        			
	        			//get totalAmtPaidByCash & totalAmtPaidByCard
	        			posCartPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").where("receiptId", receiptId).queryList();
	        			if (UtilValidate.isNotEmpty(posCartPaymentList)) {
	        	        	for (GenericValue cartPaymentrow : posCartPaymentList) {
	        	        		
	        	        		totalAmtPaid = totalAmtPaid.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		if(cartPaymentrow.getString("paymentType").equals("CASH")) {
	        	        			totalAmtPaidByCash = totalAmtPaidByCash.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		} else if(cartPaymentrow.getString("paymentType").equals("CARD")){
	        	        			totalAmtPaidByCard = totalAmtPaidByCard.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("CREDIT_NOTE")){
	        	        			totalAmtPaidByCreditnote = totalAmtPaidByCreditnote.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("CREDIT")){
	        	        			totalAmtPaidByCredit = totalAmtPaidByCredit.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("GIFT_COUPON")){
	        	        			totalAmtPaidByGiftcoupon = totalAmtPaidByGiftcoupon.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("SODEXO")){
	        	        			totalAmtPaidBySodexo = totalAmtPaidBySodexo.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("PAYTM")){
	        	        			totalAmtPaidByPaytm = totalAmtPaidByPaytm.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("UPI")){
	        	        			totalAmtPaidByUpi = totalAmtPaidByUpi.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("BHIM")){
	        	        			totalAmtPaidByBhim = totalAmtPaidByBhim.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("TICKET")){
	        	        			totalAmtPaidByTicket = totalAmtPaidByTicket.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        		else if(cartPaymentrow.getString("paymentType").equals("PHONE_PE")){
	        	        			totalAmtPaidByPhonepe = totalAmtPaidByPhonepe.add(cartPaymentrow.getBigDecimal("receivedPayment"));
	        	        		}
	        	        	
	        	        	}
	        	        	orderIds = EntityUtil.getFieldListFromEntityList(posCartPaymentList, "receiptId", true);
	        	        }else {
	        	        	cartCashMap.put("totalAmtPaidByCash", "");
	        	    		cartCashMap.put("totalAmtPaidByCard", "");
	        	    		cartCashMap.put("totalAmtPaidByCreditnote", "");
	        	    		cartCashMap.put("totalAmtPaidByCredit", "");
	        	    		cartCashMap.put("totalAmtPaidByGiftcoupon", "");
	        	    		cartCashMap.put("totalAmtPaidBySodexo", "");
	        	    		cartCashMap.put("totalAmtPaidByPaytm", "");
	        	    		cartCashMap.put("totalAmtPaidByUpi", "");
	        	    		cartCashMap.put("totalAmtPaidByBhim", "");
	        	    		cartCashMap.put("totalAmtPaidByTicket", "");
	        	    		cartCashMap.put("totalAmtPaidByPhonepe", "");

	        	        }
	        			
	        			cartCashMap.put("billQty", customerMobileNum);
	        			cartCashMap.put("totalAmtPaidByCash", totalAmtPaidByCash);
	            		cartCashMap.put("totalAmtPaidByCard", totalAmtPaidByCard);
	            		cartCashMap.put("billDate", convertStringToTmstp(billCreatedDate));
	        			cartCashMap.put("billTotalAmt", orderAmtTotal);
	        			cartCashMap.put("checkIssued", checkIssued);
	        			posCartSummaryList.add(cartCashMap);
	        			checkIssued = false;
	        		}
	        	}
	        	
	        }
	        Map<String, Object> result = ServiceUtil.returnSuccess();
	        result.put("posCartSummaryList", posCartSummaryList);

	        return result;
	    } catch (GenericEntityException genericEntityException) {
	        Debug.logError(genericEntityException, module);
	        return ServiceUtil.returnError("Error in getting POSTerminal in " + module);
	    }
	}
	public static Map<String, Object> getBillItems (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
	        
		Delegator delegator = dispatchContext.getDelegator();
	        LocalDispatcher dispatcher = dispatchContext.getDispatcher();
	        
	        String receiptId = (String) context.get("receiptId");
	        String productStoreGroupId = (String) context.get("productStoreGroupId");
	        String currencyUomId = (String) context.get("currencyUomId");
	        
	        String productId = "", productName = "", customerId = "", customerMobileNo = "";
	        BigDecimal qty = BigDecimal.ZERO;
	        BigDecimal mrp = BigDecimal.ZERO;
		BigDecimal sp = BigDecimal.ZERO;
	        BigDecimal sellingPrice = BigDecimal.ZERO;
	        BigDecimal totalPrice = BigDecimal.ZERO;
	        List<Map<String, Object>> productListMap = new ArrayList<Map<String,Object>>();
	        try {
	        	//code for getting customerId
	        	customerMobileNo = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).orderBy("receiptId").
	        			distinct(true).queryOne().getString("customerMobileNo");
	        	if(UtilValidate.isNotEmpty(customerMobileNo)) {
	        		customerId = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("contactNumber", customerMobileNo).
	        				orderBy("contactNumber").queryFirst().getString("partyId");
	        	}
	        	//Code for getting payments
	            List<GenericValue> posCartItemList = EntityQuery.use(delegator).from("PosCartItem").where("receiptId", receiptId).orderBy("receiptId").
	        			distinct(true).queryList();
	            if(UtilValidate.isNotEmpty(posCartItemList)) {
	            	for (GenericValue posCartItem : posCartItemList) {
	            		Map<String, Object> productMap = new HashMap<String, Object>();
	            		
	            		productId = posCartItem.getString("productId");
	            		qty = new BigDecimal(posCartItem.getString("quantity"));
	            		//get ProductName
	            		GenericValue product = EntityQuery.use(delegator).from("Product").
	        					where("productId", productId).queryFirst();
	        			
	        			if(UtilValidate.isNotEmpty(product)) {
	        				productName = product.getString("productName");
	        			}
	        			//get Product Mrp
	        			Map<String, Object> prdPriceResult = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product,
	                    		"productStoreGroupId", productStoreGroupId, "currencyUomId",currencyUomId));
	        			mrp = new BigDecimal(prdPriceResult.get("listPrice").toString());
					    sp = new BigDecimal(prdPriceResult.get("basePrice").toString());
	        			if(UtilValidate.isNotEmpty(posCartItem.getString("productPrice"))) {
	            			sellingPrice = new BigDecimal(posCartItem.getString("productPrice"));
	            			productMap.put("mrp", mrp.toBigInteger());
	            			productMap.put("sp", sellingPrice.toBigInteger());
	            		}else {
	            			productMap.put("mrp", mrp.toBigInteger());
	            			productMap.put("sp", sp.toBigInteger());
	            		}
	        			if(sellingPrice.compareTo(BigDecimal.ZERO) == 0) {
	        				totalPrice = qty.multiply(sp);//always take sp
	        			}else {
	        				totalPrice = qty.multiply(sellingPrice);
	        			}
	        			totalPrice = totalPrice.setScale(0, BigDecimal.ROUND_HALF_UP);
	        			totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
	        			
	        			productMap.put("dayId", posCartItem.getString("dayId"));
	        			productMap.put("totalQty", qty.toBigInteger());
	        			productMap.put("quantity", qty.toBigInteger());
	        			productMap.put("productId", productId);
	        			productMap.put("productName", productName);
	        			productMap.put("totalPrice", totalPrice.toBigInteger());
	        			
	        			productListMap.add(productMap);
	                }
	            }
	        } catch (Exception e) {
				// TODO: handle exception
			}
	        
	        Map<String, Object> result = ServiceUtil.returnSuccess();
	        result.put("productListMap", productListMap);
	        result.put("customerId", customerId);

	        return result;
	}
	public static Map<String, Object> enableTaxInvoice(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            String isChecked = (String) context.get("isChecked");
            String receiptId = (String) context.get("receiptId");
            
            GenericValue posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
            if(UtilValidate.isNotEmpty(posCartTxn)) {
            	posCartTxn.put("withTax", isChecked);
            	posCartTxn.store();
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("Success", "Success");

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in updating DayDetail in " + module);
        }
    }
	public static Map<String, Object> getTaxInvoice(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
        String isChecked = "";
        
        try {
            String receiptId = (String) context.get("receiptId");
            
            GenericValue posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
            if(UtilValidate.isNotEmpty(posCartTxn)) {
            	isChecked = posCartTxn.getString("withTax");
            }

        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in updating DayDetail in " + module);
        }
        
        result.put("isChecked", isChecked);
        return result;
    }
	
	public static Map<String, Object> updateCartTxn(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();
            String receiptId = (String) context.get("receiptId");
            String addl_discount = (String) context.get("addl_discount");
            String isDiscountPer = (String) context.get("isDiscountPer");
            String charges = (String) context.get("charges");
            String isChargePer = (String) context.get("isChargePer");
            String billAmount = (String) context.get("billAmount");
            BigDecimal addlCharges = BigDecimal.ZERO;
            BigDecimal addlDiscount = BigDecimal.ZERO;
            BigDecimal totBillAmt = BigDecimal.ZERO;
            GenericValue posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
            if(UtilValidate.isNotEmpty(posCartTxn)) {
            	if(UtilValidate.isNotEmpty(addl_discount)) {
            		addlDiscount = new BigDecimal(addl_discount);
            	}
            	if(UtilValidate.isNotEmpty(charges)) {
            		addlCharges = new BigDecimal(charges);
            	}
            	if(UtilValidate.isNotEmpty(totBillAmt)) {
            		totBillAmt = new BigDecimal(billAmount);
            	}
            	
            	posCartTxn.put("totalBillAmount", totBillAmt);
            	posCartTxn.put("balanceAmount", totBillAmt);
            	posCartTxn.put("discount", addlDiscount);
            	posCartTxn.put("isPercentage", isDiscountPer);
            	posCartTxn.put("charges", addlCharges);
            	posCartTxn.put("isChargePercentage", isChargePer);
            	posCartTxn.store();
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("Success", "Success");

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in updating Cart Transaction " + module);
        }
    }
	public static Map<String, Object> getAdditionalCartTxn(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		Delegator delegator = dispatchContext.getDelegator();
		        Map<String, Object> result = ServiceUtil.returnSuccess();
		       
		        String discount = "0",isPercentage = "N";
		        String charges = "0",isChargePercentage = "N";
		        String discountPer = "";
		        BigDecimal addlDisc = BigDecimal.ZERO;
		        BigDecimal addlCharges = BigDecimal.ZERO;
		        BigDecimal billAmt = BigDecimal.ZERO;
		        BigDecimal totBillAmt = BigDecimal.ZERO;
		        BigDecimal percentage = BigDecimal.ZERO;
		        BigDecimal chargePercentage = BigDecimal.ZERO;
		        try {
		            String receiptId = (String) context.get("receiptId");
		            String billAmount = (String) context.get("billAmount");
		            
		            GenericValue posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
		            if(UtilValidate.isNotEmpty(posCartTxn)) {
		            	//billAmt = posCartTxn.getBigDecimal("totalBillAmount");
		            	billAmt = new BigDecimal(billAmount);
			            if(UtilValidate.isNotEmpty(posCartTxn.getBigDecimal("discount"))) {
			            	addlDisc = posCartTxn.getBigDecimal("discount");
			            	//addlDisc = addlDisc.setScale(0, BigDecimal.ROUND_HALF_UP);
			            	//addlDisc = addlDisc.setScale(2, BigDecimal.ROUND_HALF_UP);
			            	discount = addlDisc.toString();
			            }
			            if(UtilValidate.isNotEmpty(posCartTxn.getBigDecimal("charges"))) {
			            	addlCharges = posCartTxn.getBigDecimal("charges");
			            	charges = addlCharges.toBigInteger().toString();
			            }
			            if(UtilValidate.isNotEmpty(posCartTxn.getString("isPercentage"))) {
			            	isPercentage = posCartTxn.getString("isPercentage");
			            }
			            if(UtilValidate.isNotEmpty(posCartTxn.getString("isChargePercentage"))) {
			            	isChargePercentage = posCartTxn.getString("isChargePercentage");
			            }
			            //calculate disc per
			            //billAmt = billAmt.add(addlDisc);
			            if(UtilValidate.isNotEmpty(addlDisc)) {
				            if(isPercentage.equals("Y")) {
				            	percentage = addlDisc.divide(billAmt).multiply(new BigDecimal("100"));
				            	percentage = percentage.setScale(0, BigDecimal.ROUND_HALF_UP);
				            	percentage = percentage.setScale(2, BigDecimal.ROUND_HALF_UP);
				            }
			            }
			            //end of calculating disc per
			            //calculate charges per
			            if(UtilValidate.isNotEmpty(addlCharges)) {
				            if(isChargePercentage.equals("Y")) {
				            	chargePercentage = addlCharges.divide(billAmt).multiply(new BigDecimal("100"));
				            	chargePercentage = chargePercentage.setScale(0, BigDecimal.ROUND_HALF_UP);
				            	chargePercentage = chargePercentage.setScale(2, BigDecimal.ROUND_HALF_UP);
				            }
			            }
			            //end of calculating charges per
		            }

		        } catch (GenericEntityException genericEntityException) {
		            Debug.logError(genericEntityException, module);
		            return ServiceUtil.returnError("Error in updating DayDetail in " + module);
		        }
		       
		        result.put("discount", discount);
		        result.put("discountPer", percentage.toString());
		        result.put("isPercentage", isPercentage);
		        result.put("charges", charges);
		        result.put("chargePer", chargePercentage.toString());
		        result.put("isChargePercentage", isChargePercentage);
		        return result;
		    }
	
	public static Map<String, Object> customerReport (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
	    
		Delegator delegator = dispatchContext.getDelegator();
	        
	        String customerId = (String) context.get("customerId");
	        String productStoreGroupId = (String) context.get("productStoreGroupId");
	        String currencyUomId = (String) context.get("currencyUomId");
	        
	        String customerMobileNo = "";
	        String txnCustomerId = "", customerName = "";
	        BigDecimal totCustomerCreditDue = BigDecimal.ZERO;
	        BigDecimal totCustomerPaidAmt = BigDecimal.ZERO;
	        BigDecimal totCustomerCreditAmt = BigDecimal.ZERO;
	        List<String> orderIds = new ArrayList<String>();
	        List<String> customerIds = new ArrayList<String>();
	        List<String> contactNumbers = new ArrayList<String>();
	        
	        List<GenericValue> posCartPaymentList = new ArrayList<GenericValue>();
	        List<Map<String, Object>> posCartCustomerList = new ArrayList<Map<String,Object>>();
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            List<GenericValue> orderHeaderItemList = new LinkedList<GenericValue>();
            List<GenericValue> customerList = new LinkedList<GenericValue>();
            GenericValue ohRow = new GenericValue();
            List<String> partyIds = new ArrayList<String>();
            List<String> contactNumberIds = new ArrayList<String>();
	        try {
	        	//get Distinct customers
	        	EntityCondition partyCond = EntityCondition.makeCondition("contactNumber", EntityOperator.NOT_EQUAL, null);
	        	
	        	customerList = EntityQuery.use(delegator).select("partyId","contactNumber").from("PartyAndTelecomNumberAndPerson")
	        						.where(partyCond).distinct(true).queryList();
	        	if(UtilValidate.isNotEmpty(customerList)) {
	        		for(GenericValue row : customerList) {
	        			if(!(contactNumberIds.contains(row.getString("contactNumber")))) {
	        				partyIds.add(row.getString("partyId"));
	        				contactNumberIds.add(row.getString("contactNumber"));
	        			}
	        		}
	        	}
	        	EntityCondition partyRoleCond = EntityCondition.makeCondition(EntityOperator.AND,
	        			EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "CUSTOMER"),
	        			EntityCondition.makeCondition("partyId", EntityOperator.IN, partyIds));
	        	
	        	customerIds = EntityQuery.use(delegator).from("PartyRole").where(partyRoleCond).distinct(true).getFieldList("partyId");
	        			
	        	for (String customerIdEach : customerIds) {
	        		List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
	        		Map<String, Object> customerMap = new HashMap<String, Object>();
	        		Map<String, Object> posCartSummary = new HashMap<String, Object>();
		            BigDecimal orderItemQty = BigDecimal.ZERO;
	             	BigDecimal orderItemGrandTot = BigDecimal.ZERO;
	             	BigDecimal amtPaidByCash = BigDecimal.ZERO;
	             	BigDecimal amtPaidByCard = BigDecimal.ZERO;
	             	
	        		//get customer Name
	        		customerName = getCustomerName(delegator, customerIdEach);
	        		customerMobileNo = getCustomerMobileNumById(delegator, customerIdEach);
	        		
	        		posCartSummary.put("customerName", customerName);
	        		posCartSummary.put("customerMobileNo", customerMobileNo);
	        		
                	contactNumbers = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("partyId", customerIdEach).
                			getFieldList("contactNumber");
                	
                	andExprs.add(EntityCondition.makeCondition("customerMobileNo",EntityOperator.IN,contactNumbers));
	                
	        		EntityCondition posTrxCond = EntityCondition.makeCondition(andExprs,EntityOperator.AND);
	                orderIds = EntityQuery.use(delegator).from("PosCartTransaction").where(posTrxCond).orderBy("receiptId").
	            			distinct(true).getFieldList("receiptId");
	                EntityCondition posPaymentCond = EntityCondition.makeCondition("receiptId",EntityOperator.IN,orderIds);
	                posCartPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").where(posPaymentCond).queryList();
	                if (UtilValidate.isNotEmpty(posCartPaymentList)) {
	                	orderIds = EntityUtil.getFieldListFromEntityList(posCartPaymentList, "receiptId", true);
	                }
	                if(UtilValidate.isNotEmpty(orderIds)) {
	                	//get sorted order list
	                	List<String> sortOrderList = sortedList(orderIds);
	                	EntityCondition orderCond = EntityCondition.makeCondition("orderId",EntityOperator.IN,orderIds);
	                	
	                	for(String orderId : sortOrderList) {
	                		ohRow = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();
	                		if(UtilValidate.isNotEmpty(ohRow)) {
	                			Date date = formatter.parse(ohRow.getTimestamp("orderDate").toString());
		                		String orderDate = formatter.format(date).toString();
		                		posCartSummary.put("receiptId", ohRow.getString("orderId"));
		                		posCartSummary.put("orderDate", orderDate);
		                		
		                		orderHeaderItemList = EntityQuery.use(delegator).from("OrderHeaderAndItems").where("orderId", ohRow.getString("orderId")).
		                    			orderBy("quantity").queryList();
		                		for(GenericValue orderHeaderItemRow : orderHeaderItemList) {
		                			orderItemQty = orderItemQty.add(orderHeaderItemRow.getBigDecimal("quantity"));
		                			orderItemGrandTot = orderHeaderItemRow.getBigDecimal("grandTotal");
		                		}
		                		posCartPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").where("receiptId", ohRow.getString("orderId")).
		                				queryList();
		                		for(GenericValue paymentRow : posCartPaymentList) {
		                			if(paymentRow.getString("paymentType").equals("CASH")) {
		                				amtPaidByCash = amtPaidByCash.add(paymentRow.getBigDecimal("receivedPayment"));
		                    		} else {
		                    			amtPaidByCard = amtPaidByCard.add(paymentRow.getBigDecimal("receivedPayment"));
		                    		}
		                		}
	                		}
	                	}
	                }
	                posCartSummary.put("totalSales", orderItemQty.intValue());
	        		posCartSummary.put("orderItemAmt", orderItemGrandTot);
	        		posCartSummary.put("amtPaidByCash", amtPaidByCash);
	        		posCartSummary.put("amtPaidByCard", amtPaidByCard);
	            	posCartCustomerList.add(posCartSummary);
	        	}
	        	
	        } catch (Exception e) {
				// TODO: handle exception
			}
	        
	        Map<String, Object> result = ServiceUtil.returnSuccess();
	        result.put("posCartCustomerList", posCartCustomerList);
	        return result;
	}
	public static Map<String, Object> createUpdateCustomer (DispatchContext dispatchContext, Map<String, ? extends Object> context) {
		
		GenericValue userLogin = (GenericValue) context.get("userLogin");
		String contactNumber = (String) context.get("contactNumber");
		String firstName = (String) context.get("firstName");
		String lastName = (String) context.get("lastName");
		String emailAddress = (String) context.get("emailAddress");
		String address1 = (String) context.get("address1");
		String city = (String) context.get("city");
		String postalCode = (String) context.get("postalCode");
		String stateProvinceGeoId = (String) context.get("stateProvinceGeoId");
		String productStoreId = (String) context.get("productStoreId");
		
		String partyId = "", contactMechId = "";
		
		Delegator delegator = dispatchContext.getDelegator();
		LocalDispatcher dispatcher = dispatchContext.getDispatcher();
		Map<String, Object> result = ServiceUtil.returnSuccess();
		List<GenericValue> partyTelecom = new ArrayList<GenericValue>();
        try {
            //check contactNumber exists or not
        	partyTelecom = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("contactNumber", contactNumber).queryList();
        	if (UtilValidate.isNotEmpty(partyTelecom)) {
        		partyId = partyTelecom.get(0).getString("partyId");
        		//if exists update customer
        		//update Person
        		GenericValue person = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
        		if(UtilValidate.isNotEmpty(person)) {
        			person.put("firstName", firstName);
        			person.store();
        		}
        		//update email
        		GenericValue personList = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
        		if(UtilValidate.isNotEmpty(personList)) {
        			personList.put("firstName", firstName);
        			personList.store();
        		}
        		//update PostalAddress
        		if(UtilValidate.isNotEmpty(address1)) {
        			List<GenericValue> PartyAndPostalAddress = EntityQuery.use(delegator).from("PartyAndPostalAddress").
        					where("partyId", partyId).queryList();
        			if(UtilValidate.isNotEmpty(PartyAndPostalAddress)) {
            			for (GenericValue row : PartyAndPostalAddress) {
            				contactMechId = row.getString("contactMechId");
            				GenericValue postalAddress = EntityQuery.use(delegator).from("PostalAddress").
            						where("contactMechId", contactMechId).queryOne();
            				if(UtilValidate.isNotEmpty(postalAddress)) {
            					if(UtilValidate.isNotEmpty(address1)) {
            						postalAddress.put("address1", address1);
            					}
            					if(UtilValidate.isNotEmpty(city)) {
            						postalAddress.put("city", city);
            					}
            					if(UtilValidate.isNotEmpty(postalCode)) {
            						postalAddress.put("postalCode", postalCode);
            					}
            					postalAddress.store();
            				}
            			}
            		}else {
            			Map<String, Object> createPartyPostalAddressMap = dispatcher.runSync("createPartyPostalAddress", 
            					UtilMisc.toMap("partyId", partyId, "address1", address1, "city", city, "postalCode", postalCode,
            							"userLogin", userLogin));
            		}
        		}
        	}else {
        		//else create customer
        		//create Person
        		Map<String, Object> createPersonMap = dispatcher.runSync("createPerson", UtilMisc.toMap("firstName", firstName,
        										"lastName", lastName));
        		if(ServiceUtil.isSuccess(createPersonMap)) {
        			partyId = (String) createPersonMap.get("partyId");
        		}
        		
        		//create email
        		Map<String, Object> createEmailMap = dispatcher.runSync("createPartyEmailAddress", UtilMisc.toMap("partyId", partyId,
						"contactMechPurposeTypeId", "PRIMARY_EMAIL", "userLogin", userLogin));
        		
        		//create PartyRole
        		Map<String, Object> createPartyRoleMap = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", partyId,
						"roleTypeId", "CUSTOMER", "emailAddress", emailAddress, "userLogin", userLogin));
        		
        		//create Party postal address
        		Map<String, Object> createPartyPostalAddressMap = dispatcher.runSync("createPartyPostalAddress", UtilMisc.toMap("partyId", partyId,
        				"address1", address1, "city", city, "postalCode", postalCode,"userLogin", userLogin));
        		
        		//create Party Telecomnumber
        		Map<String, Object> createPartyTelecomMap = dispatcher.runSync("createPartyTelecomNumber", UtilMisc.toMap("partyId", partyId,
						"contactNumber", contactNumber, "roleTypeId", "CUSTOMER", "contactMechId", getRandomString(5),"userLogin", userLogin));
        		
        		//create product store role
        		Map<String, Object> createProductStoreRoleMap = dispatcher.runSync("createProductStoreRole", UtilMisc.toMap("partyId", partyId,
						"productStoreId", productStoreId, "roleTypeId", "CUSTOMER","fromDate",UtilDateTime.nowTimestamp(),"userLogin", userLogin));
        	}
        	
        	
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error in creating Customer in " + module);
        }
        return result;
    }
	
	public static Map<String, Object> findInventoryItems(DispatchContext dispatchContext, Map<String, ? extends Object> context) {

		Delegator delegator = dispatchContext.getDelegator();
		String days = (String) context.get("days");
		String facilityId = (String) context.get("facilityId");
		Calendar cal = Calendar.getInstance();
		Timestamp endDate = null;
		        try {
		       
		        List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		       
		        if(UtilValidate.isNotEmpty(facilityId)) {
		        andExprs.add(EntityCondition.makeCondition("facilityId",EntityOperator.EQUALS, facilityId));
		        }
		        //convert to timestamp
		        andExprs.add(EntityCondition.makeCondition("expireDate",EntityOperator.GREATER_THAN_EQUAL_TO,UtilDateTime.nowTimestamp()));
		            if(UtilValidate.isNotEmpty(days)) {
		            //get no of days after today date
		            cal.add(Calendar.DATE, Integer.parseInt(days));
		           
		            endDate = new Timestamp(cal.getTimeInMillis());
		            endDate = UtilDateTime.getDayEnd(endDate);
		            andExprs.add(EntityCondition.makeCondition("expireDate",EntityOperator.LESS_THAN_EQUAL_TO,endDate));
		            }
		            EntityCondition cond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		            List<GenericValue> inventoryItem = EntityQuery.use(delegator).from("InventoryItemAndLocation").where(cond).queryList();

		            Map<String, Object> result = ServiceUtil.returnSuccess();
		            result.put("inventoryItemList", inventoryItem);

		            return result;
		        } catch (GenericEntityException genericEntityException) {
		            Debug.logError(genericEntityException, module);
		            return ServiceUtil.returnError("Error in finding InventoryItems in " + module);
		        }
		    }

	public static String getRandomString(int number) {
		char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder((100000 + rnd.nextInt(900000)) + "-");
		for (int i = 0; i < number; i++)
		    sb.append(chars[rnd.nextInt(chars.length)]);

		return sb.toString();
	}
}
