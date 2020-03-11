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

import java.net.Authenticator.RequestorType
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
result = [:]
println("came into cart items--&&&&&&-------------")
if (webPosSession) {
	shoppingCart = webPosSession.getCart();
	String receiptId = request.getParameter("receiptId");
	String productId = request.getParameter("add_product_id");
	String quantity = request.getParameter("quantity");
	String posTerminalId = request.getParameter("posTerminalId");
	String dayId = request.getParameter("dayId");
	String username = request.getParameter("USERNAME")
	String customerShipState = request.getParameter("customerShipState")
	String customerMobileNo = request.getParameter("customerMobileNo")
	println("receiptId--&&&&&&-------------${receiptId}")
	println("productId--&&&&&&-------------${productId}")
	println("quantity--&&&&&&-------------${quantity}")
	println("posTerminalId--&&&&&&-------------${posTerminalId}")
	println("dayId--&&&&&&-------------${dayId}")
	String facilityFromState = ""
	String posQuantity = "";
	int qty = 0;
	int totQty = 0
	String updatedQty = ""
	String addCartMessage = ""
	BigDecimal displayGrandTotal = BigDecimal.ZERO
	BigDecimal balanceAmount = BigDecimal.ZERO
	BigDecimal paidAmt = BigDecimal.ZERO
	if (UtilValidate.isNotEmpty(receiptId)) {
		println("came into Add cart item-------------")
		try {
			//Check the PosCartTransaction exists or not
			posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").
								where("receiptId", receiptId).queryOne();
			if(UtilValidate.isEmpty(posCartTxnData)) {
				//Create PosCart Transaction
				posCartTxn = delegator.makeValue("PosCartTransaction", UtilMisc.toMap("receiptId", receiptId,
													"dayId", dayId, "terminalId", posTerminalId,
													"posStatus", "CREATED","customerMobileNo", customerMobileNo));
				posCartTxn.create()
			}
			
			//Check the Pos Cart Item value exists or not
			posCartItemData = EntityQuery.use(delegator).from("PosCartItem").
							where("receiptId", receiptId, "productId",productId, "dayId", dayId).queryOne();
			//if value exists update the value in pos cart item
			if (UtilValidate.isNotEmpty(posCartItemData)) {
				posQuantity = posCartItemData.getString("quantity");
				totQty = Integer.parseInt(posQuantity) + Integer.parseInt(quantity)
				updatedQty = Integer.toString(totQty)
				
				posCartItemData.set("quantity", updatedQty)
				println("posCartItemData----------${posCartItemData}");
				posCartItemData.store()
			}else {//else save the values in pos cart item
				if (UtilValidate.isNotEmpty(receiptId)) {
					
					posCartItem = delegator.makeValue("PosCartItem", UtilMisc.toMap("receiptId", receiptId,
						"productId", productId, "dayId", dayId, "quantity",quantity,
						"terminalId",posTerminalId, "posStatus", "CREATED"));
					posCartItem.create()
				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, "Error while updating Pos Cart Item", module);
		}
	}
	//Get the Pos Cart Item List
	try {
		posCartItemList = EntityQuery.use(delegator).from("PosCartItem")
		.where(EntityCondition.makeCondition("receiptId", EntityOperator.EQUALS, receiptId)).queryList();
	} catch (GenericEntityException e) {
		
	}
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
			println("partyId-------------${partyId}");
			//get party facility
			partyFacilityList = EntityQuery.use(delegator).from("FacilityParty")
							.where("partyId", partyId).queryList();
			//get facility postal contact mech id
			if(UtilValidate.isNotEmpty(partyFacilityList)) {
				facilityId = partyFacilityList.get(0).getString("facilityId");
				facilityContactList = EntityQuery.use(delegator).from("FacilityAndContactMech")
				.where("facilityId", facilityId).queryList();
				contactMechId = facilityContactList.get(0).getString("contactMechId");
				println("contactMechId-------------${contactMechId}");
			}
			shipAddress = EntityQuery.use(delegator).from("PostalAddress")
							.where(EntityCondition.makeCondition("contactMechId", EntityOperator.EQUALS, contactMechId)).queryOne();
			if(UtilValidate.isNotEmpty(shipAddress)) {
				facilityFromState = shipAddress.getString("stateProvinceGeoId");
			}
			CheckOutHelper coh = new CheckOutHelper(dispatcher, delegator, shoppingCart);
			println("facilityFromState-----&&&&&&&&&&&&&&&--------------${facilityFromState}");
			println("customerShipState-----&&&&&&&&&&&&&&&--------------${customerShipState}");
			if (facilityFromState != customerShipState) {
				//Need to add IGST tax logic.
				println("came into calc IGST-----&&&&&&&&&&&&&&&--------------");
				coh.calcAndAddTax(shipAddress,"IGST")
			} else {
				coh.calcAndAddTax(shipAddress)
			}
			
			ShoppingCartEvents.ShowPosCart(request,response)
			addCartMessage = (String) request.getAttribute("_ERROR_MESSAGE_");
			println("posmessage-------&&--------------${addCartMessage}");
			if(UtilValidate.isNotEmpty(addCartMessage)) {
				toDeletePosCartItemList = EntityQuery.use(delegator).from("PosCartItem")
				.where("receiptId", receiptId, "dayId", dayId, "productId",request.getParameter("add_product_id")).queryList();
				
				println("receiptId-------&&--------------${receiptId}");
				println("dayId-------&&--------------${dayId}");
				println("productId-------&&--------------${productId}");
				println("toDeletePosCartItemList-------&&--------------${toDeletePosCartItemList}");
				
				if (UtilValidate.isNotEmpty(toDeletePosCartItemList)) {
					for (GenericValue dposCartItemEach : toDeletePosCartItemList) {
						dposCartItemEach.remove();
					}
				}
			}
		}
		//Update PosCart transactions
		posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
		if(UtilValidate.isNotEmpty(posCartTxnData)) {
			//displayGrandTotal = shoppingCart.getDisplayGrandTotal()
			displayGrandTotal = shoppingCart.getSellingPriceGrandTotal()
			println("new-displayGrandTotal-------&&--------------${displayGrandTotal}")
			if(UtilValidate.isEmpty(posCartTxnData.getBigDecimal("paidAmount"))) {
				balanceAmount = displayGrandTotal.subtract(BigDecimal.ZERO)
			} else {
				paidAmt = posCartTxnData.getBigDecimal("paidAmount")
				balanceAmount = displayGrandTotal.subtract(paidAmt)
			}
			println("displayGrandTotal-------&&--------------${displayGrandTotal}")
			posCartTxnData.set("paidAmount", paidAmt)
			posCartTxnData.set("balanceAmount", balanceAmount)
			posCartTxnData.set("totalBillAmount", displayGrandTotal)
			posCartTxnData.store()
			request.setAttribute("paidAmount", paidAmt)
			request.setAttribute("balanceAmount", balanceAmount)
			request.setAttribute("totalBillAmount", displayGrandTotal)
			//Calculate return Amt
			if (paidAmt.compareTo(displayGrandTotal) == 1) {
				returnAmount = paidAmt.subtract(displayGrandTotal)
				request.setAttribute("returnAmount", returnAmount)
			} else {
				request.setAttribute("returnAmount", BigDecimal.ZERO)
			}
			result.put("paidAmount", paidAmt)
			result.put("balanceAmount", paidAmt)
			result.put("totalBillAmount", displayGrandTotal)
		}
	}
} else {
	shoppingCart = null;
}
println("result-------&&--------------${result}")
return result
if (shoppingCart) {
	//return "Success"
	
} else {
	context.shoppingCartSize = 0;
}



