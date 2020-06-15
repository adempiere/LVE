CREATE OR REPLACE VIEW LVE_RV_C_Invoice_Day_Summary
(AD_Client_ID, AD_Org_ID, DateInvoiced, LineNetAmt, 
 LineListAmt, LineLimitAmt, LineDiscountAmt, LineDiscount, LineOverLimitAmt, 
 LineOverLimit, IsSOTrx, C_DocTypeTarget_ID, DocStatus, IsFiscalDocument, BeginingDocumentNo, EndingDocumentNo, BeginingControlNo, EndingControlNo)
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
    il.IsSOTrx, il.C_DocTypeTarget_ID, il.DocStatus,
    i.IsFiscalDocument, 
    MIN(i.DocumentNo) AS BeginingDocumentNo,
    MAX(i.DocumentNo) AS EndingDocumentNo,
    MIN(i.ControlNo) AS BeginingControlNo,
    MAX(i.ControlNo) AS EndingControlNo
FROM RV_C_InvoiceLine il
INNER JOIN C_Invoice i ON(i.C_Invoice_ID = il.C_Invoice_ID)
GROUP BY il.AD_Client_ID, il.AD_Org_ID, firstOf(il.DateInvoiced, 'DD'), il.IsSOTrx, il.C_DocTypeTarget_ID, il.DocStatus, i.IsFiscalDocument;