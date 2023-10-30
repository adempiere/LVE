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
import org.compiere.model.MTable;
import org.compiere.util.KeyNamePair;

/** Generated Interface for LVE_AutoDebitNoteSetting
 *  @author Adempiere (generated) 
 *  @version Release 3.9.3
 */
public interface I_LVE_AutoDebitNoteSetting 
{

    /** TableName=LVE_AutoDebitNoteSetting */
    public static final String Table_Name = "LVE_AutoDebitNoteSetting";

    /** AD_Table_ID=54919 */
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

    /** Column name C_DocType_ID */
    public static final String COLUMNNAME_C_DocType_ID = "C_DocType_ID";

	/** Set Document Type.
	  * Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID);

	/** Get Document Type.
	  * Document type or rules
	  */
	public int getC_DocType_ID();

	public org.adempiere.core.domains.models.I_C_DocType getC_DocType() throws RuntimeException;

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

    /** Column name LVE_AllocationCharge_ID */
    public static final String COLUMNNAME_LVE_AllocationCharge_ID = "LVE_AllocationCharge_ID";

	/** Set Allocation Charge	  */
	public void setLVE_AllocationCharge_ID (int LVE_AllocationCharge_ID);

	/** Get Allocation Charge	  */
	public int getLVE_AllocationCharge_ID();

	public org.adempiere.core.domains.models.I_C_Charge getLVE_AllocationCharge() throws RuntimeException;

    /** Column name LVE_AllocationDocType_ID */
    public static final String COLUMNNAME_LVE_AllocationDocType_ID = "LVE_AllocationDocType_ID";

	/** Set Allocation Document Type	  */
	public void setLVE_AllocationDocType_ID (int LVE_AllocationDocType_ID);

	/** Get Allocation Document Type	  */
	public int getLVE_AllocationDocType_ID();

	public org.adempiere.core.domains.models.I_C_DocType getLVE_AllocationDocType() throws RuntimeException;

    /** Column name LVE_AutoDebitNoteSetting_ID */
    public static final String COLUMNNAME_LVE_AutoDebitNoteSetting_ID = "LVE_AutoDebitNoteSetting_ID";

	/** Set Automatic Debit Note Setting	  */
	public void setLVE_AutoDebitNoteSetting_ID (int LVE_AutoDebitNoteSetting_ID);

	/** Get Automatic Debit Note Setting	  */
	public int getLVE_AutoDebitNoteSetting_ID();

    /** Column name LVE_DebitNoteCharge_ID */
    public static final String COLUMNNAME_LVE_DebitNoteCharge_ID = "LVE_DebitNoteCharge_ID";

	/** Set Debit Note Charge	  */
	public void setLVE_DebitNoteCharge_ID (int LVE_DebitNoteCharge_ID);

	/** Get Debit Note Charge	  */
	public int getLVE_DebitNoteCharge_ID();

	public org.adempiere.core.domains.models.I_C_Charge getLVE_DebitNoteCharge() throws RuntimeException;

    /** Column name LVE_DebitNoteDocType_ID */
    public static final String COLUMNNAME_LVE_DebitNoteDocType_ID = "LVE_DebitNoteDocType_ID";

	/** Set Debit Note Document Type	  */
	public void setLVE_DebitNoteDocType_ID (int LVE_DebitNoteDocType_ID);

	/** Get Debit Note Document Type	  */
	public int getLVE_DebitNoteDocType_ID();

	public org.adempiere.core.domains.models.I_C_DocType getLVE_DebitNoteDocType() throws RuntimeException;

    /** Column name LVE_IsCopyLinesFromInvoice */
    public static final String COLUMNNAME_LVE_IsCopyLinesFromInvoice = "LVE_IsCopyLinesFromInvoice";

	/** Set Copy Lines from Invoice	  */
	public void setLVE_IsCopyLinesFromInvoice (boolean LVE_IsCopyLinesFromInvoice);

	/** Get Copy Lines from Invoice	  */
	public boolean isLVE_IsCopyLinesFromInvoice();

    /** Column name M_PriceList_ID */
    public static final String COLUMNNAME_M_PriceList_ID = "M_PriceList_ID";

	/** Set Price List.
	  * Unique identifier of a Price List
	  */
	public void setM_PriceList_ID (int M_PriceList_ID);

	/** Get Price List.
	  * Unique identifier of a Price List
	  */
	public int getM_PriceList_ID();

	public org.adempiere.core.domains.models.I_M_PriceList getM_PriceList() throws RuntimeException;

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
}
