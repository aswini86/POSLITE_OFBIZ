<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<script type="text/javascript">

    jQuery(document).ready( function() {
        jQuery('#allcheck').change( function() {
            setCheckboxes();
        });

        jQuery('.statuscheck').change( function() {
            setAllCheckbox();
        });
        
    });

    function setCheckboxes() {
        if (jQuery('#allcheck').is(':checked')) {
            jQuery('.statuscheck').attr ('checked', true);
        } else {
            jQuery('.statuscheck').attr ('checked', false );
        }
    }
    function setAllCheckbox() {
        var allChecked = true;
        jQuery('.statuscheck').each (function () {
            if (!jQuery(this).is(':checked')) {
                allChecked = false;
            }
        });
        if (allChecked == false && jQuery('#allcheck').is(':checked')) {
            jQuery('#allcheck').attr('checked', false);
        }
        if (allChecked == true && !jQuery('#allcheck').is(':checked')) {
            jQuery('#allcheck').attr('checked', true);
        }
    }
    function mapArticle(ean,internalName,facilityId) {
    	var form = document.unmapproductlist;
    	document.getElementById("ean").value = ean;
		document.getElementById("internalName").value = internalName;
		document.getElementById("facilityId").value = facilityId;
    	form.action = "<@ofbizUrl>createunMapProduct</@ofbizUrl>";
    	form.submit();
    	
    }
    function deleteArticle(ean) {
    	var form = document.unmapproductlist;
    	document.getElementById("ean").value = ean;
    	form.action = "<@ofbizUrl>deleteunMapProduct</@ofbizUrl>";
    	form.submit();
    }

</script>

<#-- order list -->
<div id="orderLookup" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">Search UnMap Products</li>
      </ul>
      <br class="clear"/>
    </div>
	<div class="screenlet-body">
		<form method="post" name="findUnMapProduct" action="<@ofbizUrl>findunmapproduct</@ofbizUrl>">
			<table class="basic-table" cellspacing='0'>
          		<tr>
          			<td>FacilityId</td>
          			<td><input type="text" name="facilityId" value=""></td>
          			
          			<td>EAN</td>
          			<td><input type="text" name="ean" value=""></td>
          		</tr>
          		<tr>
		            <td colspan="3" align="center">
		              <a href="javascript:document.findUnMapProduct.submit()" class="buttontext">${uiLabelMap.CommonFind}</a>
		            </td>
          		</tr>
          	</table>
		</form>
	</div>
 </div>
  <div id="findOrdersList" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3"></li>
      </ul>
      <br class="clear"/>
    </div>
    <form name="unmapproductlist" id="unmapproductlist" method="post">
    <input type="hidden" name="ean" id="ean" value="">
    <input type="hidden" name="internalName" id="internalName" value="">
    <input type="hidden" name="productTypeId" id="productTypeId" value="FINISHED_GOOD">
    <input type="hidden" name="facilityId" id="facilityId" value="">
    <div class="screenlet-body">
        <table class="basic-table hover-bar" cellspacing='0'>
          <tr class="header-row">
            <td width="15%">EAN CODE</td>
            <td width="10%">Item Description</td>
            <td width="10%">Map Action</td>
           	<td width="10%">Delete Action</td>
          </tr>
          <#list unMapProductList as unMapProductList>
            
            <tr>
               	<td>${unMapProductList.ean?if_exists}</td>
              	<td>${unMapProductList.description?if_exists}</td>
              	<td><input type="button" onclick="javascript:mapArticle('${unMapProductList.ean?if_exists}','${unMapProductList.description?if_exists}','${unMapProductList.facilityId?if_exists}');" value="Map"/></td>
              	<td><input type="button" onclick="javascript:deleteArticle('${unMapProductList.ean?if_exists}');" value="Delete"/></td>
            </tr>
         </#list>
        </table>
    </div>
    </form>
  </div>