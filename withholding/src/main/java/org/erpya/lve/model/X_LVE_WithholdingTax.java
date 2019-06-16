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

/** Generated Model for LVE_WithholdingTax
 *  @author Adempiere (generated) 
 *  @version Release 3.9.2 - $Id$ */
public class X_LVE_WithholdingTax extends PO implements I_LVE_WithholdingTax, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190615L;

    /** Standard Constructor */
    public X_LVE_WithholdingTax (Properties ctx, int LVE_WithholdingTax_ID, String trxName)
    {
      super (ctx, LVE_WithholdingTax_ID, trxName);
      /** if (LVE_WithholdingTax_ID == 0)
        {
			setDefaultWithholdingRate_ID (0);
			setLVE_WithholdingTax_ID (0);
			setName (null);
			setWithholdingRateType_ID (0);
        } */
    }

    /** Load Constructor */
    public X_LVE_WithholdingTax (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LVE_WithholdingTax[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.erpya.lve.model.I_LVE_List getDefaultWithholdingRate() throws RuntimeException
    {
		return (org.erpya.lve.model.I_LVE_List)MTable.get(getCtx(), org.erpya.lve.model.I_LVE_List.Table_Name)
			.getPO(getDefaultWithholdingRate_ID(), get_TrxName());	}

	/** Set Default Withholding Rate.
		@param DefaultWithholdingRate_ID 
		Default Withholding Rate
	  */
	public void setDefaultWithholdingRate_ID (int DefaultWithholdingRate_ID)
	{
		if (DefaultWithholdingRate_ID < 1) 
			set_Value (COLUMNNAME_DefaultWithholdingRate_ID, null);
		else 
			set_Value (COLUMNNAME_DefaultWithholdingRate_ID, Integer.valueOf(DefaultWithholdingRate_ID));
	}

	/** Get Default Withholding Rate.
		@return Default Withholding Rate
	  */
	public int getDefaultWithholdingRate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DefaultWithholdingRate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Client Excluded.
		@param IsClientExcluded 
		Client Excluded for Withholding
	  */
	public void setIsClientExcluded (boolean IsClientExcluded)
	{
		set_Value (COLUMNNAME_IsClientExcluded, Boolean.valueOf(IsClientExcluded));
	}

	/** Get Client Excluded.
		@return Client Excluded for Withholding
	  */
	public boolean isClientExcluded () 
	{
		Object oo = get_Value(COLUMNNAME_IsClientExcluded);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Withholding Tax for Venezuela.
		@param LVE_WithholdingTax_ID 
		Withholding Tax Maintaining for Venezuela
	  */
	public void setLVE_WithholdingTax_ID (int LVE_WithholdingTax_ID)
	{
		if (LVE_WithholdingTax_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LVE_WithholdingTax_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LVE_WithholdingTax_ID, Integer.valueOf(LVE_WithholdingTax_ID));
	}

	/** Get Withholding Tax for Venezuela.
		@return Withholding Tax Maintaining for Venezuela
	  */
	public int getLVE_WithholdingTax_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LVE_WithholdingTax_ID);
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

	public org.erpya.lve.model.I_LVE_ListType getWithholdingRateType() throws RuntimeException
    {
		return (org.erpya.lve.model.I_LVE_ListType)MTable.get(getCtx(), org.erpya.lve.model.I_LVE_ListType.Table_Name)
			.getPO(getWithholdingRateType_ID(), get_TrxName());	}

	/** Set Withholding Rate Type.
		@param WithholdingRateType_ID 
		Withholding Rate Type for handle Withholding Tax Rate
	  */
	public void setWithholdingRateType_ID (int WithholdingRateType_ID)
	{
		if (WithholdingRateType_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WithholdingRateType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WithholdingRateType_ID, Integer.valueOf(WithholdingRateType_ID));
	}

	/** Get Withholding Rate Type.
		@return Withholding Rate Type for handle Withholding Tax Rate
	  */
	public int getWithholdingRateType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WithholdingRateType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}