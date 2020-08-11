CREATE OR REPLACE VIEW LVE_RV_C_Invoice_Day_Summary
(AD_Client_ID, AD_Org_ID, DateInvoiced, LineNetAmt, 
 LineListAmt, LineLimitAmt, LineDiscountAmt, LineDiscount, LineOverLimitAmt, 
 LineOverLimit, IsSOTrx, DocNoSequence_ID, ControlNoSequence_ID, IsFiscalDocument, BeginingDocumentNo, EndingDocumentNo, BeginingControlNo, EndingControlNo, DocBaseType)
AS 
SELECT il.AD_Client_ID, il.AD_Org_ID,
	firstOf(il.DateInvoiced, 'DD') AS DateInvoiced,	--	DD Day, DY Week, MM Month
	SUM(il.LineNetAmt) AS LineNetAmt,
	SUM(il.LineListAmt) AS LineListAmt,
	SUM(il.LineLimitAmt) AS LineLimitAmt,
	SUM(il.LineDiscountAmt) AS LineDiscountAmt,
	CASE WHEN SUM(il.LineListAmt)=0 THEN 0 ELSE
	  ROUND((SUM(il.LineListAmt)-SUM(il.LineNetAmt))/SUM(il.LineListAmt)*100,2) END AS LineDiscount,
	SUM(il.LineOverLimitAmt) AS LineOverLimitAmt,
	CASE WHEN SUM(il.LineNetAmt)=0 THEN 0 ELSE
	  100-ROUND((SUM(il.LineNetAmt)-SUM(il.LineOverLimitAmt))/SUM(il.LineNetAmt)*100,2) END AS LineOverLimit,
    il.IsSOTrx, COALESCE(dt.DefiniteSequence_ID, dt.DocNoSequence_ID) AS DocNoSequence_ID, dt.ControlNoSequence_ID,
    dt.IsFiscalDocument, 
    MIN(CASE WHEN il.DocStatus IN('CO', 'CL') THEN i.DocumentNo ELSE NULL END) AS BeginingDocumentNo,
    MAX(CASE WHEN il.DocStatus IN('CO', 'CL') THEN i.DocumentNo ELSE '' END) AS EndingDocumentNo,
    MIN(CASE WHEN il.DocStatus IN('CO', 'CL') THEN i.ControlNo ELSE NULL END) AS BeginingControlNo,
    MAX(CASE WHEN il.DocStatus IN('CO', 'CL') THEN i.ControlNo ELSE '' END) AS EndingControlNo,
    dt.DocBaseType,
    SUM(currencyBase(il.LineNetAmt, i.C_Currency_ID, i.DateAcct, i.C_ConversionType_ID, i.AD_Client_ID, i.AD_Org_ID)) AS ConvertedAmt
FROM RV_C_InvoiceLine il
INNER JOIN C_Invoice i ON(i.C_Invoice_ID = il.C_Invoice_ID)
INNER JOIN C_DocType dt ON(dt.C_DocType_ID = il.C_DocTypeTarget_ID)
WHERE il.DocStatus IN('CO', 'CL', 'RE')
GROUP BY il.AD_Client_ID, il.AD_Org_ID, firstOf(il.DateInvoiced, 'DD'), il.IsSOTrx, COALESCE(dt.DefiniteSequence_ID, dt.DocNoSequence_ID), dt.ControlNoSequence_ID, dt.IsFiscalDocument, dt.DocBaseType;