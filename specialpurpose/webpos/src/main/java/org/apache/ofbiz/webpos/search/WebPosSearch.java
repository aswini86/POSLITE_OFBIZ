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
package org.apache.ofbiz.webpos.search;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class WebPosSearch {

	public static final String module = WebPosSearch.class.getName();

	public static Map<String, Object> findProducts(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByProductIdValue = (String) context.get("searchByProductIdValue");
		String searchByProductName = (String) context.get("searchByProductName");
		String searchByProductDescription = (String) context.get("searchByProductDescription");
		String goodIdentificationTypeId = (String) context.get("goodIdentificationTypeId");
		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "Product";

		// search by product name
		if (UtilValidate.isNotEmpty(searchByProductName)) {
			searchByProductName = searchByProductName.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productName"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByProductName + "%")));
		}
		// search by description
		if (UtilValidate.isNotEmpty(searchByProductDescription)) {
			searchByProductDescription = searchByProductDescription.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("description"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByProductDescription + "%")));
		}
		// search by good identification
		if (UtilValidate.isNotEmpty(searchByProductIdValue)) {
			entityName = "GoodIdentificationAndProduct";
			searchByProductIdValue = searchByProductIdValue.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.EQUALS,
					searchByProductIdValue));
			if (UtilValidate.isNotEmpty(goodIdentificationTypeId)) {
				andExprs.add(EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS,
						goodIdentificationTypeId));
			}
		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> products = null;
		try {
			products = EntityQuery.use(delegator).from(entityName).where(mainCond).orderBy("productName", "description")
					.queryList();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("productsList", products);
		return result;
	}

	public static Map<String, Object> findParties(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByPartyLastName = (String) context.get("searchByPartyLastName");
		String searchByPartyFirstName = (String) context.get("searchByPartyFirstName");
		String searchByPartyIdValue = (String) context.get("searchByPartyIdValue");
		String partyIdentificationTypeId = (String) context.get("partyIdentificationTypeId");
		String billingLocation = (String) context.get("billingLocation");
		String shippingLocation = (String) context.get("shippingLocation");
		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		List<EntityCondition> orExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		List<String> orderBy = new LinkedList<String>();

		// default view settings
		DynamicViewEntity dynamicView = new DynamicViewEntity();
		dynamicView.addMemberEntity("PT", "Party");
		dynamicView.addAlias("PT", "partyId");
		dynamicView.addAlias("PT", "statusId");
		dynamicView.addAlias("PT", "partyTypeId");
		dynamicView.addMemberEntity("PI", "PartyIdentification");
		dynamicView.addAlias("PI", "partyIdentificationTypeId");
		dynamicView.addAlias("PI", "idValue");
		dynamicView.addViewLink("PT", "PI", Boolean.TRUE, ModelKeyMap.makeKeyMapList("partyId"));
		dynamicView.addMemberEntity("PER", "Person");
		dynamicView.addAlias("PER", "lastName");
		dynamicView.addAlias("PER", "firstName");
		dynamicView.addViewLink("PT", "PER", Boolean.TRUE, ModelKeyMap.makeKeyMapList("partyId"));
		dynamicView.addMemberEntity("PCP", "PartyContactMechPurpose");
		dynamicView.addAlias("PCP", "contactMechId");
		dynamicView.addAlias("PCP", "contactMechPurposeTypeId");
		dynamicView.addAlias("PCP", "fromDate");
		dynamicView.addAlias("PCP", "thruDate");
		dynamicView.addViewLink("PT", "PCP", Boolean.TRUE, ModelKeyMap.makeKeyMapList("partyId"));
		dynamicView.addMemberEntity("CM", "ContactMech");
		dynamicView.addAlias("CM", "contactMechId");
		dynamicView.addAlias("CM", "contactMechTypeId");
		dynamicView.addAlias("CM", "infoString");
		dynamicView.addViewLink("PCP", "CM", Boolean.TRUE, ModelKeyMap.makeKeyMapList("contactMechId"));
		dynamicView.addMemberEntity("PA", "PostalAddress");
		dynamicView.addAlias("PA", "address1");
		dynamicView.addAlias("PA", "city");
		dynamicView.addAlias("PA", "postalCode");
		dynamicView.addAlias("PA", "countryGeoId");
		dynamicView.addAlias("PA", "stateProvinceGeoId");
		dynamicView.addViewLink("CM", "PA", Boolean.TRUE, ModelKeyMap.makeKeyMapList("contactMechId"));

		if (UtilValidate.isNotEmpty(billingLocation) && "Y".equalsIgnoreCase(billingLocation)) {
			orExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS,
					"BILLING_LOCATION"));
		}

		if (UtilValidate.isNotEmpty(shippingLocation) && "Y".equalsIgnoreCase(shippingLocation)) {
			orExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS,
					"SHIPPING_LOCATION"));
		}

		if (orExprs.size() > 0) {
			andExprs.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
		}
		andExprs.add(EntityCondition.makeCondition("partyTypeId", EntityOperator.EQUALS, "PERSON"));
		andExprs.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "POSTAL_ADDRESS"));

		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);

		orderBy.add("lastName");
		orderBy.add("firstName");

		// search by last name
		if (UtilValidate.isNotEmpty(searchByPartyLastName)) {
			searchByPartyLastName = searchByPartyLastName.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByPartyLastName + "%")));
		}
		// search by first name
		if (UtilValidate.isNotEmpty(searchByPartyFirstName)) {
			searchByPartyFirstName = searchByPartyFirstName.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByPartyFirstName + "%")));
		}
		// search by party identification
		if (UtilValidate.isNotEmpty(searchByPartyIdValue)) {
			searchByPartyIdValue = searchByPartyIdValue.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.EQUALS,
					searchByPartyIdValue));
			if (UtilValidate.isNotEmpty(partyIdentificationTypeId)) {
				andExprs.add(EntityCondition.makeCondition("partyIdentificationTypeId", EntityOperator.EQUALS,
						partyIdentificationTypeId));
			}
		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> parties = null;
		try {
			EntityListIterator pli = delegator.findListIteratorByCondition(dynamicView, mainCond, null, null, orderBy,
					null);
			parties = EntityUtil.filterByDate(pli.getCompleteList(), true);
			pli.close();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("partiesList", parties);
		return result;
	}

	public static Map<String, Object> findFacilities(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByFacilityId = (String) context.get("searchByFacilityId");
		String searchByFacilityName = (String) context.get("searchByFacilityName");
		String searchByOwnerPartyId = (String) context.get("searchByOwnerPartyId");
		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "Facility";

		// search by facility name
		if (UtilValidate.isNotEmpty(searchByFacilityName)) {
			searchByFacilityName = searchByFacilityName.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("facilityName"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByFacilityName + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByOwnerPartyId)) {
			searchByOwnerPartyId = searchByOwnerPartyId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("ownerPartyId"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByOwnerPartyId + "%")));
		}
		// search by description
