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
package org.erpya.lve.bank.exp;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MDocType;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * 	Implementation for Export Payment from Bancaribe bank 2022
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 *		<a href="https://github.com/adempiere/LVE/issues/51">
 * 		@see FR [ 51 ] Nuevo TXT de Bancaribe</a>
 */
public class Bancaribe_2022 extends LVEPaymentExportList {

	/** Logger								*/
	private static CLogger	s_log = CLogger.getCLogger (Bancaribe_2022.class);
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
			MPaySelection paymentSelection = (MPaySelection) checks.get(0).getC_PaySelection();
			MDocType documentType = MDocType.get(Env.getCtx(), paymentSelection.getC_DocType_ID());
			MBankAccount bankAccount = (MBankAccount) paymentSelection.getC_BankAccount();
			//	Default fields
			AtomicReference<String> service = new AtomicReference<String>("PAP");
			if(documentType.isPayrollPayment()) {
				service.set("NOM");
			}
			//	Account No
			AtomicReference<String> bankAccountNo = new AtomicReference<String>(processValue(bankAccount.getAccountNo()));
			bankAccountNo.set(bankAccountNo.get().replace(" ", ""));
			bankAccountNo.set(leftPadding(bankAccountNo.get(), 20, "0", true));
			//	Currency Type
			String currencyType = "0";
			checks.stream()
					.filter(paySelectionCheck -> paySelectionCheck != null)
					.forEach(payselectionCheck -> {
						//  BPartner Info
						MBPBankAccount businessPartnerAccount = getBPAccountInfo(payselectionCheck, true);
						MBPartner businessPartner = MBPartner.get(payselectionCheck.getCtx(), payselectionCheck.getC_BPartner_ID());
						if(businessPartnerAccount != null) {
							//	Process Business Partner Account No
							String businessPartnerAccountNo = processValue(businessPartnerAccount.getAccountNo());
							MBank businessPartnerBank = MBank.get(Env.getCtx(), businessPartnerAccount.getC_Bank_ID());
							String routingNo = businessPartnerBank.getRoutingNo();
							if(Optional.ofNullable(businessPartnerAccountNo).isPresent()) {
								businessPartnerAccountNo = leftPadding(businessPartnerAccountNo, 20, "", false);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@AccountNo@ @NotFound@: " + businessPartner.getValue() + " - " + businessPartner.getName()));
							}
							//	Process Account Name
							String businessPartnerName = processValue(businessPartnerAccount.getA_Name());
							if(Optional.ofNullable(businessPartnerName).isPresent()) {
								businessPartnerName = rightPadding(businessPartnerName, 64, "", false);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@A_Name@ @NotFound@: " + businessPartner.getValue() + " - " + businessPartner.getName()));
							}
							String businessPartnerTaxId = businessPartnerAccount.getA_Ident_SSN();
							if(!Util.isEmpty(businessPartnerTaxId)){
								businessPartnerTaxId = businessPartnerTaxId.replace("-", "").trim();
								String personType = businessPartnerTaxId.substring(0, 1);
								businessPartnerTaxId = getNumericOnly(businessPartnerTaxId);
								businessPartnerTaxId = personType + leftPadding(businessPartnerTaxId, 14, "", false);
							} else {
								addError(Msg.parseTranslation(Env.getCtx(), "@BPTaxID@ @NotFound@: " + businessPartner.getValue() + " - " + businessPartner.getName()));
							}
							String reference = processValue(payselectionCheck.getDocumentNo());
							reference = rightPadding(reference, 17, "", false);
							//	EMail
							String businessPartnerEmail = rightPadding(businessPartnerAccount.getA_EMail(), 64, "", false);
							if(!isValidEmail(businessPartnerEmail)) {
								businessPartnerEmail = "";
							}
							//	Phone
							String businessPartnerPhone = "";
							String businessPartnerPhone2 = "";
							if(businessPartnerAccount.getAD_User_ID() > 0) {
								MUser contact = MUser.get(Env.getCtx(), businessPartnerAccount.getAD_User_ID());
								if(!Util.isEmpty(contact.getPhone())) {
									businessPartnerPhone = leftPadding(getNumericOnly(contact.getPhone()), 64, "", false);
								}
								if(!Util.isEmpty(contact.getPhone2())) {
									businessPartnerPhone2 = leftPadding(getNumericOnly(contact.getPhone2()), 64, "", false);
								}
							}
							if(!isValidEmail(businessPartnerEmail)) {
								businessPartnerEmail = "";
							}
							//	String Account Type
							String businessPartnerAccountType = "CTE";
							if(businessPartnerAccount.getBankAccountType().equals(MBPBankAccount.BANKACCOUNTTYPE_Savings)) {
								businessPartnerAccountType = "AHO";
							}
							//	Validate
							if(Optional.ofNullable(businessPartnerAccountNo).isPresent()
									&& Optional.ofNullable(businessPartnerName).isPresent()) {
								//	Payment Amount
								String paymentAmount = String.format("%.2f", payselectionCheck.getPayAmt()).replace(",", ".");
								//	Write Credit Register
								StringBuffer line = new StringBuffer();
								line.append(service.get())						//	Service Type
									.append(SEPARATOR)							//	Blank Space
									.append(bankAccountNo.get())				//	Source Account
									.append(SEPARATOR)							//	Blank Space
									.append(currencyType)						//  Currency Type 0 = VES
									.append(SEPARATOR)							//	Blank Space
									.append(routingNo)							//  Third Party Code
									.append(SEPARATOR)							//	Blank Space
									.append(businessPartnerAccountNo)			//  Third Party Account Code
									.append(SEPARATOR)							//	Blank Space
									.append(businessPartnerAccountType)			//  Account Type Saving = AHO, Checking = CTE
									.append(SEPARATOR)							//	Blank Space
									.append(currencyType)						//  Currency Type 0 = VES
									.append(SEPARATOR)							//	Blank Space
									.append(paymentAmount)						// 	Payment Amount
									.append(SEPARATOR)							//	Blank Space
									.append(businessPartnerTaxId)				// 	Third Party Tax ID
									.append(SEPARATOR)							//	Blank Space
									.append(businessPartnerName)				//	BP Name
									.append(SEPARATOR)							//	Blank Space
									.append(reference)							//	Payment Reference
									.append(SEPARATOR)							//	Blank Space
									.append(businessPartnerEmail)				//	e-Mail
									.append(SEPARATOR)							//	Blank Space
									.append(businessPartnerPhone)				//	Phone
									.append(SEPARATOR)							//	Blank Space
									.append(businessPartnerPhone2)				//	Phone 2
									.append(SEPARATOR)							//	Blank Space
									.append(Env.NL);							//	New Line
								//	Write it
								s_log.fine("Write Line");
								writeLine(line.toString());
							}
						} else {
							addError(Msg.parseTranslation(Env.getCtx(), "@C_BP_BankAccount_ID@ @NotFound@: " + businessPartner.getValue() + " - " + businessPartner.getName()));
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
		throw new AdempiereException("Unsupported method");
	}
}
