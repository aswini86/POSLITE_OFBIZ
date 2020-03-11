package org.apache.ofbiz.unmapproduct;

import org.apache.ofbiz.entity.*;
import org.apache.ofbiz.entity.util.*;
import org.apache.ofbiz.base.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;

import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.DispatchContext;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

public class UnmappedItemService {

    public static final String module = UnmappedItemService.class.getName();
    public static final String resource = "UnmappedproductsUiLabels";
    public static final String resourceError = "UnmappedproductsUiLabels";

    public static Map<String, Object> insertUnmappedItem(DispatchContext dispatchContext, Map<String, ? extends Object> context) {
        try {
            Delegator delegator = dispatchContext.getDelegator();

            GenericValue unmappedItem = (GenericValue) delegator.makeValue("UnmappedItem");
            unmappedItem.setNextSeqId();
            unmappedItem.setNonPKFields(context);
            unmappedItem = delegator.create(unmappedItem);

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("unmappedItem", unmappedItem);

            return result;
        } catch (GenericEntityException genericEntityException) {
            Debug.logError(genericEntityException, module);
            return ServiceUtil.returnError("Error in creating ProductStaging in " + module);
        }
    }
    
    public static Map<String, Object> createunMapProduct(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String ean = (String) context.get("ean");
        String internalName = (String) context.get("internalName");
        String productTypeId = (String) context.get("productTypeId");
        String facilityId = (String) context.get("facilityId");
        String productId = "", mrp = "", checkProductId = "";
        Timestamp productEndDate = null;
        BigDecimal quantity = BigDecimal.ZERO;
        try {
            if (UtilValidate.isNotEmpty(ean)) {
                List<GenericValue> goodIdentificationList = EntityQuery.use(delegator).from("GoodIdentification").where(
                        "goodIdentificationTypeId", "EAN", "idValue", ean).queryList();
                if (UtilValidate.isNotEmpty(goodIdentificationList)) {
                    checkProductId = goodIdentificationList.get(0).getString("productId");
                }
                List<GenericValue> facilityPrdList = EntityQuery.use(delegator).from("ProductFacility").where(
                        "facilityId", facilityId, "productId", checkProductId).queryList();

                if (UtilValidate.isNotEmpty(goodIdentificationList) && UtilValidate.isNotEmpty(facilityPrdList)) {
                    return ServiceUtil.returnError("EAN is already mapped");
                }

                GenericValue productStagingData = EntityQuery.use(delegator).from("UnmappedItem").where("ean", ean).queryOne();
                if (UtilValidate.isNotEmpty(productStagingData)) {
                    productEndDate = (Timestamp) convertStringToTimestamp(productStagingData.getString("expiryDate"));
                    quantity = productStagingData.getBigDecimal("quantity");
                    mrp = productStagingData.getString("mrp");
                }
                if (UtilValidate.isEmpty(goodIdentificationList)) {
                    productId = delegator.getNextSeqId("Product");
                    Map<String, Object> createProductMap = dispatcher.runSync("createProduct", UtilMisc.toMap("productId", productId,
                            "internalName", internalName, "productTypeId", productTypeId, "salesDiscontinuationDate", productEndDate,
                            "userLogin", userLogin));
                    Map<String, Object> createProductPriceMap = dispatcher.runSync("createProductPrice", UtilMisc.toMap("productId", productId,
                            "currencyUomId", "USD", "price", new BigDecimal(mrp), "productPriceTypeId", "DEFAULT_PRICE", "productPricePurposeId", "PURCHASE",
                            "productStoreGroupId", "_NA_", "userLogin", userLogin));
                    if (ServiceUtil.isSuccess(createProductMap)) {
                        Map<String, Object> createGoodIdentification = dispatcher.runSync("createGoodIdentification",
                                UtilMisc.toMap("goodIdentificationTypeId", "EAN", "idValue", ean, "productId", productId, "userLogin", userLogin));
                    }
                }

                if (UtilValidate.isEmpty(facilityPrdList)) {
                    if (productId.isEmpty()) {
                        productId = checkProductId;
                    }
                    Map<String, Object> createInventoryItem = dispatcher.runSync("createInventoryItem",
                            UtilMisc.toMap("facilityId", facilityId, "productId", productId, "inventoryItemTypeId",
                                    "NON_SERIAL_INV_ITEM", "partyId", userLogin.get("partyId"), "datetimeReceived", now,
                                    "quantityOnHandTotal", quantity, "availableToPromiseTotal", quantity,
                                    "accountingQuantityTotal", quantity, "unitCost", mrp, "userLogin", userLogin));
                    Map<String, Object> deleteProductStagingMap = dispatcher.runSync("deleteunMapProduct",
                            UtilMisc.toMap("ean", ean, "userLogin", userLogin));
                    if (ServiceUtil.isError(deleteProductStagingMap)) {
                        return ServiceUtil.returnError("Error while Deleting Unmaped Article");
                    }
                } else {
                    return ServiceUtil.returnError("Error while Creating Product");
                }

            }
        } catch (GenericServiceException | GenericEntityException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "UnmapProductService.groovy");
        }