//        if (UtilValidate.isNotEmpty(searchByProductDescription)) {
//            searchByProductDescription = searchByProductDescription.toUpperCase().trim();
//            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("description"), EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByProductDescription + "%")));
//        }
		// search by good identification
		if (UtilValidate.isNotEmpty(searchByFacilityId)) {

			searchByFacilityId = searchByFacilityId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.EQUALS,
					searchByFacilityId));
//            if (UtilValidate.isNotEmpty(goodIdentificationTypeId)) {
//                andExprs.add(EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS, goodIdentificationTypeId));
//            }
		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> facilities = null;
		try {
			facilities = EntityQuery.use(delegator).from(entityName).where(mainCond)
					.orderBy("facilityId", "facilityName").queryList();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("facilityList", facilities);
		return result;
	}

	public static Map<String, Object> findProductStore(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByPrimaryStoreGroupId = (String) context.get("searchByPrimaryStoreGroupId");

		String searchByProductStoreId = (String) context.get("searchByProductStoreId");
		String searchByStoreName = (String) context.get("searchByStoreName");
		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "ProductStore";

		// search by product name
		if (UtilValidate.isNotEmpty(searchByProductStoreId)) {
			searchByProductStoreId = searchByProductStoreId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productStoreId"),
					EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByProductStoreId + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByStoreName)) {
			searchByStoreName = searchByStoreName.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("storeName"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByStoreName + "%")));
		}

		// search by good identification
		if (UtilValidate.isNotEmpty(searchByPrimaryStoreGroupId)) {

			searchByPrimaryStoreGroupId = searchByPrimaryStoreGroupId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("primaryStoreGroupId"),
					EntityOperator.EQUALS, searchByPrimaryStoreGroupId));

		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> productStores = null;
		try {
			productStores = EntityQuery.use(delegator).from(entityName).where(mainCond)
					.orderBy("primaryStoreGroupId", "storeName").queryList();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("productStoreList", productStores);
		return result;
	}

	public static Map<String, Object> findInventoryItem(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByInventoryItemId = (String) context.get("searchByInventoryItemId");
		String searchByProductId = (String) context.get("searchByProductId");
		String searchByPartyId = (String) context.get("searchByPartyId");
		String searchByOwnerPartyId = (String) context.get("searchByOwnerPartyId");
		String searchByFacilityId = (String) context.get("searchByFacilityId");
		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "InventoryItem";

		// search by product name
		if (UtilValidate.isNotEmpty(searchByProductId)) {
			searchByProductId = searchByProductId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productId"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByProductId + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByPartyId)) {
			searchByPartyId = searchByPartyId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyId"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByPartyId + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByOwnerPartyId)) {

			searchByOwnerPartyId = searchByOwnerPartyId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("ownerPartyId"),
					EntityOperator.EQUALS, searchByOwnerPartyId));

		}

		if (UtilValidate.isNotEmpty(searchByFacilityId)) {

			searchByFacilityId = searchByFacilityId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("facilityId"), EntityOperator.EQUALS,
					searchByFacilityId));

		}

		// search by good identification
		if (UtilValidate.isNotEmpty(searchByInventoryItemId)) {

			searchByInventoryItemId = searchByInventoryItemId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("inventoryItemId"),
					EntityOperator.EQUALS, searchByInventoryItemId));

		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> inventoryItems = null;
		try {
			inventoryItems = EntityQuery.use(delegator).from(entityName).where(mainCond)
					.orderBy("inventoryItemId", "productId").queryList();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("inventoryItemList", inventoryItems);
		return result;
	}

	public static Map<String, Object> findInventoryTransfer(DispatchContext dctx,
			Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByInventoryItemId = (String) context.get("searchByInventoryItemId");
		String searchByInventoryTransferId = (String) context.get("searchByInventoryTransferId");
		String searchByfacilityId = (String) context.get("searchByfacilityId");
		String searchByfacilityIdTo = (String) context.get("searchByfacilityIdTo");
		String searchBystatusId = (String) context.get("searchBystatusId");

		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "InventoryTransfer";

		// search by product name
		if (UtilValidate.isNotEmpty(searchByInventoryTransferId)) {
			searchByInventoryTransferId = searchByInventoryTransferId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("inventoryTransferId"),
					EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByInventoryTransferId + "%")));
		}

		// search by good identification
		if (UtilValidate.isNotEmpty(searchByInventoryItemId)) {

			searchByInventoryItemId = searchByInventoryItemId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("inventoryItemId"),
					EntityOperator.EQUALS, searchByInventoryItemId));

		}

		if (UtilValidate.isNotEmpty(searchByfacilityId)) {
			searchByfacilityId = searchByfacilityId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("facilityId"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByfacilityId + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByfacilityIdTo)) {
			searchByfacilityIdTo = searchByfacilityIdTo.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("facilityIdTo"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByfacilityIdTo + "%")));
		}

		if (UtilValidate.isNotEmpty(searchBystatusId)) {
			searchBystatusId = searchBystatusId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("statusId"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchBystatusId + "%")));
		}

		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> inventoryTransferList = null;
		try {
			inventoryTransferList = EntityQuery.use(delegator).from(entityName).where(mainCond)
					.orderBy("inventoryItemId", "inventoryTransferId").queryList();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("inventoryTransferList", inventoryTransferList);
		return result;
	}

	public static Map<String, Object> findPartyMapping(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByPartyIdTo = (String) context.get("searchByPartyIdTo");
		String searchByPartyIdFrom = (String) context.get("searchByPartyIdFrom");
		String searchByType = (String) context.get("searchByType");

		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "PartyIdMapping";

		// search by product name
		if (UtilValidate.isNotEmpty(searchByPartyIdFrom)) {
			searchByPartyIdFrom = searchByPartyIdFrom.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyIdFrom"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByPartyIdFrom + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByType)) {
			searchByType = searchByType.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("type"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByType + "%")));
		}

		// search by good identification
		if (UtilValidate.isNotEmpty(searchByPartyIdTo)) {

			searchByPartyIdTo = searchByPartyIdTo.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyIdTo"), EntityOperator.EQUALS,
					searchByPartyIdTo));

		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> inventoryItems = null;
		try {
			inventoryItems = EntityQuery.use(delegator).from(entityName).where(mainCond)
					.orderBy("partyIdTo", "partyIdFrom").queryList();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("partyMappingList", inventoryItems);
		return result;
	}

	public static Map<String, Object> findCustomerByPhoneNumber(DispatchContext dctx,
			Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String phoneNumber = (String) context.get("phoneNumber");

		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		List<EntityCondition> customerExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		EntityCondition  contactNumCond = null;
		String entityName = "PartyAndTelecomNumberAndPerson";
		String partyId = "";
		String receiptId = "", customerMobileNo = "";
		BigDecimal billAmt = BigDecimal.ZERO;
        BigDecimal billReturnAmt = BigDecimal.ZERO;
        BigDecimal creditNoteAmount = BigDecimal.ZERO;
        GenericValue posCartTxn = new GenericValue();
        List<GenericValue> billReturnTxn = new LinkedList<GenericValue>();

		// search by product name
		if (UtilValidate.isNotEmpty(phoneNumber)) {
			phoneNumber = phoneNumber.trim();
			andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE,
					"%" + phoneNumber + "%"));
			andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE,
					phoneNumber + "%"));
			andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE,
					"%" + phoneNumber));
			contactNumCond = EntityCondition.makeCondition(andExprs, EntityOperator.OR);
			customerExprs.add(contactNumCond);
		}
		customerExprs.add(EntityCondition.makeCondition("roleTypeId",EntityOperator.EQUALS, "CUSTOMER"));
		
		mainCond = EntityCondition.makeCondition(customerExprs, EntityOperator.AND);
		List<GenericValue> customerContactList = null;
		List<HashMap<String, Object>> customerDetails = new ArrayList<HashMap<String,Object>>();
		HashMap<String, Object> customerAddressMap = new HashMap<String,Object>();
		HashMap<String, Object> defCustomerAddressMap = new HashMap<String,Object>();
		defCustomerAddressMap.put("contactNumber", "Select Mobile Number");
		customerDetails.add(defCustomerAddressMap);
		try {
			if (UtilValidate.isEmpty(phoneNumber)) {
				customerContactList = EntityQuery.use(delegator).from(entityName).
						where(EntityCondition.makeCondition("roleTypeId",EntityOperator.EQUALS, "CUSTOMER"))
						.orderBy("lastModifiedDate").queryList();
			} else {
				customerContactList = EntityQuery.use(delegator).from(entityName).where(mainCond)
						.orderBy("lastModifiedDate").queryList();
			}
			if(UtilValidate.isNotEmpty(customerContactList)) {
				
				for (GenericValue customerListEach : customerContactList) {
					
					partyId = customerListEach.getString("partyId");
					customerAddressMap = (HashMap<String, Object>) customerListEach.getAllFields();
					GenericValue postalAddress = EntityQuery.use(delegator).from("PartyAndPostalAddress").
							where("partyId",partyId).orderBy("lastUpdatedStamp").queryFirst();
					if(UtilValidate.isNotEmpty(postalAddress)) {
						customerAddressMap.put("address1",postalAddress.getString("address1"));
						customerAddressMap.put("city",postalAddress.getString("city"));
						customerAddressMap.put("postalCode",postalAddress.getString("postalCode"));
						customerAddressMap.put("stateProvinceGeoId",postalAddress.getString("stateProvinceGeoId"));
					} else {
						customerAddressMap.put("address1","");
						customerAddressMap.put("city","");
						customerAddressMap.put("postalCode","");
						customerAddressMap.put("stateProvinceGeoId","");
					}
					
					//Get customer MobileNo by customerId
					if(UtilValidate.isNotEmpty(partyId)) {
						customerMobileNo = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("partyId", partyId).
		        				orderBy("contactNumber").queryFirst().getString("contactNumber");
		        	}
					//Get customer previous bill details
		        	posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("customerMobileNo", customerMobileNo,
		        			"posStatus", "COMPLETE").orderBy("-createdStamp").queryFirst();
		        	if(UtilValidate.isNotEmpty(posCartTxn)) {
		        		receiptId = posCartTxn.getString("receiptId");
			        	billAmt = posCartTxn.getBigDecimal("totalBillAmount");
			        	billAmt = billAmt.setScale(0, BigDecimal.ROUND_HALF_UP);
			        	billAmt = billAmt.setScale(2, BigDecimal.ROUND_HALF_UP);
			        	
			        	customerAddressMap.put("billAmt", billAmt);
			        	//Get Customer Bill return Amt
			        	billReturnTxn = EntityQuery.use(delegator).from("PosCredit").where("billId", receiptId,"retailer", Long.parseLong("1")).queryList();
			        	if(UtilValidate.isNotEmpty(billReturnTxn)) {
			        		for (GenericValue billReturnEach : billReturnTxn) {
			        			if(UtilValidate.isNotEmpty(billReturnEach.getBigDecimal("creditNoteAmount"))) {
			        				creditNoteAmount = billReturnEach.getBigDecimal("creditNoteAmount");
			        				billReturnAmt = billReturnAmt.add(creditNoteAmount);
			        			}
			        		}
			        		customerAddressMap.put("billReturnAmt", billReturnAmt);
			        	}else {
			        		customerAddressMap.put("billReturnAmt", billReturnAmt);
			        	}
		        	}else {
		        		customerAddressMap.put("billAmt", BigDecimal.ZERO);
		        		customerAddressMap.put("billReturnAmt", BigDecimal.ZERO);
		        	}
					customerDetails.add(customerAddressMap);
				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("customerList", customerDetails);
		return result;
	}
	
	public static Map<String, Object> findPosCustomers(DispatchContext dctx,
			Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String phoneNumber = (String) context.get("phoneNumber");

		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		List<EntityCondition> customerExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		EntityCondition  contactNumCond = null;
		String entityName = "PartyAndTelecomNumberAndPerson";
		String partyId = "";
		String receiptId = "", customerMobileNo = "";
		BigDecimal billAmt = BigDecimal.ZERO;
        BigDecimal billReturnAmt = BigDecimal.ZERO;
        BigDecimal creditNoteAmount = BigDecimal.ZERO;
        GenericValue posCartTxn = new GenericValue();
        List<GenericValue> billReturnTxn = new LinkedList<GenericValue>();
        System.out.println("came into pos customers------------------------------#"+phoneNumber);
		// search by product name
		if (UtilValidate.isNotEmpty(phoneNumber)) {
			phoneNumber = phoneNumber.trim();
			andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE,
					"%" + phoneNumber + "%"));
			andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE,
					phoneNumber + "%"));
			andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.LIKE,
					"%" + phoneNumber));
			contactNumCond = EntityCondition.makeCondition(andExprs, EntityOperator.OR);
			customerExprs.add(contactNumCond);
		}
		customerExprs.add(EntityCondition.makeCondition("roleTypeId",EntityOperator.EQUALS, "CUSTOMER"));
		
		mainCond = EntityCondition.makeCondition(customerExprs, EntityOperator.AND);
		List<GenericValue> customerContactList = null;
		List<HashMap<String, Object>> customerDetails = new ArrayList<HashMap<String,Object>>();
		HashMap<String, Object> defCustomerAddressMap = new HashMap<String,Object>();
		defCustomerAddressMap.put("contactNumber", "Select Mobile Number");
		customerDetails.add(defCustomerAddressMap);
		try {
			if (UtilValidate.isEmpty(phoneNumber)) {
				customerContactList = EntityQuery.use(delegator).from(entityName).
						where(EntityCondition.makeCondition("roleTypeId",EntityOperator.EQUALS, "CUSTOMER"))
						.orderBy("lastModifiedDate").queryList();
			} else {
				customerContactList = EntityQuery.use(delegator).from(entityName).where(mainCond)
						.orderBy("lastModifiedDate").queryList();
			}
			if(UtilValidate.isNotEmpty(customerContactList)) {
				System.out.println("came into pos customerContactList------------------------------#");
				for (GenericValue customerListEach : customerContactList) {
					HashMap<String, Object> customerAddressMap = new HashMap<String,Object>();
					partyId = customerListEach.getString("partyId");
					//customerAddressMap = (HashMap<String, Object>) customerListEach.getAllFields();
					customerAddressMap.put("firstName", customerListEach.getString("firstName"));
					if(UtilValidate.isEmpty(customerListEach.getString("contactNumber"))) {
						customerAddressMap.put("contactNumber", "111");
					} else {
						customerAddressMap.put("contactNumber", customerListEach.getString("contactNumber"));
					}
					GenericValue postalAddress = EntityQuery.use(delegator).from("PartyAndPostalAddress").
							where("partyId",partyId).orderBy("lastUpdatedStamp").queryFirst();
					if(UtilValidate.isNotEmpty(postalAddress)) {
						customerAddressMap.put("partyId",postalAddress.getString("partyId"));
						customerAddressMap.put("address1",postalAddress.getString("address1"));
						customerAddressMap.put("city",postalAddress.getString("city"));
						customerAddressMap.put("postalCode",postalAddress.getString("postalCode"));
						customerAddressMap.put("stateProvinceGeoId",postalAddress.getString("stateProvinceGeoId"));
					} else {
						customerAddressMap.put("partyId","");
						customerAddressMap.put("address1","");
						customerAddressMap.put("city","");
						customerAddressMap.put("postalCode","");
						customerAddressMap.put("stateProvinceGeoId","");
					}
					//Get customer MobileNo by customerId
					if(UtilValidate.isNotEmpty(partyId)) {
						customerMobileNo = EntityQuery.use(delegator).from("PartyAndTelecomNumber").where("partyId", partyId).
		        				orderBy("contactNumber").queryFirst().getString("contactNumber");
		        	}
					System.out.println("came into pos partyId------------------------------#"+partyId);
					System.out.println("came into pos customerMobileNo------------------------------#"+customerMobileNo);
					//Get customer previous bill details
		        	posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("customerMobileNo", customerMobileNo,
		        			"posStatus", "COMPLETE").orderBy("-createdStamp").queryFirst();
		        	if(UtilValidate.isNotEmpty(posCartTxn)) {
		        		receiptId = posCartTxn.getString("receiptId");
			        	billAmt = posCartTxn.getBigDecimal("totalBillAmount");
			        	billAmt = billAmt.setScale(0, BigDecimal.ROUND_HALF_UP);
			        	billAmt = billAmt.setScale(2, BigDecimal.ROUND_HALF_UP);
			        	System.out.println("billAmt----###########################################"+billAmt);
			        	customerAddressMap.put("billAmt", billAmt);
			        	//Get Customer Bill return Amt
			        	billReturnTxn = EntityQuery.use(delegator).from("PosCredit").where("billId", receiptId,"retailer", Long.parseLong("1")).queryList();
			        	if(UtilValidate.isNotEmpty(billReturnTxn)) {
			        		for (GenericValue billReturnEach : billReturnTxn) {
			        			if(UtilValidate.isNotEmpty(billReturnEach.getBigDecimal("creditNoteAmount"))) {
			        				creditNoteAmount = billReturnEach.getBigDecimal("creditNoteAmount");
			        				billReturnAmt = billReturnAmt.add(creditNoteAmount);
			        			}
			        		}
			        		customerAddressMap.put("billReturnAmt", billReturnAmt);
			        	}else {
			        		customerAddressMap.put("billReturnAmt", billReturnAmt);
			        	}
		        	}else {
		        		customerAddressMap.put("billAmt", BigDecimal.ZERO);
		        		customerAddressMap.put("billReturnAmt", BigDecimal.ZERO);
		        	}
					customerDetails.add(customerAddressMap);
				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("customerList", customerDetails);
		return result;
	}
	
	public static Map<String, Object> findProductStoreFacility(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByPrimaryStoreGroupId = (String) context.get("searchByPrimaryStoreGroupId");

		String searchByProductStoreId = (String) context.get("searchByProductStoreId");
		String searchByStoreName = (String) context.get("searchByStoreName");
		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "ProductStoreAndFacility";

		// search by product name
		if (UtilValidate.isNotEmpty(searchByProductStoreId)) {
			searchByProductStoreId = searchByProductStoreId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productStoreId"),
					EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByProductStoreId + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByStoreName)) {
			searchByStoreName = searchByStoreName.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("storeName"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByStoreName + "%")));
		}

		// search by good identification
		if (UtilValidate.isNotEmpty(searchByPrimaryStoreGroupId)) {

			searchByPrimaryStoreGroupId = searchByPrimaryStoreGroupId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("primaryStoreGroupId"),
					EntityOperator.EQUALS, searchByPrimaryStoreGroupId));

		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> productStores = null;
		try {
			productStores = EntityQuery.use(delegator).from(entityName).where(mainCond)
					.orderBy("primaryStoreGroupId", "storeName").queryList();
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("productStoreList", productStores);
		return result;
	}
	
	public static Map<String, Object> findInventoryUniqueItem(DispatchContext dctx, 
			Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		String searchByInventoryItemId = (String) context.get("searchByInventoryItemId");
		String searchByProductId = (String) context.get("searchByProductId");
		String searchByPartyId = (String) context.get("searchByPartyId");
		String searchByOwnerPartyId = (String) context.get("searchByOwnerPartyId");
		String searchByFacilityId = (String) context.get("searchByFacilityId");
		Map<String, Object> result = ServiceUtil.returnSuccess();

		List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
		EntityCondition mainCond = null;
		String entityName = "InventoryItem";
		List<String> productIds = new ArrayList<String>();

		// search by product name
		if (UtilValidate.isNotEmpty(searchByProductId)) {
			searchByProductId = searchByProductId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productId"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByProductId + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByPartyId)) {
			searchByPartyId = searchByPartyId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("partyId"), EntityOperator.LIKE,
					EntityFunction.UPPER("%" + searchByPartyId + "%")));
		}

		if (UtilValidate.isNotEmpty(searchByOwnerPartyId)) {

			searchByOwnerPartyId = searchByOwnerPartyId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("ownerPartyId"),
					EntityOperator.EQUALS, searchByOwnerPartyId));

		}

		if (UtilValidate.isNotEmpty(searchByFacilityId)) {

			searchByFacilityId = searchByFacilityId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("facilityId"), EntityOperator.EQUALS,
					searchByFacilityId));

		}

		// search by good identification
		if (UtilValidate.isNotEmpty(searchByInventoryItemId)) {

			searchByInventoryItemId = searchByInventoryItemId.toUpperCase().trim();
			andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("inventoryItemId"),
					EntityOperator.EQUALS, searchByInventoryItemId));

		}
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		List<GenericValue> inventoryItems = new ArrayList<GenericValue>();
		List<GenericValue> productList = new ArrayList<GenericValue>();
		GenericValue productPriceList = new GenericValue();
		GenericValue productEANList = new GenericValue();
		HashMap<String, Object> productFieldMap = new HashMap<String, Object>();
		List<HashMap<String, Object>> productFinalFieldMap = new ArrayList<HashMap<String,Object>>();
		String productFieldId = "";
		try {
			inventoryItems = EntityQuery.use(delegator).from(entityName).where(mainCond)
					.orderBy("productId").queryList();
			
			if (UtilValidate.isNotEmpty(inventoryItems)) {
				productIds = EntityUtil.getFieldListFromEntityList(inventoryItems, "productId", true);
				//get product list
				EntityCondition productCond = EntityCondition.makeCondition("productId",
						EntityOperator.IN,productIds);
				productList = EntityQuery.use(delegator).from("Product").where(productCond)
						.orderBy("productId").queryList();
				if(UtilValidate.isNotEmpty(productList)) {
					for (GenericValue productsEach : productList) {
						productFieldId = productsEach.getString("productId");
						productFieldMap = (HashMap<String, Object>) productsEach.getAllFields();
						//get product mrp
						productPriceList = EntityQuery.use(delegator).from("ProductPrice").
								where("productId",productFieldId, "productPriceTypeId", "DEFAULT_PRICE").
								orderBy("createdDate").queryFirst();
						
						productFieldMap.put("mrp", productPriceList.getString("price"));
						//get product EAN code.
						productEANList = EntityQuery.use(delegator).from("GoodIdentification").
								where("productId",productFieldId, "goodIdentificationTypeId", "EAN").
								orderBy("createdStamp").queryFirst();
						
						productFieldMap.put("eanCode", productEANList.getString("idValue"));
						productFinalFieldMap.add(productFieldMap);
					}
				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		result.put("inventoryItemList", productFinalFieldMap);
		return result;
	}
	
	public static Map<String, Object> findProductNameAndBarcode(DispatchContext dctx, Map<String, ? extends Object> context) {
		Delegator delegator = dctx.getDelegator();
		LocalDispatcher dispatcher = dctx.getDispatcher();
		String productStoreId = (String) context.get("productStoreId"); 
		String barcodeNo = (String) context.get("barcodeNo");
		String productName = (String) context.get("productName");
		Map<String, Object> result = ServiceUtil.returnSuccess();
		String productId = "", facilityId = "", contactMechId = "";
		String barProductName = "";
		List<HashMap<String, Object>> barcodeDetails = new ArrayList<HashMap<String,Object>>();
		BigDecimal productMrp = BigDecimal.ZERO;
		try {
			List<EntityCondition> andExprs = new ArrayList<EntityCondition>();
			List<GenericValue> productEANList = new ArrayList<GenericValue>();
			EntityCondition barCodeCond = null;
			EntityCondition finalBarCodeCond = null;
			List<EntityCondition> barCodeExprs = new ArrayList<EntityCondition>();
			GenericValue postalAddress = new GenericValue();
			//get list of distinct Barcodes
			if (UtilValidate.isNotEmpty(barcodeNo)) {
				barcodeNo = barcodeNo.trim();
				andExprs.add(EntityCondition.makeCondition("idValue", EntityOperator.LIKE, "%" + barcodeNo + "%"));
				andExprs.add(EntityCondition.makeCondition("idValue", EntityOperator.LIKE, barcodeNo + "%"));
				andExprs.add(EntityCondition.makeCondition("idValue", EntityOperator.LIKE, "%" + barcodeNo));
				barCodeCond = EntityCondition.makeCondition(andExprs, EntityOperator.OR);
				barCodeExprs.add(barCodeCond);
			}
			barCodeExprs.add(EntityCondition.makeCondition("goodIdentificationTypeId",EntityOperator.EQUALS,"EAN"));
			finalBarCodeCond = EntityCondition.makeCondition(barCodeExprs, EntityOperator.OR);
			if (UtilValidate.isEmpty(barcodeNo)) {
				productEANList = EntityQuery.use(delegator).from("GoodIdentification").
						where("goodIdentificationTypeId", "EAN").orderBy("createdStamp").queryList();
			} else {
				productEANList = EntityQuery.use(delegator).from("GoodIdentification").
						where(finalBarCodeCond).orderBy("createdStamp").queryList();
			}
			if(UtilValidate.isNotEmpty(productStoreId)) {
				GenericValue facilityVal = EntityQuery.use(delegator).from("ProductStoreFacility").where("productStoreId", productStoreId).queryFirst();
				if(UtilValidate.isNotEmpty(facilityVal)) {
					facilityId = facilityVal.getString("facilityId");
					List<GenericValue> facilityContactMechs = EntityQuery.use(delegator).from("FacilityAndContactMech").
							where("facilityId", facilityId, "contactMechTypeId", "POSTAL_ADDRESS").queryList();
		        	if(UtilValidate.isNotEmpty(facilityContactMechs)) {
		        		contactMechId = facilityContactMechs.get(0).getString("contactMechId");
		        		postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", contactMechId).queryOne();
		        	}
				}
			}
			int i = 0;
			for (GenericValue barCodeRow : productEANList) {
				BigDecimal taxAmount = BigDecimal.ZERO;
				List<BigDecimal> amount = new ArrayList<BigDecimal>();
				List<BigDecimal> price = new ArrayList<BigDecimal>();
				HashMap<String, Object> barcodeAddressMap = new HashMap<String,Object>();
				//get product details
				productId = barCodeRow.getString("productId");
				barcodeAddressMap.put("barcode", barCodeRow.getString("idValue"));
				List<GenericValue> productList = EntityQuery.use(delegator).from("Product").where("productId",productId).queryList();
				barProductName = productList.get(0).getString("productName");
				//get product price
				List<GenericValue> productPriceList = EntityQuery.use(delegator).from("ProductPrice").where("productId",productId,
														"productPriceTypeId", "DEFAULT_PRICE").queryList();
				amount.add(i, productPriceList.get(0).getBigDecimal("price"));
				price.add(i, productPriceList.get(0).getBigDecimal("price"));
				List<GenericValue> productMrpList = EntityQuery.use(delegator).from("ProductPrice").where("productId",productId,
						"productPriceTypeId", "LIST_PRICE").queryList();
				barcodeAddressMap.put("productId", productId);
				barcodeAddressMap.put("productName", barProductName);
				barcodeAddressMap.put("price", productPriceList.get(0).getBigDecimal("price"));
				//barcodeAddressMap.put("mrp", productMrpList.get(0).getBigDecimal("price"));
				barcodeAddressMap.put("amount", productPriceList.get(0).getBigDecimal("price"));
				//productMrp = productPriceList.get(0).getBigDecimal("price");
				if(UtilValidate.isNotEmpty(productMrpList)) {
					productMrp = productMrpList.get(0).getBigDecimal("price");
				}
				Map<String, Object> taxMap = dispatcher.runSync("calcTax", UtilMisc.toMap("itemAmountList", amount,
												"itemPriceList", price, "itemProductList", productList,"productStoreId",productStoreId,
												"shippingAddress",postalAddress,"taxCategory", "GST"));
				if(ServiceUtil.isSuccess(taxMap)) {
					List<List<GenericValue>> itemAdj = UtilGenerics.checkList(taxMap.get("itemAdjustments"));
					if (UtilValidate.isNotEmpty(itemAdj)) {
                        for (int j = 0; j < itemAdj.size(); j++) {
                            List<GenericValue> itemAdjustments = itemAdj.get(j);
                            for (GenericValue ia : itemAdjustments) {
                                if (ia.get("amount") != null) {
                                	taxAmount = taxAmount.add(ia.getBigDecimal("amount"));
                                }
                            }
                        }
                    }
				}
				//productMrp = productMrp.add(taxAmount);
				barcodeAddressMap.put("mrp", productMrp);
				barcodeDetails.add(barcodeAddressMap);
				//i++;
			}
			//get list of didtinct product names
		} catch (GenericEntityException | GenericServiceException e) {
			
		}
		//result.put("productNameList", productFinalFieldMap);
		result.put("barcodeList", barcodeDetails);
		return result;
	}
}
