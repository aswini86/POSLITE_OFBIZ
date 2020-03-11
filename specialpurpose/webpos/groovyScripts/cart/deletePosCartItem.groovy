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
if (webPosSession) {
	println("came into web pos delete cart item--------&&&&&&&&&&&&&&&&&&&&_----------------");
	shoppingCart = webPosSession.getCart();
	String receiptId = request.getParameter("receiptId");
	String productId = request.getParameter("delete_product_id");
	String posTerminalId = request.getParameter("posTerminalId");
	String dayId = request.getParameter("dayId");
	String username = request.getParameter("USERNAME")
	String customerShipState = request.getParameter("customerShipState")
	String facilityFromState = ""
	String posQuantity = "";
	int qty = 0;
	int totQty = 0
	String updatedQty = ""
	String addCartMessage = ""
	String quantity = ""
	BigDecimal displayGrandTotal = BigDecimal.ZERO
	BigDecimal balanceAmount = BigDecimal.ZERO
	BigDecimal paidAmt = BigDecimal.ZERO
	BigDecimal returnAmount = BigDecimal.ZERO
	//Get the Pos Cart Item List
	if (UtilValidate.isNotEmpty(receiptId)) {
		
		try {
			//Check the value exists or not
			posCartItemData = EntityQuery.use(delegator).from("PosCartItem").
							where("receiptId", receiptId, "productId",productId, "dayId", dayId).queryOne();
			//if value exists Delete the value in pos cart item
			if (UtilValidate.isNotEmpty(posCartItemData)) {
				posCartItemData.remove();
			}else {//else save the values in pos cart item
				request.setAttribute("errorMessage", "Error while Deleting Pos cart item")
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, "Error while updating Pos Cart Item", module);
		}
	}
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
		}
		//Update PosCart transactions
		posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
		if(UtilValidate.isNotEmpty(posCartTxnData)) {
			displayGrandTotal = shoppingCart.getDisplayGrandTotal()
			
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
			paidAmt = posCartTxnData.getBigDecimal("paidAmount")
			
			if (paidAmt.compareTo(balanceAmount) == 1) {//check if paid amt is more than Balance Amt
				returnAmount = paidAmt.subtract(displayGrandTotal)
				request.setAttribute("returnAmount", returnAmount)
			}else {
				request.setAttribute("returnAmount", BigDecimal.ZERO)
			}
		}
	} else {
		posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
		if(UtilValidate.isNotEmpty(posCartTxnData)) {
			paidAmt = posCartTxnData.getBigDecimal("paidAmount")
			posCartTxnData.set("totalBillAmount", BigDecimal.ZERO)
			posCartTxnData.set("balanceAmount", BigDecimal.ZERO)
			posCartTxnData.set("discount", BigDecimal.ZERO)
			posCartTxnData.set("charges", BigDecimal.ZERO)
			posCartTxnData.set("isChargePercentage", "Y")
			posCartTxnData.set("isPercentage", "Y")
			
			posCartTxnData.store()
			request.setAttribute("returnAmount", paidAmt)
			request.setAttribute("successMessage", "PosCartItem Deleted Successfully")
		}else {
			request.setAttribute("successMessage", "PosCartItem Deleted Successfully")
		}
	}
} else {
	shoppingCart = null;
}

if (shoppingCart) {
	
	return "Success"
} else {
	context.shoppingCartSize = 0;
}



