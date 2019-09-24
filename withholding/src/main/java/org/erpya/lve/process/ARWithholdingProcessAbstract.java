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

package org.erpya.lve.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.process.SvrProcess;

/** Generated Process for (Withholding Process for AR Invoice / AR Credit Memo)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.2
 */
public abstract class ARWithholdingProcessAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "Withholding Process (ARI / ARC)";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Withholding Process for AR Invoice / AR Credit Memo";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54308;
	/**	Parameter Name for Withholding Type	*/
	public static final String WH_TYPE_ID = "WH_Type_ID";
	/**	Parameter Name for Withholding Setting	*/
	public static final String WH_SETTING_ID = "WH_Setting_ID";
	/**	Parameter Name for Withholding 	*/
	public static final String WH_DEFINITION_ID = "WH_Definition_ID";
	/**	Parameter Name for A_Base_Amount	*/
	public static final String A_BASE_AMOUNT = "A_Base_Amount";
	/**	Parameter Name for Withholding Rate	*/
	public static final String WITHHOLDINGRATE = "WithholdingRate";
	/**	Parameter Name for Withholding Amt	*/
	public static final String WITHHOLDINGAMT = "WithholdingAmt";
	/**	Parameter Name for Document No	*/
	public static final String DOCUMENTNO = "DocumentNo";
	/**	Parameter Name for Document Date	*/
	public static final String DATEDOC = "DateDoc";
	/**	Parameter Value for Withholding Type	*/
	private int typeId;
	/**	Parameter Value for Withholding Setting	*/
	private int settingId;
	/**	Parameter Value for Withholding 	*/
	private int definitionId;
	/**	Parameter Value for A_Base_Amount	*/
	private BigDecimal baseAmount;
	/**	Parameter Value for Withholding Rate	*/
	private BigDecimal withholdingRate;
	/**	Parameter Value for Withholding Amt	*/
	private BigDecimal withholdingAmt;
	/**	Parameter Value for Document No	*/
	private String documentNo;
	/**	Parameter Value for Document Date	*/
	private Timestamp dateDoc;

	@Override
	protected void prepare() {
		typeId = getParameterAsInt(WH_TYPE_ID);
		settingId = getParameterAsInt(WH_SETTING_ID);
		definitionId = getParameterAsInt(WH_DEFINITION_ID);
		baseAmount = getParameterAsBigDecimal(A_BASE_AMOUNT);
		withholdingRate = getParameterAsBigDecimal(WITHHOLDINGRATE);
		withholdingAmt = getParameterAsBigDecimal(WITHHOLDINGAMT);
		documentNo = getParameterAsString(DOCUMENTNO);
		dateDoc = getParameterAsTimestamp(DATEDOC);
	}

	/**	 Getter Parameter Value for Withholding Type	*/
	protected int getTypeId() {
		return typeId;
	}

	/**	 Setter Parameter Value for Withholding Type	*/
	protected void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	/**	 Getter Parameter Value for Withholding Setting	*/
	protected int getSettingId() {
		return settingId;
	}

	/**	 Setter Parameter Value for Withholding Setting	*/
	protected void setSettingId(int settingId) {
		this.settingId = settingId;
	}

	/**	 Getter Parameter Value for Withholding 	*/
	protected int getDefinitionId() {
		return definitionId;
	}

	/**	 Setter Parameter Value for Withholding 	*/
	protected void setDefinitionId(int definitionId) {
		this.definitionId = definitionId;
	}

	/**	 Getter Parameter Value for A_Base_Amount	*/
	protected BigDecimal getBaseAmount() {
		return baseAmount;
	}

	/**	 Setter Parameter Value for A_Base_Amount	*/
	protected void setBaseAmount(BigDecimal baseAmount) {
		this.baseAmount = baseAmount;
	}

	/**	 Getter Parameter Value for Withholding Rate	*/
	protected BigDecimal getWithholdingRate() {
		return withholdingRate;
	}

	/**	 Setter Parameter Value for Withholding Rate	*/
	protected void setWithholdingRate(BigDecimal withholdingRate) {
		this.withholdingRate = withholdingRate;
	}

	/**	 Getter Parameter Value for Withholding Amt	*/
	protected BigDecimal getWithholdingAmt() {
		return withholdingAmt;
	}

	/**	 Setter Parameter Value for Withholding Amt	*/
	protected void setWithholdingAmt(BigDecimal withholdingAmt) {
		this.withholdingAmt = withholdingAmt;
	}

	/**	 Getter Parameter Value for Document No	*/
	protected String getDocumentNo() {
		return documentNo;
	}

	/**	 Setter Parameter Value for Document No	*/
	protected void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}

	/**	 Getter Parameter Value for Document Date	*/
	protected Timestamp getDateDoc() {
		return dateDoc;
	}

	/**	 Setter Parameter Value for Document Date	*/
	protected void setDateDoc(Timestamp dateDoc) {
		this.dateDoc = dateDoc;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}