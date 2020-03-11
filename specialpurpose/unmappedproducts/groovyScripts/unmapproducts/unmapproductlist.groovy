 import org.apache.ofbiz.base.util.*
 import org.apache.ofbiz.entity.*
 import org.apache.ofbiz.entity.util.*
 import org.apache.ofbiz.entity.condition.*
 import org.apache.ofbiz.entity.util.EntityQuery
 import org.apache.ofbiz.entity.GenericValue
 import java.util.List;
 
 
 List<GenericValue> unMapProductList = EntityQuery.use(delegator).from("UnmappedItem").queryList();
 context.unMapProductList = unMapProductList