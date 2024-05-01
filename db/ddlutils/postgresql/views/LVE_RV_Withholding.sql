CREATE OR REPLACE VIEW LVE_RV_Withholding AS 
SELECT 
	w.AD_Client_ID,
	w.AD_Org_ID,
	w.C_Invoice_ID,
	w.C_InvoiceLine_ID,
	ROUND(w.A_Base_Amount * w.CurrencyRate,w.PricePrecision) A_Base_Amount,
	w.WithholdingAmt,
	w.WithholdingRate,
	ROUND(w.Subtrahend * w.CurrencyRate,w.PricePrecision) Subtrahend,
	w.DocumentNo,
	w.ControlNo,
	w.DateInvoiced,
	w.Line ,
	w.type,
	ROUND(w.GrandTotal * w.CurrencyRate,w.PricePrecision) GrandTotal,
	ROUND(w.TotalLines * w.CurrencyRate,w.PricePrecision) TotalLines,
	ROUND(w.TaxAmt * w.CurrencyRate,w.PricePrecision) TaxAmt,
	w.InvoiceNo,
	w.CreditDocumentNo, 
	w.DebitDocumentNo,
	w.AffectedDocumentNo,
	w.DateAcct,
	w.Description,
	w.PersonType,
	w.DocumentNote,
	w.PrintName,
	ROUND(w.ExemptAmt * w.CurrencyRate,w.PricePrecision) ExemptAmt,
	w.IsDeclared,
	w.TaxID,
	w.OrgValue,
	w.DocumentType,
	w.Concept_Value,
	w.WHDocumentNo,
	w.Rate,
	w.WH_Definition_ID,
	w.WH_Setting_ID,
	w.WH_Type_ID,
	w.WithholdingDeclaration_ID,
	w.IsManual,
	w.FiscalDocumentType,
	ROUND(w.TaxBaseAmt * w.CurrencyRate,w.PricePrecision) TaxBaseAmt,
	w.Concept_Name,
	w.CurrencyRate,
	w.C_ConversionType_ID,
	w.C_Currency_ID,
	w.PricePrecision
