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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Model for LVE_ListVersion
 *  @author Adempiere (generated) 
 *  @version Release 3.9.0 - $Id$ */
public class X_LVE_ListVersion extends PO implements I_LVE_ListVersion, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20180808L;

    /** Standard Constructor */
    public X_LVE_ListVersion (Properties ctx, int LVE_ListVersion_ID, String trxName)
    {
      super (ctx, LVE_ListVersion_ID, trxName);
      /** if (LVE_ListVersion_ID == 0)
        {
			setLVE_List_ID (0);
			setLVE_ListVersion_ID (0);
			setName (null);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
			setValidTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_LVE_ListVersion (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LVE_ListVersion[")
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

	public org.erpya.lve.model.I_LVE_List getLVE_List() throws RuntimeException
    {
		return (org.erpya.lve.model.I_LVE_List)MTable.get(getCtx(), org.erpya.lve.model.I_LVE_List.Table_Name)
			.getPO(getLVE_List_ID(), get_TrxName());	}

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

	/** Set Valid from.
		@param ValidFrom 
		Valid from including this date (first day)
	  */
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_Value (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}

	/** Set Valid to.
		@param ValidTo 
		Valid to including this date (last day)
	  */
	public void setValidTo (Timestamp ValidTo)
	{
		set_Value (COLUMNNAME_ValidTo, ValidTo);
	}

	/** Get Valid to.
		@return Valid to including this date (last day)
	  */
	public Timestamp getValidTo () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidTo);
	}
}