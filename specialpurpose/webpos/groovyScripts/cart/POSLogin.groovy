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
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.webpos.WebPosEvents;
import org.apache.ofbiz.webpos.session.WebPosSession;
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.apache.ofbiz.securityext.login.LoginEvents

import java.util.List;

import javax.servlet.http.HttpSession

HttpSession session = request.getSession(true);

// get the posTerminalId
String posTerminalId = request.getParameter("posTerminalId");
session.removeAttribute("shoppingCart");
session.removeAttribute("webPosSession");
WebPosSession webPosSession = WebPosEvents.getWebPosSession(request, posTerminalId);
String responseString = LoginEvents.storeLogin(request, response);
GenericValue userLoginNew = (GenericValue)session.getAttribute("userLogin");

if (userLoginNew != null && UtilValidate.isNotEmpty(posTerminalId)) {
	webPosSession.setUserLogin(userLoginNew);
}
request.setAttribute("userLoginNew", userLoginNew);