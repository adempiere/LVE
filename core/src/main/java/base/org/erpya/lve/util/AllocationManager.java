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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Allocation util class allows create allocation for documents
 * it can be applicable to sales documents and account payable
 * @author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class AllocationManager {
	
	public AllocationManager(MInvoice document) {
		if(document == null) {
			throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
		}
		setDocument(document);
	}
	
	/**	Document	*/
	private MInvoice document;
	/**	Applied documents	*/
	Map<Integer, AllocationValues> documentsToAllocate;
	/**	Context	*/
	private Properties context;
	/**	Transaction Name	*/
	private String transactionName;
	
	/**
	 * Get Context
	 * @return
	 */
	public Properties getContext() {
		return context;
	}
	
	/**
	 * Set context locally
	 * @param context
	 */
	private void setContext(Properties context) {
		this.context = context;
	}
	
	/**
	 * Set Transaction Name
	 * @param transactionName
	 */
	public void setTransactionName(String transactionName) {
		this.transactionName = transactionName;
	}
	
	/**
	 * Get Transaction Name for this process
	 * @return
	 */
	public String getTransactionName() {
		return transactionName;
	}
	
	/**
	 * Set document for process
	 * @param document
	 */
	public AllocationManager setDocument(MInvoice document) {
		this.document = document;
		setContext(document.getCtx());
		setTransactionName(document.get_TrxName());
		documentsToAllocate = new HashMap<Integer, AllocationValues>();
		return this;
	}
	
	/**
	 * Add Allocate Document
	 * @param invoiceToAllocateId
	 * @param appliedAmount
	 * @param discountAmount
	 * @param writeOffAmount
	 * @return
	 */
	public AllocationManager addAllocateDocument(MInvoice invoiceToAllocate, BigDecimal appliedAmount, BigDecimal discountAmount, BigDecimal writeOffAmount) {
		return addAllocateDocument(invoiceToAllocate.getC_Invoice_ID(), appliedAmount, discountAmount, writeOffAmount);	
	}
	
	/**
	 * 
	 * @param invoiceToAllocate
	 * @return
	 */
	public AllocationManager addAllocateDocument(MInvoice invoiceToAllocate) {
		return addAllocateDocument(invoiceToAllocate.getC_Invoice_ID(), invoiceToAllocate.getOpenAmt(), Env.ZERO, Env.ZERO);
	}
	
	/**
	 * Add document to allocate
	 * @param invoiceToAllocate
	 * @param appliedAmount
	 * @param discountAmount
	 * @param writeOffAmount
	 * @return
	 */
	public AllocationManager addAllocateDocument(int invoiceToAllocateId, BigDecimal appliedAmount, BigDecimal discountAmount, BigDecimal writeOffAmount) {
		if(invoiceToAllocateId <= 0) {
			throw new AdempiereException("@C_Invoice_ID@ @NotFound@");
		}
		//	Validate access
		AllocationValues value = documentsToAllocate.get(invoiceToAllocateId);
		if(value != null) {
			value.addAppliedAmount(appliedAmount);
			value.addDiscountAmount(discountAmount);
			value.addWriteOffAmount(writeOffAmount);
		} else {
			value = new AllocationValues(appliedAmount, discountAmount, writeOffAmount);
		}
		//validate if not allocated
		MInvoice invoiceToAllocated = MInvoice.get(getContext(), invoiceToAllocateId);
		if (invoiceToAllocated.testAllocation())
			return this;
		
		documentsToAllocate.put(invoiceToAllocateId, value);
		return this;
	}
	
	/**
	 * Get Multiplier from document
	 * @param invoice
	 * @return
	 */
	private BigDecimal getMultiplier(MInvoice invoice) {
		BigDecimal multiplier = Env.ONE;
		//	Get Multiplier
		if(invoice.isCreditMemo()) {
			return multiplier.negate();
		}
		//	Default
		return multiplier;
	}
	
	/**
	 * Get Multiplier AP
	 * @param invoice
	 * @return
	 */
	private BigDecimal getMultiplierAP(MInvoice invoice) {
		MDocType documentType = MDocType.get(getContext(), invoice.getC_DocTypeTarget_ID());
		BigDecimal multiplier = Env.ONE;
		//	For AP transactions
		if(documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APInvoice)
				|| documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_APCreditMemo)) {
			return multiplier.negate();
		}
		//	Default
		return multiplier;
	}
	
	/**
	 * Save Allocations added
	 */
	public void createAllocation() {
		if(document == null 
				|| documentsToAllocate.size() == 0) {
			return;
		}
		
		//	Create allocation
		String allocationMessage = Msg.parseTranslation(document.getCtx(), "@CreatedFromDocument@ @C_Invoice_ID@: " + document.getDocumentNo());
		MAllocationHdr allocation = new MAllocationHdr(document.getCtx(), true, document.getDateInvoiced(), document.getC_Currency_ID(), allocationMessage, document.get_TrxName());
		allocation.setDateAcct(document.getDateAcct());
		allocation.setAD_Org_ID(document.getAD_Org_ID());
		//	Set Description
		allocation.saveEx();
		BigDecimal summaryAppliedAmount = Env.ZERO;
		//	Allocate all documents
		for(Entry<Integer, AllocationValues> allocationSet : documentsToAllocate.entrySet()) {
			//	OverUnderAmt needs to be in Allocation Currency
			int invoiceToAllocateId = allocationSet.getKey();
			MInvoice invoiceToAllocate = new MInvoice(getContext(), invoiceToAllocateId, getTransactionName());
			BigDecimal multiplier = getMultiplier(invoiceToAllocate);
			BigDecimal multiplierAP = getMultiplierAP(invoiceToAllocate);
			BigDecimal openAmount = invoiceToAllocate.getOpenAmt().multiply(multiplierAP);
			BigDecimal appliedAmount = allocationSet.getValue().getAppliedAmount().multiply(multiplier).multiply(multiplierAP);
			BigDecimal discountAmmount = allocationSet.getValue().getDiscountAmount().multiply(multiplier).multiply(multiplierAP);
			BigDecimal writeOffAmount = allocationSet.getValue().getWriteOffAmount().multiply(multiplier).multiply(multiplierAP);
			//	Calculate over under amount
			BigDecimal overUnderAmount = openAmount
					.subtract(appliedAmount)
					.subtract(discountAmmount)
					.subtract(writeOffAmount);
			//	Add Line
			MAllocationLine allocationLine = new MAllocationLine(allocation, appliedAmount, discountAmmount, writeOffAmount, overUnderAmount);
			allocationLine.setDocInfo(invoiceToAllocate.getC_BPartner_ID(), invoiceToAllocate.getC_Order_ID(), invoiceToAllocate.getC_Invoice_ID());
			allocationLine.saveEx();
			//	Add allocation
			summaryAppliedAmount = summaryAppliedAmount.add(allocationSet.getValue().getAppliedAmount());
		}
		//	Add allocation for initial document
		BigDecimal multiplier = getMultiplier(document);
		BigDecimal multiplierAP = getMultiplierAP(document);
		BigDecimal openAmount = document.getOpenAmt().multiply(multiplierAP);
		summaryAppliedAmount = summaryAppliedAmount.multiply(multiplier).multiply(multiplierAP);
		BigDecimal overUnderAmount = openAmount
				.subtract(summaryAppliedAmount)
				.subtract(Env.ZERO)
				.subtract(Env.ZERO);
		MAllocationLine allocationLine = new MAllocationLine(allocation, summaryAppliedAmount, Env.ZERO, Env.ZERO, overUnderAmount);
		allocationLine.setDocInfo(document.getC_BPartner_ID(), document.getC_Order_ID(), document.getC_Invoice_ID());
		allocationLine.saveEx();
		if (!allocation.processIt(DocAction.ACTION_Complete)) {
			throw new AdempiereException("@ProcessFailed@: " + allocation.getProcessMsg()); //@Trifon
		}
		allocation.saveEx();
	}

	/**
	 * Private class
	 */
	private class AllocationValues {
		/**
		 * Standard constructor
		 * @param appliedAmount
		 * @param discountAmount
		 * @param writeOffAmount
		 */
		public AllocationValues(BigDecimal appliedAmount, BigDecimal discountAmount, BigDecimal writeOffAmount) {
			this.appliedAmount = appliedAmount;
			this.discountAmount = discountAmount;
			this.writeOffAmount = writeOffAmount;
		}
		
		/**	Applied Amount	*/
		private BigDecimal appliedAmount;
		/**	Discount Amount	*/
		private BigDecimal discountAmount;
		/**	Write Amount	*/
		private BigDecimal writeOffAmount;
		
		public BigDecimal getAppliedAmount() {
			return appliedAmount;
		}
		
		public void addAppliedAmount(BigDecimal appliedAmount) {
			this.appliedAmount = this.appliedAmount.add(appliedAmount);
		}
		
		public BigDecimal getDiscountAmount() {
			return discountAmount;
		}
		
		public void addDiscountAmount(BigDecimal discountAmount) {
			this.discountAmount = this.discountAmount.add(discountAmount);
		}
		
		public BigDecimal getWriteOffAmount() {
			return writeOffAmount;
		}
		
		public void addWriteOffAmount(BigDecimal writeOffAmount) {
			this.writeOffAmount = this.writeOffAmount.add(writeOffAmount);
		}
	}
	
	public static void main(String args[]) {
		org.compiere.Adempiere.startup(true);
		Env.setContext(Env.getCtx(), "#AD_Client_ID", 11);
		MInvoice invoice = new MInvoice(Env.getCtx(), 1000005, null);
		AllocationManager allocationManager = new AllocationManager(invoice);
		Arrays.asList(invoice.getLines())
		.stream()
		.filter(invoiceLine -> invoiceLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID) != 0)
		.forEach(invoiceLine -> {
			allocationManager.addAllocateDocument(invoiceLine.get_ValueAsInt(ColumnsAdded.COLUMNNAME_InvoiceToAllocate_ID), invoiceLine.getLineNetAmt(), Env.ZERO, Env.ZERO);
		});
		//	Create Allocation
		allocationManager.createAllocation();
	}
	
}
