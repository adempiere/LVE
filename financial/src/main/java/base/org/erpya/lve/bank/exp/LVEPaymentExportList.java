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
import java.util.List;

import org.compiere.model.MBPBankAccount;
import org.compiere.model.MBPartner;
import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPaySelectionLine;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentBatch;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.PaymentExportList;
import org.compiere.util.Util;
import org.eevolution.model.MHRMovement;
import org.eevolution.model.MHRProcess;

/**
 * This class is used like a parent class for make helper method used on 
 * Location for Venezuela, if you use LVE, you can extend it instead of PaymentExportList class
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/1">
 * 		@see FR [ 1 ] Initial commit</a>
 */
public abstract class LVEPaymentExportList extends PaymentExportList {

	/**
	 * Used for verification
	 * @param bankAccount
	 * @param payments
	 * @param file
	 * @param error
	 * @return
	 */
	public int exportToFileAsVerification(MBankAccount bankAccount, List<MPayment> payments, File file, StringBuffer error) {
		return 0;
	}
	
	/**
	 * Used for account Enrollment 
	 * @param bPartnerList
	 * @param file
	 * @param error
	 * @return
	 */
	public int exportToFileAsEnrollmentRequest(List<MBPartner> bPartnerList, File file, StringBuffer error) {
		return 0;
	}
	
	/**
	 * Used for account Enrollment 
	 * @param bankAccount
	 * @param bPartnerList
	 * @param bPartnerAccountList
	 * @param isEnroll
	 * @param file
	 * @param error
	 * @return
	 */
	public int exportToFileAsEnrollment(MBankAccount bankAccount, List<MBPBankAccount> bPartnerAccountList, boolean isEnroll, File file, StringBuffer error) {
		return 0;
	}
	
	/**
	 * 
	 * @param checks
	 * @param file
	 * @param error
	 * @return
	 */
	public int exportToFileAsPayroll(List<MPaySelectionCheck> checks, File file, StringBuffer error) {
		return 0;
	}
	
	/**
	 * Export to file as AP
	 * @param checks
	 * @param file
	 * @param error
	 * @return
	 */
	public int exportToFileAsAccountPayable(List<MPaySelectionCheck> checks, File file, StringBuffer error) {
		return 0;
	}
	
	/**
	 * Export to file from type: AP Payment, Payroll, Enrollment
	 * @param checks
	 * @param file
	 * @param error
	 * @return
	 */
	@Override
	public int exportToFile(List<MPaySelectionCheck> checks, File file, StringBuffer error) {
		//	Validate
		if (checks == null || checks.size() == 0) {
			return 0;
		}
		//	Validate if is from payroll
		MPaySelectionCheck check = checks.get(0);
		int payselectionlineId = DB.getSQLValue(check.get_TrxName(), "SELECT C_PaySelectionLine_ID "
				+ "FROM C_PaySelectionLine "
				+ "WHERE C_PaySelectionCheck_ID = ?", check.getC_PaySelectionCheck_ID());
		if(payselectionlineId > 0) {
			MPaySelectionLine line = new MPaySelectionLine(check.getCtx(), payselectionlineId, check.get_TrxName());
			if(line.getHR_Movement_ID() > 0) {
				return exportToFileAsPayroll(checks, file, error);
			} else {
				return exportToFileAsAccountPayable(checks, file, error);
			}
		}
		//	Default
		return exportToFileAsAccountPayable(checks, file, error);
	}
	
	/**
	 * Open File from Payment Selection
	 * @param file
	 * @param checks
	 */
	public void openFileWriter(File file, List<MPaySelectionCheck> checks) {
		MPaySelectionCheck check = checks.get(0);
		MPaySelection paymentSelection = check.getParent();
		MBankAccount bankAccount = MBankAccount.get(Env.getCtx(), paymentSelection.getC_BankAccount_ID());
		MBank bank = MBank.get(Env.getCtx(), bankAccount.getC_Bank_ID());
		String fileName = getFileName(file, bank.getName(), processValue(paymentSelection.getDocumentNo()));
		openFileWriter(fileName);
	}
	
