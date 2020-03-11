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

String creditId = request.getParameter("creditId")
String receiptId = request.getParameter("receiptId")
String dayId = request.getParameter("dayId")
String terminalId = request.getParameter("posTerminalId")
String paymentType = request.getParameter("paymentType")
String cashToPay = request.getParameter("cashToPay")
String paymentTransaction = request.getParameter("paymentTransaction")
BigDecimal receivedPayment = new BigDecimal(cashToPay)
BigDecimal remainingPayment = BigDecimal.ZERO
BigDecimal totalDue = BigDecimal.ZERO
BigDecimal totalBillAmount = BigDecimal.ZERO
BigDecimal posCartTxnPaidAmt = BigDecimal.ZERO
BigDecimal txnBalanceAmt = BigDecimal.ZERO
BigDecimal recPayment = BigDecimal.ZERO
BigDecimal remainingAmount = BigDecimal.ZERO

HttpSession session = request.getSession(true);
session.removeAttribute("shoppingCart");
session.removeAttribute("webPosSession");
println("came into receive payment -----&&&&&&&&&&&&&&&--------------")
webPosSession = WebPosEvents.getWebPosSession(request, null);
List<GenericValue> receivedPaymentList = new ArrayList<GenericValue>();
GenericValue cartPayment = new GenericValue();
List<Map<String, Object>> paymentItems = new LinkedList<Map<String,Object>>();
BigDecimal discount = BigDecimal.ZERO
BigDecimal charges = BigDecimal.ZERO
BigDecimal addlDiscAmt = BigDecimal.ZERO
BigDecimal addlCharge = BigDecimal.ZERO
BigDecimal percentage = new BigDecimal("0.01")
BigDecimal balanceAmount = BigDecimal.ZERO
String isPercentage = "N"
String isChargePercentage = "N"
if (webPosSession) {
	if (UtilValidate.isNotEmpty(receiptId)) {
		try {
			//Get PosCart transaction details
			posCartTxnData = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId", receiptId).queryOne();
			if (UtilValidate.isNotEmpty(posCartTxnData)) {
				println("PosCartTransaction---------------${posCartTxnData.getBigDecimal("paidAmount")}")
				
				if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("totalBillAmount"))) {
					request.setAttribute("totalBillAmount", BigDecimal.ZERO)
				} else {
					request.setAttribute("totalBillAmount", posCartTxnData.getBigDecimal("totalBillAmount"))
					totalBillAmount = posCartTxnData.getBigDecimal("totalBillAmount")
				}
				if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("paidAmount"))) {
					request.setAttribute("paidAmount", BigDecimal.ZERO)
				} else {
					posCartTxnPaidAmt = receivedPayment.add(posCartTxnData.getBigDecimal("paidAmount"))
					
					posCartTxnData.set("paidAmount", posCartTxnPaidAmt)
					request.setAttribute("paidAmount", posCartTxnPaidAmt)
				}
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
				if (UtilValidate.isEmpty(posCartTxnData.getBigDecimal("balanceAmount"))) {
					request.setAttribute("balanceAmount", BigDecimal.ZERO)
				} else {
					balanceAmount = posCartTxnData.getBigDecimal("balanceAmount")
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
						balanceAmount = balanceAmount.subtract(addlDiscAmt)
					}
					if(addlCharge.compareTo(BigDecimal.ZERO) > 0) {
						totalBillAmount = totalBillAmount.add(addlCharge)
						balanceAmount = balanceAmount.subtract(addlCharge)
					}*/
					txnBalanceAmt = totalBillAmount.subtract(posCartTxnPaidAmt)
					posCartTxnData.set("balanceAmount", txnBalanceAmt)
					
					request.setAttribute("balanceAmount", txnBalanceAmt)
				}
				posCartTxnData.store();
				
				if (posCartTxnPaidAmt.compareTo(posCartTxnData.getBigDecimal("totalBillAmount")) == 1) {//check if paid amt is more than Balance Amt
					returnAmount = posCartTxnPaidAmt.subtract(posCartTxnData.getBigDecimal("totalBillAmount"))
					request.setAttribute("returnAmount", returnAmount)
				}else {
					request.setAttribute("returnAmount", BigDecimal.ZERO)
				}
			}
		} catch(GenericEntityException e) {
			
		}
	}
	//totalDue = webPosSession.getCurrentTransaction().getTotalDue();
}
println("totalDue -----&&&&&&&&&&&&&&&--------------${totalBillAmount}")
if (UtilValidate.isNotEmpty(paymentType)) {
	//check for the receipt and day whether received payments or not.
	receivedPaymentList = EntityQuery.use(delegator).from("PosCartReceivePayment").
								where("receiptId", receiptId,"paymentType", paymentType).queryList();
	if(paymentType.equals("CREDIT_NOTE")) {
		paymentTransaction = creditId;
	}
	println("old receivedPayment-------------${receivedPayment}");
	println("new totalBillAmount-------------${totalBillAmount}");
	println("new balanceAmount-------------${balanceAmount}");
	//check receivedPayment > bill amt
	//check paymentDetails exists
	receivedPayments = EntityQuery.use(delegator).from("PosCartReceivePayment").where("receiptId", receiptId).queryList()
	if((UtilValidate.isNotEmpty(receivedPayments)) && (receivedPayment.compareTo(totalBillAmount) == 1)) {
		receivedPayment = totalBillAmount
	}
	if(receivedPayment.compareTo(balanceAmount) == 1) {
		println("balanceAmount-------------${balanceAmount}");
		receivedPayment = receivedPayment.ZERO
		receivedPayment = balanceAmount
		
	}
	posCartPaymentId = delegator.getNextSeqId("PosCartReceivePayment")
	cartPayment = delegator.makeValue("PosCartReceivePayment", UtilMisc.toMap("posCartPaymentId",posCartPaymentId,
		"receiptId",receiptId,"dayId",dayId,"terminalId",terminalId,"paymentType",paymentType,"receivedPayment",receivedPayment,
		"paymentTransaction", paymentTransaction))
	cartPayment.create()
	//get paymentList
	//receivedPayments = EntityQuery.use(delegator).from("PosCartReceivePayment").
								//where("receiptId", receiptId).queryList()
	for (GenericValue row : receivedPayments) {
		if(UtilValidate.isNotEmpty(receivedPayments)) {
			Map paymentItemsMap = [:]
			
			recPayment = recPayment.add(row.getBigDecimal("receivedPayment"))
			remainingAmount = totalBillAmount.subtract(recPayment)
			
			paymentItemsMap.put("paymentType", row.getString("paymentType"))
			paymentItemsMap.put("receivedPayment", row.getBigDecimal("receivedPayment").toString())
			paymentItemsMap.put("remainingAmount", remainingAmount.toString())
			paymentItemsMap.put("paymentTransaction", row.getString("paymentTransaction"))
			paymentItems.add(paymentItemsMap)
		}
		println("payment--------------"+paymentItems)
	}					
	println("totalBillAmount-----------############################---${totalBillAmount}")
}
remainingPayment = totalBillAmount.subtract(receivedPayment)
request.setAttribute("paymentItems", paymentItems)
request.setAttribute("totalDue", totalBillAmount)

