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
 * See: www.erpya.com                                                                 *
 *************************************************************************************/
package org.erpya.lve.bank.imp;

import java.math.BigDecimal;
import java.text.ParseException;

import org.compiere.util.Util;
import org.spin.util.impexp.MT940BankTransaction;

/**
 * Parse String line from file to values of transaction
 * <table>
 * 	<body>
 * 		<tr>
 * 			<th>Format</th>
 * 			<th>Description</th>
 * 		</tr>
 * 		<tr>
 * 			<th>n</th>
 * 			<th>Only Digits: 0 1 2 3 4 5 6 7 8 9 </th>
 * 		</tr>
 * 		<tr>
 * 			<th>a</th>
 * 			<th>Only letters: A B C D E F G H I J K L M N O P Q R S T U V W X Y Z a b c d e f g h i j k l m n o p q r s t u v w x y z Ś Ź ś ź Ł Ą Ż ł ą ż Ć Ę Ń Ó ć ę ń ó</th>
 * 		</tr>
 *  	<tr>
 * 			<th>c</th>
 * 			<th>Alphanumeric = digits + letters</th>
 * 		</tr>
 *  	<tr>
 * 			<th>x</th>
 * 			<th>space ' ( ) + , - . / 0 1 2 3 4 5 6 7 8 9 : ? A B C D E F G H I J K L M N O P Q R S T U V W X Y Z a b c d e f g h i j k l m n o p q r s t u v w x y z Ś Ź ś ź Ł Ą Ż ł ą ż Ć Ę Ń Ó ć ę ń ó </th>
 * 		</tr>
 *   	<tr>
 * 			<th>d</th>
 * 			<th>Amount – digits with coma ( , ) as decimal symbol</th>
 * 		</tr>
 * 	</body>
 * </table>
 * Note:  
 * <li>35x: means that there may be up to 35 characters from x group, including empty field.
 * <li>3!a: exactly 3 letters
 * <li>2n: up to 2 digits
 * <li>4*35x: up to 4 subfields up to 35 characters each. 
 * @author Yamel Senih, ysenih@erpya.com , http://www.erpya.com
 * <li> FR [ 1701 ] Add support to MT940 format
 * @see https://github.com/adempiere/adempiere/issues/1701
 */
public class Provincial_MT940_Transaction extends MT940BankTransaction {
	
