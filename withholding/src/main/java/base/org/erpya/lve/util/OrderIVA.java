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

import org.compiere.model.I_C_Order;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderTax;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MTax;
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
public class OrderIVA extends AbstractWithholdingSetting {

	public OrderIVA(MWHSetting setting) {
		super(setting);
	}
	/**	Current order	*/
	private MOrder order;
	/**	Current Business Partner	*/
	private MBPartner businessPartner;
	/**	Taxes	*/
	private List<MOrderTax> taxes;
	/**Manual Withholding*/
	private boolean isManual = false;
	/**Withholding Rate*/
	BigDecimal withholdingRate = Env.ZERO;
	@Override
	public boolean isValid() {
		boolean isValid = true;
		//	Validate Document
		if(getDocument().get_Table_ID() != I_C_Order.Table_ID) {
			addLog("@C_Order_ID@ @NotFound@");
			isValid = false;
		}
		order = (MOrder) getDocument();
		businessPartner = (MBPartner) order.getC_BPartner();
		
		//Valid Business Partner
		Optional.ofNullable(order)
				.ifPresent(order ->{
					//Add reference
					setReturnValue(I_WH_Withholding.COLUMNNAME_SourceOrder_ID, order.getC_Order_ID());
					setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, order.getAD_Org_ID());
					if (order.isSOTrx()) {
						isManual = true;
						Optional.ofNullable(MOrgInfo.get(getContext(), order.getAD_Org_ID(), order.get_TrxName()))
								.ifPresent(orgInfo ->{
								businessPartner = MBPartner.get(getContext(), orgInfo.get_ValueAsInt(LVEUtil.COLUMNNAME_WH_BPartner_ID));
						});
					}else
						isManual = false;
				});
		
		if (businessPartner==null) {
			addLog("@C_BPartner_ID@ @NotFound@");
			isValid = false;
		} else {
			//	Add reference
			setReturnValue(I_WH_Withholding.COLUMNNAME_SourceOrder_ID, order.getC_Order_ID());
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
			if(!order.getDocStatus().equals(MOrder.DOCSTATUS_Completed)) {
				addLog("@Invalid@ @C_Order_ID@ @DocStatus@");
				isValid = false;
			}
			
			MDocType documentType = MDocType.get(getContext(), order.getC_DocTypeTarget_ID());
			if(documentType == null) {
				addLog("@C_DocType_ID@ @NotFound@");
				isValid = false;
			}
			//	Validate Exempt Document
			if(order.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsWithholdingTaxExempt)) {
				isValid = false;
				addLog("@DocumentWithholdingTaxExempt@");
			}
			//	Validate Exempt Business Partner
			if(businessPartner.get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsWithholdingTaxExempt)) {
				isValid = false;
				addLog("@BPartnerWithholdingTaxExempt@");
			}
			//	Validate Withholding Definition
			//MLVEWithholdingTax withholdingTaxDefinition = MLVEWithholdingTax.getFromClient(getContext(), order.getAD_Org_ID());
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
				withholdingRate = MLVEList.get(getContext(), withholdingRateId).getListVersionAmount(order.getDateOrdered());
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
				tributeUnitAmount = currentWHTax.getValidTributeUnitAmount(order.getDateAcct());
			
			if(tributeUnitAmount.equals(Env.ZERO)) {
				addLog("@TributeUnit@ (@Rate@ @NotFound@)");
				isValid = false;
			}
			//	Validate if it have taxes
			taxes = Arrays.asList(order.getTaxes(false))
				.stream()
				.filter(orderTax -> MTax.get(getContext(), orderTax.getC_Tax_ID()).get_ValueAsBoolean(LVEUtil.COLUMNNAME_IsWithholdingTaxApplied) 
						&& orderTax.getTaxAmt() != null 
						&& orderTax.getTaxAmt().compareTo(Env.ZERO) > 0)
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
		taxes.forEach(orderTax -> {
			setWithholdingRate(withholdingRate);
			addBaseAmount(orderTax.getTaxAmt());
			addWithholdingAmount(orderTax.getTaxAmt().multiply(getWithholdingRate(true)));
			MTax tax = MTax.get(getContext(), orderTax.getC_Tax_ID());
			addDescription(tax.getName() + " @Processed@");
			setReturnValue(MWHWithholding.COLUMNNAME_IsManual, isManual);
			int WHThirdParty_ID = order.get_ValueAsInt(LVEUtil.COLUMNNAME_WHThirdParty_ID);
			if (WHThirdParty_ID != 0)
				setReturnValue(LVEUtil.COLUMNNAME_WHThirdParty_ID, WHThirdParty_ID);
			setReturnValue(MWHWithholding.COLUMNNAME_C_Tax_ID, orderTax.getC_Tax_ID());
			setReturnValue(MWHWithholding.COLUMNNAME_IsSimulation, true);
			saveResult();
			
		});
		return null;
	}
	
	/**
	 * Validate if the document has withholding allocated
	 * @return
	 */
	private boolean isGenerated() {
		if (order!=null) 
			return new Query(getContext(), MWHWithholding.Table_Name, "SourceOrder_ID = ? "
																	+ "AND WH_Definition_ID = ? "
																	+ "AND WH_Setting_ID = ? "
																	+ "AND Processed = 'Y' "
																	+ "AND IsSimulation='Y' "
																	+ "AND DocStatus IN (?,?)" , getTransactionName())
						.setParameters(order.get_ID(),getDefinition().get_ID(),getSetting().get_ID(),MWHWithholding.DOCSTATUS_Completed,MWHWithholding.DOCSTATUS_Closed)
						.match();
		return false;
	}
}


