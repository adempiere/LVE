/**************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                               *
 * This program is free software; you can redistribute it and/or modify it    		  *
 * under the terms version 2 or later of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope           *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                   *
 * See the GNU General Public License for more details.                               *
 * You should have received a copy of the GNU General Public License along            *
 * with this program; if not, printLine to the Free Software Foundation, Inc.,        *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                             *
 * For the text or an alternative of this public license, you may reach us            *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved.  *
 * Contributor: Yamel Senih ysenih@erpya.com                                          *
 * Contributor: Carlos Parada cparada@erpya.com                                       *
 * See: www.erpya.com                                                                 *
 *************************************************************************************/
package org.lve.bank.exp;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBankAccount;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentBatch;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Implementation for Export Payment from Banesco bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class Provincial extends LVEPaymentExportList {

	/** Logger								*/
	static private CLogger	s_log = CLogger.getCLogger (Provincial.class);
	/**	separator	*/
	private final String SEPARATOR = "/";
	
	@Override
	public int exportToFile(List<MPaySelectionCheck> checks, File file, StringBuffer error) {
		if (checks == null || checks.size() == 0)
			return 0;
		//	
		try {
			s_log.fine("Delete file if exist");
			openFileWriter(file, checks);
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			checks.stream()
					.filter(paySelectionCheck -> paySelectionCheck != null)
					.forEach(payselectionCheck -> {
						//  BPartner Info
						MBPBankAccount bpAccount = getBPAccountInfo(payselectionCheck, true);
						MBPartner bpartner = MBPartner.get(payselectionCheck.getCtx(), payselectionCheck.getC_BPartner_ID());
						if(bpAccount != null) {
							//	Process Business Partner Account No
							String bPAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bPAccountNo).isPresent()) {
								bPAccountNo = leftPadding(bPAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							String bPTaxId = bpAccount.getA_Ident_SSN();
							if(!Util.isEmpty(bPTaxId)) {
								bPTaxId = bPTaxId.replace("-", "").trim();
								String bpTaxIdChar = bPTaxId.substring(0, 1);
								bPTaxId = bPTaxId.substring(1, bPTaxId.length());
								bPTaxId = leftPadding(bPTaxId, 9, "0", true);
								bPTaxId = bpTaxIdChar + bPTaxId;
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Account Name
							String bPName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bPName).isPresent()) {
								bPName = rightPadding(bPName, 35, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = rightPadding(bPEmail, 35, " ", true);
							//	Process Document No
							String documentNo = processValue(payselectionCheck.getDocumentNo());
							documentNo = documentNo.replaceAll("\\D+","");
							documentNo = leftPadding(documentNo, 8, "0", true);
							//	Validate
							if(Optional.ofNullable(bPAccountNo).isPresent()
									&& Optional.ofNullable(bPName).isPresent()) {
								//	Payment Amount
								String amountAsString = String.format("%.2f", payselectionCheck.getPayAmt().abs()).replace(".", "").replace(",", "");
								if(amountAsString.length() > 13) {
									addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@: " + bpartner.getValue() + " - " + bpartner.getName()));
								} else {
									amountAsString = leftPadding(amountAsString, 13, "0", true);
								}
								//	Write Credit Register
								StringBuffer line = new StringBuffer();
								line.append(bPTaxId)						//  BP Name
									.append(SEPARATOR)						//	Blank Space	
									.append(bPAccountNo)					//  BP Bank Account
									.append(SEPARATOR)						//	Blank Space	
									.append(amountAsString)					// 	Payment Amount
									.append(SEPARATOR)						//	Blank Space
									.append(documentNo)						//	Document No
									.append(SEPARATOR)						//	Blank Space
									.append(bPName)							//	BP Name
									.append(SEPARATOR)						//	Blank Space
									.append(bPEmail)						//	BP EMail
									.append(Env.NL);						//	New Line
								//	Write it
								s_log.fine("Write Line");
								writeLine(line.toString());
							}
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@C_BP_BankAccount_ID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
			});
			closeFileWriter();
		} catch (Exception e) {
			addError(e.toString());
		} finally {
			closeFileWriter();
			error.append(getError());
			if(error.length() > 0) {
				setExportedPayments(-1);
			}
		}
		//	
		return getExportedPayments();
	}
	
	@Override
	public int exportToFileAsVerification(MBankAccount bankAccount, List<MPayment> payments, File file, StringBuffer error) {
		if (payments == null || payments.size() == 0)
			return 0;
		//	
		try {
			s_log.fine("Delete file if exist");
			//	Open File
			openFileWriter(file, bankAccount, payments, "CS");
			//  Write Credit Note
			s_log.fine("Iterate Payments");
			payments.stream()
					.forEach(payment -> {
						MBPBankAccount bpAccount = getBPAccountInfo(payment, true);
						MBPartner bpartner = MBPartner.get(payment.getCtx(), payment.getC_BPartner_ID());
						MOrg org = MOrg.get(payment.getCtx(), payment.getAD_Org_ID());
						MOrgInfo orgInfo = MOrgInfo.get(payment.getCtx(), payment.getAD_Org_ID(), payment.get_TrxName());
						//	Process Organization Tax ID
						String orgTaxId = processValue(orgInfo.getTaxID().replace("-", "")).trim();
						//	Process Person Type
						if(!Util.isEmpty(orgTaxId)){
							orgTaxId = orgTaxId.replace("-", "").trim();
							String organizationType = orgTaxId.substring(0, 1);
							orgTaxId = orgTaxId.replaceAll("\\D+", "");
							orgTaxId = leftPadding(orgTaxId, 15, "0", true);
							orgTaxId = organizationType + orgTaxId;
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@TaxID@ @NotFound@: " + org.getValue() + " - " + org.getName()));
						}
						String bPTaxId = bpAccount.getA_Ident_SSN();
						if(!Util.isEmpty(bPTaxId)) {
							bPTaxId = bPTaxId.replace("-", "").trim();
							String bpTaxIdChar = bPTaxId.substring(0, 1);
							bPTaxId = bPTaxId.substring(1, bPTaxId.length());
							bPTaxId = leftPadding(bPTaxId, 9, "0", true);
							bPTaxId = bpTaxIdChar + bPTaxId;
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
						//	Process Account Name
						String bPName = processValue(bpAccount.getA_Name());
						if(Optional.ofNullable(bPName).isPresent()) {
							bPName = rightPadding(bPName, 35, " ", true);
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
						//	Process Document No
						String checkNo = processValue(payment.getCheckNo());
						checkNo = checkNo.replaceAll("\\D+","");
						checkNo = leftPadding(checkNo, 8, "0", true);
						//	Payment Amount
						String amountAsString = String.format("%.2f", payment.getPayAmt().abs()).replace(".", "").replace(",", "");
						if(amountAsString.length() > 15) {
							addError(Msg.parseTranslation(Env.getCtx(), "@PayAmt@ @Invalid@: " + bpartner.getValue() + " - " + bpartner.getName()));
						} else {
							amountAsString = leftPadding(amountAsString, 15, "0", true);
						}
						// Batch Document No
						MPaymentBatch paymentBatch = (MPaymentBatch) payment.getC_PaymentBatch();
						//	Validate
						if(paymentBatch == null) {
							addError(Msg.parseTranslation(Env.getCtx(), "@C_PaymentBatch_ID@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
						}
						String batchDocumentNo = paymentBatch.getDocumentNo();
						batchDocumentNo = leftPadding(batchDocumentNo, 8, "0", true);
						//	Write Credit Register
						StringBuffer line = new StringBuffer();
						line.append(checkNo)			//	Document No
							.append(bPTaxId)			//  BP Name
							.append(orgTaxId)			//  Organization Tax ID
							.append(bPName)				//	BP Name
							.append(amountAsString)		// 	Payment Amount
							.append(batchDocumentNo)	// 	Batch Document No
							.append(Env.NL);			//	New Line
						//	Write it
						s_log.fine("Write Line");
						writeLine(line.toString());
			});
			closeFileWriter();
		} catch (Exception e) {
			addError(e.toString());
		} finally {
			closeFileWriter();
			error.append(getError());
			if(error.length() > 0) {
				setExportedPayments(-1);
			}
		}
		//	
		return getExportedPayments();
	}
}
