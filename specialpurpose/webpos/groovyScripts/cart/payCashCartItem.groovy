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
import org.apache.ofbiz.webpos.transaction.WebPosTransaction
import org.apache.ofbiz.order.shoppingcart.CheckOutHelper

HttpSession session = request.getSession(true);
session.removeAttribute("shoppingCart");
session.removeAttribute("webPosSession");

webPosSession = WebPosEvents.getWebPosSession(request, null);
List<GenericValue> posCartItemList = new ArrayList<GenericValue>();
GenericValue posCartItemData = new GenericValue();
List<Map<String, Object>> cartItems = new ArrayList<Map<String, Object>>();
if (webPosSession) {
	println("came into web pos add cart item--------&&&&&&&&&&&&&&&&&&&&_----------------");
	shoppingCart = webPosSession.getCart();
	String receiptId = request.getParameter("receiptId");
	String posTerminalId = request.getParameter("posTerminalId");
	String dayId = request.getParameter("dayId");
	String amountCash = request.getParameter("amountCash");
	BigDecimal cashToPay = new BigDecimal(amountCash);
	String customerShipState = request.getParameter("customerShipState")
	String username = request.getParameter("USERNAME")
	String refNum = ""
	String authCode = ""
	String totalDue = ""
	String facilityFromState = ""
	BigDecimal amount = BigDecimal.ZERO;
	if (UtilValidate.isNotEmpty(receiptId) && UtilValidate.isNotEmpty(amountCash)) {
		
		try {
			//Check the value exists or not
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
					
					//ShoppingCartEvents.ShowPosCart(request,response)
				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, "Error while updating Pos Cart Item", module);
		}
		
		try {
			//
			WebPosTransaction wpt = new WebPosTransaction(webPosSession);
			wpt.addPayment("CASH", cashToPay, refNum, authCode)
			
			totalDue = webPosSession.getCurrentTransaction().getTotalDue();
			request.setAttribute("totalDue", totalDue)
			if (shoppingCart) {
				shoppingCartSize = shoppingCart.size();
				println("shoppingcart size------------${shoppingCartSize}")
				payments = shoppingCart.selectedPayments();
				println("payments------------${payments}")
				for (i = 0; i < payments; i++) {
					paymentInfo = shoppingCart.getPaymentInfo(i);
					println("paymentInfo---------${paymentInfo}")
					if (paymentInfo.amount != null) {
						amount = paymentInfo.amount;
						println("amount---------${amount}")
					}
				}
			}
			request.setAttribute("cashAmount", amount)
			request.setAttribute("successMsg", "Cash Payment done Successfully")
		} catch (GenericEntityException e) {
			Debug.logError(e, "Error while Paying Pos Cart Item", module);
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



