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

import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.util.impexp.BankTransactionAbstract;

/**
 * Custom format for Tesoro Bank Transaction
 * Semicolon Separed Value
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * <li> FR [ 1701 ] Add support to MT940 format
 * @see https://github.com/adempiere/adempiere/issues/1701
 */
public abstract class BNC_v1 extends BankTransactionAbstract {
	/**	Value Date [dddMMyyyy]	*/
	private static final String LINE_TRANSACTION_Date = "TrxDate";
	/**	Transaction type Transaction type (description)	*/ 
	private static final String LINE_TRANSACTION_Type = "Type";
	/**	Memo of transaction	*/
	private static final String LINE_TRANSACTION_Memo = "Memo";
	/**	Concept of transaction	*/
	private static final String LINE_TRANSACTION_Concept = "Concept";
	/**	Sequence number [35x] Sequential number of transaction on account	*/
	private static final String LINE_TRANSACTION_ReferenceNo = "ReferenceNo";
	/**	Check No	*/
	private static final String LINE_TRANSACTION_CheckNo = "CheckNo";
	/**	Transaction Code	*/
	private static final String LINE_TRANSACTION_TrxCode = "TrxCode";
	/**	Amount	*/
	private static final String LINE_TRANSACTION_Amount = "Amount";
	/**	Is a transaction	*/
	private boolean isTransaction = false;
	/**	Maximum  columns	*/
	private final int COLUMN_SIZE = 11;
	/**	Date Column	*/
	private final int COLUMN_DATE = 0;
	/**	Reference Column	*/
	private final int COLUMN_CHECK_NO = 2;
	/**	Code Column	*/
	private final int COLUMN_CODE = 3;
	/**	Type Column	*/
	private final int COLUMN_TYPE = 4;
	/**	Concept Operation Type	*/
	private final int COLUMN_OPERATION_TYPE = 5;
	/**	Concept Column	*/
	private final int COLUMN_CONCEPT = 6;
	/**	Debit Column	*/
	private final int COLUMN_DEBIT = 7;
	/**	Credit Column	*/
	private final int COLUMN_CREDIT = 8;
	/**	Reference Column	*/
	private final int COLUMN_REFERENCE_NO = 10;
	
	public abstract String getSeparator();
	
	private boolean isValidLine(String[] columns) {
		if(columns == null
				|| columns.length != COLUMN_SIZE) {
			return false;
		}
		try {
			getDate("dd/MM/yy", columns[COLUMN_DATE]);
		} catch (Exception e) {
			return false;
		}
		return !Util.isEmpty(columns[COLUMN_CODE]);
	}
	
	/**
	 * Parse Line
	 * @param line
	 */
	public void parseLine(String line) throws Exception {
		if(Util.isEmpty(line)) {
			return;
		}
		//	Validate
		line = processValue(line);
		if(Util.isEmpty(line)) {
			return;
		}
		//	Replace bad characters
		line = line.replaceAll("\"", "");
		String[] columns = line.split(getSeparator());
		if(!isValidLine(columns)) {
			isTransaction = false;
			return;
		}
		//	
		addValue(LINE_TRANSACTION_Date, getDate("dd/MM/yy", columns[COLUMN_DATE]));
		String transactionType = columns[COLUMN_TYPE].replaceAll(",", "").trim();
		String transactionCode = columns[COLUMN_CODE].replaceAll(",", "").trim();
		String transactionCheckNo = columns[COLUMN_REFERENCE_NO].replaceAll(",", "").trim();
		String transactionReferenceNo = columns[COLUMN_CHECK_NO].replaceAll(",", "").trim();
		String transactionConceptNo = columns[COLUMN_CONCEPT].replaceAll(",", "").trim();
		String transactionOperationTypeNo = columns[COLUMN_OPERATION_TYPE].replaceAll(",", "").trim();
		addValue(LINE_TRANSACTION_Type, transactionType);
		addValue(LINE_TRANSACTION_TrxCode, transactionCode);
		addValue(LINE_TRANSACTION_CheckNo, transactionCheckNo);
		addValue(LINE_TRANSACTION_ReferenceNo, transactionReferenceNo);
		addValue(LINE_TRANSACTION_Concept, transactionConceptNo);
		addValue(LINE_TRANSACTION_Memo, transactionOperationTypeNo);
		BigDecimal debit = getNumber('.', "#,###,###,###,###,###.##", columns[COLUMN_DEBIT].trim());
		debit = debit.abs();
		if(debit != null
				&& debit.compareTo(Env.ZERO) > 0) {
			addValue(LINE_TRANSACTION_Amount, debit.negate());
		}
		BigDecimal credit = getNumber('.', "#,###,###,###,###,###.##", columns[COLUMN_CREDIT].trim());
		credit = credit.abs();
		if(credit != null
				&& credit.compareTo(Env.ZERO) > 0) {
			addValue(LINE_TRANSACTION_Amount, credit);
		}
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
		return value;
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
		return getString(LINE_TRANSACTION_CheckNo);
	}

	@Override
	public String getTrxCode() {
		return getString(LINE_TRANSACTION_TrxCode);
	}
	
	@Override
	public String getPayeeName() {
		return null;
	}

	@Override
	public String getPayeeDescription() {
		return getString(LINE_TRANSACTION_Concept);
	}
}
