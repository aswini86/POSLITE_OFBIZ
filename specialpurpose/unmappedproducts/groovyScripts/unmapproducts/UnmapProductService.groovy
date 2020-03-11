import org.apache.ofbiz.entity.*
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.util.*
import org.apache.ofbiz.base.util.*
import java.util.HashMap
import java.util.LinkedList
import java.util.Map
import java.util.Locale
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilDateTime
import java.util.LinkedList
import java.util.List

import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.service.LocalDispatcher
import org.apache.ofbiz.service.DispatchContext
import java.sql.Timestamp;
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue


public static Map<String, Object> createunMapProduct(DispatchContext ctx, Map<String, ? extends Object> context) {
    Map<String, Object> result = new HashMap<String, Object>();
    Delegator delegator = ctx.getDelegator();
    Timestamp now = UtilDateTime.nowTimestamp();
    LocalDispatcher dispatcher = ctx.getDispatcher();
    Locale locale = (Locale) context.get("locale");
    GenericValue userLogin = (GenericValue) context.get("userLogin");

    String productId = (String) context.get("productId");
    String internalName = (String) context.get("internalName");
    String productTypeId = (String) context.get("productTypeId");
    System.out.println("productId----------------------${productId}");
    System.out.println("internalName----------------------${internalName}");
    System.out.println("productTypeId----------------------${productTypeId}");
    try {
        if (UtilValidate.isNotEmpty(productId)) {
            Map<String, Object> createProductMap = dispatcher.runSync("createProduct", UtilMisc.toMap("productId", productId, "internalName", 
                    internalName, "productTypeId", productTypeId, "userLogin", userLogin));
            System.out.println("createProductMap----------------------${createProductMap}");
        }
    } catch (GenericEntityException e) {
        Debug.logError(e, "Failure in operation, rolling back transaction", "UnmapProductService.groovy")
    }

	
    result.put("productId", productId);
    result.put(ModelService.SUCCESS_MESSAGE, "Article Mapped Succesfully");
    return result;
}