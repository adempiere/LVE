/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.erpya.lve.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

/** Generated Model for LVE_AutoDebitNoteSetting
 *  @author Adempiere (generated) 
 *  @version Release 3.9.3 - $Id$ */
public class X_LVE_AutoDebitNoteSetting extends PO implements I_LVE_AutoDebitNoteSetting, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20230331L;

    /** Standard Constructor */
    public X_LVE_AutoDebitNoteSetting (Properties ctx, int LVE_AutoDebitNoteSetting_ID, String trxName)
    {
      super (ctx, LVE_AutoDebitNoteSetting_ID, trxName);
      /** if (LVE_AutoDebitNoteSetting_ID == 0)
        {
			setLVE_AllocationCharge_ID (0);
			setLVE_AllocationDocType_ID (0);
			setLVE_AutoDebitNoteSetting_ID (0);
			setLVE_DebitNoteDocType_ID (0);
			setM_PriceList_ID (0);
        } */
    }

    /** Load Constructor */
    public X_LVE_AutoDebitNoteSetting (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_LVE_AutoDebitNoteSetting[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.adempiere.core.domains.models.I_C_DocType getC_DocType() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_DocType)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_DocType.Table_Name)
			.getPO(getC_DocType_ID(), get_TrxName());	}

	/** Set Document Type.
		@param C_DocType_ID 
		Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0) 
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_C_Charge getLVE_AllocationCharge() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Charge)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Charge.Table_Name)
			.getPO(getLVE_AllocationCharge_ID(), get_TrxName());	}

	/** Set Allocation Charge.
		@param LVE_AllocationCharge_ID Allocation Charge	  */
	public void setLVE_AllocationCharge_ID (int LVE_AllocationCharge_ID)
	{
		if (LVE_AllocationCharge_ID < 1) 
			set_Value (COLUMNNAME_LVE_AllocationCharge_ID, null);
		else 
			set_Value (COLUMNNAME_LVE_AllocationCharge_ID, Integer.valueOf(LVE_AllocationCharge_ID));
	}

	/** Get Allocation Charge.
		@return Allocation Charge	  */
	public int getLVE_AllocationCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_AllocationCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_C_DocType getLVE_AllocationDocType() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_DocType)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_DocType.Table_Name)
			.getPO(getLVE_AllocationDocType_ID(), get_TrxName());	}

	/** Set Allocation Document Type.
		@param LVE_AllocationDocType_ID Allocation Document Type	  */
	public void setLVE_AllocationDocType_ID (int LVE_AllocationDocType_ID)
	{
		if (LVE_AllocationDocType_ID < 1) 
			set_Value (COLUMNNAME_LVE_AllocationDocType_ID, null);
		else 
			set_Value (COLUMNNAME_LVE_AllocationDocType_ID, Integer.valueOf(LVE_AllocationDocType_ID));
	}

	/** Get Allocation Document Type.
		@return Allocation Document Type	  */
	public int getLVE_AllocationDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_AllocationDocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Automatic Debit Note Setting.
		@param LVE_AutoDebitNoteSetting_ID Automatic Debit Note Setting	  */
	public void setLVE_AutoDebitNoteSetting_ID (int LVE_AutoDebitNoteSetting_ID)
	{
		if (LVE_AutoDebitNoteSetting_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LVE_AutoDebitNoteSetting_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LVE_AutoDebitNoteSetting_ID, Integer.valueOf(LVE_AutoDebitNoteSetting_ID));
	}

	/** Get Automatic Debit Note Setting.
		@return Automatic Debit Note Setting	  */
	public int getLVE_AutoDebitNoteSetting_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_AutoDebitNoteSetting_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_C_Charge getLVE_DebitNoteCharge() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_Charge)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_Charge.Table_Name)
			.getPO(getLVE_DebitNoteCharge_ID(), get_TrxName());	}

	/** Set Debit Note Charge.
		@param LVE_DebitNoteCharge_ID Debit Note Charge	  */
	public void setLVE_DebitNoteCharge_ID (int LVE_DebitNoteCharge_ID)
	{
		if (LVE_DebitNoteCharge_ID < 1) 
			set_Value (COLUMNNAME_LVE_DebitNoteCharge_ID, null);
		else 
			set_Value (COLUMNNAME_LVE_DebitNoteCharge_ID, Integer.valueOf(LVE_DebitNoteCharge_ID));
	}

	/** Get Debit Note Charge.
		@return Debit Note Charge	  */
	public int getLVE_DebitNoteCharge_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_DebitNoteCharge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.adempiere.core.domains.models.I_C_DocType getLVE_DebitNoteDocType() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_C_DocType)MTable.get(getCtx(), org.adempiere.core.domains.models.I_C_DocType.Table_Name)
			.getPO(getLVE_DebitNoteDocType_ID(), get_TrxName());	}

	/** Set Debit Note Document Type.
		@param LVE_DebitNoteDocType_ID Debit Note Document Type	  */
	public void setLVE_DebitNoteDocType_ID (int LVE_DebitNoteDocType_ID)
	{
		if (LVE_DebitNoteDocType_ID < 1) 
			set_Value (COLUMNNAME_LVE_DebitNoteDocType_ID, null);
		else 
			set_Value (COLUMNNAME_LVE_DebitNoteDocType_ID, Integer.valueOf(LVE_DebitNoteDocType_ID));
	}

	/** Get Debit Note Document Type.
		@return Debit Note Document Type	  */
	public int getLVE_DebitNoteDocType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_DebitNoteDocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Copy Lines from Invoice.
		@param LVE_IsCopyLinesFromInvoice Copy Lines from Invoice	  */
	public void setLVE_IsCopyLinesFromInvoice (boolean LVE_IsCopyLinesFromInvoice)
	{
		set_Value (COLUMNNAME_LVE_IsCopyLinesFromInvoice, Boolean.valueOf(LVE_IsCopyLinesFromInvoice));
	}

	/** Get Copy Lines from Invoice.
		@return Copy Lines from Invoice	  */
	public boolean isLVE_IsCopyLinesFromInvoice () 
	{
		Object oo = get_Value(COLUMNNAME_LVE_IsCopyLinesFromInvoice);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public org.adempiere.core.domains.models.I_M_PriceList getM_PriceList() throws RuntimeException
    {
		return (org.adempiere.core.domains.models.I_M_PriceList)MTable.get(getCtx(), org.adempiere.core.domains.models.I_M_PriceList.Table_Name)
			.getPO(getM_PriceList_ID(), get_TrxName());	}

	/** Set Price List.
		@param M_PriceList_ID 
		Unique identifier of a Price List
	  */
	public void setM_PriceList_ID (int M_PriceList_ID)
	{
		if (M_PriceList_ID < 1) 
			set_Value (COLUMNNAME_M_PriceList_ID, null);
		else 
			set_Value (COLUMNNAME_M_PriceList_ID, Integer.valueOf(M_PriceList_ID));
	}

	/** Get Price List.
		@return Unique identifier of a Price List
	  */
	public int getM_PriceList_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_PriceList_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Immutable Universally Unique Identifier.
		@param UUID 
		Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID)
	{
		set_Value (COLUMNNAME_UUID, UUID);
	}

	/** Get Immutable Universally Unique Identifier.
		@return Immutable Universally Unique Identifier
	  */
	public String getUUID () 
	{
		return (String)get_Value(COLUMNNAME_UUID);
	}
}