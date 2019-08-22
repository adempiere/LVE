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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for LVE_ListVersion
 *  @author Adempiere (generated) 
 *  @version Release 3.9.2 - $Id$ */
public class X_LVE_ListVersion extends PO implements I_LVE_ListVersion, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20190815L;

    /** Standard Constructor */
    public X_LVE_ListVersion (Properties ctx, int LVE_ListVersion_ID, String trxName)
    {
      super (ctx, LVE_ListVersion_ID, trxName);
      /** if (LVE_ListVersion_ID == 0)
        {
			setLVE_List_ID (0);
			setLVE_ListVersion_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
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

	/** Set Amount.
		@param Amount 
		Amount in a defined currency
	  */
	public void setAmount (BigDecimal Amount)
	{
		set_Value (COLUMNNAME_Amount, Amount);
	}

	/** Get Amount.
		@return Amount in a defined currency
	  */
	public BigDecimal getAmount () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Amount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Cumulative Withholding.
		@param IsCumulativeWithholding 
		Cumulative Withholding, calculated over old documents
	  */
	public void setIsCumulativeWithholding (boolean IsCumulativeWithholding)
	{
		set_Value (COLUMNNAME_IsCumulativeWithholding, Boolean.valueOf(IsCumulativeWithholding));
	}

	/** Get Cumulative Withholding.
		@return Cumulative Withholding, calculated over old documents
	  */
	public boolean isCumulativeWithholding () 
	{
		Object oo = get_Value(COLUMNNAME_IsCumulativeWithholding);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Variable Rate.
		@param IsVariableRate 
		Variable Rate for Withholding Tax Calculation
	  */
	public void setIsVariableRate (boolean IsVariableRate)
	{
		set_Value (COLUMNNAME_IsVariableRate, Boolean.valueOf(IsVariableRate));
	}

	/** Get Variable Rate.
		@return Variable Rate for Withholding Tax Calculation
	  */
	public boolean isVariableRate () 
	{
		Object oo = get_Value(COLUMNNAME_IsVariableRate);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
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

	/** PersonType AD_Reference_ID=54145 */
	public static final int PERSONTYPE_AD_Reference_ID=54145;
	/** Resident Natural Person = PNR */
	public static final String PERSONTYPE_ResidentNaturalPerson = "PNR";
	/** Non Resident Natural Person = PNNR */
	public static final String PERSONTYPE_NonResidentNaturalPerson = "PNNR";
	/** Legal Person Domiciled = PJD */
	public static final String PERSONTYPE_LegalPersonDomiciled = "PJD";
	/** Legal Person Not Domiciled = PJND */
	public static final String PERSONTYPE_LegalPersonNotDomiciled = "PJND";
	/** Legal Person Not Established Domiciled = PJNCD */
	public static final String PERSONTYPE_LegalPersonNotEstablishedDomiciled = "PJNCD";
	/** Set Person Type.
		@param PersonType 
		Person Type for Withholding
	  */
	public void setPersonType (String PersonType)
	{

		set_Value (COLUMNNAME_PersonType, PersonType);
	}

	/** Get Person Type.
		@return Person Type for Withholding
	  */
	public String getPersonType () 
	{
		return (String)get_Value(COLUMNNAME_PersonType);
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

	/** Set Withholding Base Rate.
		@param WithholdingBaseRate 
		Withholding Base Rate
	  */
	public void setWithholdingBaseRate (BigDecimal WithholdingBaseRate)
	{
		set_Value (COLUMNNAME_WithholdingBaseRate, WithholdingBaseRate);
	}

	/** Get Withholding Base Rate.
		@return Withholding Base Rate
	  */
	public BigDecimal getWithholdingBaseRate () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_WithholdingBaseRate);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}