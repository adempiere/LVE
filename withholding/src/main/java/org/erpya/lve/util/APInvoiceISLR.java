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
import java.math.MathContext;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MProduct;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.erpya.lve.model.MLVEList;
import org.erpya.lve.model.MLVEListLine;
import org.erpya.lve.model.MLVEListVersion;
import org.erpya.lve.model.MLVEWithholdingTax;
import org.erpya.lve.model.X_LVE_ListVersion;
import org.spin.model.I_WH_Withholding;
import org.spin.model.MWHSetting;
import org.spin.util.AbstractWithholdingSetting;

/**
 * 	Implementación de retención de I.S.L.R para la locacización de Venezuela
 * 	Note que básicamente se realiza una validación del documento en cuestión y luego se procesa
 * 	la generación del documento de retención o el log del documento queda a libertad de la clase abstracta
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *  @contributor Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 */
public class APInvoiceISLR extends AbstractWithholdingSetting {

	public APInvoiceISLR(MWHSetting setting) {
		super(setting);
	}
	/**	Current Invoice	*/
	private MInvoice invoice;
	/**	Current Business Partner	*/
	private MBPartner businessPartner;
	/**Person Type*/
	private String bpartnerPersonType = null;
	/**	Withholding Rental Exempt for Business Partner	*/
	private HashMap<MLVEList,BigDecimal>  conceptsToApply= new HashMap<MLVEList,BigDecimal>();
	/**Withholding Rental Rates to Apply*/
	private HashMap<MLVEListVersion,MLVEListLine> ratesToApply= new HashMap<MLVEListVersion,MLVEListLine>();
	/**Tribute Unit Amount */
	BigDecimal tributeUnitAmount = Env.ZERO;
	/**Factor*/
	private static BigDecimal FACTOR = new BigDecimal(83.3334);
	/**Subtract Amount*/
	private BigDecimal subtractAmt = Env.ZERO;
	/**Currency Precision */
	int curPrecision = 0 ;
	@Override
	public boolean isValid() {
		boolean isValid = true;
		//	Validate Document
		if(getDocument().get_Table_ID() != I_C_Invoice.Table_ID) {
			addLog("@C_Invoice_ID@ @NotFound@");
			isValid = false;
		}
		invoice = (MInvoice) getDocument();
		if (invoice!=null) {
			MCurrency currency = (MCurrency) invoice.getC_Currency();
			curPrecision = currency.getStdPrecision();
		}
		//	Add reference
		setReturnValue(I_WH_Withholding.COLUMNNAME_SourceInvoice_ID, invoice.getC_Invoice_ID());
		//	Validate if exists Withholding Tax Definition for client
		if(MLVEWithholdingTax.getFromClient(getContext(), getDocument().getAD_Org_ID()) == null) {
			addLog("@LVE_WithholdingTax_ID@ @NotFound@");
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
		//	Validate AP only
		if(documentType!=null 
				&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice)
					&& !documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APCreditMemo)) {
			addLog("@APDocumentRequired@");
			isValid = false;
		}
		//	Validate Person Type
		businessPartner = (MBPartner) invoice.getC_BPartner();
		bpartnerPersonType = businessPartner.get_ValueAsString(ColumnsAdded.COLUMNNAME_PersonType);
		if(Util.isEmpty(bpartnerPersonType)) {
			addLog("@" + ColumnsAdded.COLUMNNAME_PersonType + "@ @NotFound@ @C_BPartner_ID@ " + businessPartner.getValue() + " - " + businessPartner.getName());
			isValid = false;
		}
		//	Validate Exempt Business Partner
		if(businessPartner.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsWithholdingRentalExempt)) {
			isValid = false;
			addLog("@BPartnerWithholdingRentalExempt@");
		}
		//	Validate Withholding Definition
		setConcepts();
		if (conceptsToApply.size()==0) {
			isValid = false;
			addLog("@NotWitholdingRentalConcept@");
		}
		
		//	Validate Tribute Unit
		MLVEWithholdingTax withholdingTaxDefinition = MLVEWithholdingTax.getFromClient(getContext(), invoice.getAD_Org_ID());
		tributeUnitAmount = withholdingTaxDefinition.getValidTributeUnitAmount(invoice.getDateInvoiced());
		if(tributeUnitAmount.equals(Env.ZERO)) {
			addLog("@TributeUnit@ (@Rate@ @NotFound@)");
			isValid = false;
		}
		
		setRates();
		if (ratesToApply.size()==0) {
			isValid = false;
			addLog("@NotWitholdingRentalRates@");
		}

		return isValid;
	}

	@Override
	public String run() {
		conceptsToApply.forEach((whConcept,baseAmount) ->{
			ratesToApply.entrySet()
						.stream()
						.filter(ratesToApply -> ratesToApply.getKey().getLVE_List_ID() == whConcept.getLVE_List_ID())
						.forEach((rateToApply) -> {
							BigDecimal rate = Env.ZERO;
							if (!rateToApply.getKey().isVariableRate()) {
								rate = rateToApply.getKey().getAmount();
								setReturnValue(ColumnsAdded.COLUMNNAME_WithholdingRentalRate_ID, rateToApply.getKey().getLVE_ListVersion_ID());
							}
							else  {
								rate = (BigDecimal)rateToApply.getValue().get_Value(ColumnsAdded.COLUMNNAME_VariableRate);
								setReturnValue(ColumnsAdded.COLUMNNAME_WithholdingRentalRate_ID, rateToApply.getKey().getLVE_ListVersion_ID());
								setReturnValue(ColumnsAdded.COLUMNNAME_WithholdingVariableRate_ID, rateToApply.getValue().getLVE_ListLine_ID());
							}
							
							if (rate==null) 
								rate = Env.ZERO;
							
							if (rate.compareTo(Env.ZERO)!=0) {
								setWithholdingRate(rate);
								rate = getWithholdingRate(true);
								addBaseAmount(baseAmount);
								addWithholdingAmount(baseAmount.multiply(rate,MathContext.DECIMAL128)
																.setScale(curPrecision,BigDecimal.ROUND_HALF_UP)
																.subtract(subtractAmt));
								addDescription(whConcept.getName() + "@Processed@");
								setReturnValue(ColumnsAdded.COLUMNNAME_Subtrahend, subtractAmt);
								saveResult();
							}

						}
					);			
			}
		);
		
		conceptsToApply.clear();
		ratesToApply.clear();
		curPrecision = 0;
		subtractAmt = Env.ZERO;
		return null;
	}
	
	/**
	 * Set concepts from invoice document
	 */
	private void setConcepts() {
		if (invoice!=null) {
			if (invoice.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)!=0) {
				conceptsToApply.put(MLVEList.get(getContext(), invoice.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)),
										invoice.getTotalLines());
				return;
			}
			
			MInvoiceLine[] iLines = invoice.getLines();
			for (MInvoiceLine line : iLines) {
				//Search concept for product
				if (line.getM_Product_ID()!=0) {
					MProduct product = (MProduct)line.getM_Product();
					if (product.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)!=0) {
						MLVEList list = MLVEList.get(getContext(), product.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID));
						BigDecimal currentAmount = conceptsToApply.get(list);
						if (currentAmount==null)
							conceptsToApply.put(list,line.getLineNetAmt());
						else
							conceptsToApply.put(list,currentAmount.add(line.getLineNetAmt()));
					}
				}
				
				if (line.getC_Charge_ID()!=0) {
					MCharge charge = (MCharge)line.getC_Charge();
					if (charge.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)!=0) {
						MLVEList list = MLVEList.get(getContext(), charge.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID));
						BigDecimal currentAmount = conceptsToApply.get(list);
						if (currentAmount==null)
							conceptsToApply.put(list,line.getLineNetAmt());
						else
							conceptsToApply.put(list,currentAmount.add(line.getLineNetAmt()));
					}
				}
			}
		}
	}
	
	/**
	 * Set Rates
	 */
	private void setRates() {
		
		conceptsToApply.forEach((whConcept,baseAmount) ->{
			MLVEListVersion rateToApply = whConcept.getValidVersionInstance(invoice.getDateInvoiced(), ColumnsAdded.COLUMNNAME_PersonType, bpartnerPersonType);
			if (rateToApply!=null) {
				if (rateToApply.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsVariableRate)) {
					List<MLVEListLine> varRate= rateToApply.getListLine();
					if (varRate!=null) 
						ratesToApply.put(rateToApply,varRate.stream()
														.filter(listLine -> (
																baseAmount.compareTo(listLine.getMinValue().multiply(tributeUnitAmount))>=0 && 
																	(baseAmount.compareTo(listLine.getMaxValue().multiply(tributeUnitAmount))<=0 
																		|| listLine.getMaxValue().compareTo(Env.ZERO)==0))
																)
														.sorted(Comparator.comparing(MLVEListLine::getSeqNo))
														.findFirst()
														.get());
				}else {
					if (bpartnerPersonType.equals(X_LVE_ListVersion.PERSONTYPE_ResidentNaturalPerson)) {
						BigDecimal minValue = tributeUnitAmount.multiply(FACTOR,MathContext.DECIMAL128).setScale(curPrecision,BigDecimal.ROUND_HALF_UP);
						if (baseAmount.compareTo(minValue)>=0) {
							subtractAmt = minValue.multiply(rateToApply.getAmount()
																.divide(Env.ONEHUNDRED,MathContext.DECIMAL128)
															,MathContext.DECIMAL128)
													.setScale(curPrecision,BigDecimal.ROUND_HALF_UP);
							ratesToApply.put(rateToApply,null);
						}
					}else 
						ratesToApply.put(rateToApply,null);
				}
				
			}
		}
		);
		
	}
}