FROM (
	SELECT 
		whDoc.AD_Client_ID,
		whDoc.AD_Org_ID,
		whDoc.C_Invoice_ID,
		whDocLine.C_InvoiceLine_ID,
		wh.A_Base_Amount,
		whDocLine.LineNetAmt WithholdingAmt,
		wh.WithholdingRate,
		wh.Subtrahend,
		origDoc.DocumentNo,
		origDoc.ControlNo,
		origDoc.DateInvoiced,
		whDocLine.Line ,
		CASE WHEN whDoc.DocStatus IN ('VO','RE') THEN '3-anu' 
			 ELSE '1-reg' END  "type",
		origDoc.GrandTotal,
		origDoc.TotalLines,
		it.TaxAmt,
		CASE WHEN dtOrigDoc.FiscalDocumentType='01' THEN origDoc.DocumentNo ELSE null::varchar END InvoiceNo,
		CASE WHEN dtOrigDoc.FiscalDocumentType='03' THEN origDoc.DocumentNo ELSE null::varchar END CreditDocumentNo, 
		CASE WHEN dtOrigDoc.FiscalDocumentType='02' THEN origDoc.DocumentNo ELSE null::varchar END DebitDocumentNo,
		COALESCE(affectedHdrDoc.DocumentNo,affectedLineDoc.DocumentNo) AffectedDocumentNo,
		origDoc.DateAcct,
		whc.Description,
		whc.PersonType,
		dtWhDoc.DocumentNote,
		dtWhDoc.PrintName,
		COALESCE(et.ExemptAmt,0) ExemptAmt,
		COALESCE(whDec.IsDeclared, 'N') IsDeclared,
		bp.TaxID,
		oi.TaxID OrgValue,
		CASE WHEN whDoc.IsSOTrx = 'N' THEN 'C' ELSE 'V' END DocumentType,
		whc.Value Concept_Value,
		whDoc.DocumentNo WHDocumentNo,
		it.Rate,
		wh.WH_Definition_ID,
		whs.WH_Setting_ID,
		whs.WH_Type_ID,
		wh.WithholdingDeclaration_ID,
		wh.IsManual,
		dtOrigDoc.FiscalDocumentType,
		it.TaxBaseAmt,
		whconcept.Name Concept_Name,
		CurrencyRate(origDoc.C_Currency_ID, whDoc.C_Currency_ID, wh.DateAcct, wh.C_ConversionType_ID, origDoc.AD_Client_ID, origDoc.AD_Org_ID) CurrencyRate,
		pl.PricePrecision,
		origDoc.C_Currency_ID Doc_C_Currency_ID,
		whDoc.C_Currency_ID WH_C_Currency_ID, 
		wh.C_ConversionType_ID,
		wh.C_Currency_ID
	FROM C_Invoice whDoc
	INNER JOIN C_InvoiceLine whDocLine ON (whDoc.C_Invoice_ID = whDocLine.C_Invoice_ID)
	INNER JOIN C_BPartner bp ON (whDoc.C_BPartner_ID = bp.C_BPartner_ID)
	INNER JOIN AD_OrgInfo oi ON (whDoc.AD_Org_ID = oi.AD_Org_ID)
	INNER JOIN WH_Withholding wh ON (wh.C_Invoice_ID = whDoc.C_Invoice_ID AND wh.C_InvoiceLine_ID = whDocLine.C_InvoiceLine_ID)
	INNER JOIN WH_Setting whs ON (wh.WH_Setting_ID = whs.WH_Setting_ID)
	INNER JOIN C_Invoice origDoc ON (origDoc.C_Invoice_ID = wh.SourceInvoice_ID)
	INNER JOIN C_DocType dtOrigDoc ON (dtOrigDoc.C_DocType_ID = origDoc.C_DocTypeTarget_ID)
	INNER JOIN C_DocType dtWhDoc ON (dtWhDoc.C_DocType_ID = whDoc.C_DocTypeTarget_ID)
	LEFT JOIN C_Invoice affectedHdrDoc ON (affectedHdrDoc.C_Invoice_ID = origDoc.InvoiceToAllocate_ID)
	LEFT JOIN (SELECT DISTINCT i.DocumentNo,il.C_Invoice_ID 
			   FROM C_Invoice i
			   INNER JOIN C_InvoiceLine il ON (i.C_Invoice_ID = il.InvoiceToAllocate_ID)
			  )affectedLineDoc ON (affectedLineDoc.C_Invoice_ID = origDoc.C_Invoice_ID)
	LEFT JOIN (SELECT it.C_Invoice_ID,
						SUM(it.TaxAmt) TaxAmt, 
						SUM(CASE WHEN it.TaxAmt != 0 THEN it.TaxBaseAmt ELSE 0 END) TaxBaseAmt, 
						t.Rate,
			   			t.C_Tax_ID
			   FROM C_InvoiceTax it 
			   LEFT JOIN C_Tax t ON (it.C_Tax_ID = t.C_Tax_ID)
			   GROUP BY it.C_Invoice_ID, t.Rate, t.C_Tax_ID) it ON (it.C_Invoice_ID = origDoc.C_Invoice_ID AND wh.C_Tax_ID = it.C_Tax_ID)
	LEFT JOIN (SELECT it.C_Invoice_ID,
						SUM(CASE WHEN it.TaxAmt = 0 THEN it.TaxBaseAmt ELSE 0 END) ExemptAmt
			   FROM C_InvoiceTax it 
			   GROUP BY it.C_Invoice_ID) et ON (et.C_Invoice_ID = origDoc.C_Invoice_ID)
	LEFT JOIN LVE_ListVersion whc ON (whc.LVE_ListVersion_ID = wh.WithholdingRentalRate_ID)
	LEFT JOIN LVE_List whconcept ON ((whc.LVE_List_ID = whconcept.LVE_List_ID))
	LEFT JOIN (SELECT C_Invoice_ID,'Y'::VARCHAR IsDeclared FROM C_Invoice i WHERE DocStatus IN ('CO','CL')) whDec ON (whDec.C_Invoice_ID = wh.WithholdingDeclaration_ID)
	LEFT JOIN M_PriceList pl ON (whDoc.M_PriceList_ID = pl.M_PriceList_ID)
	WHERE 
	wh.DocStatus IN ('CO','CL')) AS w 
UNION ALL
SELECT 
w.AD_Client_ID,
	w.AD_Org_ID,
	w.C_Invoice_ID,
	w.C_InvoiceLine_ID,
	ROUND(w.A_Base_Amount * w.CurrencyRate,w.PricePrecision) A_Base_Amount,
	w.WithholdingAmt,
	w.WithholdingRate,
	ROUND(w.Subtrahend * w.CurrencyRate,w.PricePrecision) Subtrahend,
	w.DocumentNo,
	w.ControlNo,
	w.DateInvoiced,
	w.Line ,
	w.type,
	ROUND(w.GrandTotal * w.CurrencyRate,w.PricePrecision) GrandTotal,
	ROUND(w.TotalLines * w.CurrencyRate,w.PricePrecision) TotalLines,
	ROUND(w.TaxAmt * w.CurrencyRate,w.PricePrecision) TaxAmt,
	w.InvoiceNo,
	w.CreditDocumentNo, 
	w.DebitDocumentNo,
	w.AffectedDocumentNo,
	w.DateAcct,
	w.Description,
	w.PersonType,
	w.DocumentNote,
	w.PrintName,
	ROUND(w.ExemptAmt * w.CurrencyRate,w.PricePrecision) ExemptAmt,
	w.IsDeclared,
	w.TaxID,
	w.OrgValue,
	w.DocumentType,
	w.Concept_Value,
	w.WHDocumentNo,
	w.Rate,
	w.WH_Definition_ID,
	w.WH_Setting_ID,
	w.WH_Type_ID,
	w.WithholdingDeclaration_ID,
	w.IsManual,
	w.FiscalDocumentType,
	ROUND(w.TaxBaseAmt * w.CurrencyRate,w.PricePrecision) TaxBaseAmt,
	w.Concept_Name,
	w.CurrencyRate,
	w.C_ConversionType_ID,
	w.C_Currency_ID,
	w.PricePrecision
