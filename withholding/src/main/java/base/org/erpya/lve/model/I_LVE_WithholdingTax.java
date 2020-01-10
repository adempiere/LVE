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
package org.erpya.lve.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for LVE_WithholdingTax
 *  @author Adempiere (generated) 
 *  @version Release 3.9.2
 */
public interface I_LVE_WithholdingTax 
{

    /** TableName=LVE_WithholdingTax */
    public static final String Table_Name = "LVE_WithholdingTax";

    /** AD_Table_ID=54653 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DefaultWithholdingRate_ID */
    public static final String COLUMNNAME_DefaultWithholdingRate_ID = "DefaultWithholdingRate_ID";

	/** Set Default Withholding Rate.
	  * Default Withholding Rate
	  */
	public void setDefaultWithholdingRate_ID (int DefaultWithholdingRate_ID);

	/** Get Default Withholding Rate.
	  * Default Withholding Rate
	  */
	public int getDefaultWithholdingRate_ID();

	public org.erpya.lve.model.I_LVE_List getDefaultWithholdingRate() throws RuntimeException;

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsClientExcluded */
    public static final String COLUMNNAME_IsClientExcluded = "IsClientExcluded";

	/** Set Client Excluded.
	  * Client Excluded for Withholding
	  */
	public void setIsClientExcluded (boolean IsClientExcluded);

	/** Get Client Excluded.
	  * Client Excluded for Withholding
	  */
	public boolean isClientExcluded();

    /** Column name LVE_WithholdingTax_ID */
    public static final String COLUMNNAME_LVE_WithholdingTax_ID = "LVE_WithholdingTax_ID";

	/** Set Withholding Tax for Venezuela.
	  * Withholding Tax Maintaining for Venezuela
	  */
	public void setLVE_WithholdingTax_ID (int LVE_WithholdingTax_ID);

	/** Get Withholding Tax for Venezuela.
	  * Withholding Tax Maintaining for Venezuela
	  */
	public int getLVE_WithholdingTax_ID();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name Type */
    public static final String COLUMNNAME_Type = "Type";

	/** Set Type.
	  * Type of Validation (SQL, Java Script, Java Language)
	  */
	public void setType (String Type);

	/** Get Type.
	  * Type of Validation (SQL, Java Script, Java Language)
	  */
	public String getType();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name UUID */
    public static final String COLUMNNAME_UUID = "UUID";

	/** Set Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID);

	/** Get Immutable Universally Unique Identifier.
	  * Immutable Universally Unique Identifier
	  */
	public String getUUID();

    /** Column name WithholdingRateType_ID */
    public static final String COLUMNNAME_WithholdingRateType_ID = "WithholdingRateType_ID";

	/** Set Withholding Rate Type.
	  * Withholding Rate Type for handle Withholding Tax Rate
	  */
	public void setWithholdingRateType_ID (int WithholdingRateType_ID);

	/** Get Withholding Rate Type.
	  * Withholding Rate Type for handle Withholding Tax Rate
	  */
	public int getWithholdingRateType_ID();

	public org.erpya.lve.model.I_LVE_ListType getWithholdingRateType() throws RuntimeException;
}
