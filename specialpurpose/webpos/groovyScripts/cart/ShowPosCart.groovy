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
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webpos.WebPosEvents;
import org.apache.ofbiz.webpos.session.WebPosSession;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import java.util.List;

webPosSession = WebPosEvents.getWebPosSession(request, null);
List<Map<String, Object>> cartItems = new ArrayList<Map<String, Object>>();
if (webPosSession) {
	
	shoppingCart = webPosSession.getCart();
	context.isManager = webPosSession.isManagerLoggedIn();
	context.transactionId = webPosSession.getCurrentTransaction().getTransactionId();
	context.userLoginId = webPosSession.getUserLoginId();
	context.drawerNumber = webPosSession.getCurrentTransaction().getDrawerNumber();
	context.totalDue = webPosSession.getCurrentTransaction().getTotalDue();
	context.totalQuantity = webPosSession.getCurrentTransaction().getTotalQuantity();
	context.isOpen = webPosSession.getCurrentTransaction().isOpen();
	request.setAttribute("totalQuantity", shoppingCart.getTotalQuantity());
	request.setAttribute("currencyIsoCode", shoppingCart.getCurrency());
	request.setAttribute("subTotal", shoppingCart.getSubTotal());
	
	request.setAttribute("totalShipping", shoppingCart.getTotalShipping());
	request.setAttribute("totalSalesTax", shoppingCart.getTotalSalesTax());
	request.setAttribute("displayGrandTotal", shoppingCart.getDisplayGrandTotal());
	
	request.setAttribute("totalDue", context.totalDue);
	request.setAttribute("cartLineItem", shoppingCart.size());
	request.setAttribute("cartSize", shoppingCart.size());
	
	for (ShoppingCartItem cartLine : shoppingCart) {
		Map<String, Object> cartLineItem = new HashMap<String, Object>();
		cartLineItem.put("productId", cartLine.getProductId());
		cartLineItem.put("productName", cartLine.getName());
		cartLineItem.put("productQty", cartLine.getQuantity());
		cartLineItem.put("productPrice", cartLine.getDisplayPrice());
		cartLineItem.put("productTotalAmt", cartLine.getDisplayItemSubTotal());
		cartItems.add(cartLineItem);
	}
	request.setAttribute("cartItems", cartItems);
	
} else {
	shoppingCart = null;
}

if (shoppingCart) {
	
	return "Success"
} else {
	context.shoppingCartSize = 0;
}
