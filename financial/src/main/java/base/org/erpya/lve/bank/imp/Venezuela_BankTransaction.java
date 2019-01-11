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
package org.erpya.lve.bank.imp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.util.Util;
import org.spin.util.impexp.BankTransactionAbstract;

/**
 * Custom format for Venezuela Bank Transaction
 * It is a specific solution for a bank
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * <li> FR [ 1701 ] Add support to MT940 format
 * @see https://github.com/adempiere/adempiere/issues/1701
 */
public class Venezuela_BankTransaction extends BankTransactionAbstract {
	/**	Ignore it line because is a first line as head */
	public static final String HEAD_REFERENCE_FIRST_LINE_FLAG = "NOCUENTA            FECHA   REFERENCIA   CONCEPTO                      CARGO             ABONO             BALANCE           TMCT";
	/**	Ignore it line because is a begin balance */
	public static final String HEAD_BEGIN_BALANCE_FLAG = "SALDO INICIAL                 ";
	/**	Value Date [dddMMyyyy]	*/
	public static final String LINE_TRANSACTION_Date = "TrxDate";
	/**	Transaction code [3!n] Numerical transaction code. List of codes available in separate document. Transaction code enables automatical recognition of transaction type	*/
	public static final String LINE_TRANSACTION_AccountNo = "AccountNo";
	/**	Transaction type Transaction type (description)	*/ 
	public static final String LINE_TRANSACTION_Type = "Type";
	/**	Memo of transaction	*/
	public static final String LINE_TRANSACTION_Memo = "Memo";
	/**	Sequence number [35x] Sequential number of transaction on account	*/
	public static final String LINE_TRANSACTION_ReferenceNo = "ReferenceNo";
	/**	Reconciliation code (Annotations) (TMCT) */
	public static final String LINE_TRANSACTION_Reconciliation_Code = "ReconciliationCode";
	/**	Amount	*/
	public static final String LINE_TRANSACTION_Amount = "Amount";
	
	/**	Is a transaction	*/
	private boolean isTransaction = false;
	
	/**
	 * Parse Line
	 * @param line
	 */
	public void parseLine(String line) throws Exception {
		line = processValue(line);
		if(Util.isEmpty(line)) {
			return;
		}
		if(line.contains(HEAD_REFERENCE_FIRST_LINE_FLAG)
				|| line.contains(HEAD_BEGIN_BALANCE_FLAG)) {
			isTransaction = false;
			return;
		}
		//	
		int index = 0;
		//	Set Account
		addValue(LINE_TRANSACTION_AccountNo, subString(line, index, index += 20));
		//	Set Transaction Date
		addValue(LINE_TRANSACTION_Date, getDate("ddMMyyyy", subString(line, index, index += 8)));
		//	Set Reference
		addValue(LINE_TRANSACTION_ReferenceNo, subString(line, index, index += 13));
		//	Set Memo
		addValue(LINE_TRANSACTION_Memo, subString(line, index, index += 30));
		//	Set Debit
		BigDecimal debit = getNumber('.', "##################", subString(line, index, index += 18));
		//	Set Credit
		BigDecimal credit = getNumber('.', "##################", subString(line, index, index += 18));
		//	Add to index (ignore balance)
		if(debit != null
				&& debit.doubleValue() != 0) {
			addValue(LINE_TRANSACTION_Amount, debit.negate());			
		} else if(credit != null
				&& credit.doubleValue() != 0) {
			addValue(LINE_TRANSACTION_Amount, credit);
		}
		index += 18;
		//	Add Transaction Type
		addValue(LINE_TRANSACTION_Type, subString(line, index, index += 2));
		//	Add Transaction Type Code
		addValue(LINE_TRANSACTION_Reconciliation_Code, subString(line, index, index += 4));
		//	fine
		isTransaction = true;
	}
	
	/**
	 * Get Bank Transaction Date
	 * @return
	 */
	public Timestamp getTrxDate() {
		return getDate(LINE_TRANSACTION_Date);
	}
	
	/**
	 * Get Amount of transaction
	 * @return
	 */
	public BigDecimal getAmount() {
		return getNumber(LINE_TRANSACTION_Amount);
	}

	/**
	 * Get Payee Account
	 * @return
	 */
	public String getPayeeAccountNo() {
		return null;
	}
	
	/**
	 * Get Memo of Transaction
	 * @return
	 */
	public String getMemo() {
		return getString(LINE_TRANSACTION_Memo);
	}
	
	/**
	 * Get Category
	 * @return
	 */
	public String getTrxType() {
		return getString(LINE_TRANSACTION_Type);
	}
	
	/**
	 * Get Check Numbers
	 * @return
	 */
	public String getCheckNo() {
		return getString(LINE_TRANSACTION_ReferenceNo);
	}
	
	/**
	 * Process or change value for import
	 * you can implement it method for replace special characters
	 * @param value
	 * @return
	 */
	protected String processValue(String value) {
		return value.replaceAll("[	+^&áàäéèëíìïóòöúùñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ$><]", "");
	}

	@Override
	public boolean isEndTransactionLine(String line) {
		return true;
	}
	
	@Override
	public boolean isCompleteData() {
		return isTransaction;
	}

	@Override
	public String getCurrency() {
		return null;
	}

	@Override
	public Timestamp getValueDate() {
		return getDate(LINE_TRANSACTION_Date);
	}

	@Override
	public Timestamp getStatementDate() {
		return getDate(LINE_TRANSACTION_Date);
	}

	@Override
	public String getReferenceNo() {
		return getString(LINE_TRANSACTION_ReferenceNo);
	}

	@Override
	public String getTrxCode() {
		return getString(LINE_TRANSACTION_Reconciliation_Code);
	}
	
	@Override
	public String getPayeeName() {
		return null;
	}

	@Override
	public String getPayeeDescription() {
		return null;
	}
}
