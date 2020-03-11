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
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents

webPosSession = WebPosEvents.getWebPosSession(request, null);
List<Map<String, Object>> cartItems = new ArrayList<Map<String, Object>>();
if (webPosSession) {
	println("came into web pos add--------&&&&&&&&&&&&&&&&&&&&_----------------");
	shoppingCart = webPosSession.getCart();
	String transId = webPosSession.id;
	println("id-----------${transId}");
	String posTerminalId = ""
	String[] add_product_id = request.getParameterValues("add_product_id")
	
	List productIdList = []
	if (add_product_id) {
		productIdList.addAll(Arrays.asList(add_product_id))
	}
	println("add_product_id&&&&&&&&&7---"+productIdList);
	if (UtilValidate.isNotEmpty(productIdList)) {
		for (int i = 0; i < add_product_id.length; i++) {
			println("add_product_id---each&&&&&&&&&7---"+add_product_id[i]);
			posTerminalId = add_product_id[i]
			request.setAttribute("posTerminalId", posTerminalId)
			ShoppingCartEvents.addToCart(request,response)
			ShoppingCartEvents.ShowPosCart(request,response)
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