FROM (
	SELECT pr.AD_Client_ID,
		pr.AD_Org_ID,
		NULL::NUMERIC(10, 0) AS C_Invoice_ID,
		NULL::NUMERIC(10, 0) AS C_InvoiceLine_ID,
		SUM(CASE WHEN c.Type <> 'R' THEN m.Amount ELSE 0 END) A_Base_Amount,
		0::NUMERIC(10, 0) AS WithholdingAmt,
		MAX(CASE WHEN c.Type = 'R' THEN m.Amount ELSE 0 END) AS WithholdingRate,
		0::NUMERIC(10, 0) Subtrahend,
		prl.PrintName::Varchar(30) AS DocumentNo,
		prl.PrintName::Varchar(255) AS ControlNo,
		(date_trunc('month', pr.DateAcct) + interval '1 month - 1 day')::date AS DateInvoiced,
		0::NUMERIC(10, 0) Line ,
		CASE WHEN pr.DocStatus IN ('VO','RE') THEN '3-anu' 
				 ELSE '1-reg' END "type",
		0::NUMERIC(10, 0) AS GrandTotal,
		0::NUMERIC(10, 0) AS TotalLines,
		0::NUMERIC(10, 0) AS TaxAmt,
		prl.PrintName::Varchar(30) AS InvoiceNo,
		prl.PrintName::Varchar(30) AS CreditDocumentNo, 
		prl.PrintName::Varchar(30) AS DebitDocumentNo,
		prl.PrintName::Varchar(30) AS AffectedDocumentNo,
		(date_trunc('month', pr.DateAcct) + interval '1 month - 1 day')::date AS DateAcct,
		NULL::Varchar(255) AS Description,
		bp.PersonType,
		NULL::Varchar(2000) AS DocumentNote,
		NULL::Varchar(60) AS PrintName,
		0::NUMERIC(10, 0) AS ExemptAmt,
		'N'::character(1) AS IsDeclared,
		bp.TaxID,
		o.TaxID::Varchar(20) AS OrgValue,
		NULL AS DocumentType,
		prl.PrintName AS Concept_Value,
		NULL::Varchar(30) AS WHDocumentNo,
		MAX(CASE WHEN c.Type = 'R' THEN m.Amount ELSE 0 END) AS Rate,
		NULL::NUMERIC(10, 0) AS WH_Definition_ID,
		NULL::NUMERIC(10, 0) AS WH_Setting_ID,
		wt.WH_Type_ID,
		NULL::NUMERIC(10, 0) AS WithholdingDeclaration_ID,
		'N'::character(1) IsManual,
		NULL::Varchar(2) AS FiscalDocumentType,
		0::NUMERIC(10, 0) AS TaxBaseAmt,
		NULL::Varchar(255) AS Concept_Name,
		0::NUMERIC(10, 0) AS CurrencyRate,
		pr.C_ConversionType_ID,
		pr.C_Currency_ID,
		0::NUMERIC(10, 0) AS PricePrecision
	FROM HR_Process pr
	INNER JOIN HR_Movement m ON(m.HR_Process_ID = pr.HR_Process_ID)
	INNER JOIN HR_ProcessReportLine prl ON(prl.HR_Concept_ID = m.HR_Concept_ID)
	INNER JOIN HR_Concept c ON(c.HR_Concept_ID = m.HR_Concept_ID)
	INNER JOIN WH_Type wt ON(wt.HR_ProcessReport_ID = prl.HR_ProcessReport_ID)
	INNER JOIN C_BPartner bp ON(bp.C_BPartner_ID = m.C_BPartner_ID)
	INNER JOIN AD_OrgInfo o ON(o.AD_Org_ID = pr.AD_Org_ID)
	WHERE wt.WH_Type_ID IS NOT NULL
	GROUP BY pr.AD_Client_ID, pr.AD_Org_ID, prl.PrintName, (date_trunc('month', pr.DateAcct) + interval '1 month - 1 day'), bp.PersonType, bp.TaxID, o.TaxID, wt.WH_Type_ID, pr.DocStatus, pr.C_ConversionType_ID, pr.C_Currency_ID
) AS w

;
