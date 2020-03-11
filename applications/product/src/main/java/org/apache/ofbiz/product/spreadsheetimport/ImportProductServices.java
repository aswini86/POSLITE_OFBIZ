/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

package org.apache.ofbiz.product.spreadsheetimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ImportProductServices {

    public static String module = ImportProductServices.class.getName();
    public static final String resource = "ProductUiLabels";
    
    /**
     * This method is responsible to import spreadsheet data into "Product" and
     * "InventoryItem" entities into database. The method uses the
     * ImportProductHelper class to perform its operation. The method uses "Apache
     * POI" api for importing spreadsheet (xls files) data.
     *
     * Note : Create the spreadsheet directory in the ofbiz home folder and keep
     * your xls files in this folder only.
     *
     * @param dctx the dispatch context
     * @param context the context
     * @return the result of the service execution
     * @throws IOException 
     */
    public static Map<String, Object> productImportFromSpreadsheet(DispatchContext dctx, Map<String, ? extends Object> context) throws IOException {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        // System.getProperty("user.dir") returns the path upto ofbiz home
        // directory
        String path = System.getProperty("user.dir") + "/spreadsheet";
        List<File> fileItems = new LinkedList<File>();

        if (UtilValidate.isNotEmpty(path)) {
            File importDir = new File(path);
            if (importDir.isDirectory() && importDir.canRead()) {
                File[] files = importDir.listFiles();
                // loop for all the containing xls file in the spreadsheet
                // directory
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().toUpperCase().endsWith("XLS")) {
                        fileItems.add(files[i]);
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductProductImportDirectoryNotFound", locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "ProductProductImportPathNotSpecified", locale));
        }

        if (fileItems.size() < 1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "ProductProductImportPathNoSpreadsheetExists", locale) + path);
        }

        for (File item: fileItems) {
            // read all xls file and create workbook one by one.
            List<Map<String, Object>> products = new LinkedList<Map<String,Object>>();
            List<Map<String, Object>> inventoryItems = new LinkedList<Map<String,Object>>();
            POIFSFileSystem fs = null;
            HSSFWorkbook wb = null;
            try {
                fs = new POIFSFileSystem(new FileInputStream(item));
                wb = new HSSFWorkbook(fs);
            } catch (IOException e) {
                Debug.logError("Unable to read or create workbook from file", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductProductImportCannotCreateWorkbookFromFile", locale));
            }

            // get first sheet
            HSSFSheet sheet = wb.getSheetAt(0);
            wb.close();
            int sheetLastRowNumber = sheet.getLastRowNum();
            for (int j = 1; j <= sheetLastRowNumber; j++) {
                HSSFRow row = sheet.getRow(j);
                if (row != null) {
                    // read productId from first column "sheet column index
                    // starts from 0"
                    HSSFCell cell2 = row.getCell(2);
                    cell2.setCellType(HSSFCell.CELL_TYPE_STRING);
                    String productId = cell2.getRichStringCellValue().toString();
                    // read QOH from ninth column
                    HSSFCell cell5 = row.getCell(5);
                    BigDecimal quantityOnHand = BigDecimal.ZERO;
                    if (cell5 != null && cell5.getCellType() == HSSFCell.CELL_TYPE_NUMERIC)
                        quantityOnHand = new BigDecimal(cell5.getNumericCellValue());

                    // check productId if null then skip creating inventory item
                    // too.
                    boolean productExists = ImportProductHelper.checkProductExists(productId, delegator);

                    if (productId != null && !productId.trim().equalsIgnoreCase("") && !productExists) {
                        products.add(ImportProductHelper.prepareProduct(productId));
                        if (quantityOnHand.compareTo(BigDecimal.ZERO) >= 0)
                            inventoryItems.add(ImportProductHelper.prepareInventoryItem(productId, quantityOnHand,
                                    delegator.getNextSeqId("InventoryItem")));
                        else
                            inventoryItems.add(ImportProductHelper.prepareInventoryItem(productId, BigDecimal.ZERO, delegator
                                    .getNextSeqId("InventoryItem")));
                    }
                    int rowNum = row.getRowNum() + 1;
                    if (row.toString() != null && !row.toString().trim().equalsIgnoreCase("") && productExists) {
                        Debug.logWarning("Row number " + rowNum + " not imported from " + item.getName(), module);
                    }
                }
            }
            // create and store values in "Product" and "InventoryItem" entity
            // in database
            for (int j = 0; j < products.size(); j++) {
                GenericValue productGV = delegator.makeValue("Product", products.get(j));
                GenericValue inventoryItemGV = delegator.makeValue("InventoryItem", inventoryItems.get(j));
                if (!ImportProductHelper.checkProductExists(productGV.getString("productId"), delegator)) {
                    try {
                        delegator.create(productGV);
                        delegator.create(inventoryItemGV);
                    } catch (GenericEntityException e) {
                        Debug.logError("Cannot store product", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                                "ProductProductImportCannotStoreProduct", locale));
                    }
                }
            }
            int uploadedProducts = products.size() + 1;
            if (products.size() > 0)
                Debug.logInfo("Uploaded " + uploadedProducts + " products from file " + item.getName(), module);
        }
        return ServiceUtil.returnSuccess();
    }
    
    public static Map<String, Object> productCategoryImportFromSpreadsheet(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        String encoding = System.getProperty("file.encoding");
        String csvString = Charset.forName(encoding).decode(fileBytes).toString();
        final BufferedReader csvReader = new BufferedReader(new StringReader(csvString));
        CSVFormat fmt = CSVFormat.DEFAULT.withHeader();
        List<String> errMsgs = new LinkedList<String>();
        List<String> newErrMsgs = new LinkedList<String>();
        Map<String, Object> result = null;

        String productId = null;
        String mainCategory = null;
        String subCategory = null;
        String productCategoryId = null, productSubCategoryId = null,finalePrdCategoryId = null;
        List<GenericValue> productCategoryList = new ArrayList<GenericValue>();
        List<GenericValue> productSubCategoryList = new ArrayList<GenericValue>();
        GenericValue productVal = new GenericValue();
        if (fileBytes == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "PartyUploadedFileDataNotFound", locale));
        }
        
        try {
            for (final CSVRecord rec : fmt.parse(csvReader)) {
                mainCategory = rec.get("mainCategory");
                Debug.logInfo("mainCategory-------------------&&&--", mainCategory);
                if(UtilValidate.isNotEmpty(mainCategory)) {
                	mainCategory = mainCategory.trim();
                	productCategoryList = EntityQuery.use(delegator).from("ProductCategory").where(
                			EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("categoryName"), 
                					EntityOperator.LIKE, EntityFunction.UPPER("%" + mainCategory + "%"))).queryList();
                	if (UtilValidate.isEmpty(productCategoryList)) {
                		productCategoryId = delegator.getNextSeqId("ProductCategory");
        				GenericValue createProductCategory = delegator.makeValue("ProductCategory", 
        						UtilMisc.toMap("productCategoryId", productCategoryId, "categoryName", mainCategory,
        								"description", mainCategory,"longDescription",mainCategory));
                		createProductCategory.create();
                		finalePrdCategoryId = productCategoryId;
                	}else {
                		finalePrdCategoryId = productCategoryList.get(0).getString("productCategoryId");
                	}
                }
                subCategory = rec.get("subCategory"); 
                Debug.logInfo("subCategory-------------------&&&--", subCategory);
                if(UtilValidate.isNotEmpty(subCategory)) {
                	subCategory = subCategory.trim();
                	productSubCategoryList = EntityQuery.use(delegator).from("ProductCategory").where(
                			EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("categoryName"), 
                					EntityOperator.LIKE, EntityFunction.UPPER("%" + subCategory + "%"))).queryList();
                	if (UtilValidate.isEmpty(productSubCategoryList)) {
                		productSubCategoryId = delegator.getNextSeqId("ProductCategory");
        				GenericValue createSubProductCategory = delegator.makeValue("ProductCategory", 
        						UtilMisc.toMap("productCategoryId", productSubCategoryId, "categoryName", subCategory,
        								"description", subCategory,"longDescription",subCategory,"primaryParentCategoryId",productCategoryId));
        				createSubProductCategory.create();
        				finalePrdCategoryId = productSubCategoryId;
                	}else {
                		finalePrdCategoryId = productSubCategoryList.get(0).getString("productCategoryId");
                	}
                }
                
                if (UtilValidate.isNotEmpty(rec.get("productId"))) {
                	productId = rec.get("productId");
                	productVal = EntityQuery.use(delegator).from("Product").where("productId",productId).queryOne();
                	if(UtilValidate.isNotEmpty(productVal)) {
                		
                		Map prdMap = dispatcher.runSync("safeAddProductToCategory", UtilMisc.toMap("productId",productId,
                				"productCategoryId",finalePrdCategoryId, "fromDate",UtilDateTime.nowTimestamp(),"userLogin", userLogin));
						/*
						 * productVal.set("primaryProductCategoryId", finalePrdCategoryId);
						 * productVal.store();
						 */
                	}
                }
            }
        } catch (GenericServiceException e) {
        	Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
		}
        catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (errMsgs.size() > 0) {
            return ServiceUtil.returnError(errMsgs);
        }

        result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "Product Category Created Successfully", locale));
        return result;
    }
    
    public static Map<String, Object> bulkProductImportFromSpreadsheet(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        String encoding = System.getProperty("file.encoding");
        String csvString = Charset.forName(encoding).decode(fileBytes).toString();
        final BufferedReader csvReader = new BufferedReader(new StringReader(csvString));
        CSVFormat fmt = CSVFormat.DEFAULT.withHeader();
        List<String> errMsgs = new LinkedList<String>();
        List<String> newErrMsgs = new LinkedList<String>();
        Map<String, Object> result = null;

        String productId = null;
        String internalName = null;
        String productName = null;
        String brandName = null;
        String mainCategory = null;
        String subCategory = null;
        String currencyUOMId = null;
        String price = null;
        String termUomId = null;
        String productTax = null;
        String expiryDate = null;
        String idType = null;
        String idValue = null;
        Timestamp expiryDateTimestamp = null;
        
        String productCategoryId = null, productSubCategoryId = null,finalePrdCategoryId = null;
        List<GenericValue> productCategoryList = new ArrayList<GenericValue>();
        List<GenericValue> productSubCategoryList = new ArrayList<GenericValue>();
        GenericValue productVal = new GenericValue();
        if (fileBytes == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "BulkUploadedFileDataNotFound", locale));
        }
        
        try {
            for (final CSVRecord rec : fmt.parse(csvReader)) {
            	if(UtilValidate.isNotEmpty(rec.get("InternalName"))) {
            		internalName = rec.get("InternalName").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("ProductName"))) {
            		productName = rec.get("ProductName").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("BrandName"))) {
            		brandName = rec.get("BrandName").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("CategoryName"))) {
            		mainCategory = rec.get("CategoryName").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("SubCategoryName"))) {
            		subCategory = rec.get("SubCategoryName").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("Currency_UOM_ID"))) {
            		currencyUOMId = rec.get("Currency_UOM_ID").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("Price"))) {
            		price = rec.get("Price").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("Term_UOM_Id"))) {
            		termUomId = rec.get("Term_UOM_Id").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("Tax"))) {
            		productTax = rec.get("Tax").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("ThroughDate"))) {
            		expiryDate = rec.get("ThroughDate").trim();
            		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            	    Date parsedDate = (Date) dateFormat.parse(expiryDate);
            	    expiryDateTimestamp = new java.sql.Timestamp(parsedDate.getTime());
            	}
            	if(UtilValidate.isNotEmpty(rec.get("IDType"))) {
            		idType = rec.get("IDType").trim();
            	}
            	if(UtilValidate.isNotEmpty(rec.get("IDValue"))) {
            		idValue = rec.get("IDValue").trim();
            	}
            	
                Debug.logInfo("InternalName-------------------&&&--", internalName);
                if(UtilValidate.isNotEmpty(internalName)) {
                	//Code to create Product
                	Map<String, Object> createProduct = dispatcher.runSync("createProduct", UtilMisc.toMap("productName", productName,
                			"productTypeId", "FINISHED_GOOD", "brandName", brandName, "salesDiscontinuationDate", expiryDateTimestamp,
                			"internalName",internalName, "userLogin", userLogin));
                	productId = (String) createProduct.get("productId");
                	//Code to create ProductCategory
                	mainCategory = mainCategory.trim();
                	productCategoryList = EntityQuery.use(delegator).from("ProductCategory").where(
                			EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("categoryName"), 
                					EntityOperator.LIKE, EntityFunction.UPPER("%" + mainCategory + "%"))).queryList();
                	if (UtilValidate.isEmpty(productCategoryList)) {
                		productCategoryId = delegator.getNextSeqId("ProductCategory");
        				GenericValue createProductCategory = delegator.makeValue("ProductCategory", 
        						UtilMisc.toMap("productCategoryId", productCategoryId, "categoryName", mainCategory,
        								"description", mainCategory,"longDescription",mainCategory));
                		createProductCategory.create();
                		finalePrdCategoryId = productCategoryId;
                	}else {
                		finalePrdCategoryId = productCategoryList.get(0).getString("productCategoryId");
                	}
                }
                Debug.logInfo("subCategory-------------------&&&--", subCategory);
                if(UtilValidate.isNotEmpty(subCategory)) {
                	subCategory = subCategory.trim();
                	productSubCategoryList = EntityQuery.use(delegator).from("ProductCategory").where(
                			EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("categoryName"), 
                					EntityOperator.LIKE, EntityFunction.UPPER("%" + subCategory + "%"))).queryList();
                	if (UtilValidate.isEmpty(productSubCategoryList)) {
                		productSubCategoryId = delegator.getNextSeqId("ProductCategory");
        				GenericValue createSubProductCategory = delegator.makeValue("ProductCategory", 
        						UtilMisc.toMap("productCategoryId", productSubCategoryId, "categoryName", subCategory,
        								"description", subCategory,"longDescription",subCategory,"primaryParentCategoryId",productCategoryId));
        				createSubProductCategory.create();
        				finalePrdCategoryId = productSubCategoryId;
                	}else {
                		finalePrdCategoryId = productSubCategoryList.get(0).getString("productCategoryId");
                	}
                }
                
                if (UtilValidate.isNotEmpty(productId)) {
                		
                	Map<String, Object> addPrdCategoryMap = dispatcher.runSync("safeAddProductToCategory", UtilMisc.toMap("productId",productId,
            				"productCategoryId",finalePrdCategoryId, "fromDate",UtilDateTime.nowTimestamp(),"userLogin", userLogin));
                }
              //Code to create ProductPrice
                if (UtilValidate.isNotEmpty(productId)) {
                		
                		Map<String, Object> prdPriceMap = dispatcher.runSync("createProductPrice", UtilMisc.toMap("productId",productId,
                				"productPricePurposeId","PURCHASE", "productPriceTypeId","DEFAULT_PRICE","termUomId",termUomId,
                				"productStoreGroupId", "_NA_","currencyUomId", currencyUOMId,"price", new BigDecimal(price),
                				"taxPercentage", new BigDecimal(productTax),"userLogin", userLogin));
                		Map<String, Object> prdListPriceMap = dispatcher.runSync("createProductPrice", UtilMisc.toMap("productId",productId,
                				"productPricePurposeId","PURCHASE", "productPriceTypeId","LIST_PRICE","termUomId",termUomId,
                				"productStoreGroupId", "_NA_","currencyUomId", currencyUOMId,"price", new BigDecimal(price),
                				"taxPercentage", new BigDecimal(productTax),"userLogin", userLogin));
                }
              //Code to create EAN
                if (UtilValidate.isNotEmpty(productId)) {
            		
            		Map<String, Object> prdPriceMap = dispatcher.runSync("createGoodIdentification", 
            				UtilMisc.toMap("goodIdentificationTypeId","EAN","idValue", idValue,
            						"productId", productId,"userLogin", userLogin));
                }
              
            }
        } catch (GenericServiceException | GenericEntityException e) {
        	Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
		} catch (IOException | ParseException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (errMsgs.size() > 0) {
            return ServiceUtil.returnError(errMsgs);
        }

        result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "Product Category Created Successfully", locale));
        return result;
    }
}
