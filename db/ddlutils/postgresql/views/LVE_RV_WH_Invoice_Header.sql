CREATE OR RepLACE VIEW LVE_RV_WH_Invoice_Header AS
 SELECT i.AD_Client_ID,
    i.AD_Org_ID,
    i.IsActive,
    i.Created,
    i.CreatedBy,
    i.Updated,
    i.UpdatedBy,
    i.C_Invoice_ID,
    i.IsSOTrx,
    i.DocumentNo,
    i.DocStatus,
    i.C_DocType_ID,
    bp.C_BPartner_ID,
    bp.Value AS bpvalue,
    bp.TaxID AS bpTaxID,
    bp.Naics,
    bp.Duns,
    oi.C_Location_ID AS org_Location_ID,
    oi.TaxID,
    dt.PrintName AS documentType,
    dt.DocumentNote AS documentTypeNote,
    i.C_Order_ID,
    i.SalesRep_ID,
    COALESCE(ubp.Name, u.Name) AS salesRep_Name,
    i.DateInvoiced,
    bpg.Greeting AS BPGreeting,
    bp.Name,
    bp.Name2,
    bpcg.Greeting AS BPContactGreeting,
    bpc.Title,
    bpc.Phone,
    NULLIF(bpc.Name::text, bp.Name::text) AS ContactName,
    bpl.C_Location_ID,
    bp.ReferenceNo,
    l.Postal::text || l.Postal_Add::text AS postal,
    i.Description,
    i.POReference,
    i.DateOrdered,
    i.C_Currency_ID,
    pt.Name AS PaymentTerm,
    pt.DocumentNote AS PaymentTermNote,
    i.C_Charge_ID,
    i.ChargeAmt,
    i.TotalLines,
    i.GrandTotal,
    i.GrandTotal AS AmtInWords,
    i.M_PriceList_ID,
    i.IsTaxIncluded,
    i.C_Campaign_ID,
    i.C_Project_ID,
    i.C_Activity_ID,
    i.IsPaid,
    COALESCE(oi.logo_ID, ci.logo_ID) AS logo_ID,
    bp.PersonType,
    wh.WHThirdParty_ID
   FROM c_Invoice i
   	 INNER JOIN ( SELECT MAX(wh.WHThirdParty_ID) AS whthirdparty_ID,
            			wh.C_Invoice_ID
           		  FROM wh_withholding wh
          		  WHERE wh.C_Invoice_ID IS NOT NULL
          		  GROUP BY wh.C_Invoice_ID) wh ON wh.C_Invoice_ID = i.C_Invoice_ID
     INNER JOIN c_DocType dt ON i.C_DocType_ID = dt.C_DocType_ID
     INNER JOIN c_PaymentTerm pt ON i.C_PaymentTerM_ID = pt.C_PaymentTerM_ID
     INNER JOIN c_BPartner bp ON COALESCE(wh.WHThirdParty_ID,i.C_BPartner_ID) = bp.C_BPartner_ID
     LEFT JOIN c_Greeting bpg ON bp.C_Greeting_ID = bpg.C_Greeting_ID
     INNER JOIN c_BPartner_Location bpl ON i.C_BPartner_Location_ID = bpl.C_BPartner_Location_ID
     INNER JOIN c_Location l ON bpl.C_Location_ID = l.C_Location_ID
     LEFT JOIN AD_user bpc ON i.AD_user_ID = bpc.AD_user_ID
     LEFT JOIN c_Greeting bpcg ON bpc.C_Greeting_ID = bpcg.C_Greeting_ID
     INNER JOIN AD_Orginfo oi ON i.AD_Org_ID = oi.AD_Org_ID
     INNER JOIN AD_Clientinfo ci ON i.AD_Client_ID = ci.AD_Client_ID
     LEFT JOIN AD_user u ON i.SalesRep_ID = u.AD_user_ID
     LEFT JOIN c_BPartner ubp ON u.C_BPartner_ID = ubp.C_BPartner_ID
     ;