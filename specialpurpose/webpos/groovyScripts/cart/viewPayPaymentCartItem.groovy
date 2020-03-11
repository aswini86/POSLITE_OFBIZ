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
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.base.util.UtilMisc
import javax.servlet.http.HttpSession
import org.apache.ofbiz.webpos.transaction.WebPosTransaction
import org.apache.ofbiz.webpos.WebPosEvents
import org.apache.ofbiz.webpos.session.WebPosSession


String receiptId = request.getParameter("receiptId")
String dayId = request.getParameter("dayId")
String terminalId = request.getParameter("posTerminalId")
List<GenericValue> receivedPaymentList = new ArrayList<GenericValue>();

BigDecimal paidAmt = BigDecimal.ZERO
BigDecimal posCartPaidAmt = BigDecimal.ZERO
BigDecimal receivedPayment = BigDecimal.ZERO
BigDecimal totalBillAmount = BigDecimal.ZERO
BigDecimal posCartTxnPaidAmt = BigDecimal.ZERO
BigDecimal recPayment = BigDecimal.ZERO
BigDecimal remainingAmount = BigDecimal.ZERO

BigDecimal discount = BigDecimal.ZERO
BigDecimal charges = BigDecimal.ZERO
BigDecimal addlDiscAmt = BigDecimal.ZERO
BigDecimal addlCharge = BigDecimal.ZERO
BigDecimal percentage = new BigDecimal("0.01")
String isPercentage = "N"
String isChargePercentage = "N"

HttpSession session = request.getSession(true);
session.removeAttribute("shoppingCart");
session.removeAttribute("webPosSession");

webPosSession = WebPosEvents.getWebPosSession(request, null);
List<Map<String, Object>> paymentItems = new LinkedList<Map<String,Object>>();

if (webPosSession) {
	if (UtilValidate.isNotEmpty(receiptId)) {
		try {
			posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").
								where("receiptId", receiptId).queryOne();
			
			//Code added for addl disc
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
			//End of Code added for addl disc
			if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("totalBillAmount"))) {
				request.setAttribute("totalBillAmount", BigDecimal.ZERO)
			} else {
				request.setAttribute("totalBillAmount", posCartTxnData.getBigDecimal("totalBillAmount"))
				totalBillAmount = posCartTxnData.getBigDecimal("totalBillAmount")
				
				//check for Addl discount
				/*if(isPercentage.equals("Y")) {
					addlDiscAmt = totalBillAmount.multiply(discount).multiply(percentage)
				}else {
					addlDiscAmt = discount
				}
				//check for Charges
				if(isChargePercentage.equals("Y")) {
					addlCharge = totalBillAmount.multiply(charges).multiply(percentage)
				}else {
					addlCharge = charges
				}
				
				if(addlDiscAmt.compareTo(BigDecimal.ZERO) > 0) {
					totalBillAmount = totalBillAmount.subtract(addlDiscAmt)
				}
				if(addlCharge.compareTo(BigDecimal.ZERO) > 0) {
					totalBillAmount = totalBillAmount.add(addlCharge)
				}*/
			}
			if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("paidAmount"))) {
				request.setAttribute("paidAmount", BigDecimal.ZERO)
			} else {
				posCartTxnPaidAmt = receivedPayment.add(posCartTxnData.getBigDecimal("paidAmount"))
				
				posCartTxnData.set("paidAmount", posCartTxnPaidAmt)
				request.setAttribute("paidAmount", posCartTxnPaidAmt)
			}
			if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("balanceAmount"))) {
				request.setAttribute("balanceAmount", BigDecimal.ZERO)
			} else {
				txnBalanceAmt = posCartTxnData.getBigDecimal("totalBillAmount").subtract(posCartTxnPaidAmt)
				posCartTxnData.set("balanceAmount", txnBalanceAmt)
				
				request.setAttribute("balanceAmount", txnBalanceAmt)
			}
			if (posCartTxnPaidAmt.compareTo(posCartTxnData.getBigDecimal("totalBillAmount")) == 1) {//check if paid amt is more than Balance Amt
				returnAmount = posCartTxnPaidAmt.subtract(posCartTxnData.getBigDecimal("totalBillAmount"))
				request.setAttribute("returnAmount", returnAmount)
			}else {
				request.setAttribute("returnAmount", BigDecimal.ZERO)
			}
			
			if (UtilValidate.isNotEmpty(posCartTxnData)) {
				posCartPaidAmt = posCartTxnData.getBigDecimal("paidAmount")
				
			}

			//get paymentList
			receivedPayments = EntityQuery.use(delegator).from("PosCartReceivePayment").
						where("receiptId", receiptId).queryList()
			for (GenericValue row : receivedPayments) {
				if(UtilValidate.isNotEmpty(receivedPayments)) {
					Map paymentItemsMap = [:]
					recPayment = recPayment.add(row.getBigDecimal("receivedPayment"))
					remainingAmount = totalBillAmount.subtract(recPayment)
					
					paymentItemsMap.put("paymentType", row.getString("paymentType"))
					paymentItemsMap.put("receivedPayment", row.getBigDecimal("receivedPayment"))
					paymentItemsMap.put("remainingAmount", remainingAmount)
					paymentItems.add(paymentItemsMap)
					println("paymentItemsMap--------------"+paymentItemsMap)
				}
				println("payment--------------"+paymentItems)
			}
			
		} catch (GenericEntityException e) {
			
		}
	}
}
request.setAttribute("paymentItems", paymentItems)
request.setAttribute("addlTotalDue", totalBillAmount)

