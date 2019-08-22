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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/** Generated Model for LVE_ListLine
 *  @author Adempiere (generated) 
 *  @version Release 3.9.2 - $Id$ */
public class X_LVE_ListLine extends PO implements I_LVE_ListLine, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190815L;

    /** Standard Constructor */
    public X_LVE_ListLine (Properties ctx, int LVE_ListLine_ID, String trxName)
    {
      super (ctx, LVE_ListLine_ID, trxName);
      /** if (LVE_ListLine_ID == 0)
        {
			setLVE_ListLine_ID (0);
			setLVE_ListVersion_ID (0);
			setMaxValue (Env.ZERO);
			setMinValue (Env.ZERO);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_LVE_ListLine (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
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
      StringBuffer sb = new StringBuffer ("X_LVE_ListLine[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Col_1.
		@param Col_1 Col_1	  */
	public void setCol_1 (BigDecimal Col_1)
	{
		set_Value (COLUMNNAME_Col_1, Col_1);
	}

	/** Get Col_1.
		@return Col_1	  */
	public BigDecimal getCol_1 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_1);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Col_2.
		@param Col_2 Col_2	  */
	public void setCol_2 (BigDecimal Col_2)
	{
		set_Value (COLUMNNAME_Col_2, Col_2);
	}

	/** Get Col_2.
		@return Col_2	  */
	public BigDecimal getCol_2 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_2);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Col_3.
		@param Col_3 Col_3	  */
	public void setCol_3 (BigDecimal Col_3)
	{
		set_Value (COLUMNNAME_Col_3, Col_3);
	}

	/** Get Col_3.
		@return Col_3	  */
	public BigDecimal getCol_3 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_3);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Col_4.
		@param Col_4 Col_4	  */
	public void setCol_4 (BigDecimal Col_4)
	{
		set_Value (COLUMNNAME_Col_4, Col_4);
	}

	/** Get Col_4.
		@return Col_4	  */
	public BigDecimal getCol_4 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_4);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Col_5.
		@param Col_5 Col_5	  */
	public void setCol_5 (BigDecimal Col_5)
	{
		set_Value (COLUMNNAME_Col_5, Col_5);
	}

	/** Get Col_5.
		@return Col_5	  */
	public BigDecimal getCol_5 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_5);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Col_6.
		@param Col_6 Col_6	  */
	public void setCol_6 (BigDecimal Col_6)
	{
		set_Value (COLUMNNAME_Col_6, Col_6);
	}

	/** Get Col_6.
		@return Col_6	  */
	public BigDecimal getCol_6 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_6);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Col_7.
		@param Col_7 Col_7	  */
	public void setCol_7 (BigDecimal Col_7)
	{
		set_Value (COLUMNNAME_Col_7, Col_7);
	}

	/** Get Col_7.
		@return Col_7	  */
	public BigDecimal getCol_7 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_7);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Col_8.
		@param Col_8 Col_8	  */
	public void setCol_8 (BigDecimal Col_8)
	{
		set_Value (COLUMNNAME_Col_8, Col_8);
	}

	/** Get Col_8.
		@return Col_8	  */
	public BigDecimal getCol_8 () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Col_8);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set List Line.
		@param LVE_ListLine_ID List Line	  */
	public void setLVE_ListLine_ID (int LVE_ListLine_ID)
	{
		if (LVE_ListLine_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LVE_ListLine_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LVE_ListLine_ID, Integer.valueOf(LVE_ListLine_ID));
	}

	/** Get List Line.
		@return List Line	  */
	public int getLVE_ListLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_ListLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.erpya.lve.model.I_LVE_ListVersion getLVE_ListVersion() throws RuntimeException
    {
		return (org.erpya.lve.model.I_LVE_ListVersion)MTable.get(getCtx(), org.erpya.lve.model.I_LVE_ListVersion.Table_Name)
			.getPO(getLVE_ListVersion_ID(), get_TrxName());	}

	/** Set List Version.
		@param LVE_ListVersion_ID List Version	  */
	public void setLVE_ListVersion_ID (int LVE_ListVersion_ID)
	{
		if (LVE_ListVersion_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LVE_ListVersion_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LVE_ListVersion_ID, Integer.valueOf(LVE_ListVersion_ID));
	}

	/** Get List Version.
		@return List Version	  */
	public int getLVE_ListVersion_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_ListVersion_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Max Value.
		@param MaxValue Max Value	  */
	public void setMaxValue (BigDecimal MaxValue)
	{
		set_Value (COLUMNNAME_MaxValue, MaxValue);
	}

	/** Get Max Value.
		@return Max Value	  */
	public BigDecimal getMaxValue () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MaxValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Min Value.
		@param MinValue Min Value	  */
	public void setMinValue (BigDecimal MinValue)
	{
		set_Value (COLUMNNAME_MinValue, MinValue);
	}

	/** Get Min Value.
		@return Min Value	  */
	public BigDecimal getMinValue () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MinValue);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set Sequence.
		@param SeqNo 
		Method of ordering records; lowest number comes first
	  */
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
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