	/**
	 * Open File from Payments
	 * @param file
	 * @param bankAccount
	 * @param payments
	 * @param suffix
	 */
	public void openFileWriter(File file, MBankAccount bankAccount, List<MPayment> payments, String suffix) {
		if(Util.isEmpty(suffix)) {
			suffix = "";
		}
		MPayment firstPayment = payments.get(0);
		MPaymentBatch paymentBatch = (MPaymentBatch) firstPayment.getC_PaymentBatch();
		MBank bank = MBank.get(Env.getCtx(), bankAccount.getC_Bank_ID());
		String fileName = getFileName(file, bank.getName(), paymentBatch.getDocumentNo() + suffix);
		openFileWriter(fileName);
	}
	
	/**
	 * Open file with new name and delete if exist
	 * @param file
	 * @param bankName
	 * @param documentNo
	 */
	public void openFileWriter(File file, String bankName, String documentNo) {
		String fileName = getFileName(file, bankName, documentNo);
		openFileWriter(fileName);
	}
	
	/**
	 * Open File with a new name
	 * @param file
	 * @param newName
	 */
	public void openFileWriter(String newName) {
		File newFile = new File(newName);
		deleteIfExist(newFile);
		openFileWriter(newFile);
	}
	
	/**
	 * Get Parent File Path
	 * @param file
	 * @return
	 */
	public String getParentFileName(File file) {
		StringBuffer pathName = new StringBuffer();
		if(file.isFile() || !file.exists()) {
			pathName.append(file.getParent());
		} else {
			pathName.append(file.getAbsolutePath());
		}
		//	Return
		return pathName.toString();
	}
	
	/**
	 * Get File Name for document
	 * @param file
	 * @param bankName
	 * @param documentNo
	 * @return
	 */
	public String getFileName(File file, String bankName, String documentNo) {
		if(file == null) {
			return null;
		}
		//	Extension
		String extension = ".txt";
		//	Set new File Name
		StringBuffer pathName = new StringBuffer(getParentFileName(file));
		//	Add Separator
		pathName.append(File.separator)
				.append(bankName)
				.append("_")
				.append(documentNo)
				.append(extension);
		//	Return
		return pathName.toString().replace(" ", "_");
	}
	
	/**
	 * Validate if is numeric
	 * @param value
	 * @return
	 */
	public boolean isNumeric(String value) {
		if(Util.isEmpty(value)) {
			return false;
		}
		//	
		return value.matches("[+-]?\\d*(\\.\\d+)?");
	}
	
	/**
	 * replace all character distinct of number
	 * @param value
	 * @return
	 */
	public String getNumericOnly(String value) {
		if(Util.isEmpty(value)) {
			return value;
		}
		return value.replaceAll("\\D+","");
	}
	
	/**
	 * Get Detail
	 **/
	public String getDetail(MPaySelectionCheck check) {
		StringBuffer detail = new StringBuffer();
		for(MPaySelectionLine paySelectionLine : check.getPaySelectionLinesAsList(false)) {
			String documentNo = null;
			MPaySelection paymentSelection = (MPaySelection) paySelectionLine.getC_PaySelection();
			//	Validate for fill
			if(paySelectionLine.getC_Invoice_ID() != 0) {
				MInvoice invoice = (MInvoice) paySelectionLine.getC_Invoice();
				documentNo = invoice.getDocumentNo();
			} else if(paySelectionLine.getC_Order_ID() != 0) {
				MOrder order = (MOrder) paySelectionLine.getC_Order();
				documentNo = order.getDocumentNo();
			} else if(paySelectionLine.getHR_Movement_ID() != 0) {
				MHRMovement movement = (MHRMovement) paySelectionLine.getHR_Movement();
				MHRProcess payrollProcess = (MHRProcess) movement.getHR_Process();
				documentNo = payrollProcess.getDocumentNo();
			} else {
				documentNo = "SP-" +  paymentSelection.getDocumentNo();
			}
			//	Get Default ISO Code
			if(Util.isEmpty(documentNo)) {
				continue;
			}
			//	Add
			if(detail.length() > 0) {
				detail.append("-");
			}
			detail.append(documentNo);
		}
		//	
		return detail.toString();
	}
	
}
