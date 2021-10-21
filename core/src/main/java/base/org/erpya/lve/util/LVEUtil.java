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

/**
 * Added for hamdle custom columns for ADempiere core
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class LVEUtil {
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
	/**SICA Code (Sistema Integral de Control Agroalimentario)*/
	public static final String COLUMNNAME_SICACode = "SICACode";
	/**BANAVIH Code */
	public static final String COLUMNNAME_BANAVIHCode = "BANAVIHCode";
	/**Recalculate Price on Invoice	*/
	public static final String COLUMNNAME_IsReCalculatePriceOnInvoice = "IsReCalculatePriceOnInvoice";
	/**Withholding Business Partner for Organization*/
	public static final String COLUMNNAME_WH_BPartner_ID = "WH_BPartner_ID";
	
	/**
	 * Process Business Partner Value
	 * @param value
	 * @return
	 */
	public static String processBusinessPartnerValue(String value) {
		return Optional.ofNullable(value).orElse("")
				.replaceAll("[^0-9JVEGjveg]", "")
				.toUpperCase();
	}
}
