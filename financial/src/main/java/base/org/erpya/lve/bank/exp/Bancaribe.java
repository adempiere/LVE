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
package org.erpya.lve.bank.exp;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Implementation for Export Payment from Bancaribe bank
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public class Bancaribe extends LVEPaymentExportList {

	/** Logger								*/
	private static CLogger	s_log = CLogger.getCLogger (Bancaribe.class);
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
							String bpAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bpAccountNo).isPresent()) {
								bpAccountNo = leftPadding(bpAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Process Account Name
							String bpaName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bpaName).isPresent()) {
								bpaName = rightPadding(bpaName, 30, " ", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bpartner.getValue() + " - " + bpartner.getName()));
							}
							//	Validate
							if(Optional.ofNullable(bpAccountNo).isPresent()
									&& Optional.ofNullable(bpaName).isPresent()) {
								//	Payment Amount
								BigDecimal amt = payselectionCheck.getPayAmt().abs();
								//String strange = "#,##0.###";
								String strange = "#,##0.00";
								NumberFormat numFormat = DisplayType.getNumberFormat(
										DisplayType.Number, 
										Language.getBaseLanguage(), 
										strange);
								//	Write Credit Register
								StringBuffer line = new StringBuffer();
								line.append(bpAccountNo)						//  BP Bank Account
									.append(SEPARATOR)							//	Blank Space	
									.append(numFormat.format(amt))				// 	Payment Amount
									.append(SEPARATOR)							//	Blank Space	
									.append(bpaName)							//	BP Name
									.append(Env.NL);							//	New Line
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
	public int exportToFileAsEnrollment(MBankAccount bankAccount, List<MBPBankAccount> bPartnerAccountList, boolean isEnroll, File file, StringBuffer error) {
		if (bPartnerAccountList == null || bPartnerAccountList.size() == 0)
			return 0;
		//	
		try {
			s_log.fine("Delete file if exist");
			MBank bank = MBank.get(Env.getCtx(), bankAccount.getC_Bank_ID());
			//	For same bank
			String enrollmentPath = getFileName(file, bank.getName(), isEnroll, true);
			if(!Util.isEmpty(enrollmentPath)) {
				File enrollmentFile = new File(enrollmentPath);
				deleteIfExist(enrollmentFile);
				s_log.fine("Open File Writer");
				openFileWriter(enrollmentFile);
				//	For Same Bank
				//  Write Credit Note
				s_log.fine("Iterate Payments");
				bPartnerAccountList.stream()
						.filter(bpAccount -> bpAccount.getC_Bank_ID() == bankAccount.getC_Bank_ID())
						.forEach(bpAccount -> {
							MBPartner bPartner = MBPartner.get(Env.getCtx(), bpAccount.getC_BPartner_ID());
							//	Process Business Partner Account No
							String bpAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bpAccountNo).isPresent()) {
								bpAccountNo = leftPadding(bpAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bPartner.getValue() + " - " + bPartner.getName()));
							}
							//	Process Account Name
							String bpaName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bpaName).isPresent()) {
								bpaName = bpaName.substring(0, bpaName.length() >= 30? 30: bpaName.length());
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bPartner.getValue() + " - " + bPartner.getName()));
							}
							//	Tax ID
							String bPTaxId = bpAccount.getA_Ident_SSN();
							if(!Util.isEmpty(bPTaxId)) {
								bPTaxId = bPTaxId.replace("-", "").trim();
								String bpTaxIdChar = bPTaxId.substring(0, 1);
								bPTaxId = bPTaxId.substring(1, bPTaxId.length());
								bPTaxId = leftPadding(bPTaxId, 9, "0", true);
								bPTaxId = bpTaxIdChar + bPTaxId;
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bPartner.getValue() + " - " + bPartner.getName()));
							}
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(bpAccountNo)	//  BP Bank Account
								.append(SEPARATOR)		//	Blank Space	
								.append(bpaName)		//	BP Name
								.append(SEPARATOR)		//	Blank Space
								.append(bPTaxId)		//	BP Tax ID
								.append(Env.NL);		//	New Line
							//	Write it
							s_log.fine("Write Line");
							writeLine(line.toString());
				});
				closeFileWriter();
				//	For other bank
				deleteIfExist(enrollmentFile);
			}
			//	For Other Bank
			enrollmentPath = getFileName(file, bank.getName(), isEnroll, false);
			if(!Util.isEmpty(enrollmentPath)) {
				File enrollmentFile = new File(enrollmentPath);
				s_log.fine("Open File Writer");
				openFileWriter(enrollmentFile);
				//	For Same Bank
				//  Write Credit Note
				s_log.fine("Iterate Payments");
				bPartnerAccountList.stream()
						.filter(bpAccount -> bpAccount.getC_Bank_ID() != bankAccount.getC_Bank_ID())
						.forEach(bpAccount -> {
							//	Constant
							String constant = "3";
							//	BPartner
							MBPartner bPartner = MBPartner.get(Env.getCtx(), bpAccount.getC_BPartner_ID());
							//	Process Business Partner Account No
							String bpAccountNo = processValue(bpAccount.getAccountNo());
							if(Optional.ofNullable(bpAccountNo).isPresent()) {
								bpAccountNo = leftPadding(bpAccountNo, 20, "0", true);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + bPartner.getValue() + " - " + bPartner.getName()));
							}
							//	Process Account Name
							String bpaName = processValue(bpAccount.getA_Name());
							if(Optional.ofNullable(bpaName).isPresent()) {
								bpaName = bpaName.substring(0, bpaName.length() >= 30? 30: bpaName.length());
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + bPartner.getValue() + " - " + bPartner.getName()));
							}
							//	Tax ID
							String bPTaxId = bpAccount.getA_Ident_SSN();
							if(!Util.isEmpty(bPTaxId)) {
								bPTaxId = bPTaxId.replace("-", "").trim();
								String bpTaxIdChar = bPTaxId.substring(0, 1);
								bPTaxId = bPTaxId.substring(1, bPTaxId.length());
								bPTaxId = leftPadding(bPTaxId, 9, "0", true);
								bPTaxId = bpTaxIdChar + bPTaxId;
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + bPartner.getValue() + " - " + bPartner.getName()));
							}
							//	EMail
							String bPEmail = "";
							if(!Util.isEmpty(bpAccount.getA_EMail())) {
								bPEmail = bpAccount.getA_EMail();
							}
							bPEmail = rightPadding(bPEmail, 40, " ", true);
							MUser bpContact = null;
							if(bpAccount.getAD_User_ID() != 0) {
								bpContact = MUser.get(Env.getCtx(), bpAccount.getAD_User_ID());
							}
							//	Phone
							String bPPhone = "";
							if(Optional.ofNullable(bpContact).isPresent()) {
								if(!Util.isEmpty(bpContact.getPhone())) {
									bPPhone = processValue(bpContact.getPhone());
								}
							}
							bPPhone = rightPadding(bPPhone, 20, " ", true);
							//	Write Credit Register
							StringBuffer line = new StringBuffer();
							line.append(constant)		//	Constant
								.append(SEPARATOR)		//	Blank Space
								.append(bpaName)		//	BP Name
								.append(SEPARATOR)		//	Blank Space
								.append(bPTaxId)		//	BP Tax ID
								.append(SEPARATOR)		//	Blank Space	
								.append(bpAccountNo)	//  BP Bank Account
								.append(SEPARATOR)		//	Blank Space	
								.append(bPPhone)		//	BP Phone
								.append(SEPARATOR)		//	Blank Space
								.append(bPEmail)		//	BP EMail
								.append(Env.NL);		//	New Line
							//	Write it
							s_log.fine("Write Line");
							writeLine(line.toString());
				});
				closeFileWriter();
			}
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
	
	/**
	 * Get File Name
	 * @param file
	 * @param bankName
	 * @param isEnroll
	 * @param isSameBank
	 * @return
	 */
	private String getFileName(File file, String bankName, boolean isEnroll, boolean isSameBank) {
		if(file == null) {
			return null;
		}
		//	Bank Message
		String sameBankMessage = "MB";
		String otherBankMessage = "OB";
		//	Enroll Message
		String enrollMessage = "AFI";
		String noEnrollMessage = "DES";
		//	Date
		String dateAsString = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		//	Extension
		String extension = ".txt";
		//	Set new File Name
		StringBuffer pathName = new StringBuffer();
		if(file.isFile() || !file.exists()) {
			pathName.append(file.getParent());
		} else {
			pathName.append(file.getAbsolutePath());
		}
		//	Add Separator
		pathName.append(File.separator)
				.append(isEnroll  ? enrollMessage : noEnrollMessage)
				.append("_")
				.append(bankName)
				.append("_")
				.append(isSameBank  ? sameBankMessage : otherBankMessage)
				.append("_")
				.append(dateAsString)
				.append(extension);
		//	Return
		return pathName.toString().replace(" ", "_");
	}
}
