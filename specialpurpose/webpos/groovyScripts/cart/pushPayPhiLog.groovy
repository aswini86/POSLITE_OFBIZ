/* Licensed to the Apache Software Foundation (ASF) under one
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
import java.util.List
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.Delegator

	String txnID = request.getParameter("txnID")
	String aggregatorID = request.getParameter("aggregatorID")
	String responseCode = request.getParameter("responseCode")
	String respDescription = request.getParameter("respDescription")
	String merchantId = request.getParameter("merchantId")
	String merchantTxnNo = request.getParameter("merchantTxnNo")
	String paymentDateTime = request.getParameter("paymentDateTime")
	String paymentID = request.getParameter("paymentID")
	String paymentMode = request.getParameter("paymentMode")
	String amount = request.getParameter("amount")
	String secureHash = request.getParameter("secureHash")

	Double payAmount = Double.valueOf(amount)
	if (UtilValidate.isNotEmpty(txnID)) {
		GenericValue createPayPhiLog = delegator.makeValue("PayPhiLog", 
			UtilMisc.toMap("txnID", txnID, "aggregatorID", aggregatorID, "responseCode", responseCode,
							"respDescription", respDescription, "merchantId", merchantId,
							"merchantTxnNo", merchantTxnNo, "paymentDateTime", paymentDateTime,
							"paymentID", paymentID, "paymentMode", paymentMode, "amount", payAmount,
							"secureHash", secureHash));
		createPayPhiLog.create()

		request.setAttribute("successMessage", "PayPhi Log data created successfully.")
	}