        result.put("productId", productId);
        result.put(ModelService.SUCCESS_MESSAGE, "Article Mapped Succesfully");
        return result;
    }    

    public static Map<String, Object> deleteunMapProduct(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String ean = (String) context.get("ean");
        try {
            if (UtilValidate.isNotEmpty(ean)) {
                GenericValue productStagingData = EntityQuery.use(delegator).from("UnmappedItem").where("ean", ean).queryOne();
                productStagingData.remove();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "UnmappedItemService.groovy");
        }

        result.put("ean", ean);
        result.put(ModelService.SUCCESS_MESSAGE, "Item Removed Succesfully");
        return result;
    }

    public static Map<String, Object> getProductPriceInfo(DispatchContext ctx, Map<String, ? extends Object> context) {
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
        String prdId = new String(), partyId = "";
        Map<String, Double> productMap = new HashMap<String, Double>();
        Map<String, Double> productSaleMap = new HashMap<String, Double>();
        List<GenericValue> productPriceData = new LinkedList<GenericValue>();

        Map<String, Object> productPriceInfoMap = new HashMap<String, Object>();
        List<Map<String, Object>> finalProductPriceInfoList = new LinkedList<Map<String, Object>>();
        ArrayList<String> productIdArray = new ArrayList<String>();
        List<GenericValue> VarianceReasonList = new ArrayList<GenericValue>();
        List<GenericValue> InventoryItemList = new ArrayList<GenericValue>();
        if(UtilValidate.isNotEmpty(userLogin)) {
        	partyId = userLogin.getString("partyId");
        }

        try {
        	VarianceReasonList = EntityQuery.use(delegator).from("VarianceReason").queryList();
            if (UtilValidate.isNotEmpty(productId)) {
            	productPriceData = getFacilityProducts(delegator, partyId, productId);
                if (UtilValidate.isNotEmpty(productPriceData)) {
                    for (GenericValue productDataEach : productPriceData) {
                        prdId = productDataEach.getString("productId");
                        productMap = getProductMrp(delegator,dispatcher, prdId, productStoreId, currencyUomId);
                        productSaleMap = getProductSaleRate(delegator,dispatcher, prdId,productStoreId, currencyUomId);
                        productPriceInfoMap = productDataEach.getAllFields();

                        productPriceInfoMap.put("productPrice", productMap.get("productPrice"));
                        productPriceInfoMap.put("currencyUomId", productMap.get("currencyUomId"));
                        productPriceInfoMap.put("productSalePrice", productSaleMap.get("productSalePrice"));
                        productPriceInfoMap.put("productNewSalePrice", productSaleMap.get("newProductSalePrice"));
                        productPriceInfoMap.put("currencyUomId", currencyUomId);
                        productPriceInfoMap.put("productIdEan", getProductEan(delegator,prdId));
                        finalProductPriceInfoList.add(productPriceInfoMap);
                        productIdArray.add(prdId);
                    }
                }
            } else {
            	//Get user loggedin facility products
                //productPriceData = EntityQuery.use(delegator).from("Product").queryList();
            	if(UtilValidate.isEmpty(categoryId)) {
            		productPriceData = getUserloginFacilityProducts(delegator, partyId);
            	}else {
            		productPriceData = getUserloginFacilityProducts(delegator, partyId, categoryId);
            	}
                if (UtilValidate.isNotEmpty(productPriceData)) {
                    for (GenericValue productDataEach : productPriceData) {
                        prdId = productDataEach.getString("productId");
                        productMap = getProductMrp(delegator,dispatcher, prdId, productStoreId, currencyUomId);
                        productSaleMap = getProductSaleRate(delegator,dispatcher, prdId,productStoreId, currencyUomId);
                        productPriceInfoMap = productDataEach.getAllFields();

                        productPriceInfoMap.put("productPrice", productMap.get("productPrice"));
                        productPriceInfoMap.put("currencyUomId", productMap.get("currencyUomId"));
                        productPriceInfoMap.put("productSalePrice", productSaleMap.get("productSalePrice"));
                        productPriceInfoMap.put("productNewSalePrice", productSaleMap.get("newProductSalePrice"));
                        productPriceInfoMap.put("currencyUomId", currencyUomId);
                        productPriceInfoMap.put("productIdEan", getProductEan(delegator,prdId));
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
        result.put("invItemVarianceList", VarianceReasonList);
        return result;
    }

    public static Map<String, Double> getProductMrp(Delegator delegator,LocalDispatcher dispatcher, String productId, String productStoreId, String currencyUomId) throws GenericEntityException, GenericServiceException {
    	Double productPrice = 0.00;
        Map<String, Double> result = new HashMap<String, Double>();
        
        GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
        Map<String, Object> calcProductResult = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product,"productStoreId", productStoreId, "currencyUomId",currencyUomId));
        
        if(UtilValidate.isNotEmpty(calcProductResult.get("defaultPrice"))) {
        	result.put("productPrice", Double.parseDouble(calcProductResult.get("defaultPrice").toString()));
        }else {
        	result.put("productPrice", productPrice);
        }
        return result;
    }

    public static Map<String, Double> getProductSaleRate(Delegator delegator, LocalDispatcher dispatcher, String productId, String productStoreId, String currencyUomId) throws GenericEntityException, GenericServiceException {
		/*
		 * Double productRulePrice = 0.0d; Double productSalePrice =
		 * getProductSaleMrp(delegator, productId); Double newProductSalePrice = 0.0d;
		 */
    	Double productPrice = 0.00;
        Map<String, Double> result = new HashMap<String, Double>();
        
        GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
        Map<String, Object> calcProductResult = dispatcher.runSync("calculateProductPrice", UtilMisc.toMap("product", product,"productStoreId", productStoreId, "currencyUomId",currencyUomId));
        
        if(UtilValidate.isNotEmpty(calcProductResult.get("listPrice"))) {
        	result.put("productPrice", Double.parseDouble(calcProductResult.get("listPrice").toString()));
        }else {
        	result.put("productPrice", productPrice);
        }

        return result;
    }
    
    @SuppressWarnings("unchecked")
    public static Double getProductSaleMrp(Delegator delegator, String productId) throws GenericEntityException {
        Double productPrice = 0.0d;

        List<GenericValue> productList = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId, "productPriceTypeId", "LIST_PRICE").queryList();
        if (productList != null && productList.size() != 0) {

            productPrice = (Double) productList.get(0).get("price");
        }

        return productPrice;
    }
    
    public static String getProductEan(Delegator delegator, String productId) throws GenericEntityException, GenericServiceException {
        Map<String, String> result = new HashMap<String, String>();
        String productEan = "";
        List<GenericValue> goodIdentificationList = EntityQuery.use(delegator).from("GoodIdentification").where("goodIdentificationTypeId","EAN",
        									"productId", productId).queryList();
        if (UtilValidate.isNotEmpty(goodIdentificationList)) {
        	productEan = goodIdentificationList.get(0).getString("idValue");
        }
        result.put("productEan", productEan);
        return productEan;
    }
    private static String calcRateAdjVal(Double prdPrice,BigDecimal rateAdjPer) throws GenericEntityException, GenericServiceException {
        Double newMarginVal = 0.0;
        Double ratePer = rateAdjPer.doubleValue();
        newMarginVal = prdPrice+(prdPrice*ratePer/100);
        
        return newMarginVal.toString();
    }
    
    public static Timestamp convertStringToTimestamp(String strDate) {
        try {
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            Date date = formatter.parse(strDate);
            Timestamp timeStampDate = new Timestamp(date.getTime());

            return timeStampDate;
        } catch (ParseException e) {
            System.out.println("Exception :" + e);
            return null;
        }
    }
    
    private static List<GenericValue> getUserloginFacilityProducts(Delegator delegator, String partyId) throws GenericEntityException, GenericServiceException {
    	List<GenericValue> products = null;
    	//get User Facility
    	try {
    		GenericValue facilityParty = new GenericValue();
        	List<GenericValue> facilityList = new ArrayList<GenericValue>();
        	List<String> productIds = new ArrayList<String>();
        	
    		if(UtilValidate.isNotEmpty(partyId)) {
        		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
    					.orderBy("facilityId").orderBy("createdStamp").queryOne();
        		
        		//get Facility Inventory Items
            	if (UtilValidate.isNotEmpty(facilityParty)) {
            		String facilityId = facilityParty.getString("facilityId");
            		productIds = EntityQuery.use(delegator).from("InventoryItem").where("facilityId",facilityId)
        					.orderBy("productId").distinct(true).getFieldList("productId");
            		
            		EntityCondition faciliyPrdCond = EntityCondition.makeCondition("productId",EntityOperator.IN,productIds);
                    products = EntityQuery.use(delegator).from("Product").where(faciliyPrdCond).orderBy("productName", "description").queryList();
            	}
        	}
    	} catch (GenericEntityException e) {
    		
    	}
    	
        return products;
    }
    
    private static List<GenericValue> getUserloginFacilityProducts(Delegator delegator, String partyId, String productCategoryId) throws GenericEntityException, GenericServiceException {
    	List<GenericValue> products = null;
    	//get User Facility
    	try {
    		GenericValue facilityParty = new GenericValue();
        	List<GenericValue> facilityList = new ArrayList<GenericValue>();
        	List<String> productIds = new ArrayList<String>();
        	
    		if(UtilValidate.isNotEmpty(partyId)) {
        		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
    					.orderBy("facilityId").orderBy("createdStamp").queryOne();
        		
        		//get Facility Inventory Items
            	if (UtilValidate.isNotEmpty(facilityParty)) {
            		String facilityId = facilityParty.getString("facilityId");
            		productIds = EntityQuery.use(delegator).from("InventoryItem").where("facilityId",facilityId)
        					.orderBy("productId").distinct(true).getFieldList("productId");
            		
            		EntityCondition faciliyPrdCond = EntityCondition.makeCondition(EntityOperator.AND,
            				EntityCondition.makeCondition("productCategoryId",EntityOperator.EQUALS,productCategoryId),
            				EntityCondition.makeCondition("productId",EntityOperator.IN,productIds));
                    products = EntityQuery.use(delegator).from("ProductAndCategoryMember").where(faciliyPrdCond).
                    		orderBy("productName", "description").queryList();
            	}
        	}
    	} catch (GenericEntityException e) {
    		
    	}
    	
        return products;
    }
    
    private static List<GenericValue> getFacilityProducts(Delegator delegator, String partyId, String productId) throws GenericEntityException, GenericServiceException {
    	List<GenericValue> productList = new ArrayList<GenericValue>();
    	List<GenericValue> productNameList = new ArrayList<GenericValue>();
    	List<GenericValue> productEanList = new ArrayList<GenericValue>();
    	List<GenericValue> finalPrdList = new ArrayList<GenericValue>();
    	//get User Facility
    	try {
    		GenericValue facilityParty = new GenericValue();
        	List<String> productIds = new ArrayList<String>();
        	
    		if(UtilValidate.isNotEmpty(partyId)) {
        		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
    					.orderBy("facilityId").orderBy("createdStamp").queryOne();
        		
        		//get Facility Inventory Items
            	if (UtilValidate.isNotEmpty(facilityParty)) {
            		String facilityId = facilityParty.getString("facilityId");
            		productIds = EntityQuery.use(delegator).from("InventoryItem").where("facilityId",facilityId)
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
    
    private static List<GenericValue> getFacilityPriceProducts(Delegator delegator, String partyId, List<String> productIDS) throws GenericEntityException, GenericServiceException {
    	List<GenericValue> productList = new ArrayList<GenericValue>();
    	List<GenericValue> productNameList = new ArrayList<GenericValue>();
    	List<GenericValue> productEanList = new ArrayList<GenericValue>();
    	List<String> productIDs = new ArrayList<String>();
    	//get User Facility
    	try {
    		GenericValue facilityParty = new GenericValue();
        	List<String> productIds = new ArrayList<String>();
        	
        	if (UtilValidate.isNotEmpty(productIDS)) {
        		for (String productId : productIDS) {
        			productIds.add(productId.trim());
        		}
        	}
    		if(UtilValidate.isNotEmpty(partyId)) {
        		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
    					.orderBy("facilityId").orderBy("createdStamp").queryOne();
        		
        		//get Facility Inventory Items
            	if (UtilValidate.isNotEmpty(facilityParty)) {
            		String facilityId = facilityParty.getString("facilityId");
            		//productIds = EntityQuery.use(delegator).from("InventoryItem").where("facilityId",facilityId)
        					//.orderBy("productId").distinct(true).getFieldList("productId");
            		
            		//EntityCondition to check by productId
            		EntityCondition faciliyPrdCond = EntityCondition.makeCondition(EntityOperator.AND,
            				EntityCondition.makeCondition("productId",EntityOperator.IN,productIds));
            		productList = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where(faciliyPrdCond).
                    		orderBy("productName", "description").queryList();
            		
            	}
        	}
    	} catch (GenericEntityException e) {
    		
    	}
    	
        return productList;
    }
    
    private static List<GenericValue> getFacilityInvProducts(Delegator delegator, String partyId, List<String> productIDS) throws GenericEntityException, GenericServiceException {
    	List<GenericValue> productList = new ArrayList<GenericValue>();
    	List<GenericValue> productNameList = new ArrayList<GenericValue>();
    	List<GenericValue> productEanList = new ArrayList<GenericValue>();
    	List<GenericValue> finalPrdList = new ArrayList<GenericValue>();
    	List<String> productIDs = new ArrayList<String>();
    	//get User Facility
    	try {
    		GenericValue facilityParty = new GenericValue();
        	List<String> productIds = new ArrayList<String>();
        	
        	if (UtilValidate.isNotEmpty(productIDS)) {
        		for (String productId : productIDS) {
        			productIds.add(productId.trim());
        		}
        	}
    		if(UtilValidate.isNotEmpty(partyId)) {
        		facilityParty = EntityQuery.use(delegator).from("FacilityParty").where("partyId",partyId)
    					.orderBy("facilityId").orderBy("createdStamp").queryOne();
        		
        		//get Facility Inventory Items
            	if (UtilValidate.isNotEmpty(facilityParty)) {
            		String facilityId = facilityParty.getString("facilityId");
            		
            		//EntityCondition to check by productId
            		EntityCondition faciliyPrdCond = EntityCondition.makeCondition(EntityOperator.AND,
            				EntityCondition.makeCondition("productId",EntityOperator.IN,productIds));
            		for (String productIdEach : productIds) {
                		productList = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where("productId",productIdEach).
                        		orderBy("productName", "description").queryList();
                		
                		if(UtilValidate.isNotEmpty(productList)) {
                        	finalPrdList.addAll(productList);
                        }
            		}
            		
					/*
					 * productList =
					 * EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where(
					 * faciliyPrdCond). orderBy("productName", "description").queryList();
					 */
            		
            	}
        	}
    	} catch (GenericEntityException e) {
    		
    	}
    	
        return finalPrdList;
    }
    
    public static Map<String, Object> getFacilityCategoryInfo(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productStoreId = (String) context.get("productStoreId");
        String categoryId = (String) context.get("categoryId");
        String partyId = "",prodCatalogId = "";

        if(UtilValidate.isNotEmpty(userLogin)) {
        	partyId = userLogin.getString("partyId");
        }
        
        List<GenericValue> productCatalogList = new ArrayList<GenericValue>();
        List<GenericValue> productCatCategoryList = new ArrayList<GenericValue>();
        List<String> categoryIds = new ArrayList<String>();
        List<GenericValue> prdCategoryList = new ArrayList<GenericValue>();
        try {
        	//Get user loggedin facility products
        	productCatalogList = EntityQuery.use(delegator).from("ProductStoreCatalog").
        			where("productStoreId", productStoreId).queryList();
            if (UtilValidate.isNotEmpty(productCatalogList)) {
                for (GenericValue productCatalogEach : productCatalogList) {
                	prodCatalogId = productCatalogEach.getString("prodCatalogId");
                	productCatCategoryList = EntityQuery.use(delegator).from("ProdCatalogCategory").
                			where("prodCatalogId", prodCatalogId).queryList();
                	for (GenericValue productCatalogCategoryEach : productCatCategoryList) {
                		categoryIds.add(productCatalogCategoryEach.getString("productCategoryId"));
                	}
                }
            }
            prdCategoryList = EntityQuery.use(delegator).from("ProductCategory").
        			where(EntityCondition.makeCondition("productCategoryId",EntityOperator.IN,categoryIds)).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "UnmapProductService.groovy");
        }

        result.put("categoryList", prdCategoryList);
        return result;
    }
    
    public static Map<String, Object> findProductPriceRuleTxnList(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productPriceTxnId = (String) context.get("productPriceTxnId");
        String productStoreId = (String) context.get("productStoreId");
        String currencyUomId = (String) context.get("currencyUomId");
        
        String prdId = new String(), partyId = "";
        Map<String, Double> productMap = new HashMap<String, Double>();
        Map<String, Double> productSaleMap = new HashMap<String, Double>();
        List<GenericValue> productPriceData = new LinkedList<GenericValue>();
        List<String> productIds = new ArrayList<String>();
        List<BigDecimal> rateAdjValues = new ArrayList<BigDecimal>();
        Map<String, Object> productPriceInfoMap = new HashMap<String, Object>();
        List<Map<String, Object>> finalProductPriceInfoList = new LinkedList<Map<String, Object>>();
        ArrayList<String> productIdArray = new ArrayList<String>();
        List<String> ratePercentageArray = new ArrayList<String>();
        if(UtilValidate.isNotEmpty(userLogin)) {
        	partyId = userLogin.getString("partyId");
        }

        try {
            if (UtilValidate.isNotEmpty(productPriceTxnId)) {
            	//get product price rule txn products
            	List<String> productPriceRuleIds = EntityQuery.use(delegator).from("ProductPriceRuleTrx").
            	where("productPriceRuleTxId",productPriceTxnId).getFieldList("productPriceRuleId");
            	if(UtilValidate.isNotEmpty(productPriceRuleIds)) {
            		
            		productIds = EntityQuery.use(delegator).from("ProductPriceTrxCond").where(
            				EntityCondition.makeCondition("productPriceRuleTxId",EntityOperator.IN,
            						productPriceRuleIds)).getFieldList("condValue");
            		
            		rateAdjValues = EntityQuery.use(delegator).from("ProductPriceTrxAction").where(
            				EntityCondition.makeCondition("productPriceRuleTxId",EntityOperator.IN,
            						productPriceRuleIds)).getFieldList("amount");
            	}
            	
            	productPriceData = getFacilityPriceProducts(delegator, partyId, productIds);
                if (UtilValidate.isNotEmpty(productPriceData)) {
                	int i = 0;
                    for (GenericValue productDataEach : productPriceData) {
                    	
                        prdId = productDataEach.getString("productId");
                        productMap = getProductMrp(delegator,dispatcher, prdId, productStoreId, currencyUomId);
                        productSaleMap = getProductSaleRate(delegator,dispatcher, prdId,productStoreId, currencyUomId);
                        productPriceInfoMap = productDataEach.getAllFields();

                        productPriceInfoMap.put("productPrice", productMap.get("productPrice"));
                        productPriceInfoMap.put("currencyUomId", productMap.get("currencyUomId"));
                        productPriceInfoMap.put("productSalePrice", productSaleMap.get("productSalePrice"));
                        productPriceInfoMap.put("productNewSalePrice", productSaleMap.get("newProductSalePrice"));
                        productPriceInfoMap.put("currencyUomId", currencyUomId);
                        productPriceInfoMap.put("productIdEan", getProductEan(delegator,prdId));
                        productPriceInfoMap.put("newMargin", calcRateAdjVal(productMap.get("productPrice"),rateAdjValues.get(i)));
                        productPriceInfoMap.put("ratePercentage", rateAdjValues.get(i));
                        finalProductPriceInfoList.add(productPriceInfoMap);
                        productIdArray.add(prdId);
                        ratePercentageArray.add(rateAdjValues.get(i).toString());
                        
                        i++;
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "UnmapProductService.groovy");
        }

        result.put("finalProductPriceInfoList", finalProductPriceInfoList);
        result.put("productIdArray", productIdArray);
        result.put("ratePercentageArray", ratePercentageArray);
        return result;
    }
    
    public static Map<String, Object> createRateAdjustmentTrx(DispatchContext ctx, Map<String, ? extends Object> context) {
		
    	Map<String, Object> result = new HashMap<String, Object>();
    	Delegator delegator = ctx.getDelegator();
    	
    	String rateAdjustmentId = delegator.getNextSeqId("ProductPriceTrx");
    	result.put("rateAdjustmentId", rateAdjustmentId);
    	
        return result;
    }
    
    public static Map<String, Object> getProductInventoryInfo(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        System.out.println("came into getProductInventoryInfo-------");
        String productId = (String) context.get("productId");
        String productStoreGroupId = (String) context.get("productStoreGroupId");
        String productStoreId = (String) context.get("productStoreId");
        String currencyUomId = (String) context.get("currencyUomId");
        String inventoryArray = (String) context.get("inventoryArray");
        
        String prdId = new String(), partyId = "";
        Map<String, Double> productMap = new HashMap<String, Double>();
        Map<String, Double> productSaleMap = new HashMap<String, Double>();
        List<GenericValue> productPriceData = new LinkedList<GenericValue>();

        Map<String, Object> facilityInventoryMap = new HashMap<String, Object>();
        Map<String, Object> productInvInfoMap = new HashMap<String, Object>();
        List<Map<String, Object>> finalProductPriceInfoList = new LinkedList<Map<String, Object>>();
        List<String> inventoryItemArray = new ArrayList<String>();
        List<GenericValue> VarianceReasonList = new ArrayList<GenericValue>();
        List<GenericValue> InventoryItemList = new ArrayList<GenericValue>();
        List<String> invItemArray = new ArrayList<String>();
        if(UtilValidate.isNotEmpty(userLogin)) {
        	partyId = userLogin.getString("partyId");
        }
        String inventoryItemId = "";
        String[] inventoryIds = null;

        try {
        	if(UtilValidate.isNotEmpty(inventoryArray)) {
        		inventoryIds = inventoryArray.split(",");
        	}
        	VarianceReasonList = EntityQuery.use(delegator).from("VarianceReason").queryList();
            if (UtilValidate.isNotEmpty(productId)) {
            	facilityInventoryMap = getFacilityInventoryProducts(delegator, partyId, productId, inventoryIds);
            	productPriceData = (List<GenericValue>) facilityInventoryMap.get("finalPrdList");
            	inventoryItemArray = (List<String>) facilityInventoryMap.get("finalInvItems");
            	
                if (UtilValidate.isNotEmpty(productPriceData)) {
                	int i = 0;
                    for (GenericValue productDataEach : productPriceData) {
                    	inventoryItemId = inventoryItemArray.get(i);
                    	//Need to add Inv ItemId
                        prdId = productDataEach.getString("productId");
                        productMap = getProductMrp(delegator,dispatcher, prdId, productStoreId, currencyUomId);
                        productSaleMap = getProductSaleRate(delegator,dispatcher, prdId,productStoreId, currencyUomId);
                        productInvInfoMap = productDataEach.getAllFields();

                        productInvInfoMap.put("inventoryItemId", inventoryItemId);
                        productInvInfoMap.put("productPrice", productMap.get("productPrice"));
                        productInvInfoMap.put("currencyUomId", productMap.get("currencyUomId"));
                        productInvInfoMap.put("productSalePrice", productSaleMap.get("productSalePrice"));
                        productInvInfoMap.put("productNewSalePrice", productSaleMap.get("newProductSalePrice"));
                        productInvInfoMap.put("currencyUomId", currencyUomId);
                        productInvInfoMap.put("productIdEan", getProductEan(delegator,prdId));
                        productInvInfoMap.put("productQoh", getProductInventoryQoh(delegator,inventoryItemId,prdId));
                        productInvInfoMap.put("productAtp", getProductInventoryAtp(delegator,inventoryItemId,prdId));
                        finalProductPriceInfoList.add(productInvInfoMap);
                        invItemArray.add(inventoryItemId);
                        i++;
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "UnmapProductService.groovy");
        }

        result.put("finalProductPriceInfoList", finalProductPriceInfoList);
        result.put("inventoryItemArray", invItemArray);
        result.put("invItemVarianceList", VarianceReasonList);
        return result;
    }
    
    private static Map<String, Object> getFacilityInventoryProducts(Delegator delegator, String partyId, String productId, String[] inventoryIds)
    		throws GenericEntityException, GenericServiceException {
    	
    	Map<String, Object> result = new HashMap<String, Object>();
    	List<GenericValue> productList = new ArrayList<GenericValue>();
    	Map<String, Object> productMap = new HashMap<String, Object>();
    	List<GenericValue> finalPrdList = new ArrayList<GenericValue>();
    	List<String> finalInvItems = new ArrayList<String>();
    	List<String> invItemList = new ArrayList<String>();
    	String InventoryItemId = "";
    	//get User Facility
    	try {
        	List<String> productIds = new ArrayList<String>();
        	EntityCondition invItemCond = null;
        	
        	if (UtilValidate.isNotEmpty(inventoryIds)) {
        		for(String invItemId : inventoryIds) {
        			invItemList.add(invItemId);
        		}
        		invItemCond = EntityCondition.makeCondition(EntityOperator.AND,
						EntityCondition.makeCondition("inventoryItemId",EntityOperator.NOT_IN,invItemList),
						EntityCondition.makeCondition("productId",EntityOperator.EQUALS,productId));
        	}else {
        		invItemCond = EntityCondition.makeCondition("productId",EntityOperator.EQUALS,productId);
        	}
    		if(UtilValidate.isNotEmpty(partyId)) {
        		
        		//get Facility Inventory Items
    			
        		productIds = EntityQuery.use(delegator).from("InventoryItem").where(invItemCond)
        				.orderBy("productId").getFieldList("productId");
        		finalInvItems = EntityQuery.use(delegator).from("InventoryItem").where(invItemCond)
    					.orderBy("productId").getFieldList("inventoryItemId");
        		
        		//EntityCondition to check by productId
        		for (String productIdEach : productIds) {
            		productList = EntityQuery.use(delegator).from("GoodIdentificationAndProduct").where("productId",productIdEach).
                    		orderBy("productName", "description").queryList();
            		
            		if(UtilValidate.isNotEmpty(productList)) {
                    	finalPrdList.addAll(productList);
                    }
        		}
        		
        	}
    	} catch (GenericEntityException e) {
    		
    	}
    	result.put("finalPrdList", finalPrdList);
    	result.put("finalInvItems", finalInvItems);
        return result;
    }
    
    private static String getProductInventoryQoh(Delegator delegator,String inventoryItemId, String productId) {
    	
    	String qoh = "";
    	List<GenericValue> inventoryItemList = new ArrayList<GenericValue>();
    	try {
    		inventoryItemList = EntityQuery.use(delegator).from("InventoryItem").where("inventoryItemId", inventoryItemId, "productId",productId)
    				.orderBy("productId").queryList();
    		
    		qoh = inventoryItemList.get(0).getBigDecimal("quantityOnHandTotal").toString();
    	} catch (GenericEntityException e) {
			// TODO: handle exception
		}
    	
    	return qoh;
    }
    
    private static String getProductInventoryAtp(Delegator delegator,String inventoryItemId,String productId) {
    	
    	String atp = "";
    	List<GenericValue> inventoryItemList = new ArrayList<GenericValue>();
    	try {
    		inventoryItemList = EntityQuery.use(delegator).from("InventoryItem").where("inventoryItemId", inventoryItemId, "productId",productId)
    				.orderBy("productId").queryList();
    		
    		atp = inventoryItemList.get(0).getBigDecimal("availableToPromiseTotal").toString();
    	} catch (GenericEntityException e) {
			// TODO: handle exception
		}
    	
    	return atp;
    }
    
    public static Map<String, Object> findStockAdjustmentTxnList(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<String, Object>();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String inventoryItemTxId = (String) context.get("inventoryItemTxId");
        String productStoreId = (String) context.get("productStoreId");
        String currencyUomId = (String) context.get("currencyUomId");
        
        String prdId = new String(), partyId = "",varReasonName = "";
        Map<String, Double> productMap = new HashMap<String, Double>();
        Map<String, Double> productSaleMap = new HashMap<String, Double>();
        List<GenericValue> stockAdjData = new LinkedList<GenericValue>();
        List<String> productIds = new ArrayList<String>();
        List<BigDecimal> rateAdjValues = new ArrayList<BigDecimal>();
        Map<String, Object> stockAdjInfoMap = new HashMap<String, Object>();
        List<Map<String, Object>> inventoryItemList = new LinkedList<Map<String, Object>>();
        List<String> finalvarReasonIdArray = new ArrayList<String>();
        List<String> finalquantityArray = new ArrayList<String>();
        List<String> finalinventoryItemIds = new ArrayList<String>();
        
        List<String> varReasonIdArray = new ArrayList<String>();
        List<String> quantityArray = new ArrayList<String>();
        List<String> inventoryItemIds = new ArrayList<String>();
        if(UtilValidate.isNotEmpty(userLogin)) {
        	partyId = userLogin.getString("partyId");
        }
        GenericValue varianceList = new GenericValue();
        boolean stockSelected = false;
        try {
            if (UtilValidate.isNotEmpty(inventoryItemTxId)) {
            	//get product price rule txn products
            	inventoryItemIds = EntityQuery.use(delegator).from("InventoryItemVarianceTrx").
            	where("inventoryItemTxId",inventoryItemTxId).getFieldList("inventoryItemId");
            	varReasonIdArray = EntityQuery.use(delegator).from("InventoryItemVarianceTrx").
                    	where("inventoryItemTxId",inventoryItemTxId).getFieldList("varianceReasonId");
            	quantityArray = EntityQuery.use(delegator).from("InventoryItemVarianceTrx").
                    	where("inventoryItemTxId",inventoryItemTxId).getFieldList("quantityOnHandVar");
            	for (String inventoryItemId : inventoryItemIds) {
            		finalinventoryItemIds.add(inventoryItemId);
            	}
            	for (String quantity : quantityArray) {
            		finalquantityArray.add(quantity);
            	}
            	for (String varReasonId : varReasonIdArray) {
            		finalvarReasonIdArray.add(varReasonId);
            	}
            	if(UtilValidate.isNotEmpty(inventoryItemIds)) {
            		productIds = EntityQuery.use(delegator).from("InventoryItem").where(
            				EntityCondition.makeCondition("inventoryItemId",EntityOperator.IN,
            						inventoryItemIds)).getFieldList("productId");
            	}
            	
            	stockAdjData = getFacilityInvProducts(delegator, partyId, productIds);
                if (UtilValidate.isNotEmpty(stockAdjData)) {
                	int i = 0;
                    for (GenericValue stockAdjDataEach : stockAdjData) {
                    	
                        prdId = stockAdjDataEach.getString("productId");
                        productMap = getProductMrp(delegator,dispatcher, prdId, productStoreId, currencyUomId);
                        productSaleMap = getProductSaleRate(delegator,dispatcher, prdId,productStoreId, currencyUomId);
                        stockAdjInfoMap = stockAdjDataEach.getAllFields();
                        varianceList = EntityQuery.use(delegator).from("VarianceReason").
                        		where("varianceReasonId", varReasonIdArray.get(i)).queryOne();
                        varReasonName = varianceList.getString("description");
                        		
                        stockAdjInfoMap.put("productPrice", productMap.get("productPrice"));
                        stockAdjInfoMap.put("currencyUomId", productMap.get("currencyUomId"));
                        stockAdjInfoMap.put("productSalePrice", productSaleMap.get("productSalePrice"));
                        stockAdjInfoMap.put("productNewSalePrice", productSaleMap.get("newProductSalePrice"));
                        stockAdjInfoMap.put("currencyUomId", currencyUomId);
                        stockAdjInfoMap.put("productIdEan", getProductEan(delegator,prdId));
                        stockAdjInfoMap.put("inventoryItemId", inventoryItemIds.get(i));
                        stockAdjInfoMap.put("inventoryQty", quantityArray.get(i));
                        stockAdjInfoMap.put("varReasonId", varReasonIdArray.get(i));
                        stockAdjInfoMap.put("varReasonName", varReasonName);
                        if(inventoryItemIds.contains(inventoryItemIds.get(i))) {
                        	stockAdjInfoMap.put("stockSelected", true);
                        } else {
                        	stockAdjInfoMap.put("stockSelected", false);
                        }
                        
                        inventoryItemList.add(stockAdjInfoMap);
                        i++;
                    }
                }
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Debug.logError(e, "Failure in operation, rolling back transaction", "UnmapProductService.groovy");
        }

        result.put("inventoryItemList", inventoryItemList);
        result.put("varReasonIdArray", finalvarReasonIdArray);
        result.put("quantityArray", finalquantityArray);
        result.put("inventoryItemArray", finalinventoryItemIds);
        return result;
    }
    
    public static Map<String, Object> getProductTaxRate(Delegator delegator, LocalDispatcher dispatcher, String productId,
    		String productStoreId, String supplierId, String facilityId, String currencyUomId, Double productPrice) throws GenericEntityException, GenericServiceException {
		
    	Map<String, Object> result = new HashMap<String, Object>();
    	List<GenericValue> facilityContactList = new ArrayList<GenericValue>();
    	List<GenericValue> partyFacilityList = new ArrayList<GenericValue>();
    	List<GenericValue> supplierContactList = new ArrayList<GenericValue>();
    	List<GenericValue> taxAuthorityList = new ArrayList<GenericValue>();
    	List<GenericValue> prdPriceTaxList = new ArrayList<GenericValue>();
    	
    	GenericValue shipAddress = new GenericValue();
    	Map<String, Object> taxMap = new HashMap<String, Object>();
    	GenericValue supplierShipAddress = new GenericValue();
    	String contactMechId = "", supplierContactMechId = "";
    	String facilityFromState = "", supplierFromState = "";
    	BigDecimal taxPercentage = BigDecimal.ZERO;
    	BigDecimal taxValue = BigDecimal.ZERO;
    	//Get Facility state
		partyFacilityList = EntityQuery.use(delegator).from("FacilityParty")
					.where("partyId", facilityId).queryList();
    	if(UtilValidate.isNotEmpty(partyFacilityList)) {
			facilityId = partyFacilityList.get(0).getString("facilityId");
			facilityContactList = EntityQuery.use(delegator).from("FacilityAndContactMech")
									.where("facilityId", facilityId).queryList();
			contactMechId = facilityContactList.get(0).getString("contactMechId");
		}
    	shipAddress = EntityQuery.use(delegator).from("PostalAddress")
				.where(EntityCondition.makeCondition("contactMechId", EntityOperator.EQUALS, contactMechId)).queryOne();
		if(UtilValidate.isNotEmpty(shipAddress)) {
			facilityFromState = shipAddress.getString("stateProvinceGeoId");
		}
    	//Get Supplier State
		supplierContactList = EntityQuery.use(delegator).from("PartyAndContactMech")
									.where("partyId", supplierId).queryList();
		supplierContactMechId = supplierContactList.get(0).getString("contactMechId");
		
		supplierShipAddress = EntityQuery.use(delegator).from("PostalAddress")
				.where(EntityCondition.makeCondition("contactMechId", EntityOperator.EQUALS, supplierContactMechId)).queryOne();
		
		if(UtilValidate.isNotEmpty(shipAddress)) {
			supplierFromState = shipAddress.getString("stateProvinceGeoId");
		}
		if (facilityFromState != supplierFromState) {
			//Need to add IGST tax logic.
			//check it has price tax rate
			prdPriceTaxList = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId,
					"productPriceTypeId", "DEFAULT_PRICE","currencyUomId", currencyUomId,
					"taxAuthPartyId", "IGST","taxInPrice", "Y").queryList();
			if(UtilValidate.isNotEmpty(prdPriceTaxList)) {
				taxPercentage = prdPriceTaxList.get(0).getBigDecimal("taxPercentage");
				
			}
			taxAuthorityList = EntityQuery.use(delegator).from("TaxAuthorityRateProduct").where("productStoreId", productStoreId,
					"taxAuthorityRateTypeId", "IGST_TAX").queryList();
			if(UtilValidate.isNotEmpty(taxAuthorityList)) {
				for (GenericValue taxAuthorityRow : taxAuthorityList) {
					if(UtilValidate.isEmpty(taxPercentage)) {
						taxPercentage = taxAuthorityRow.getBigDecimal("taxPercentage");
					}
					taxValue = new BigDecimal(productPrice).multiply(taxPercentage.divide(BigDecimal.valueOf(100)));
					taxValue = taxValue.setScale(0, BigDecimal.ROUND_HALF_UP);
					taxValue = taxValue.setScale(2, BigDecimal.ROUND_HALF_UP);
					taxMap.put(taxAuthorityRow.getString("taxAuthorityRateTypeId"), taxValue);
				}
				taxMap.put("taxPercentage", taxPercentage.toString());
				taxMap.put("CGST_TAX", "0.0");
				taxMap.put("SGST_TAX", "0.0");
			}
		} else {
			//Need to calculate SGST And CGST
			//check it has price tax rate
			prdPriceTaxList = EntityQuery.use(delegator).from("ProductPrice").where("productId", productId,
					"productPriceTypeId", "DEFAULT_PRICE","currencyUomId", currencyUomId,
					"taxAuthPartyId", "GST","taxInPrice", "Y").queryList();
			if(UtilValidate.isNotEmpty(prdPriceTaxList)) {
				taxPercentage = prdPriceTaxList.get(0).getBigDecimal("taxPercentage");
				
			}
			EntityCondition taxCond = EntityCondition.makeCondition(EntityOperator.AND,
					EntityCondition.makeCondition("productStoreId",EntityOperator.EQUALS,productStoreId),
					EntityCondition.makeCondition("taxAuthorityRateTypeId",EntityOperator.NOT_EQUAL,"IGST_TAX"));
			
			taxAuthorityList = EntityQuery.use(delegator).from("TaxAuthorityRateProduct").where(taxCond).queryList();
			
			if(UtilValidate.isNotEmpty(taxAuthorityList)) {
				for (GenericValue taxAuthorityRow : taxAuthorityList) {
					if(UtilValidate.isEmpty(taxPercentage)) {
						taxPercentage = taxAuthorityRow.getBigDecimal("taxPercentage");
					}
					taxValue = new BigDecimal(productPrice).multiply(taxPercentage.divide(BigDecimal.valueOf(100)));
					taxValue = taxValue.setScale(0, BigDecimal.ROUND_HALF_UP);
					taxValue = taxValue.setScale(2, BigDecimal.ROUND_HALF_UP);
					taxMap.put(taxAuthorityRow.getString("taxAuthorityRateTypeId"), taxValue);
				}
				taxMap.put("taxPercentage", taxPercentage.toString());
				taxMap.put("IGST_TAX", "0.0");
			}
			
		}
		result.put("taxMap", taxMap);
        return result;
    }
}
