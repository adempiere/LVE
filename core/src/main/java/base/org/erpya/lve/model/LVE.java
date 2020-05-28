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
package org.erpya.lve.model;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_I_BPartner;
import org.compiere.model.I_I_Invoice;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.erpya.lve.util.AllocationManager;
import org.erpya.lve.util.ColumnsAdded;
import org.erpya.lve.util.DocumentTypeSequence;
import org.spin.model.MWHWithholding;

/**
 * 	Add Default Model Validator for Location Venezuela
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class LVE implements ModelValidator {

	/**
	 * Constructor
	 */
	public LVE() {
		super();
	}

	/** Logger */
	private static CLogger log = CLogger
			.getCLogger(LVE.class);
	/** Client */
	private int clientId = -1;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		// Add Timing change in C_Order and C_Invoice
		engine.addDocValidate(MInvoice.Table_Name, this);
		
		engine.addModelChange(MInvoice.Table_Name, this);
		engine.addDocValidate(MInOut.Table_Name, this);
		engine.addModelChange(MBPartner.Table_Name, this);
		engine.addModelChange(MWHWithholding.Table_Name, this);
		engine.addModelChange(MInvoiceLine.Table_Name, this);
		
		LVEImport importValidator = new LVEImport(); 
		engine.addImportValidate(I_I_Invoice.Table_Name,importValidator);
		engine.addImportValidate(I_I_BPartner.Table_Name, importValidator);
	}

	@Override
	public int getAD_Client_ID() {
		return clientId;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		log.info("AD_User_ID=" + AD_User_ID);
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {
		//	
		if(timing == TIMING_BEFORE_COMPLETE) {
			if (po.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if(invoice.isReversal()) {
					invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsFiscalDocument, false);
					//	Save
					invoice.saveEx();
				} else {
					//	For credit memo and invoice to allocated
					MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
					//	For credit Memo
					if(invoice.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) == 0 
							&& (documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APCreditMemo) || documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_ARCreditMemo))) {
						Optional.ofNullable(invoice.getC_Order()).ifPresent(returnOrder -> {
							MDocType returnOrderSubType = MDocType.get(invoice.getCtx(), returnOrder.getC_DocType_ID());
							if(!Util.isEmpty(returnOrderSubType.getDocSubTypeSO())
									&& returnOrderSubType.getDocSubTypeSO().equals(MDocType.DOCSUBTYPESO_ReturnMaterial)) {
								Map<Integer, Integer> invoiceToAllocate = new HashMap<>();
								Map<Integer, Integer> invoiceLinesAllocated = new HashMap<>();
								List<MInvoiceLine> creditMemoLines = Arrays.asList(invoice.getLines());
								if(creditMemoLines.stream().filter(invoiceLine -> invoiceLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) != 0).count() == 0) {
									creditMemoLines
									.stream()
									.filter(creditMemoLine -> creditMemoLine.getC_OrderLine_ID() != 0)
									.forEach(creditMemoLine -> {
										MOrderLine returnOrderLine = (MOrderLine) creditMemoLine.getC_OrderLine();
										if(returnOrderLine.get_ValueAsInt("Ref_InvoiceLine_ID") != 0) {
											MInvoiceLine sourceInvoiceLine = new MInvoiceLine(creditMemoLine.getCtx(), returnOrderLine.get_ValueAsInt("Ref_InvoiceLine_ID"), creditMemoLine.get_TrxName());
											invoiceToAllocate.put(sourceInvoiceLine.getC_Invoice_ID(), creditMemoLine.getC_InvoiceLine_ID());
											invoiceLinesAllocated.put(creditMemoLine.getC_InvoiceLine_ID(), sourceInvoiceLine.getC_Invoice_ID());
										} else {
											Optional.ofNullable((MInvoiceLine) new Query(creditMemoLine.getCtx(), 
													MInvoiceLine.Table_Name, 
													"EXISTS (SELECT 1 "
													+ "FROM C_OrderLine oLine "
													+ "INNER JOIN M_InOutLine iol ON (oLine.Ref_InOutLine_ID = iol.M_InOutLine_ID) "
													+ "WHERE oLine.C_OrderLine_ID = ? AND iol.C_OrderLine_ID = C_InvoiceLine.C_OrderLine_ID "
													+ ")", 
													creditMemoLine.get_TrxName())
												.setParameters(creditMemoLine.getC_OrderLine_ID())
												.first()).ifPresent(sourceInvoiceLine -> {
														invoiceToAllocate.put(sourceInvoiceLine.getC_Invoice_ID(), creditMemoLine.getC_InvoiceLine_ID());
														invoiceLinesAllocated.put(creditMemoLine.getC_InvoiceLine_ID(), sourceInvoiceLine.getC_Invoice_ID());
												});
										}
									});
								}
								//	Set from parent or child
								if(invoiceToAllocate.size() == 1) {
									invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID, invoiceToAllocate.keySet().stream().findFirst().get());
									invoice.saveEx();
								} else if(invoiceToAllocate.size() > 0) {
									creditMemoLines
										.forEach(invoiceLine -> {
											int invoiceToAllocateId = invoiceLinesAllocated.get(invoiceLine.getC_InvoiceLine_ID());
											if(invoiceToAllocateId != 0) {
												invoiceLine.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID, invoiceToAllocateId);
												invoiceLine.saveEx();
											}
										});
								}
							}
						});
					}
				}
			}
		} else if(timing == TIMING_AFTER_COMPLETE)	{
			if (po.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if(!invoice.isReversal()) {
					MDocType documentType = MDocType.get(invoice.getCtx(), invoice.getC_DocTypeTarget_ID());
					invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsFiscalDocument,
							documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsFiscalDocument));
					//	Set Control No
					if(!documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsSetControlNoOnPrint)
							&& Util.isEmpty(invoice.get_ValueAsString(ColumnsAdded.COLUMNNAME_ControlNo))) {
						DocumentTypeSequence sequence = new DocumentTypeSequence(documentType);
						invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ControlNo, sequence.getControlNo());
					}
					//Set Document Number for Withholding
					if (new Query(invoice.getCtx(), MWHWithholding.Table_Name, "C_Invoice_ID = ? AND IsManual = 'N'", invoice.get_TrxName()).setParameters(invoice.getC_Invoice_ID()).match()) {
						//	Get Document No
						int docNo = Integer.parseInt(invoice.getDocumentNo());
						//	Format Date
						String format = "yyyyMM";
						SimpleDateFormat sdf = new SimpleDateFormat(format);
						String prefix = sdf.format(invoice.getDateInvoiced().getTime());
						if(prefix == null)
							prefix = "";
						//	Set New Document No
						invoice.setDocumentNo(prefix + String.format("%1$" + 8 + "s", docNo).replace(" ", "0"));
					}
					//	Create Allocation
					if(documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsAllocateInvoice)) {
						AllocationManager allocationManager = new AllocationManager(invoice);
						Arrays.asList(invoice.getLines())
							.stream()
							.filter(invoiceLine -> invoiceLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) != 0)
							.forEach(invoiceLine -> {
								allocationManager.addAllocateDocument(invoiceLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID), invoiceLine.getLineTotalAmt(), Env.ZERO, Env.ZERO);
							});
						//	Create Allocation
						allocationManager.createAllocation();
					}
					//	Save
					invoice.saveEx();
				}
			} else if(po.get_TableName().equals(MInOut.Table_Name)) {
				MInOut shipment = (MInOut) po;
				if(shipment.isReversal()) {
					shipment.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsFiscalDocument, false);
				} else {
					MDocType documentType = (MDocType) shipment.getC_DocType();
					shipment.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_IsFiscalDocument,
							documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsFiscalDocument));
					//	Set Control No
					if(!documentType.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsSetControlNoOnPrint)
							&& Util.isEmpty(shipment.get_ValueAsString(ColumnsAdded.COLUMNNAME_ControlNo))) {
						DocumentTypeSequence sequence = new DocumentTypeSequence(documentType);
						shipment.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_ControlNo, sequence.getControlNo());
					}
				}
				//	Save
				shipment.saveEx();
			}
		}
		//
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		if(type == TYPE_BEFORE_NEW 
				|| type == TYPE_BEFORE_CHANGE) {
			log.fine(" TYPE_BEFORE_NEW || TYPE_BEFORE_CHANGE");
			if (po.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				
				if(invoice.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) != 0) {
					for(MInvoiceLine line : invoice.getLines()) {
						if(line.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) == 0) {
							line.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID, invoice.get_Value(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID));
							line.saveEx();
						}
					}
				}
				if (invoice.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WHThirdParty_ID)==0) {
					if (invoice.getC_BPartner_ID()>0) {
						int WHThirdParty_ID = ((MBPartner)invoice.getC_BPartner()).get_ValueAsInt(ColumnsAdded.COLUMNNAME_WHThirdParty_ID);
						if (WHThirdParty_ID != 0)
							invoice.set_ValueOfColumn(ColumnsAdded.COLUMNNAME_WHThirdParty_ID, WHThirdParty_ID);
					}
				}
			} else if(po.get_TableName().equals(MBPartner.Table_Name)) {
				MBPartner businessPartner = (MBPartner) po;
				if(type == TYPE_BEFORE_NEW
						|| businessPartner.is_ValueChanged(I_C_BPartner.COLUMNNAME_Value)) {
					String taxId = businessPartner.getTaxID();
					//	For Tax ID
					if(Util.isEmpty(taxId)) {
						businessPartner.setTaxID(businessPartner.getValue().trim());
					}
				}
				if(type == TYPE_AFTER_CHANGE) {
					//	Validate without values
					if(businessPartner.is_ValueChanged(I_C_BPartner.COLUMNNAME_Value)) {
						if(!Util.isEmpty(businessPartner.getValue())) {
							businessPartner.setValue(businessPartner.getValue().trim());
						}
						String taxId = businessPartner.getTaxID();
						//	For Tax ID
						if(Util.isEmpty(taxId.isEmpty)) {
							businessPartner.setTaxID(businessPartner.getValue());
						}
					} else if(businessPartner.is_ValueChanged(I_C_BPartner.COLUMNNAME_TaxID)) {
						if(!Util.isEmpty(businessPartner.getTaxID())) {
							businessPartner.setTaxID(businessPartner.getTaxID().trim());
						}
					}
				}
			}else if (po.get_TableName().equals(MWHWithholding.Table_Name)) {
				MWHWithholding withholding = (MWHWithholding) po;
				MInvoiceLine invoiceLine = new Query(withholding.getCtx(), MInvoiceLine.Table_Name, "C_InvoiceLine_ID = ?", withholding.get_TrxName())
												.setParameters(withholding.getC_InvoiceLine_ID())
												.first();
				if (invoiceLine!=null 
						&& invoiceLine.get_ID()>0) {
					invoiceLine.set_ValueOfColumn("InvoiceToAllocate_ID", withholding.getSourceInvoice_ID());
					invoiceLine.save();
				}
			}
		} else if (type == TYPE_AFTER_CHANGE) {
			// Set Is Paid for Auto Allocation Invoice Documents
			if (po.get_TableName().equals(MInvoice.Table_Name)) {
				MInvoice invoice = (MInvoice) po;
				if (invoice.is_ValueChanged(MInvoice.COLUMNNAME_DocStatus)
						&& invoice.getDocStatus().equals(MInvoice.DOCSTATUS_Completed)
							&& invoice.testAllocation()){
					invoice.save();
				}
			}
		}
		
		return null;
	}
}
