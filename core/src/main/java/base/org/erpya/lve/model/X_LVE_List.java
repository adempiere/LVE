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
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Model for LVE_List
 *  @author Adempiere (generated) 
 *  @version Release 3.9.2 - $Id$ */
public class X_LVE_List extends PO implements I_LVE_List, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190621L;

    /** Standard Constructor */
    public X_LVE_List (Properties ctx, int LVE_List_ID, String trxName)
    {
      super (ctx, LVE_List_ID, trxName);
      /** if (LVE_List_ID == 0)
        {
			setLVE_List_ID (0);
			setLVE_ListType_ID (0);
			setName (null);
        } */
    }

    /** Load Constructor */
    public X_LVE_List (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LVE_List[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set List.
		@param LVE_List_ID List	  */
	public void setLVE_List_ID (int LVE_List_ID)
	{
		if (LVE_List_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LVE_List_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LVE_List_ID, Integer.valueOf(LVE_List_ID));
	}

	/** Get List.
		@return List	  */
	public int getLVE_List_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_List_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.erpya.lve.model.I_LVE_ListType getLVE_ListType() throws RuntimeException
    {
		return (org.erpya.lve.model.I_LVE_ListType)MTable.get(getCtx(), org.erpya.lve.model.I_LVE_ListType.Table_Name)
			.getPO(getLVE_ListType_ID(), get_TrxName());	}

	/** Set List Type (Dynamic List).
		@param LVE_ListType_ID List Type (Dynamic List)	  */
	public void setLVE_ListType_ID (int LVE_ListType_ID)
	{
		if (LVE_ListType_ID < 1) 
			set_Value (COLUMNNAME_LVE_ListType_ID, null);
		else 
			set_Value (COLUMNNAME_LVE_ListType_ID, Integer.valueOf(LVE_ListType_ID));
	}

	/** Get List Type (Dynamic List).
		@return List Type (Dynamic List)	  */
	public int getLVE_ListType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_ListType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getValue());
    }
}