	/**	Fixed Value	*/	
	private static final String LINE_TRANSACTION_VENDOR_PAYMENT_REF = "PNCASH-PAGO A PROV";
	/**	Collect reference	*/
	private static final String LINE_TRANSACTION_PAYEE_CODE = "TPBW ";
	/**	FBTT Debt reference	*/
	private static final String LINE_TRANSACTION_FBTT_DEBT = "C./A. IGTF MOV.D: ";
	/**	FBTT Credit reference	*/
	private static final String LINE_TRANSACTION_FBTT_CREDIT = "C./A. IGTF MOV.H: ";
	/**	Payments reference	*/
	private static final String LINE_TRANSACTION_PNCPRO = "PNCPRO";
	/**
	 * Set value from prefix
	 * @param key
	 * @param value
	 * @throws ParseException 
	 */
	protected void setValue(String key, String value) throws ParseException {
		if(HEAD_STATEMENT_DATE.equals(key)) {
			//	Statement Date
			addValue(HEAD_STATEMENT_DATE, getDate("ddMMyy", subString(value, 1, 6)));
		} else if(HEAD_IBAN.equals(key)) {
			addValue(key, value);
		} else if(HEAD_STATEMENT_NUMBER.equals(key)) {
			addValue(key, getNumber('.', "#####", value));
		} else if(HEAD_ACCOUNT_OWNER_NAME.equals(key)) {
			addValue(key, subString(value, 0, 35));
		} else if(HEAD_ACCOUNT_NAME.equals(key)) {
			addValue(key, subString(value, 0, 35));
		} else if(HEAD_OPENING_BALANCE.equals(key)) {
			int index = 0;
			//	Type
			addValue(HEAD_OPENING_BALANCE_Type, subString(value, index, index += 1));
			//	Date
			addValue(HEAD_OPENING_BALANCE_Date, getDate("yyMMdd", subString(value, index, index += 6)));
			//	Currency
			addValue(HEAD_OPENING_BALANCE_Currency, subString(value, index, index += 3));
			//	Amount
			addValue(HEAD_OPENING_BALANCE_Amount, getAmountFromString(subString(value, index, value.length())));
		} else if(LINE_TRANSACTION.equals(key)) {
			int index = 0;
			//	Date
			addValue(LINE_TRANSACTION_Value_Date, getDate("yyMMdd", subString(value, index, index += 6)));
			//	Booking Date
			addValue(LINE_TRANSACTION_Booking_Date, getDate("MMdd", subString(value, index, index += 4)));
			//	Type
			String trxType = subString(value, index, index += 1);
			addValue(LINE_TRANSACTION_Type, trxType);
			//	Amount
			BigDecimal amount = getAmountFromString(subString(value, index += 1, index += 15));
			if(!Util.isEmpty(trxType)
					&& trxType.equals(DEBT)
					&& amount != null) {
				amount = amount.negate();
			}
			addValue(LINE_TRANSACTION_Amount, amount);
			//	Transaction Code
			addValue(LINE_TRANSACTION_DETAIL_Transaction_Code, subString(value, index += 1 , index += 3));
			//	Reconciliation Code
			String reference = subString(value, index, index += 16);
			reference = getValidReference(reference);
			addValue(LINE_TRANSACTION_DETAIL_Reconciliation_Code, reference);
			if(Util.isEmpty(getReferenceNo())) {
				addValue(LINE_TRANSACTION_DETAIL_Reference_Number, reference);
			}
		} else if(LINE_BOOKING_TIME.equals(key)) {
			//	Booking Time
			addValue(LINE_BOOKING_TIME, getDate("hhmm", subString(value, 0, 4)));
		} else if(LINE_TRANSACTION_DETAIL.equals(key)) {
			//	Transaction Title
			addValue(LINE_TRANSACTION_DETAIL_Transaction_Title, value.trim());
			String reference = null;
			String payeeCode = null;
			if(value.startsWith(LINE_TRANSACTION_VENDOR_PAYMENT_REF)) {
				reference = subString(value, value.indexOf(LINE_TRANSACTION_VENDOR_PAYMENT_REF) + LINE_TRANSACTION_VENDOR_PAYMENT_REF.length(), value.length());
			} else if(value.startsWith(LINE_TRANSACTION_FBTT_DEBT)) {
				reference = subString(value, value.indexOf(LINE_TRANSACTION_FBTT_DEBT) + LINE_TRANSACTION_FBTT_DEBT.length(), value.length());
			} else if(value.startsWith(LINE_TRANSACTION_FBTT_CREDIT)) {
				reference = subString(value, value.indexOf(LINE_TRANSACTION_FBTT_CREDIT) + LINE_TRANSACTION_FBTT_CREDIT.length(), value.length());
			} else if(value.startsWith(LINE_TRANSACTION_PAYEE_CODE)) {
				//	Get Payee
				int index = value.indexOf(LINE_TRANSACTION_PAYEE_CODE) + LINE_TRANSACTION_PAYEE_CODE.length();
				payeeCode = subString(value, index, index += 11);
				//	Get Reference
				reference = subString(value, index, value.length());
			} else if(value.contains(LINE_TRANSACTION_PNCPRO)) {
				//	Get Payee
				int index = 0;
				payeeCode = subString(value, index, index += 10);
				index = value.indexOf(LINE_TRANSACTION_PNCPRO) + LINE_TRANSACTION_PNCPRO.length();
				//	Get Reference
				reference = subString(value, index, value.length());
			}
			//	
			if(!Util.isEmpty(payeeCode)) {
				payeeCode = payeeCode.trim();
			}
			reference = getValidReference(reference);
			if(!Util.isEmpty(reference)) {
				addValue(LINE_TRANSACTION_DETAIL_Reference_Number, reference);
			}
			//	
			addValue(LINE_TRANSACTION_DETAIL_Counterparty_Account, payeeCode);
		} else if(LINE_CLOSING_BALANCE.equals(key)) {
			int index = 0;
			//	Transaction Type
			addValue(LINE_CLOSING_BALANCE_Type, subString(value, index, index += 1));
			//	Date
			addValue(LINE_CLOSING_BALANCE_Date, getDate("yyMMdd", subString(value, index, index += 6)));
			//	Currency
			addValue(LINE_CLOSING_BALANCE_Currency, subString(value, index, index += 3));
			//	Amount
			addValue(LINE_CLOSING_BALANCE_Amount, getAmountFromString(subString(value, index, value.length())));
		} else if(LINE_AVAILABLE_BALANCE.equals(key)) {
			int index = 0;
			//	Transaction Type
			addValue(LINE_AVAILABLE_BALANCE_Type, subString(value, index, index += 1));
			//	Date
			addValue(LINE_AVAILABLE_BALANCE_Date, getDate("yyMMdd", subString(value, index, index += 6)));
			//	Currency
			addValue(LINE_AVAILABLE_BALANCE_Currency, subString(value, index, index += 3));
			//	Amount
			addValue(LINE_AVAILABLE_BALANCE_Amount, getAmountFromString(subString(value, index, value.length())));
		} else if(LINE_TYPE_ADDITIONAL_INFORMATION.equals(key)) {
			addValue(LINE_TYPE_ADDITIONAL_INFORMATION, subString(value, 0, 65));
		}
	}
	
	/**
	 * Get valid reference for value
	 * @param reference
	 * @return
	 */
	private String getValidReference(String reference) {
		if(!Util.isEmpty(reference)) {
			reference = reference.trim();
			reference = reference.replace(".", "");
			if(reference.matches("[+-]?\\d*(\\.\\d+)?")) {
				Long longReference = null;
				try {
					longReference = new Long(reference);
				} catch (Exception e) {
					//	Nothing
				}
				if(longReference != null) {
					reference = String.valueOf(longReference);
				}
			}
		}
		return reference;
	}
	
	/**
	 * Get Amount from String
	 * @param amountAsString
	 * @return
	 * @throws ParseException
	 */
	private BigDecimal getAmountFromString(String amountAsString) throws ParseException {
		char separator = '.';
		if(amountAsString.lastIndexOf('.') > 0) {
			separator = ',';
		}
		return getNumber(separator, "########.##", amountAsString);
	}
	
	/**
	 * Get Memo of Transaction
	 * @return
	 */
	public String getMemo() {
		return getString(LINE_TRANSACTION_DETAIL_Transaction_Title);
	}
	
	/**
	 * Get Check Numbers
	 * @return
	 */
	public String getCheckNo() {
		return getString(LINE_TRANSACTION_DETAIL_Reconciliation_Code);
	}
	
	public String getReferenceNo() {
		return getString(LINE_TRANSACTION_DETAIL_Reference_Number);
	}
}
