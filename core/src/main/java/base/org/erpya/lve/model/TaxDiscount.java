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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.I_C_Invoice;
import org.compiere.model.I_C_Order;
import org.compiere.model.MClient;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoiceTax;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrderTax;
import org.compiere.model.MTax;
import org.compiere.model.MTaxCategory;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

/**
 * 	Class added from standard values
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class TaxDiscount implements ModelValidator {

	public TaxDiscount() {
		super();
	}

	/** Logger */
	private static CLogger log = CLogger.getCLogger(TaxDiscount.class);
	/** Client */
	private int clientId = -1;
	boolean notProcessed = true;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// client = null for global validator
		if (client != null) {
			clientId = client.getAD_Client_ID();
			log.info(client.toString());
		} else {
			log.info("Initializing global validator: " + this.toString());
		}
		//	Add Timing change in C_Order and C_Invoice
		engine.addDocValidate(I_C_Order.Table_Name, this);
		engine.addDocValidate(I_C_Invoice.Table_Name, this);
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
		if(timing==TIMING_BEFORE_COMPLETE)	{
			if(po.get_TableName().equals(I_C_Order.Table_Name)) {
				//	for order
				MOrder order = (MOrder) po;
				recalculateTaxForOrder(order);
			} else if(po.get_TableName().equals(I_C_Invoice.Table_Name)) {
				if(po.get_ValueAsBoolean(I_C_Invoice.COLUMNNAME_IsSOTrx)) {
					//	for invoice
					MInvoice invoice = (MInvoice) po;
					if(invoice.getReversal_ID() == 0) {
						//	Set List Identifier
						setListId(invoice);
						int docAffectedId = invoice.get_ValueAsInt("DocAffected_ID");
						if(docAffectedId <= 0) {
							for(MInvoiceLine line : invoice.getLines()) {
								docAffectedId = line.get_ValueAsInt("DocAffected_ID");
								if(docAffectedId > 0) {
									break;
								}
							}
						}
						//	Calculate Tax
						if(docAffectedId <= 0) {
							recalculateTaxForInvoice(invoice);
						} else {
							copyTaxFromAffected(invoice, docAffectedId);
						}
					}
				}
			}
		} 
		//
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		return null;
	}
	
	/**
	 * Recalculate tax for order
	 * @param order
	 */
	private void recalculateTaxForOrder(MOrder order) {
		int listId = order.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID);
		if(listId <= 0) {
			return;
		}
		MLVEList list = MLVEList.get(Env.getCtx(), listId);
		if(list == null) {
			return;
		}
		//	Apply change
		boolean applySpecialTax = false;
		MOrderTax [] orderTaxes = order.getTaxes(true);
		BigDecimal baseAmount = Env.ZERO;
		for(MOrderTax orderTax : orderTaxes) {
			if(orderTax.getTaxAmt() == null
					|| orderTax.getTaxAmt().equals(Env.ZERO)) {
				continue;
			}
			//	Tax Category
			MTax tax = MTax.get(Env.getCtx(), orderTax.getC_Tax_ID());
			if(!isHasSpecialTax(tax)) {
				continue;
			}
			//	Add to base amount
			baseAmount = baseAmount.add(orderTax.getTaxBaseAmt());
			if(!applySpecialTax) {
				applySpecialTax = true;
			}
		}
		//	Validate Special Tax
		if(!applySpecialTax) {
			return;
		}
		BigDecimal taxToApply = list.getList(order.getDateOrdered(), 
				baseAmount, I_LVE_ListLine.COLUMNNAME_Col_1);
		//	When exists
		MOrderLine [] lines = order.getLines(true, null);
		//	Change Tax
		for(MOrderLine line : lines) {
			MTax tax = MTax.get(Env.getCtx(), line.getC_Tax_ID());
			//	Not apply for it
			if(tax.getRate() == null) {
				continue;
			}
			if(tax.getRate().doubleValue() <= 0) {
				continue;
			}
			//	Get tax for it
			int taxId = 0;
			if(taxToApply != null
					&& taxToApply.doubleValue() > 0) {
				taxId = getDiscountTaxId(order.get_TrxName(), tax.getC_TaxCategory_ID(), order.getDateOrdered(), taxToApply);
			}
			if(taxId > 0) {
				line.setC_Tax_ID(taxId);
			} else {
				line.setTax();
			}
			line.saveEx();
		}
	}
	
	/**
	 * Recalculate Tax for invoice
	 * @param invoice
	 */
	private void recalculateTaxForInvoice(MInvoice invoice) {
		int listId = invoice.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID);
		if(listId <= 0) {
			return;
		}
		MLVEList list = MLVEList.get(Env.getCtx(), listId);
		if(list == null) {
			return;
		}
		//	Apply change
		boolean applySpecialTax = false;
		MInvoiceTax [] taxes = invoice.getTaxes(true);
		BigDecimal baseAmount = Env.ZERO;
		for(MInvoiceTax invoiceTax : taxes) {
			if(invoiceTax.getTaxAmt() == null
					|| invoiceTax.getTaxAmt().equals(Env.ZERO)) {
				continue;
			}
			//	Tax Category
			MTax tax = MTax.get(Env.getCtx(), invoiceTax.getC_Tax_ID());
			if(!isHasSpecialTax(tax)) {
				continue;
			}
			//	Add to base amount
			baseAmount = baseAmount.add(invoiceTax.getTaxBaseAmt());
			if(!applySpecialTax) {
				applySpecialTax = true;
			}
		}
		//	Validate Special Tax
		if(!applySpecialTax) {
			return;
		}
		BigDecimal taxToApply = list.getList(invoice.getDateInvoiced(), 
				baseAmount, I_LVE_ListLine.COLUMNNAME_Col_1);
		//	When exists
		MInvoiceLine [] lines = invoice.getLines(true);
		//	Change Tax
		for(MInvoiceLine line : lines) {
			//	Set values from parent
			line.setInvoice(invoice);
			MTax tax = MTax.get(Env.getCtx(), line.getC_Tax_ID());
			//	Not apply for it
			if(tax.getRate() == null) {
				continue;
			}
			if(tax.getRate().doubleValue() <= 0) {
				continue;
			}
			//	Get tax for it
			int taxId = 0;
			if(taxToApply != null
					&& taxToApply.doubleValue() > 0) {
				taxId = getDiscountTaxId(invoice.get_TrxName(), tax.getC_TaxCategory_ID(), invoice.getDateInvoiced(), taxToApply);
			}
			if(taxId > 0) {
				line.setC_Tax_ID(taxId);
			} else {
				line.setTax();
			}
			line.setTaxAmt();
			line.saveEx();
		}
	}
	
	/**
	 * Copy tax from document affected
	 * @param invoice
	 * @param docAffectedId
	 */
	private void copyTaxFromAffected(MInvoice invoice, int docAffectedId) {
		MInvoice affected = MInvoice.get(Env.getCtx(), docAffectedId);
		//	Get Taxes from affected
		MInvoiceTax [] taxes = affected.getTaxes(true);
		List<MTax> sourceTaxList = new ArrayList<MTax>();
		for(MInvoiceTax invoiceTax : taxes) {
			if(invoiceTax.getTaxAmt() == null
					|| invoiceTax.getTaxAmt().equals(Env.ZERO)) {
				continue;
			}
			//	Add to list tax
			sourceTaxList.add((MTax) invoiceTax.getC_Tax());
		}
		//	When exists
		MInvoiceLine [] lines = invoice.getLines(true);
		//	Change Tax
		for(MInvoiceLine line : lines) {
			//	Set values from parent
			line.setInvoice(invoice);
			MTax tax = MTax.get(Env.getCtx(), line.getC_Tax_ID());
			//	Not apply for it
			if(tax.getRate() == null) {
				continue;
			}
			if(tax.getRate().doubleValue() <= 0) {
				continue;
			}
			//	Get tax for it
			int taxId = 0;
			//	Get Tax from source
			for(MTax sourceTax : sourceTaxList) {
				if(tax.getC_TaxCategory_ID() == sourceTax.getC_TaxCategory_ID()) {
					taxId = sourceTax.getC_Tax_ID();
					break;
				}
			}
			//	Get any from other category
			if(taxId == 0
					&& sourceTaxList.size() > 0) {
				taxId = sourceTaxList.get(0).getC_Tax_ID();
			}
			if(taxId > 0) {
				line.setC_Tax_ID(taxId);
			} else {
				line.setTax();
			}
			line.setTaxAmt();
			line.saveEx();
		}
	}
	
	/**
	 * Set List ID from order or document affected
	 * @param invoice
	 */
	private void setListId(MInvoice invoice) {
		if(invoice.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID) <= 0) {
			if(invoice.getC_Order_ID() != 0) {
				MOrder order = (MOrder) invoice.getC_Order();
				if(order != null
						&& order.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID) > 0) {
					invoice.set_ValueOfColumn(I_LVE_List.COLUMNNAME_LVE_List_ID, 
							order.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID));
					invoice.saveEx();
				}
			}
		}
		int docAffectedId = invoice.get_ValueAsInt("DocAffected_ID");
		if(docAffectedId <= 0) {
			//	For document affected
			MInvoiceLine [] lines = invoice.getLines(true);
			//	Change Tax
			for(MInvoiceLine line : lines) {
				docAffectedId = line.get_ValueAsInt("DocAffected_ID");
				if(docAffectedId > 0) {
					break;
				}
			}
		}
		//	Save
		if(docAffectedId > 0) {
			MInvoice docAffected = MInvoice.get(Env.getCtx(), docAffectedId);
			if(docAffected != null
					&& docAffected.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID) > 0) {
				invoice.set_ValueOfColumn(I_LVE_List.COLUMNNAME_LVE_List_ID, 
						docAffected.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID));
				invoice.saveEx();
			}
		}
	}
	
	/**
	 * Get Tax to apply
	 * @param trxName
	 * @param taxCategoryId
	 * @param dateDoc
	 * @param taxToApply
	 * @return
	 */
	private int getDiscountTaxId(String trxName, int taxCategoryId, Timestamp dateDoc, BigDecimal taxToApply) {
		MTax [] allTaxes = MTax.getAll(Env.getCtx());
		int discountTaxId = 0;
		for(MTax tax : allTaxes) {
			//	For category
			if(tax.getC_TaxCategory_ID() != taxCategoryId
					|| !tax.isActive()) {
				continue;
			}
			//	For valid from
			if(!TimeUtil.isValid(tax.getValidFrom(), null, dateDoc)) {
				continue;
			}
			//	For rate
			if(!tax.getRate().setScale(2, BigDecimal.ROUND_HALF_UP)
					.equals(taxToApply.setScale(2, BigDecimal.ROUND_HALF_UP))) {
				continue;
			}
			//	Get it
			discountTaxId = tax.getC_Tax_ID();
			break;
		}
		return discountTaxId;
	}
	
	/**
	 * Verify if the tax category has special tax
	 * @param tax
	 * @return
	 * @return boolean
	 */
	private boolean isHasSpecialTax(MTax tax) {
		MTaxCategory taxCategory = (MTaxCategory) tax.getC_TaxCategory();
		return taxCategory.get_ValueAsBoolean("IsHasSpecialTax");
	}
}
