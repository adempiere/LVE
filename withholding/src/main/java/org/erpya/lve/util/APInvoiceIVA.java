/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                      *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                      *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 * This program is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                    *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                     *
 * GNU General Public License for more details.                                      *
 * You should have received a copy of the GNU General Public License                 *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.erpya.lve.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.MTax;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.model.I_WH_Withholding;
import org.spin.model.MWHSetting;
import org.spin.util.AbstractWithholdingSetting;

/**
 * 	Implementation for IVA withholding
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 */
public class APInvoiceIVA extends AbstractWithholdingSetting {

	public APInvoiceIVA(MWHSetting setting) {
		super(setting);
	}
	/**	Current Invoice	*/
	private MInvoice invoice;
	/**	Taxes	*/
	private List<MInvoiceTax> taxes;
	/**	Attribute	*/
	private final String WITHHOLDING_APPLIED = "IsWithholdingApplied";
	private final String PERSON_TYPE = "PersonType";
	@Override
	public boolean isValid() {
		boolean isValid = true;
		//	Validate Document
		if(getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			addLog("@C_Invoice_ID@ @NotFound@");
			isValid = false;
		}
		invoice = (MInvoice) getDocument();
		//	Add reference
		setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoice.getC_Invoice_ID());
		if(invoice.isReversal()) {
			addLog("@C_Invoice_ID@ @Voided@");
			isValid = false;
		}
		MDocType documentType = MDocType.get(getCtx(), invoice.getC_DocTypeTarget_ID());
		if(documentType == null) {
			addLog("@C_DocType_ID@ @NotFound@");
			isValid = false;
		}
		//	Validate AP only
		if(!documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice)) {
			addLog("@APDocumentRequired@");
			isValid = false;
		}
		//	Validate Person Type
		MBPartner businessPartner = (MBPartner) invoice.getC_BPartner();
		if(Util.isEmpty(businessPartner.get_ValueAsString(PERSON_TYPE))) {
			addLog("@" + PERSON_TYPE + "@ @NotFound@ @C_BPartner_ID@ " + businessPartner.getValue() + " - " + businessPartner.getName());
			isValid = false;
		}
		//	Validate if it have taxes
		taxes = Arrays.asList(invoice.getTaxes(false))
			.stream()
			.filter(invoiceTax -> MTax.get(getCtx(), invoiceTax.getC_Tax_ID()).get_ValueAsBoolean(WITHHOLDING_APPLIED) 
					&& invoiceTax.getTaxAmt() != null 
					&& invoiceTax.getTaxAmt().compareTo(Env.ZERO) > 0)
			.collect(Collectors.toList());
		if(taxes.size() == 0) {
			addLog("@NoTaxesForWithholding@");
			isValid = false;
		}
		//	
		return isValid;
	}

	@Override
	public String run() {
		//	75 %
		BigDecimal withholdingRate = new BigDecimal(75).divide(Env.ONEHUNDRED);
		//	Iterate
		taxes.forEach(invoiceTax -> {
			addBaseAmount(invoiceTax.getTaxAmt());
			addWithholdingAmount(invoiceTax.getTaxAmt().multiply(withholdingRate));
			MTax tax = MTax.get(getCtx(), invoiceTax.getC_Tax_ID());
			addDescription(tax.getName() + " @Processed@");
		});
		return null;
	}
}
