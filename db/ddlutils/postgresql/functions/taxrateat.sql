-- FUNCTION: adempiere.taxrateat(numeric, timestamp without time zone, character varying)

-- DROP FUNCTION adempiere.taxrateat(numeric, timestamp without time zone, character varying);
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
CREATE OR REPLACE FUNCTION taxrateat(
	p_c_taxcategory_id numeric,
	p_validfrom timestamp without time zone,
	issotrx character varying)
    RETURNS numeric
    LANGUAGE 'plpgsql'

    COST 100
    VOLATILE 
AS $BODY$
DECLARE
v_CurrentTax NUMERIC;
BEGIN
    -- Get Cost At
SELECT t.Rate INTO v_CurrentTax
FROM C_Tax t
WHERE t.C_TaxCategory_ID = p_C_TaxCategory_ID 
AND t.ValidFrom <= p_ValidFrom
AND (t.IsSalesTax = issotrx 
OR 
(issotrx = 'Y' AND (t.SOPOType = 'B' OR t.SOPOType = 'S'))
OR
(issotrx = 'N' AND (t.SOPOType = 'B' OR t.SOPOType = 'P'))
)
ORDER BY t.ValidFrom DESC
LIMIT 1;
--  DBMS_OUTPUT.PUT_LINE('== FTA_FarmerLiquidation_ID=' || p_FTA_FarmerLiquidation_ID || ', Amt=' || v_AvailableAmt);
-- Valid if is null Value
IF v_CurrentTax IS NULL THEN
v_CurrentTax := 0;
END IF;
-- Default Return
RETURN  v_CurrentTax;
END
$BODY$;
