CREATE OR REPLACE VIEW LVE_RV_Withholding AS 
SELECT 
	whDoc.AD_Client_ID,
	whDoc.AD_Org_ID,
	whDoc.C_Invoice_ID,
	whDocLine.C_InvoiceLine_ID,
	wh.A_Base_Amount,
	wh.WithholdingAmt,
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
	COALESCE(it.ExemptAmt,0) ExemptAmt,
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
	whconcept.Name Concept_Name
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
		   			SUM(TaxAmt) TaxAmt,
		   			SUM(CASE WHEN TaxAmt = 0 THEN TaxBaseAmt ELSE 0 END) ExemptAmt, 
		   			SUM(CASE WHEN TaxAmt != 0 THEN TaxBaseAmt ELSE 0 END) TaxBaseAmt, 
		   			MAX(t.Rate) Rate  
		   FROM C_InvoiceTax it 
		   INNER JOIN C_Tax t ON it.C_Tax_ID = t.C_Tax_ID
		   GROUP BY C_Invoice_ID) it ON it.C_Invoice_ID = origDoc.C_Invoice_ID

LEFT JOIN LVE_ListVersion whc ON (whc.LVE_ListVersion_ID = wh.WithholdingRentalRate_ID)
LEFT JOIN LVE_List whconcept ON ((whc.LVE_List_ID = whconcept.LVE_List_ID))
LEFT JOIN (SELECT C_Invoice_ID,'Y'::VARCHAR IsDeclared FROM C_Invoice i WHERE DocStatus IN ('CO','CL')) whDec ON (whDec.C_Invoice_ID = wh.WithholdingDeclaration_ID)
WHERE 
wh.DocStatus IN ('CO','CL')
;