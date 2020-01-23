/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2016 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Carlos Parada www.erpya.com                                *
 *****************************************************************************/
package org.erpya.lve.util;


import java.math.BigDecimal;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.model.I_WH_Withholding;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

public class ARInvoice extends AbstractWithholdingSetting{

	/**	Current Invoice	*/
	private MInvoice invoice;
	/**Document Number*/
	private String documentNo = null;
	/**Withholding Rate */
	private BigDecimal whRate = Env.ZERO;
	/**Withholding Base Amount*/
	private BigDecimal whBaseAmt = Env.ZERO;
	/**Withholding Amount*/
	private BigDecimal whAmt = Env.ZERO;
	
	/**
	 * Constructor
	 * @param setting
	 */
	public ARInvoice(MWHSetting setting) {
		super(setting);
	}
	
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
		
		//	Validate Reversal
		if(invoice.isReversal()) {
			addLog("@C_Invoice_ID@ @Voided@");
			isValid = false;
		}
		
		MDocType documentType = MDocType.get(getContext(), invoice.getC_DocTypeTarget_ID());
		if(documentType == null) {
			addLog("@C_DocType_ID@ @NotFound@");
			isValid = false;
		}
		
		//	Validate AP only
		if(documentType!=null && 
				!documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARInvoice)
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)) {
			addLog("@ARDocumentRequired@");
			isValid = false;
		}

		//Set Values
		//	Validate Document Number
		if(getParameter(MWHWithholding.COLUMNNAME_DocumentNo)==null) {
			addLog("@WH_Withholding_ID@ (@DocumentNo@ @NotFound@)");
			isValid = false;
		}else if (getParameter(MWHWithholding.COLUMNNAME_DocumentNo).toString().equals("")){
			addLog("@WH_Withholding_ID@ (@DocumentNo@ @NotFound@)");
			isValid = false;
		}else 
			documentNo = getParameter(MWHWithholding.COLUMNNAME_DocumentNo).toString();
		
		
		whBaseAmt= getParameterAsBigDecimal(MWHWithholding.COLUMNNAME_A_Base_Amount);
		whRate  = getParameterAsBigDecimal(MWHWithholding.COLUMNNAME_WithholdingRate);
		whAmt  =  getParameterAsBigDecimal(MWHWithholding.COLUMNNAME_WithholdingAmt);
		
		//	Validate Rate
		if(whAmt.equals(Env.ZERO)) {
			addLog("@WH_Withholding_ID@ (@" + MWHWithholding.COLUMNNAME_WithholdingRate + "@ @NotFound@)");
			isValid = false;
		}

		//	Validate Base Amount
		if(whBaseAmt.equals(Env.ZERO)) {
			addLog("@WH_Withholding_ID@ (@" + MWHWithholding.COLUMNNAME_A_Base_Amount + "@ @NotFound@)");
			isValid = false;
		}

		//	Validate Withholding Amount
		if(whAmt.equals(Env.ZERO)) {
			addLog("@WH_Withholding_ID@ (@" + MWHWithholding.COLUMNNAME_WithholdingAmt + "@ @NotFound@)");
			isValid = false;
		}
		
		//Validate if Document is Generated
		if (isGenerated()) {
			isValid = false;
		}
		
		return isValid;
	}

	@Override
	public String run() {
		setReturnValue(MWHWithholding.COLUMNNAME_DocumentNo, documentNo);
		setReturnValue(MWHWithholding.COLUMNNAME_IsManual, true);
		setBaseAmount(whBaseAmt);
		setWithholdingRate(whRate);
		setWithholdingAmount(whAmt);
		
		return null;
	}

	/**
	 * Validate if the document has withholding allocated
	 * @return
	 */
	private boolean isGenerated() {
		if (invoice!=null) 
			return new Query(getContext(), MWHWithholding.Table_Name, "SourceInvoice_ID = ? "
																	+ "AND WH_Definition_ID = ? "
																	+ "AND WH_Setting_ID = ? "
																	+ "AND Processed = 'Y' "
																	+ "AND IsSimulation='N'"
																	+ "AND DocStatus IN (?,?)" , getTransactionName())
						.setParameters(invoice.get_ID(),getDefinition().get_ID(),getSetting().get_ID(),MWHWithholding.DOCSTATUS_Completed,MWHWithholding.DOCSTATUS_Closed)
						.match();
		return false;
	}
}
