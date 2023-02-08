/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2019 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.erpya.lve.util;

import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.core.domains.models.I_C_Order;
import org.compiere.model.MDocType;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.util.Util;

/**
 * Added for hamdle custom columns for ADempiere core
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class LVEUtil {
	/**	Fiscal Currency	*/
	public static final String COLUMNNAME_LVE_FiscalCurrency_ID = "LVE_FiscalCurrency_ID";
	/**	Invoice Currency	*/
	public static final String COLUMNNAME_LVE_InvoiceCurrency_ID = "LVE_InvoiceCurrency_ID";
	/**	Invoice generated with organization currency	*/
	public static final String COLUMNNAME_IsInvoicedWithOrgCurrency = "IsInvoicedWithOrgCurrency";
	/**	Client As Organization	*/
	public static final String COLUMNNAME_IsDefinedAsClient = "IsDefinedAsClient";
	/**	Invoice to Allocate	*/
	public static final String COLUMNNAME_InvoiceToAllocate_ID = "InvoiceToAllocate_ID";
	/**	Fiscal Document	*/
	public static final String COLUMNNAME_IsFiscalDocument = "IsFiscalDocument";
	/**	Flag Set Control No on Print Document	*/
	public static final String COLUMNNAME_IsSetControlNoOnPrint = "IsSetControlNoOnPrint";
	/**	Control No	*/
	public static final String COLUMNNAME_ControlNo = "ControlNo";
	/**	Control No Sequence for Invoices	*/
	public static final String COLUMNNAME_ControlNoSequence_ID = "ControlNoSequence_ID";
	/**	Has Special Tax flag for Tax Category	*/
	public static final String COLUMNNAME_IsHasSpecialTax = "IsHasSpecialTax";
	/**	Allocate flag for Document type	*/
	public static final String COLUMNNAME_IsAllocateInvoice = "IsAllocateInvoice";
	/**	Business Partner Person Type	*/
	public static final String COLUMNNAME_PersonType = "PersonType";
	/**	Withholding Tax Rate Applied	*/
	public static final String COLUMNNAME_WithholdingTaxRate_ID = "WithholdingTaxRate_ID";
	/**	Business Partner Big transaction Tax Exempt	*/
	public static final String COLUMNNAME_IsFBTTTaxExempt = "IsFBTTTaxExempt";
	/**	Business Partner Withholding Rental Tax Exempt	*/
	public static final String COLUMNNAME_IsWithholdingRentalExempt = "IsWithholdingRentalExempt";
	/**	Business Partner Withholding Tax Exempt	*/
	public static final String COLUMNNAME_IsWithholdingTaxExempt = "IsWithholdingTaxExempt";
	/**	Tribute Unit attribute	*/
	public static final String COLUMNNAME_TributeUnitType_ID = "TributeUnitType_ID";
	/**	Withholding Tax Applied flag	*/
	public static final String COLUMNNAME_IsWithholdingTaxApplied = "IsWithholdingTaxApplied";
	/**	Withholding Rental Rate Applied	*/
	public static final String COLUMNNAME_WithholdingRentalRate_ID = "WithholdingRentalRate_ID";
	/**	Withholding Rental Concept*/
	public static final String COLUMNNAME_WithholdingRentalConcept_ID = "WithholdingRentalConcept_ID";
	/**	Withholding Rental Variable Rate Applied	*/
	public static final String COLUMNNAME_WithholdingVariableRate_ID = "WithholdingVariableRate_ID";
	/**	Withholding Municipal Rate Applied	*/
	public static final String COLUMNNAME_WithholdingMunicipalRate_ID = "WithholdingMunicipalRate_ID";
	/**	Withholding municipal business activity	*/
	public static final String COLUMNNAME_BusinessActivity_ID = "BusinessActivity_ID";
	/**	Business Partner Withholding Municipal Exempt	*/
	public static final String COLUMNNAME_IsWithholdingMunicipalExempt = "IsWithholdingMunicipalExempt";
	/**Withholding Is Variable Rate*/
	public static final String COLUMNNAME_IsVariableRate = "IsVariableRate";
	/**Withholding Variable Rate*/
	public static final String COLUMNNAME_VariableRate = "Col_1";
	/**Withholding Subtrahend*/
	public static final String COLUMNNAME_Subtrahend = "Subtrahend";
	/**Withholding Is Cumulative */
	public static final String COLUMNNAME_IsCumulativeWithholding = "IsCumulativeWithholding";
	/**	Preborn leave paid	*/
	public static final String COLUMNNAME_IsPrebornLeavePaid = "IsPrebornLeavePaid";
	/**Withholding Third Party */
	public static final String COLUMNNAME_WHThirdParty_ID = "WHThirdParty_ID";
	/**Is Simulation Withholding*/
	public static final String COLUMNNAME_IsSimulation = "IsSimulation";
	/**Affected Document No*/
	public static final String COLUMNNAME_AffectedDocumentNo = "AffectedDocumentNo";
	/**	Bank client Number	*/
	public static final String COLUMNNAME_BankClientNo = "BankClientNo";
	/**	Is a Customer Tax Payer	*/
	public static final String COLUMNNAME_IsTaxpayer = "IsTaxpayer";
	/**SICA Code (Sistema Integral de Control Agroalimentario)*/
	public static final String COLUMNNAME_SICACode = "SICACode";
	/**BANAVIH Code */
	public static final String COLUMNNAME_BANAVIHCode = "BANAVIHCode";
	/**Recalculate Price on Invoice	*/
	public static final String COLUMNNAME_IsReCalculatePriceOnInvoice = "IsReCalculatePriceOnInvoice";
	/**Withholding Business Partner for Organization*/
	public static final String COLUMNNAME_WH_BPartner_ID = "WH_BPartner_ID";
	/**	Validate Value for Business partner	*/
	public static final String ENABLE_CODE_TYPE_VALIDATION = "ENABLE_CODE_TYPE_VALIDATION";
	/**	Validate Value for Multi Client	*/
	public static final String ENABLE_MULTI_CLIENT = "ENABLE_MULTI_CLIENT";
	/**	Commercial Activity License	*/
	public static final String COLUMNNAME_LVE_CommercialActivityLicense = "LVE_CommercialActivityLicense";
	/**	Default Statement Charge	*/
	public static final String COLUMNNAME_LVE_DefaultStatementCharge_ID = "LVE_DefaultStatementCharge_ID";
	/**
	 * Process Business Partner Value
	 * @param value
	 * @return
	 */
	public static String processBusinessPartnerValue(Properties context, int clientId, int organizationId, String value) {
		if(Util.isEmpty(value)) {
			value = "";
		}
		//	Trim it
		value = value.trim();
		boolean isValidationEnabled = MSysConfig.getBooleanValue(ENABLE_CODE_TYPE_VALIDATION, false, clientId, organizationId);
		//	Validate
		if(isValidationEnabled) {
			//	validate length
			if(value.length() < 6 || value.length() > 11) {
				throw new AdempiereException("@LVEInvalidBPValue@");
			}
			Matcher matcher = Pattern.compile("[^0-9JVEGjveg]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value);
			if(matcher.find()) {
				//	Error
				throw new AdempiereException("@LVEInvalidBPValue@");
			}
			//	Validate segments
			Matcher matcherforKey = Pattern.compile("^[JVEGjveg]+$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value.substring(0,  1));
			if(!matcherforKey.find()) {
				//	Error
				throw new AdempiereException("@LVEInvalidBPValue@");
			}
			if(!value.substring(1,  value.length() -1).matches("[+-]?\\d*(\\.\\d+)?")) {
				//	Error
				throw new AdempiereException("@LVEInvalidBPValue@");
			}
		}
		//	Default
		return Optional.ofNullable(value).orElse("")
				.replaceAll("[^0-9JVEGjveg]", "")
				.toUpperCase();
	}
	
	/**
	 * Set default values from document type
	 * @param entity
	 */
	public static void setDefaultValuesFromDocumentType(PO entity) {
		int columnIndex = entity.get_ColumnIndex(I_C_Order.COLUMNNAME_C_DocTypeTarget_ID);
		if(columnIndex < 0) {
			columnIndex = entity.get_ColumnIndex(I_C_Order.COLUMNNAME_C_DocType_ID);
		}
		//	Nothing
		if(columnIndex < 0) {
			return;
		}
		int documentTypeId = entity.get_ValueAsInt(columnIndex);
		//	No has a Document Type selected
		if(documentTypeId <= 0) {
			return;
		}
		if(entity.is_new()
				|| entity.is_ValueChanged(columnIndex)) {
			MDocType documentType = MDocType.get(entity.getCtx(), documentTypeId);
			entity.set_ValueOfColumn(COLUMNNAME_IsWithholdingTaxExempt, documentType.get_Value(COLUMNNAME_IsWithholdingTaxExempt));
			//	Add here other columns from document type
		}
	}
}
