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
package org.erpya.lve.process;

import java.math.BigDecimal;
import java.math.MathContext;

import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MDocType;
import org.compiere.model.MPayment;
import org.compiere.model.MSequence;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.erpya.lve.model.I_LVE_List;
import org.erpya.lve.model.I_LVE_ListLine;
import org.erpya.lve.model.MLVEList;

/**
 * 	Process for generate FBTT from unprocessed payments
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class ProcessPaymentForFBTT extends ProcessPaymentForFBTTAbstract {

	/**	Lines					*/
	private int lines = 0;
	
	@Override
	protected void prepare() {
		super.prepare();
	}

	@Override
	protected String doIt()  {
		//	Iterate
		int listId = 0;
		int bankAccountId = 0;
		BigDecimal sourcePayAmt = Env.ZERO;
		MPayment fbttPayment = new MPayment(getCtx(), 0, get_TrxName());
		//	Validate currency
		int defaultCurrencyId = MClient.get(getCtx()).getC_Currency_ID();
		for(int key : getSelectionKeys()) {
			MPayment sourcePayment = new MPayment(getCtx(), key, get_TrxName());
			//	Validate FBTT reversed
			if(sourcePayment.get_ValueAsInt("FBTT_Payment_ID") != 0) {
				MPayment fbttRefPayment = new MPayment(sourcePayment.getCtx(), 
						sourcePayment.get_ValueAsInt("FBTT_Payment_ID"), sourcePayment.get_TrxName());
				if(fbttRefPayment.getDocStatus().equals(MPayment.STATUS_Completed)
						|| fbttRefPayment.getDocStatus().equals(MPayment.STATUS_Closed)) {
					continue;
				}
			}
			//	Validate only payment
			if(sourcePayment.isReceipt()) {
				continue;
			}
			//	Validate Partner
			MBPartner bPartner = MBPartner.get(getCtx(), sourcePayment.getC_BPartner_ID());
			if(bPartner.get_ValueAsBoolean("IsFBTTTaxExempt")) {
				continue;
			}
			//	Validate Account
			bankAccountId = sourcePayment.getC_BankAccount_ID();
			MBankAccount bankAccount = MBankAccount.get(getCtx(), bankAccountId);
			//	Verify currency
			if(bankAccount.getC_Currency_ID() != defaultCurrencyId) {
				continue;
			}
			MBank bank = MBank.get(getCtx(), bankAccount.getC_Bank_ID());
			listId = bank.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID);
			//	
			if(listId == 0) {
				continue;
			}
			//	Add payment amount
			sourcePayAmt = sourcePayAmt.add(sourcePayment.getPayAmt());
			//	Copy and reset it
			if(lines == 0) {
				PO.copyValues(sourcePayment, fbttPayment);
				fbttPayment.setIsPrepayment(false);
				fbttPayment.setC_Order_ID(0);
				fbttPayment.setC_Invoice_ID(0);
				fbttPayment.setIsOverUnderPayment(false);
				fbttPayment.setIsOnline(false);
				fbttPayment.setIsReconciled(false);
				fbttPayment.setPayAmt(Env.ZERO);
				fbttPayment.setC_Currency_ID(defaultCurrencyId);
				fbttPayment.setOverUnderAmt(Env.ZERO);
				fbttPayment.setWriteOffAmt(Env.ZERO);
				fbttPayment.setTaxAmt(Env.ZERO);
				fbttPayment.setChargeAmt(Env.ZERO);
				fbttPayment.setProcessed(false);
				fbttPayment.setDateTrx(getDateTrx());
				fbttPayment.setDateAcct(getDateTrx());
				fbttPayment.setDocStatus(MPayment.STATUS_Drafted);
				fbttPayment.setCheckNo(null);
				fbttPayment.setRoutingNo(null);
				fbttPayment.setAccountNo(null);
				fbttPayment.setMicr(null);
				fbttPayment.setA_Name(null);
				//	Set default values
				fbttPayment.addDescription(Msg.parseTranslation(getCtx(), "@Generate@ @from@ @FBTT@"));
				MClientInfo clientInfo = MClientInfo.get(getCtx());
				//	Validate and add FBTT business partner
				if(getBPartnerId() == 0) {
					setBPartnerId(clientInfo.get_ValueAsInt("FBTT_BPartner_ID"));
				}
				//	
				if(getBPartnerId() != 0) {
					fbttPayment.setC_BPartner_ID(getBPartnerId());
				}
				//	Validate charge
				int chargeId = clientInfo.get_ValueAsInt("FBTT_Charge_ID");
				if(chargeId == 0) {
					return "@FBTT_Charge_ID@ @NotFound@";
				}
				String sequence = null;
				if(getDocTypeTargetId() != 0) {
					MDocType docType = MDocType.get(getCtx(), getDocTypeTargetId());
					if(docType.getDocBaseType().equals(MDocType.DOCBASETYPE_APPayment)) {
						fbttPayment.setC_DocType_ID(getDocTypeTargetId());
						sequence = MSequence.getDocumentNo(getDocTypeTargetId(), get_TrxName(), true);
					}
				}
				//	get sequence
				if(sequence == null) {
					sequence = Msg.parseTranslation(getCtx(), "@FBTT@_") 
							+ DisplayType.getDateFormat(DisplayType.Date).format(getDateTrx());
				}
				//	Set sequence
				fbttPayment.setDocumentNo(sequence);
				//	Set charge
				fbttPayment.setC_Charge_ID(chargeId);
				fbttPayment.saveEx();
			}
			//	Set Reference
			sourcePayment.set_ValueOfColumn("FBTT_Payment_ID", fbttPayment.getC_Payment_ID());
			sourcePayment.saveEx();
			//	Add lines
			lines++;
		}
		//	Create Payment
		if(lines > 0) {
			//	reset values
			MLVEList list = MLVEList.get(Env.getCtx(), listId);
			BigDecimal rateToApply = list.getList(fbttPayment.getDateTrx(), 
					sourcePayAmt, I_LVE_ListLine.COLUMNNAME_Col_1);
			//	Validate rate to apply
			if(rateToApply == null
					|| rateToApply.equals(Env.ZERO)) {
				return "OK";
			}
			//	Set Payment Amount
			rateToApply = rateToApply.divide(Env.ONEHUNDRED, MathContext.DECIMAL128);
			BigDecimal payAmt = rateToApply.multiply(sourcePayAmt);
			//	
			fbttPayment.setPayAmt(payAmt);
			//	Save values
			fbttPayment.saveEx();
			//	Complete
			fbttPayment.setDocAction(getDocAction());
			fbttPayment.processIt(getDocAction());
			fbttPayment.saveEx();
			//	Validate Complete Document Status
			if(fbttPayment.getDocStatus() != MPayment.DOCSTATUS_Completed) {
				return fbttPayment.getProcessMsg();
			}
			//	Return
			return "@Created@: " + fbttPayment.getDocumentInfo();
		}
		return "OK";
	}
}
