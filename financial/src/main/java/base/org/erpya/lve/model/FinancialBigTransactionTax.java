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
import java.math.MathContext;

import org.compiere.model.I_C_Payment;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MPayment;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Class added from standard values
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class FinancialBigTransactionTax implements ModelValidator {

	public FinancialBigTransactionTax() {
		super();
	}

	/** Logger */
	private static CLogger log = CLogger.getCLogger(FinancialBigTransactionTax.class);
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
		//	Add Timing change in C_Payment
		engine.addDocValidate(I_C_Payment.Table_Name, this);
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
		if(timing == TIMING_AFTER_COMPLETE)	{
			if(po.get_TableName().equals(I_C_Payment.Table_Name)) {
				return createFBTTForPayment((MPayment) po);
			}
		} else if(timing == TIMING_AFTER_REVERSECORRECT
				|| timing == TIMING_AFTER_VOID) {
			return reverseFBTTForPayment((MPayment) po);
		}
		//
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		return null;
	}
	
	/**
	 * Process source payment
	 * @param sourcePayment
	 * @return
	 */
	private String createFBTTForPayment(MPayment sourcePayment) {
		//	for payment not receipt
		if(sourcePayment.getReversal_ID() == 0
				&& !sourcePayment.isReceipt()
				&& !isFBTT(sourcePayment)) {
			//	Validate FBTT reversed
			if(sourcePayment.get_ValueAsInt("FBTT_Payment_ID") != 0) {
				MPayment fbttRefPayment = new MPayment(sourcePayment.getCtx(), 
						sourcePayment.get_ValueAsInt("FBTT_Payment_ID"), sourcePayment.get_TrxName());
				if(fbttRefPayment.getDocStatus().equals(MPayment.STATUS_Completed)
						|| fbttRefPayment.getDocStatus().equals(MPayment.STATUS_Closed)) {
					return null;
				}
			}
			//	Validate Business Partner
			MBPartner bpartner = MBPartner.get(sourcePayment.getCtx(), sourcePayment.getC_BPartner_ID());
			if(bpartner.get_ValueAsBoolean("IsFBTTTaxExempt")) {
				return null;
			}
			//	Validate currency
			int defaultCurrencyId = MClient.get(sourcePayment.getCtx()).getC_Currency_ID();
			MBankAccount bankAccount = MBankAccount.get(sourcePayment.getCtx(), sourcePayment.getC_BankAccount_ID());
			if(bankAccount == null) {
				return null;
			}
			//	Verify currency
			if(bankAccount.getC_Currency_ID() != defaultCurrencyId) {
				return null;
			}
			//	Verify if exist FBTT configured
			MBank bank = MBank.get(sourcePayment.getCtx(), bankAccount.getC_Bank_ID());
			int listId = bank.get_ValueAsInt(I_LVE_List.COLUMNNAME_LVE_List_ID);
			if(listId == 0
					|| (!Util.isEmpty(bank.getBankType()) && bank.getBankType().equals(MBank.BANKTYPE_CashJournal))) {
				return null;
			}
			MLVEList list = MLVEList.get(Env.getCtx(), listId);
			if(list == null) {
				return null;
			}
			BigDecimal rateToApply = list.getList(sourcePayment.getDateTrx(), 
					sourcePayment.getPayAmt(), I_LVE_ListLine.COLUMNNAME_Col_1);
			//	Validate rate to apply
			if(rateToApply == null
					|| rateToApply.equals(Env.ZERO)) {
				return null;
			}
			//	Calculate
			MPayment fbttPayment = new MPayment(sourcePayment.getCtx(), 0, sourcePayment.get_TrxName());
			//	Copy it
			PO.copyValues(sourcePayment, fbttPayment);
			//	reset values
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
			fbttPayment.setDocStatus(MPayment.STATUS_Drafted);
			fbttPayment.setCheckNo(null);
			fbttPayment.setRoutingNo(null);
			fbttPayment.setAccountNo(null);
			fbttPayment.setMicr(null);
			fbttPayment.setA_Name(null);
			//	Set default values
			fbttPayment.addDescription(Msg.parseTranslation(sourcePayment.getCtx(), "@Generate@ @from@ @FBTT@"));
			MClientInfo clientInfo = MClientInfo.get(sourcePayment.getCtx());
			//	Validate and add FBTT business partner
			int bPartnerId = clientInfo.get_ValueAsInt("FBTT_BPartner_ID");
			if(bPartnerId != 0) {
				fbttPayment.setC_BPartner_ID(bPartnerId);
			}
			//	Validate charge
			int chargeId = clientInfo.get_ValueAsInt("FBTT_Charge_ID");
			if(chargeId == 0) {
				return "@FBTT_Charge_ID@ @NotFound@";
			}
			//	Set charge
			fbttPayment.setC_Charge_ID(chargeId);
			//	Set Payment Amount
			rateToApply = rateToApply.divide(Env.ONEHUNDRED, MathContext.DECIMAL128);
			BigDecimal payAmt = rateToApply.multiply(sourcePayment.getPayAmt());
			//	
			fbttPayment.setPayAmt(payAmt);
			//	Save values
			fbttPayment.saveEx();
			//	Set Reference
			sourcePayment.set_ValueOfColumn("FBTT_Payment_ID", fbttPayment.getC_Payment_ID());
			sourcePayment.saveEx();
			//	Complete
			fbttPayment.setDocAction(MPayment.ACTION_Complete);
			fbttPayment.processIt(MPayment.ACTION_Complete);
			fbttPayment.saveEx();
			//	Validate Complete Document Status
			if(fbttPayment.getDocStatus() != MPayment.DOCSTATUS_Completed) {
				return fbttPayment.getProcessMsg();
			}
		}
		//	Ok
		return null;
	}
	
	/**
	 * reverse FBTT
	 * @param sourcePayment
	 * @return
	 */
	private String reverseFBTTForPayment(MPayment sourcePayment) {
		//	for payment not receipt
		if(!sourcePayment.isReceipt()
				&& sourcePayment.get_ValueAsInt("FBTT_Payment_ID") != 0) {
			//	Get FBTT reference
			int igtfPaymentId = sourcePayment.get_ValueAsInt("FBTT_Payment_ID");
			MPayment fbttPayment = new MPayment(sourcePayment.getCtx(), igtfPaymentId, sourcePayment.get_TrxName());
			if(fbttPayment.getDocStatus().equals(MPayment.DOCSTATUS_Completed)) {
				fbttPayment.setDocAction(MPayment.ACTION_Reverse_Correct);
				fbttPayment.processIt(MPayment.ACTION_Reverse_Correct);
				fbttPayment.saveEx();
				//	Validate Complete Document Status
				if(fbttPayment.getDocStatus() != MPayment.DOCSTATUS_Reversed) {
					return fbttPayment.getProcessMsg();
				}
			}
		}
		//	Ok
		return null;
	}
	
	/**
	 * Is a FBTT
	 * @param payment
	 * @return
	 */
	private boolean isFBTT(MPayment payment) {
		int igtfPaymentId = DB.getSQLValue(payment.get_TrxName(), 
				"SELECT C_Payment_ID "
				+ "FROM C_Payment "
				+ "WHERE FBTT_Payment_ID = ?", payment.getC_Payment_ID());
		//	
		return igtfPaymentId > 0;
	}
}
