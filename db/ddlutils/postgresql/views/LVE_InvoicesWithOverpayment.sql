DROP VIEW IF EXISTS LVE_InvoicesWithOverpayment;
CREATE OR REPLACE VIEW LVE_InvoicesWithOverpayment AS 
SELECT 
	AD_Client_ID,
	AD_Org_ID,
	C_Invoice_ID,
	DocumentNo,
	ControlNo,
	DateInvoiced,
	DateAcct,
	IsFiscalDocument,
	IsSOTrx,
	C_DocType_ID,
	C_BP_Group_ID,
	C_BPartner_ID,
	BPTaxID,
	BPName,
	C_Currency_ID,
	GrandTotal,
	LVE_FiscalCurrency_ID,
	ConvertedAmt,
	PaidAmt,
	(PaidAmt - ConvertedAmt) InvoiceAmt
FROM (
SELECT 
	i.AD_Client_ID,
	i.AD_Org_ID,
	i.C_Invoice_ID,
	i.DocumentNo,
	i.ControlNo,
	i.DateInvoiced,
	i.DateAcct,
	i.IsFiscalDocument,
	i.IsSOTrx,
	dt.C_DocType_ID,
	bp.C_BP_Group_ID,
	bp.C_BPartner_ID,
	bp.TaxID BPTaxID,
	bp.Name BPName,
	i.C_Currency_ID,
	i.GrandTotal,
	oi.LVE_FiscalCurrency_ID,
	currencyConvert(i.GrandTotal, i.C_Currency_ID, oi.LVE_FiscalCurrency_ID, i.DateAcct, i.C_ConversionType_ID, i.AD_Client_ID, i.AD_Org_ID) ConvertedAmt,
	invoicePaid(i.C_Invoice_ID, oi.LVE_FiscalCurrency_ID, 1) PaidAmt
FROM C_Invoice i
INNER JOIN C_DocType dt ON (i.C_DocTypeTarget_ID = dt.C_DocType_ID)
INNER JOIN C_BPartner bp ON (bp.C_BPartner_ID = i.C_BPartner_ID)
INNER JOIN AD_OrgInfo oi ON (i.AD_Org_ID = oi.AD_Org_ID)
WHERE 
dt.DocBaseType IN ('ARI', 'API')
AND i.DocStatus IN ('CO', 'CL')
) as invoice
WHERE
PaidAmt > ConvertedAmt ;