/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/

package org.erpya.lve.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MAllocationLine;
import org.compiere.model.MCurrency;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPriceList;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.erpya.lve.model.MLVEAutoDebitNoteSetting;
import org.erpya.lve.util.LVEUtil;

/** Generated Process for (Generate Debits By Currency Rate)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public class GenerateDebitsByCurrencyRate extends GenerateDebitsByCurrencyRateAbstract
{
	private final static String COLUMNNAME_DebitAmt = "IWOP_InvoiceAmt";
	Optional<MLVEAutoDebitNoteSetting> maybeSettings = Optional.empty();
	@Override
	protected void prepare()
	{
		super.prepare();
	}

	@Override
	protected String doIt() throws Exception
	{
		getSelectionValues().entrySet().forEach(row -> {
			MInvoice invoice = MInvoice.get(getCtx(), row.getKey());
			maybeSettings = Optional.ofNullable(MLVEAutoDebitNoteSetting.get(getCtx(), invoice.getC_DocTypeTarget_ID()));
			maybeSettings.orElseGet(() -> {
				throw new AdempiereException("@NotFound@ @LVE_AutoDebitNoteSetting_ID@");
			});
			BigDecimal debitAmt = (BigDecimal) row.getValue().get(COLUMNNAME_DebitAmt);
			Optional<MInvoice> maybeDebitNote = Optional.ofNullable(createDebitNoteFromInvoice(invoice, debitAmt));
			maybeDebitNote
				.ifPresent(debitNote ->{
					Optional<MAllocationHdr> maybeAllocation = Optional.ofNullable(allocatedDebitNote(debitNote));
					maybeAllocation.ifPresent(allocation ->{
						addLog("@Created@ @C_Invoice_ID@ " +debitNote.getDocumentNo());
						addLog("@Created@ @C_AllocationHdr_ID@ " +allocation.getDocumentNo());
					});
				});
		});
		return "";
	}
	
	/**
	 * Create Debt Note
	 * @param invoiceFrom
	 * @param debitAmt
	 * @return
	 */
	private MInvoice createDebitNoteFromInvoice(MInvoice invoiceFrom, BigDecimal debitAmt){
		
		AtomicReference<MInvoice> documentReturn = new AtomicReference<MInvoice>(null);
		
		maybeSettings.orElseGet(() -> {
			throw new AdempiereException("@NotFound@ @LVE_AutoDebitNoteSetting_ID@");
		});
		maybeSettings.ifPresent(settings -> {
			final MOrgInfo orgInfo = MOrgInfo.get(getCtx(), invoiceFrom.getAD_Org_ID(), get_TrxName());
			final MCurrency fiscalCurrency = MCurrency.get(getCtx(), orgInfo.get_ValueAsInt(LVEUtil.COLUMNNAME_LVE_FiscalCurrency_ID));
			MInvoice debitNote = new MInvoice(getCtx(), 0, get_TrxName());
			MInvoice.copyValues(invoiceFrom, debitNote);
			
			//Get Default Fiscal Currency Price List
			Optional<MPriceList> maybePriceList = Optional.ofNullable(MPriceList.get(getCtx(), settings.getM_PriceList_ID(), get_TrxName()));
			maybePriceList.orElseGet(() -> {
				throw new AdempiereException("@NotFound@ @IsDefault@ @M_PriceList_ID@");
			});
			maybePriceList.ifPresent(priceList -> {
				debitNote.setM_PriceList_ID(priceList.get_ID());
				debitNote.setC_Currency_ID(priceList.getC_Currency_ID());
			});
			
			//Sales Transaction Blank Document Number and Control Number
			debitNote.setDocumentNo(invoiceFrom.isSOTrx() ? null : getDocumentNo());
			debitNote.set_ValueOfColumn(LVEUtil.COLUMNNAME_ControlNo, invoiceFrom.isSOTrx() ? null : getControlNo());
			debitNote.setDocStatus(MInvoice.DOCSTATUS_Drafted);
			debitNote.setProcessed(false);
			debitNote.setPosted(false);
			debitNote.setDateAcct(getDateAcct());
			debitNote.setDateInvoiced(getDateInvoiced());
			debitNote.setC_ConversionType_ID(getConversionTypeId());
			debitNote.setC_DocTypeTarget_ID(settings.getLVE_DebitNoteDocType_ID());
			debitNote.setC_DocType_ID(settings.getLVE_DebitNoteDocType_ID());
			debitNote.set_ValueOfColumn(LVEUtil.COLUMNNAME_LVE_AutoDebitInvoice_ID, invoiceFrom.get_ID());
			debitNote.set_ValueOfColumn(LVEUtil.COLUMNNAME_InvoiceToAllocate_ID, invoiceFrom.get_ID());
			debitNote.saveEx();
			
			//Create Lines for Debit Note
			boolean detailDebitNote = settings.isLVE_IsCopyLinesFromInvoice();
			int debitChargeId = settings.getLVE_DebitNoteCharge_ID();
			BinaryOperator<BigDecimal> sumValue = (previous, currentValue) -> previous.add(currentValue);
			AtomicReference<BigDecimal> amt = new AtomicReference<BigDecimal>(debitAmt);
			
			if (detailDebitNote) {
				debitNote.copyLinesFrom(invoiceFrom, false, false);
				AtomicReference<BigDecimal> lineAmt = new AtomicReference<BigDecimal>(Env.ZERO);
				final BigDecimal totalLines = new BigDecimal(debitNote.getLines().length);
				AtomicReference<BigDecimal> currentLine = new AtomicReference<BigDecimal>(Env.ZERO);
				Arrays.asList(debitNote.getLines())
					  .stream()
					  .forEach(debitNoteLine -> {
						  currentLine.accumulateAndGet(Env.ONE, sumValue);
						  BigDecimal lineRate = debitNoteLine.getLineNetAmt()
								  						.divide(debitNote.getTotalLines(), MathContext.DECIMAL128);
						  
						  lineAmt.set(debitAmt.multiply(lineRate).setScale(fiscalCurrency.getStdPrecision(), RoundingMode.HALF_UP));
						  
						  debitNoteLine.setPrice(lineAmt.get());
						  amt.accumulateAndGet(lineAmt.get().negate(), sumValue);
						  if (currentLine.get().compareTo(totalLines) ==0 
								  && amt.get().abs().compareTo(Env.ZERO) != 0)
						  	debitNoteLine.setPrice(lineAmt.get().add(amt.get()));
						  debitNoteLine.setQty(Env.ONE);
						  debitNoteLine.saveEx();
					  });
			}else {
				MInvoiceLine invoiceLine = new MInvoiceLine(debitNote);
				invoiceLine.setC_Charge_ID(debitChargeId);
				invoiceLine.setQty(BigDecimal.ONE);
				invoiceLine.setPrice(amt.get());
				invoiceLine.saveEx();
			}
			
			debitNote.processIt(MInvoice.ACTION_Complete);
			debitNote.saveEx();
			documentReturn.set(debitNote);
		});
		
		
		return documentReturn.get();
	}
	
	/**
	 * 
	 * @param debitNote
	 */
	private MAllocationHdr allocatedDebitNote(MInvoice debitNote) {
		//		Create automatic Allocation
		AtomicReference<MAllocationHdr> allocationResult = new AtomicReference<MAllocationHdr>(null);
		maybeSettings.ifPresent(settings -> {
			MAllocationHdr allocationHdr = new MAllocationHdr (getCtx(), false, debitNote.getDateAcct(), debitNote.getC_Currency_ID(),
					Msg.translate(getCtx(), "C_Invoice_ID")	+ ": " + debitNote.getDocumentNo(), get_TrxName());
			allocationHdr.setAD_Org_ID(debitNote.getAD_Org_ID());
			allocationHdr.setDateAcct(debitNote.getDateAcct());
			allocationHdr.saveEx(get_TrxName());
	
			//	Original Allocation
			MAllocationLine allocationLine = new MAllocationLine (allocationHdr, debitNote.getTotalLines(), Env.ZERO, Env.ZERO, Env.ZERO);
			allocationLine.setDocInfo(debitNote.getC_BPartner_ID(), 0, 0);
			allocationLine.setC_Invoice_ID(debitNote.get_ID());
			allocationLine.saveEx(get_TrxName());
	
			allocationLine = new MAllocationLine (allocationHdr, debitNote.getTotalLines(), Env.ZERO, Env.ZERO, Env.ZERO);
			allocationLine.setDocInfo(debitNote.getC_BPartner_ID(), 0, 0);
			allocationLine.setC_Charge_ID(settings.getLVE_AllocationCharge_ID());
			allocationLine.saveEx(get_TrxName());
	
			if (!allocationHdr.processIt(DocAction.ACTION_Complete)) {
				throw new AdempiereException(allocationHdr.getProcessMsg());
			}
	
			allocationHdr.saveEx(get_TrxName());
			allocationResult.set(allocationHdr);
		});
		return allocationResult.get();
	}
}