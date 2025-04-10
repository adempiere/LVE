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

import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.core.domains.models.I_C_Order;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPInstance;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.eevolution.distribution.model.MDDOrder;
import org.eevolution.services.dsl.ProcessBuilder;
import org.erpya.lve.model.LVE;

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
	/**LVE Cesta Ticket Code */
	public static final String LVE_CestaTicketCode = "LVE_CestaTicketCode";
	/**LVE Cesta Ticket Product Code */
	public static final String LVE_CT_ProductCode = "LVE_CT_ProductCode";
	/**LVE Cesta Ticket Product Category Code */
	public static final String LVE_CT_ProductCategoryCode = "LVE_CT_ProductCategoryCode";
	/**LVE Cesta Ticket Delivery Point */
	public static final String LVE_CT_DeliveryPoint = "LVE_CT_DeliveryPoint";
	/**Recalculate Price on Invoice	*/
	public static final String COLUMNNAME_IsReCalculatePriceOnInvoice = "IsReCalculatePriceOnInvoice";
	/**Withholding Business Partner for Organization*/
	public static final String COLUMNNAME_WH_BPartner_ID = "WH_BPartner_ID";
	/**	Validate Value for Business partner	*/
	public static final String ENABLE_CODE_TYPE_VALIDATION = "ENABLE_CODE_TYPE_VALIDATION";
	/**	Validate Value for Multi Client	*/
	public static final String ENABLE_MULTI_CLIENT = "ENABLE_MULTI_CLIENT";
	/**	Commercial Activity License	*/
	public static final String LVE_CommercialActivityLicense = "LVE_CommercialActivityLicense";
	/**	Default Statement Charge	*/
	public static final String COLUMNNAME_LVE_DefaultStatementCharge_ID = "LVE_DefaultStatementCharge_ID";
	/**	Automatic Debt Note Invoice Referenced	*/
	public static final String COLUMNNAME_LVE_AutoDebitInvoice_ID = "LVE_AutoDebitInvoice_ID";
	/**	Automatic Debt Note Invoice Referenced	*/
	public static final String COLUMNNAME_LVE_AllowOverPayInvoice = "LVE_AllowOverPayInvoice";
	/**	Allows overwrite document	*/
	public static final String LVE_AllowOverdraftReference = "LVE_AllowOverdraftReference";
	/**	Value and Tax ID Mismatch	*/
	public static final String MESSAGE_LVE_ValueTaxIdMismatch = "LVE.ValueTaxIdMismatch";
	/**	Document Type for Shipment Note ColumnName*/
	public static final String COLUMNNAME_LVE_DocTypeForShipmentNote_ID = "LVE_DocTypeForShipmentNote_ID";
	/**	Is Shipment Note ColumnName*/
	public static final String COLUMNNAME_LVE_IsShipmentNote = "LVE_IsShipmentNote";
	/**	Order Shipment Note ColumnName*/
	public static final String COLUMNNAME_LVE_DD_OrderRef_ID = "LVE_DD_OrderRef_ID";
	/**	System Configuration Variable for Validate Invoice Line with Negative Prices*/
	public static final String SYSCONFIG_LVE_ValidateInvoiceNegative = "LVE_VALIDATE_INVOICE_NEGATIVE";
	/**	System Configuration Variable for Validate Warning low Control Number*/
	public static final String SYSCONFIG_LVE_WarningControlNumberAvailable = "LVE_WARNING_CONTROL_NUMBER_AVAILABLE";
	/**	System Configuration Variable for Validate Control Number On Invoice*/
	public static final String SYSCONFIG_LVE_ValidateControlNumberOnInvoice = "LVE_VALIDATE_CONTROL_NUMBER_ON_INVOICE";
	/**	System Configuration Variable for Validate Control Number On Sales Order*/
	public static final String SYSCONFIG_LVE_ValidateControlNumberOnSalesOrder = "LVE_VALIDATE_CONTROL_NUMBER_ON_SALES_ORDER";
	/**	System Configuration Variable for Validate Control Number On Inventory Movement*/
	public static final String SYSCONFIG_LVE_ValidateControlNumberOnInventoryMovement = "LVE_VALIDATE_CONTROL_NUMBER_ON_INVENTORY_MOVEMENT";
	/**	System Message for Validate Warning low Control Number*/
	public static final String MESSAGE_LVE_WarningControlNumber= "LVE_WARNING_CONTROL_NUMBER";
	/**	End Control Number ColumnName*/
	public static final String COLUMNNAME_LVE_SequenceEndNo = "LVE_SequenceEndNo";
	/** Logger */
	private static CLogger log = CLogger.getCLogger(LVEUtil.class);
	
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
			//	Validate if don't have numbers and allowed characters
			Matcher matcher = Pattern.compile("[^0-9JVEGX]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value);
			if(matcher.find()) {
				throw new AdempiereException("@LVEInvalidBPValue@");
			}
			//	Validate the first character allowed
			Matcher matcherforKey = Pattern.compile("^[JVEGX]+$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value.substring(0,  1));
			if(!matcherforKey.find()) {
				throw new AdempiereException("@LVEInvalidBPValue@");
			}
			//	Validate that the rest of string have only numbers
			Matcher matcherForNumbers = Pattern.compile("[^0-9]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(value.substring(1,  value.length()));
			if(matcherForNumbers.find()) {
				throw new AdempiereException("@LVEInvalidBPValue@");
			}
		}
		//	Default
		return Optional.ofNullable(value).orElse("")
				.replaceAll("[^0-9JVEGXjvegx]", "")
				.toUpperCase();
	}
	
	/**
	 * Get Valid Tax ID
	 * @param context
	 * @param clientId
	 * @param organizationId
	 * @param value
	 * @return
	 */
	public static String getValidTaxId(Properties context, int clientId, int organizationId, String value) {
		String taxId = processBusinessPartnerValue(context, clientId, organizationId, value);
		String taxIdOnlyNumbers = taxId.replaceAll("\\D+","");
		taxIdOnlyNumbers = String.format("%1$" + 9 + "s", taxIdOnlyNumbers).replace(" ", "0");
		return taxId.substring(0, 1) + taxIdOnlyNumbers;
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
	
	/**
	 * Create Sales Order (Shipment Guide From Distribution Order)
	 * @param ddOrder
	 */
	public static String createSalesOrderFromDistributionOrder(MDDOrder ddOrder) {
		MOrder currentSalesOrderGenerated = new Query(ddOrder.getCtx(), MOrder.Table_Name, "DocStatus IN('CO', 'CL') AND LVE_DD_OrderRef_ID=?", ddOrder.get_TrxName())
												.setParameters(ddOrder.get_ID())
												.first();
		Optional.ofNullable(currentSalesOrderGenerated)
				.ifPresent(SalesOrderGenerated ->{
					throw new AdempiereException("@LVE_SHIPMENT_NOTE_GENERATED_WARNING@ ".concat(SalesOrderGenerated.getDocumentNo()));
				});
		MWarehouse warehouse = getDistributionOrderWarehouseDestinationIdentifier(ddOrder);
		String returnValue = "";
		if (warehouse !=null &&
				warehouse.get_ID() > 0) {
			MOrder salesOrder  = new MOrder(ddOrder.getCtx(), 0, ddOrder.get_TrxName());
			MBPartner bPartner = MBPartner.get(ddOrder.getCtx(), ddOrder.getC_BPartner_ID());
			MDocType ddOrderDocumentType = MDocType.get(ddOrder.getCtx(), ddOrder.getC_DocType_ID());
			if (ddOrderDocumentType.get_ValueAsInt(COLUMNNAME_LVE_DocTypeForShipmentNote_ID) == 0)
				throw new AdempiereException("@C_DocType_ID@ ".concat(ddOrderDocumentType.getName()).concat("-> @LVE_DocTypeForShipmentNote_ID@ @NotValid@"));
			
			MDocType salesOrderDocumentType =  MDocType.get(ddOrder.getCtx(), ddOrderDocumentType.get_ValueAsInt(COLUMNNAME_LVE_DocTypeForShipmentNote_ID));
			MOrder.copyValues(ddOrder, salesOrder, true);
			salesOrder.setIsSOTrx(true);
			salesOrder.setAD_Org_ID(warehouse.getAD_Org_ID());
			salesOrder.setPOReference(ddOrder.getPOReference());
			salesOrder.setC_DocTypeTarget_ID(salesOrderDocumentType.getC_DocType_ID());
			salesOrder.setC_DocType_ID(salesOrderDocumentType.getC_DocType_ID());
			salesOrder.setBPartner(bPartner);
			salesOrder.setM_Warehouse_ID(warehouse.get_ID());
			salesOrder.set_ValueOfColumn(COLUMNNAME_LVE_DD_OrderRef_ID, ddOrder.get_ID());
			salesOrder.saveEx();
			
			ddOrder.getLines()
				   .forEach(ddOrderLine -> {
					   MOrderLine salesOrderLine = new MOrderLine(salesOrder);
					   MProduct product = MProduct.get(ddOrderLine.getCtx(), ddOrderLine.getM_Product_ID());
					   MOrderLine.copyValues(ddOrderLine, salesOrderLine, true);
					   salesOrderLine.setAD_Org_ID(warehouse.getAD_Org_ID());
					   salesOrderLine.setProduct(product);
					   salesOrderLine.setQty(ddOrderLine.getQtyEntered());
					   salesOrderLine.setPrice();
					   salesOrderLine.saveEx();
				   });
			salesOrder.setDocAction(MOrder.DOCACTION_Complete);
			salesOrder.saveEx();
			salesOrder.processIt(MOrder.DOCACTION_Complete);
			salesOrder.saveEx();
			returnValue = salesOrder.getDocumentNo();
		}
		return returnValue;
	}
	
	/**
	 * Get Destination Warehouse Identifier
	 * @param ddOrder
	 * @return
	 */
	private static MWarehouse getDistributionOrderWarehouseDestinationIdentifier(MDDOrder ddOrder) {
		AtomicReference<MWarehouse> warehouse = new AtomicReference<MWarehouse>(null);
		AtomicReference<String> warehouseName = new AtomicReference<String>("");
		ddOrder.getLines()
			   .forEach(ddOrderLine -> {
				   if (warehouse.get() == null) {
					   MLocator currentLocator = MLocator.get(ddOrderLine.getCtx(), ddOrderLine.getM_LocatorTo_ID());
					   warehouse.set(MWarehouse.get(ddOrderLine.getCtx(), currentLocator.getM_Warehouse_ID()));
					   warehouseName.set(warehouse.get().getName());
				   }
				   MLocator currentLocator = MLocator.get(ddOrderLine.getCtx(), ddOrderLine.getM_LocatorTo_ID());
				   if (warehouse.get() != null 
						   && warehouse.get().get_ID() != currentLocator.getM_Warehouse_ID()) {
					   throw new AdempiereException("@M_Warehouse_ID@ ".concat(warehouseName.get()).concat(" != ").concat(currentLocator.getWarehouseName()));
				   }
			   });
		
		return warehouse.get();
	}
	
	/**
	 * Set Document as not printed
	 * @param maybeDocument
	 */
	public static void authorizePrinting(PO maybeDocument) {
		Optional.ofNullable(maybeDocument)
				.ifPresent(document -> {
					if (document.get_ColumnIndex(MInvoice.COLUMNNAME_IsPrinted) > 0) {
						updateDocumentPrintedStatus(document, false);
					}
				});
	}
	
	/**
	 * Set Document as not printed
	 * @param maybeDocument
	 */
	public static void printDocument(PO maybeDocument) {
		Optional.ofNullable(maybeDocument)
				.ifPresent(document -> {
					ReportEngine reportEngine = null;
					AtomicInteger reportType = new AtomicInteger(0);
					if (document.get_ColumnIndex(MInvoice.COLUMNNAME_IsPrinted) > 0) {
						if (document.get_Table_ID() == MOrder.Table_ID) {
							reportEngine = ReportEngine.get (document.getCtx(), ReportEngine.ORDER, document.get_ID(), document.get_TrxName());
							reportType.set(ReportEngine.ORDER);
						} else if (document.get_Table_ID() == MInvoice.Table_ID) {
							reportEngine = ReportEngine.get (document.getCtx(), ReportEngine.INVOICE, document.get_ID(), document.get_TrxName());
							reportType.set(ReportEngine.INVOICE);
						} else if (document.get_Table_ID() == MInOut.Table_ID) {
							reportEngine = ReportEngine.get (document.getCtx(), ReportEngine.SHIPMENT, document.get_ID(), document.get_TrxName());
							reportType.set(ReportEngine.SHIPMENT);
						}else if (document.get_Table_ID() == MMovement.Table_ID) {
							reportEngine = ReportEngine.get (document.getCtx(), ReportEngine.MOVEMENT, document.get_ID(), document.get_TrxName());
							reportType.set(ReportEngine.MOVEMENT);
						}
					}
					
					Optional.ofNullable(reportEngine)
							.ifPresent(reportEngineInstance -> {
								MPrintFormat printFormat= reportEngineInstance.getPrintFormat();
								if (printFormat.getJasperProcess_ID() > 0) {
									ProcessBuilder
										.create(document.getCtx())
										.process(printFormat.getJasperProcess_ID())
										.withRecordId(document.get_Table_ID(), document.get_ID())
										.withParameter(MPInstance.COLUMNNAME_Record_ID.toUpperCase(),document.get_ID())
										.withPrintPreview()
										.execute(document.get_TrxName());
								}else
									throw new AdempiereException("@AD_PrintFormat_ID@ ".concat(printFormat.getName()).concat("@NotFound@ @JasperProcess_ID@"));
								
								if (document.get_ColumnIndex(MInvoice.COLUMNNAME_Processed) > 0
										&& document.get_ValueAsBoolean(MInvoice.COLUMNNAME_Processed)) 
									updateDocumentPrintedStatus(document, true);
							});
					
					
				});
	}

	/**
	 * Method for Validate Invoice Line with Negative or ZERO Price
	 * @param invoice
	 */
	public static void validatePrice(MInvoice invoice) {
		boolean validateInvoiceNegativePrice = MSysConfig.getBooleanValue(SYSCONFIG_LVE_ValidateInvoiceNegative, false, invoice.getAD_Client_ID(), invoice.getAD_Org_ID());
		if (validateInvoiceNegativePrice) {
			Optional.ofNullable(invoice)
					.ifPresent(invoiceInstance -> {
						if (invoiceInstance.getReversal_ID() == 0
								&& invoiceInstance.isSOTrx()
									&& invoiceInstance.get_ValueAsBoolean(COLUMNNAME_IsFiscalDocument)) {
							Arrays.asList(invoiceInstance.getLines())
								  .forEach(invoiceLine -> {
									 if (invoiceLine.getPriceEntered().compareTo(Env.ZERO) <= 0)
										throw new AdempiereException("@C_InvoiceLine_ID@ -> ".concat(String.valueOf(invoiceLine.getLine())).concat(" @PriceEntered@ <= 0")); 
								  });
						}
					});
		}
	}
	
	/**
	 * Set Document Printed Status
	 * @param document
	 * @param printed
	 */
	public static void updateDocumentPrintedStatus (PO document, boolean printed)
	{
		StringBuffer sql = new StringBuffer();
		String keyColumn = (document.get_KeyColumns().length > 0 ? document.get_KeyColumns()[0] :"");
		if (!keyColumn.isEmpty()) {
			sql.append("UPDATE ").append(document.get_TableName())
				.append(" SET DatePrinted=SysDate, IsPrinted='" + (printed ? "Y" : "N") + "' WHERE ")
				.append(keyColumn).append("=").append(document.get_ID());
			int no = DB.executeUpdate(sql.toString(), document.get_TrxName());
			if (no != 1)
				log.log(Level.SEVERE, "Updated records=" + no + " - should be just one");
		}
	}	//	printConfirm
}
