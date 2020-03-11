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
List<Map<String, Object>> cartItems = new ArrayList<Map<String, Object>>();
finalPosCartItem = [:]
List<GenericValue> finalPosCartItemList = new ArrayList<GenericValue>();
String customerMobileNo =""
String firstName = ""
if (webPosSession) {
	String dayId = request.getParameter("dayId");
	if (UtilValidate.isEmpty(dayId)) {
		try {
			posCartItemList = EntityQuery.use(delegator).from("PosCartItem").queryList();
			println("came into view Pos bill-------&&&&&&&&&&&&&&&&&---${posCartItemList}")
			if (UtilValidate.isNotEmpty(posCartItemList)) {
				for (GenericValue posCartItemRow : posCartItemList) {
					
					finalPosCartItem = posCartItemRow.getAllFields()
					receiptId = posCartItemRow.getString("receiptId");
					
					if (UtilValidate.isNotEmpty(receiptId)) {
						GenericValue posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").where("receiptId",receiptId).queryOne();
						customerMobileNo = posCartTxn.getString("customerMobileNo")
						
						finalPosCartItem.put("customerMobileNo", customerMobileNo)
						finalPosCartItemList.add(finalPosCartItem)
					}
				}
			}
			if (UtilValidate.isNotEmpty(posCartItemList)) {
				request.setAttribute("posCartItemList", finalPosCartItemList)
				
			}else {
				request.setAttribute("successMsg", "Cart Item is Empty")
			}
		} catch (GenericEntityException e) {
			
		}
	}
	if (UtilValidate.isNotEmpty(dayId)) {
		try {
			println("came into view Pos bill-------&&&&&&&&&&&&&&&&&&--${dayId}")
			posCartItemList = EntityQuery.use(delegator).from("PosCartItem").where("dayId", dayId).queryList();
			if (UtilValidate.isNotEmpty(posCartItemList)) {
				for (GenericValue posCartItemRow : posCartItemList) {
					
					finalPosCartItem = posCartItemRow.getAllFields()
					receiptId = posCartItemRow.getString("receiptId");
					
					if (UtilValidate.isNotEmpty(receiptId)) {
						GenericValue posCartTxn = EntityQuery.use(delegator).from("PosCartTransaction").
							where("receiptId",receiptId).queryOne();
						customerMobileNo = posCartTxn.getString("customerMobileNo")
						if (UtilValidate.isNotEmpty(posCartTxn.getString("customerMobileNo"))) {
							customerMobileNo = posCartTxn.getString("customerMobileNo");
							customerContactList = EntityQuery.use(delegator).from("PartyAndTelecomNumberAndPerson").
								where("contactNumber",customerMobileNo, "roleTypeId", "CUSTOMER").orderBy("lastModifiedDate").queryList();
							if(UtilValidate.isNotEmpty(customerContactList)) {
								firstName = customerContactList.get(0).getString("firstName");
							}
						}
						finalPosCartItem.put("firstName", firstName)
						finalPosCartItem.put("customerMobileNo", customerMobileNo)
						finalPosCartItemList.add(finalPosCartItem)
					}
				}
			}
			
			if (UtilValidate.isNotEmpty(posCartItemList)) {
				request.setAttribute("posCartItemList", finalPosCartItemList)
				
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