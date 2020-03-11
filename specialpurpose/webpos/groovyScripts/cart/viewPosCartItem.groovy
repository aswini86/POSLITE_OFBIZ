/*
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
 */
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.webpos.WebPosEvents
import org.apache.ofbiz.webpos.session.WebPosSession
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem
import java.util.List
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import javax.servlet.http.HttpSession
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper

HttpSession session = request.getSession(true);
session.removeAttribute("shoppingCart");
session.removeAttribute("webPosSession");

webPosSession = WebPosEvents.getWebPosSession(request, null);
List<GenericValue> posCartItemList = new ArrayList<GenericValue>();
GenericValue posCartItemData = new GenericValue();
List<Map<String, Object>> cartItems = new ArrayList<Map<String, Object>>();
String facilityFromState = "";
if (webPosSession) {
	println("came into view pos cart item----------------------")
	shoppingCart = webPosSession.getCart();
	String receiptId = request.getParameter("receiptId");
	String dayId = request.getParameter("dayId");
	String username = request.getParameter("USERNAME")
	String customerShipState = request.getParameter("customerShipState")
	BigDecimal paidAmt = BigDecimal.ZERO
	BigDecimal balanceAmount = BigDecimal.ZERO
	BigDecimal discount = BigDecimal.ZERO
	BigDecimal charges = BigDecimal.ZERO
	BigDecimal addlDiscAmt = BigDecimal.ZERO
	BigDecimal addlCharge = BigDecimal.ZERO
	BigDecimal percentage = new BigDecimal("0.01")
	String isPercentage = "N"
	String isChargePercentage = "N"
	println("receiptId---------------${receiptId}")
	println("dayId---------------${dayId}")
	if (UtilValidate.isNotEmpty(receiptId)) {
		try {
			//Get PosCart transaction details
			posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").
									where("receiptId", receiptId).queryOne();
			
			if (UtilValidate.isNotEmpty(posCartTxnData)) {
				if (UtilValidate.isNotEmpty(posCartTxnData.getString("customerMobileNo"))) {
					customerMobileNo = posCartTxnData.getString("customerMobileNo");
					customerContactList = EntityQuery.use(delegator).from("PartyAndTelecomNumberAndPerson").
					where("contactNumber",customerMobileNo, "roleTypeId", "CUSTOMER").orderBy("lastModifiedDate").queryList();
					if(UtilValidate.isNotEmpty(customerContactList)) {
						String partyId = customerContactList.get(0).getString("partyId");
						String firstName = customerContactList.get(0).getString("firstName");
						request.setAttribute("firstName", firstName)
						request.setAttribute("customerMobileNo", customerMobileNo)
						GenericValue postalAddress = EntityQuery.use(delegator).from("PartyAndPostalAddress").
						where("partyId",partyId).orderBy("lastUpdatedStamp").queryFirst();
						if(UtilValidate.isNotEmpty(postalAddress)) {
							customerShipState = postalAddress.getString("stateProvinceGeoId");
							request.setAttribute("address1", postalAddress.getString("address1"))
						}
						
					}
				}
				if (UtilValidate.isNotEmpty(posCartTxnData.getBigDecimal("discount"))) {
					discount = posCartTxnData.getBigDecimal("discount")
				}
				if (UtilValidate.isNotEmpty(posCartTxnData.getBigDecimal("charges"))) {
					charges = posCartTxnData.getBigDecimal("charges")
				}
				if(UtilValidate.isNotEmpty(posCartTxnData.getString("isPercentage"))) {
            		isPercentage = posCartTxnData.getString("isPercentage");
            	}
            	if(UtilValidate.isNotEmpty(posCartTxnData.getString("isChargePercentage"))) {
            		isChargePercentage = posCartTxnData.getString("isChargePercentage");
            	}
				customerShipState = "IN-MH";
				request.setAttribute("customerShipState", customerShipState)
				println("PosCartTransaction---------------${posCartTxnData.getBigDecimal("paidAmount")}")
				if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("paidAmount"))) {
					request.setAttribute("paidAmount", BigDecimal.ZERO)
				} else {
					request.setAttribute("paidAmount", posCartTxnData.getBigDecimal("paidAmount"))
				}
				if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("balanceAmount"))) {
					request.setAttribute("balanceAmount", BigDecimal.ZERO)
				} else {
					//check balance amt is in negative
					balanceAmount = posCartTxnData.getBigDecimal("balanceAmount");
					if(balanceAmount.compareTo(BigDecimal.ZERO) == -1) {
						request.setAttribute("balanceAmount", BigDecimal.ZERO)
					}else {
						if(balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
							//check for Addl discount
							/*if(isPercentage.equals("Y")) {
								addlDiscAmt = balanceAmount.multiply(discount).multiply(percentage)
							}else {
								addlDiscAmt = discount
							}
							//check for Charges
							if(isChargePercentage.equals("Y")) {
								addlCharge = balanceAmount.multiply(charges).multiply(percentage)
							}else {
								addlCharge = charges
							}
							if(addlDiscAmt.compareTo(BigDecimal.ZERO) > 0) {
								balanceAmount = balanceAmount.subtract(addlDiscAmt)
							}
							if(addlCharge.compareTo(BigDecimal.ZERO) > 0) {
								balanceAmount = balanceAmount.add(addlCharge)
							}*/
						}
						println("balanceAmount-------------777777777777777777------------${balanceAmount}");
						request.setAttribute("balanceAmount", balanceAmount)
					}
					
				}
				if(UtilValidate.isEmpty(posCartTxnData.getBigDecimal("balanceAmount")) && 
						(UtilValidate.isEmpty(posCartTxnData.getBigDecimal("paidAmount")))){
					
					paidAmt = posCartTxnData.getBigDecimal("paidAmount")
					request.setAttribute("returnAmount", paidAmt)
				} else if(paidAmt.compareTo(posCartTxnData.getBigDecimal("balanceAmount")) == 1){
					returnAmount = paidAmt.subtract(posCartTxnData.getBigDecimal("balanceAmount"))
					request.setAttribute("returnAmount", returnAmount)
				}else {
					request.setAttribute("returnAmount", BigDecimal.ZERO)
				}
				
			} else {
				request.setAttribute("paidAmount", BigDecimal.ZERO)
				request.setAttribute("balanceAmount", BigDecimal.ZERO)
			}
			println("came into view Pos bill-------&&&&&&&&&&&&&&&&&&");
			posCartItemList = EntityQuery.use(delegator).from("PosCartItem")
					.where("receiptId", receiptId, "dayId", dayId).queryList();
			
			if (UtilValidate.isNotEmpty(posCartItemList)) {
				for (GenericValue posCartItemEach : posCartItemList) {
					productId = posCartItemEach.getString("productId");
					quantity = posCartItemEach.getString("quantity");
					terminalId = posCartItemEach.getString("terminalId");
					println("add_product_id---each&&&&&&&&&7---"+productId);
					
					request.setAttribute("posTerminalId", terminalId)
					request.setAttribute("quantity", quantity)
					request.setAttribute("add_product_id", productId)
					
					ShoppingCartEvents.addToCart(request,response)
					
					//calc tax
					//get user partyId
					String contactMechId = "", facilityId = ""
					userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", username).queryOne();
					String partyId = userLogin != null ? userLogin.getString("partyId") : null;
					//get party facility
					partyFacilityList = EntityQuery.use(delegator).from("FacilityParty").where("partyId", partyId).queryList();
					//get facility postal contact mech id
					if(UtilValidate.isNotEmpty(partyFacilityList)) {
						facilityId = partyFacilityList.get(0).getString("facilityId");
						facilityContactList = EntityQuery.use(delegator).from("FacilityAndContactMech")
						.where("facilityId", facilityId).queryList();
						contactMechId = facilityContactList.get(0).getString("contactMechId");
						println("contactMechId-------------${contactMechId}");
					}
					shipAddress = EntityQuery.use(delegator).from("PostalAddress")
									.where(EntityCondition.makeCondition("contactMechId",
									EntityOperator.EQUALS, contactMechId)).queryOne();
					if(UtilValidate.isNotEmpty(shipAddress)) {
						facilityFromState = shipAddress.getString("stateProvinceGeoId");
					}
					CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, shoppingCart);
					if (facilityFromState != customerShipState) {
						//Need to add IGST tax logic.
						println("came into calc IGST-----&&&&&&&&&&&&&&&--------------");
						coh.calcAndAddTax(shipAddress,"IGST")
					} else {
						coh.calcAndAddTax(shipAddress)
					}
					
					ShoppingCartEvents.ShowPosCart(request,response)
				}
			}else {
				println("came into view Pos bill-------&&&&&&&&&&&&&&&&&&");
				request.setAttribute("successMsg", "Cart Item is Empty")
			}
		} catch (GenericEntityException e) {
			
		}
	}
}else {
	println("came into view Pos bill-------&&&&&&&&&&&&&&&&&&");
	request.setAttribute("successMsg", "No Session")
}