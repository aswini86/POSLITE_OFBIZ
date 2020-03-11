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

HttpSession session = request.getSession(true);
session.removeAttribute("shoppingCart");
session.removeAttribute("webPosSession");

webPosSession = WebPosEvents.getWebPosSession(request, null);
List<GenericValue> posCartItemList = new ArrayList<GenericValue>();
GenericValue posCartItemData = new GenericValue();
List<GenericValue> posCartReceivePaymentList = new ArrayList<GenericValue>();
BigDecimal paidAmt = BigDecimal.ZERO
BigDecimal receivedPayment = BigDecimal.ZERO
BigDecimal posPaidAmount = BigDecimal.ZERO
BigDecimal balAmt = BigDecimal.ZERO

BigDecimal revertPaidAmount = BigDecimal.ZERO
BigDecimal revertBalAmt = BigDecimal.ZERO
String paymentType = ""
String creditId = ""
List<Map<String, Object>> cartItems = new ArrayList<Map<String, Object>>();
if (webPosSession) {
	String receiptId = request.getParameter("receiptId");
	String dayId = request.getParameter("dayId");
	println("came into void bill-------&&&&&&&&&&&&&&&&&&--${receiptId}");
	if (UtilValidate.isNotEmpty(receiptId)) {
		try {
			posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").
							where("receiptId", receiptId).queryOne();
			if (UtilValidate.isNotEmpty(posCartTxnData)) {
				paidAmt = posCartTxnData.getBigDecimal("paidAmount")
				request.setAttribute("returnAmount", paidAmt)
				posCartTxnData.remove()
			}
			println("came into void bill-------&&&&&&&&&&&&&&&&&&");
			posCartItemList = EntityQuery.use(delegator).from("PosCartItem")
					.where("receiptId", receiptId).queryList();
			
			if (UtilValidate.isNotEmpty(posCartItemList)) {
				for (GenericValue posCartItemEach : posCartItemList) {
					posCartItemEach.remove();
				}
			}
			posCartReceivePaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment")
					.where("receiptId", receiptId).queryList();
			
			if (UtilValidate.isNotEmpty(posCartReceivePaymentList)) {
				for (GenericValue posCartReceivePaymentEach : posCartReceivePaymentList) {
					//code for credit note
					paymentType = posCartReceivePaymentEach.getString("paymentType")
					if(paymentType.equals("CREDIT_NOTE")) {
						creditId = posCartReceivePaymentEach.getString("paymentTransaction")
						receivedPayment = posCartReceivePaymentEach.getBigDecimal("receivedPayment")
						//get credit Note Txn
						GenericValue posCredit = EntityQuery.use(delegator).from("PosCredit").where("creditId", creditId).queryOne()
						if(UtilValidate.isNotEmpty(posCredit)) {
							posPaidAmount = posCredit.getBigDecimal("paidAmount")
							balAmt = posCredit.getBigDecimal("balanceAmount")
							
							revertPaidAmount = posPaidAmount.subtract(receivedPayment)
							revertBalAmt = balAmt.add(receivedPayment)
							
							posCredit.put("paidAmount", revertPaidAmount)
							posCredit.put("balanceAmount", revertBalAmt)
							posCredit.put("statusId", "REDEEM")
							posCredit.store();
							
							receiptId = posCartReceivePaymentEach.getString("receiptId")
							//get credit Note Txn
							List<GenericValue> posCreditNoteVal = EntityQuery.use(delegator).from("PosCredit").where("billId", receiptId,
								"type", "CREDIT_NOTE").queryList()
							if(UtilValidate.isNotEmpty(posCreditNoteVal)) {
								for (GenericValue posCreditNoteEach : posCreditNoteVal) {
									posCreditNoteEach.remove();
								}
							}
							
						}
					}
					if(paymentType.equals("CREDIT")) {
						receiptId = posCartReceivePaymentEach.getString("receiptId")
						//get credit Note Txn
						List<GenericValue> posCreditVal = EntityQuery.use(delegator).from("PosCredit").where("billId", receiptId,
							"type", "CREDIT").queryList()
						if(UtilValidate.isNotEmpty(posCreditVal)) {
							for (GenericValue posCreditEach : posCreditVal) {
								posCreditEach.remove();
							}
						}
					}
					posCartReceivePaymentEach.remove();
				}
			}
					
			request.setAttribute("receiptId", receiptId)
			request.setAttribute("successMsg", "Bill void done successfully")
		} catch (GenericEntityException e) {
			
		}
	}
}

