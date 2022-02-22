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
import java.util.Optional;
import java.util.stream.Collectors;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPayment;
import org.compiere.model.MTable;
import org.compiere.model.MTax;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.erpya.lve.model.MLVEList;
import org.erpya.lve.model.MLVEWithholdingTax;
import org.spin.model.I_WH_Withholding;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * 	Implementación de retención de I.V.A para la localización de Venezuela
 * 	Esto puede aplicar para Documentos por Pagar y Notas de Crédito de Documentos por Pagar
 * 	Note que la validación de las 20 UT solo aplica para documentos por pagar
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *  @contributor Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 */
public class APInvoiceIVA extends AbstractWithholdingSetting {

	public APInvoiceIVA(MWHSetting setting) {
		super(setting);
	}
	/**	Current Invoice	*/
	private MInvoice invoice;
	/**	Current Business Partner	*/
	private MBPartner businessPartner;
	/**	Taxes	*/
	private List<MInvoiceTax> taxes;
	/**Manual Withholding*/
	private boolean isManual = false;
	/**Withholding Rate*/
	BigDecimal withholdingRate = Env.ZERO;
	@Override
	public boolean isValid() {
		boolean isValid = true;
		//	Validate Document
		if(getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			addLog("@C_Invoice_ID@ @NotFound@");
			isValid = false;
		}
		invoice = (MInvoice) getDocument();
		businessPartner = (MBPartner) invoice.getC_BPartner();
		if(!businessPartner.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsTaxpayer)
				&& invoice.isSOTrx()) {
			addLog("@C_BPartner_ID@ @" + LVEUtil.COLUMNNAME_IsTaxpayer + "@ @NotFound@");
			isValid = false;
		}
		//Valid Business Partner
		Optional.ofNullable(invoice)
				.ifPresent(invoice ->{
					//Add reference
					setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoice.getC_Invoice_ID());
					setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, invoice.getAD_Org_ID());
					if (invoice.isSOTrx()) {
						isManual = true;
						Optional.ofNullable(MOrgInfo.get(getContext(), invoice.getAD_Org_ID(), invoice.get_TrxName()))
								.ifPresent(orgInfo ->{
								businessPartner = MBPartner.get(getContext(), orgInfo.get_ValueAsInt(LVEUtil.COLUMNNAME_WH_BPartner_ID));
						});
					} else {
						isManual = false;
					}
				});
		
		if (businessPartner==null) {
			addLog("@C_BPartner_ID@ @NotFound@");
			isValid = false;
		} else {
			//	Add reference
			setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoice.getC_Invoice_ID());
			MLVEWithholdingTax currentWHTax = MLVEWithholdingTax.getFromClient(getContext(), getDocument().getAD_Org_ID(),MLVEWithholdingTax.TYPE_IVA);
			//	Validate if exists Withholding Tax Definition for client
			if(currentWHTax == null) {
				addLog("@LVE_WithholdingTax_ID@ @NotFound@");
				isValid = false;
			}
			
			//	Validate if withholding if exclude for client
			if(currentWHTax!=null 
					&& currentWHTax.isClientExcluded()) {
				addLog("@IsClientExcluded@ " + currentWHTax.getName());
				isValid = false;
			}
			
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
			//	Validate AP/AR Invoice only
			if(documentType!=null 
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice)
						&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APCreditMemo)
							&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARInvoice)
								&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo)) {
				addLog("@APDocumentRequired@ / @ARDocumentRequired@");
				isValid = false;
			}
			//	Validate Exempt Document
			if(invoice.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsWithholdingTaxExempt)) {
				isValid = false;
				addLog("@DocumentWithholdingTaxExempt@");
			}
			//	Validate Exempt Business Partner
			if(businessPartner.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsWithholdingTaxExempt)) {
				isValid = false;
				addLog("@BPartnerWithholdingTaxExempt@");
			}
			//	
			if(invoice.isSOTrx() && !businessPartner.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsTaxpayer)) {
				isValid = false;
				addLog("@C_BPartner_ID@ @" + LVEUtil.COLUMNNAME_IsTaxpayer + "@ @Invalid@");
			}
			//	Validate Withholding Definition
			//MLVEWithholdingTax withholdingTaxDefinition = MLVEWithholdingTax.getFromClient(getContext(), invoice.getAD_Org_ID());
			int withholdingRateId = businessPartner.get_ValueAsInt(LVEUtil.COLUMNNAME_WithholdingTaxRate_ID);
			if(withholdingRateId == 0
					&& currentWHTax!=null) {
				withholdingRateId = currentWHTax.getDefaultWithholdingRate_ID();
			}
			//	Validate Definition
			if(withholdingRateId == 0) {
				addLog("@" + LVEUtil.COLUMNNAME_WithholdingTaxRate_ID + "@ @NotFound@");
				isValid = false;
			} else {
				withholdingRate = MLVEList.get(getContext(), withholdingRateId).getListVersionAmount(invoice.getDateInvoiced());
				setWithholdingRate(withholdingRate);
			}
			//	Validate Tax
			if(getWithholdingRate().equals(Env.ZERO)) {
				addLog("@LVE_WithholdingTax_ID@ (@Rate@ @NotFound@)");
				isValid = false;
			}
			//	Validate Tribute Unit
			
			BigDecimal tributeUnitAmount = Env.ZERO;
			if (currentWHTax != null)
				tributeUnitAmount = currentWHTax.getValidTributeUnitAmount(invoice.getDateAcct());
			
			if(tributeUnitAmount.equals(Env.ZERO)) {
				addLog("@TributeUnit@ (@Rate@ @NotFound@)");
				isValid = false;
			}
			//	Validate if it have taxes
			taxes = Arrays.asList(invoice.getTaxes(false))
				.stream()
				.filter(invoiceTax -> MTax.get(getContext(), invoiceTax.getC_Tax_ID()).get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsWithholdingTaxApplied) 
						&& invoiceTax.getTaxAmt() != null 
						&& invoiceTax.getTaxAmt().compareTo(Env.ZERO) > 0)
				.collect(Collectors.toList());
			if(taxes.size() == 0) {
				addLog("@NoTaxesForWithholding@");
				isValid = false;
			}
	
			//Validate if Document is Generated
			if (isGenerated()) {
				isValid = false;
			}
		}
		return isValid;
		
	}

	@Override
	public String run() {
		//	Iterate
		taxes.forEach(invoiceTax -> {
			setWithholdingRate(withholdingRate);
			addBaseAmount(invoiceTax.getTaxAmt());
			addWithholdingAmount(invoiceTax.getTaxAmt().multiply(getWithholdingRate(true)));
			MTax tax = MTax.get(getContext(), invoiceTax.getC_Tax_ID());
			addDescription(tax.getName() + " @Processed@");
			setReturnValue(MWHWithholding.COLUMNNAME_IsManual, isManual);
			int WHThirdParty_ID = invoice.get_ValueAsInt(LVEUtil.COLUMNNAME_WHThirdParty_ID);
			if (WHThirdParty_ID != 0)
				setReturnValue(LVEUtil.COLUMNNAME_WHThirdParty_ID, WHThirdParty_ID);
			setReturnValue(MWHWithholding.COLUMNNAME_C_Tax_ID, invoiceTax.getC_Tax_ID());
			saveResult();
		});
		removeReferencesFromPOS();
		return null;
	}
	
	/**
	 * Remove POS references
	 */
	private void removeReferencesFromPOS() {
		if(invoice.isSOTrx() && invoice.getC_Order_ID() > 0) {
			//	Add backward compatibility
			MTable paymentReferenceDefinition = MTable.get(getContext(), "C_POSPaymentReference");
			if(paymentReferenceDefinition != null) {
				PO paymentReferenceToCreate = new Query(getContext(), "C_POSPaymentReference", "C_Order_ID = ? AND TenderType = ?", getTransactionName()).setParameters(invoice.getC_Order_ID(), MPayment.TENDERTYPE_CreditMemo).first();
				if(paymentReferenceToCreate != null
						&& paymentReferenceToCreate.get_ID() > 0) {
					paymentReferenceToCreate.set_ValueOfColumn("Processed", true);
					paymentReferenceToCreate.saveEx();
				}
			}
		}
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
																	+ "AND IsSimulation='N' "
																	+ "AND DocStatus IN (?,?)" , getTransactionName())
						.setParameters(invoice.get_ID(),getDefinition().get_ID(),getSetting().get_ID(),MWHWithholding.DOCSTATUS_Completed,MWHWithholding.DOCSTATUS_Closed)
						.match();
		return false;
	}
}


