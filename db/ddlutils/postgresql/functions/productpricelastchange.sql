-- FUNCTION: adempiere.productpricelastchange(numeric, numeric, numeric, timestamp without time zone)

-- DROP FUNCTION adempiere.productpricelastchange(numeric, numeric, numeric, timestamp without time zone);
/*************************************************************************
 * The contents of this file are subject to the Compiere License.  You may
 * obtain a copy of the License at    http://www.compiere.org/license.html
 * Software is on an  "AS IS" basis,  WITHOUT WARRANTY OF ANY KIND, either
 * express or implied. See the License for details. Code: Compiere ERP+CRM
 * Copyright (C) 1999-2001 Jorg Janke, ComPiere, Inc. All Rights Reserved.
 *
 * Created By Yamel Senih, 
 * ysenih@erpcya.com
 *************************************************************************
 * Title: Search Tax Rate
 * Description:
 * 
 ************************************************************************/
CREATE OR REPLACE FUNCTION productpricelastchange(
	p_m_pricelist_id numeric,
	p_m_product_id numeric,
	p_pricelist numeric,
	p_date timestamp without time zone)
    RETURNS timestamp without time zone
    LANGUAGE 'plpgsql'

    COST 100
    VOLATILE 
AS $BODY$
DECLARE
v_Record RECORD;
v_ValidFrom TIMESTAMP;
v_Count INTEGER := 0;
BEGIN
    FOR v_Record IN 
SELECT pp.PriceList, plv.ValidFrom
FROM M_PriceList_Version plv 
INNER JOIN M_ProductPrice pp ON(pp.M_PriceList_Version_ID = plv.M_PriceList_Version_ID AND pp.M_Product_ID = p_M_Product_ID)
WHERE plv.M_PriceList_ID = p_M_PriceList_ID
AND plv.ValidFrom < p_Date
ORDER BY plv.ValidFrom DESC
    LOOP
v_Count := v_Count + 1;
IF(ROUND(v_Record.PriceList, 2) <> ROUND(p_PriceList, 2) AND v_Count = 1) THEN
    RETURN NULL;
END IF;
IF(ROUND(v_Record.PriceList, 2) <> ROUND(p_PriceList, 2) AND v_Count > 1) THEN
    RETURN v_Record.ValidFrom;
END IF;
    END LOOP;

     SELECT INTO v_ValidFrom plv.ValidFrom
FROM M_PriceList_Version plv 
INNER JOIN M_ProductPrice pp ON(pp.M_PriceList_Version_ID = plv.M_PriceList_Version_ID AND pp.M_Product_ID = p_M_Product_ID)
WHERE plv.M_PriceList_ID = p_M_PriceList_ID
AND plv.ValidFrom < p_Date 
ORDER BY plv.ValidFrom ASC
LIMIT 1;
    RETURN v_ValidFrom;
END;

$BODY$;
