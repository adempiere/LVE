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
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicInteger;

import org.compiere.util.Env;
import org.compiere.util.Util;
import org.spin.util.impexp.BankTransactionAbstract;

/**
 * Custom format for Provincial Bank Transaction
 * It is a specific solution for a bank
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * <li> FR [ 1701 ] Add support to MT940 format
 * @see https://github.com/adempiere/adempiere/issues/1701
 */
public class Provincial_ISO_4217_Transaction extends BankTransactionAbstract {
	/**	Valid Line start */
	public static final String VALID_LINE_FLAG = "22,";
	/**	Transaction Date [yyMMdd]	*/
	public static final String LINE_TRANSACTION_Date = "TrxDate";
	/**	Value Date [yyMMdd]	*/
	public static final String LINE_TRANSACTION_ValueDate = "ValueDate";
	/**	Transaction type Transaction type (description)	*/ 
	public static final String LINE_TRANSACTION_Type = "Type";
	/**	Memo of transaction	*/
	public static final String LINE_TRANSACTION_Memo = "Memo";
	/**	Concept of transaction	*/
	public static final String LINE_TRANSACTION_Concept = "Concept";
	/**	Sequence number [35x] Sequential number of transaction on account	*/
	public static final String LINE_TRANSACTION_ReferenceNo = "ReferenceNo";
	/**	Amount	*/
	public static final String LINE_TRANSACTION_Amount = "Amount";
	/**	Transaction Code	*/
	public static final String LINE_TRANSACTION_TrxCode = "TrxCode";
	/**	Start Column Index	*/
	private static final String START_CHAR_VALUE = ",";
	/**	Debt Constant	*/
	public static final String DEBT = "1";
	/**	Credit Constant	*/
	public static final String CREDIT = "2";
	/**	Is a transaction	*/
	private boolean isTransaction = false;
	
	/**
	 * Parse Line
	 * @param line
	 */
	public void parseLine(String line) throws Exception {
		if(!line.startsWith(VALID_LINE_FLAG)) {
			isTransaction = false;
			return;
		}
		//	Validate
		line = processValue(line);
		if(Util.isEmpty(line)) {
			isTransaction = false;
			return;
		}
		//	Replace bad characters
		line = line.replaceAll("\"", "");
		AtomicInteger counter = new AtomicInteger();
		//	Split String
		for(String value : line.split(START_CHAR_VALUE)) {
			int currentPosition = counter.getAndIncrement();
			switch (currentPosition) {
			case 3:
				addValue(LINE_TRANSACTION_Date, getDate("yyMMdd", value));
				break;
			case 4:
				addValue(LINE_TRANSACTION_ValueDate, getDate("yyMMdd", value));
				break;
			case 6:
				addValue(LINE_TRANSACTION_TrxCode, value);
				break;
			case 7:
				addValue(LINE_TRANSACTION_Type, value);
				break;
			case 8:
				BigDecimal amount = getAmountFromString(value);
				if(getTrxType().equals(DEBT)) {
					amount = amount.negate();
				}
				addValue(LINE_TRANSACTION_Amount, amount);
				break;
			case 9:
				addValue(LINE_TRANSACTION_ReferenceNo, value);
				break;
			case 10:
				addValue(LINE_TRANSACTION_Memo, value);
				break;
			default:
				break;
			}
		}
		//	fine
		isTransaction = true;
	}
	
	/**
	 * Get Amount from String
	 * @param amountAsString
	 * @return
	 * @throws ParseException
	 */
	private BigDecimal getAmountFromString(String amountAsString) throws ParseException {
		//	Instance it
		DecimalFormat decimalFormat = new DecimalFormat("##################");
		decimalFormat.setMinimumFractionDigits(2);
		decimalFormat.setParseBigDecimal(true);
		//	Parse
		BigDecimal amount = (BigDecimal) decimalFormat.parse(amountAsString);
		if(amount == null) {
			amount = Env.ZERO;
		}
		amount = amount.divide(Env.ONEHUNDRED);
		return amount;
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
		return getDate(LINE_TRANSACTION_ValueDate);
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
		return null;
	}
	
	@Override
	public String getPayeeName() {
		return getString(LINE_TRANSACTION_TrxCode);
	}

	@Override
	public String getPayeeDescription() {
		return getString(LINE_TRANSACTION_Concept);
	}
}
