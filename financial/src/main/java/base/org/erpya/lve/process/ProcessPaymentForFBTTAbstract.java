/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                      *
 * Contributor(s): Yamel Senih ysenih@erpya.com                                      *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 * This program is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                    *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                     *
 * GNU General Public License for more details.                                      *
 * You should have received a copy of the GNU General Public License                 *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.erpya.lve.process;

import java.sql.Timestamp;
import org.compiere.process.SvrProcess;

/** Generated Process for (Process Payments for FBTT)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.0
 */
public abstract class ProcessPaymentForFBTTAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "SBP_LVE_PaymentsForFBTT";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Process Payments for FBTT";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 54005;
	/**	Parameter Name for Business Partner 	*/
	public static final String C_BPARTNER_ID = "C_BPartner_ID";
	/**	Parameter Name for Transaction Date	*/
	public static final String DATETRX = "DateTrx";
	/**	Parameter Name for Target Document Type	*/
	public static final String C_DOCTYPETARGET_ID = "C_DocTypeTarget_ID";
	/**	Parameter Name for Document Action	*/
	public static final String DOCACTION = "DocAction";
	/**	Parameter Value for Business Partner 	*/
	private int bPartnerId;
	/**	Parameter Value for Transaction Date	*/
	private Timestamp dateTrx;
	/**	Parameter Value for Target Document Type	*/
	private int docTypeTargetId;
	/**	Parameter Value for Document Action	*/
	private String docAction;

	@Override
	protected void prepare() {
		bPartnerId = getParameterAsInt(C_BPARTNER_ID);
		dateTrx = getParameterAsTimestamp(DATETRX);
		docTypeTargetId = getParameterAsInt(C_DOCTYPETARGET_ID);
		docAction = getParameterAsString(DOCACTION);
	}

	/**	 Getter Parameter Value for Business Partner 	*/
	protected int getBPartnerId() {
		return bPartnerId;
	}

	/**	 Setter Parameter Value for Business Partner 	*/
	protected void setBPartnerId(int bPartnerId) {
		this.bPartnerId = bPartnerId;
	}

	/**	 Getter Parameter Value for Transaction Date	*/
	protected Timestamp getDateTrx() {
		return dateTrx;
	}

	/**	 Setter Parameter Value for Transaction Date	*/
	protected void setDateTrx(Timestamp dateTrx) {
		this.dateTrx = dateTrx;
	}

	/**	 Getter Parameter Value for Target Document Type	*/
	protected int getDocTypeTargetId() {
		return docTypeTargetId;
	}

	/**	 Setter Parameter Value for Target Document Type	*/
	protected void setDocTypeTargetId(int docTypeTargetId) {
		this.docTypeTargetId = docTypeTargetId;
	}

	/**	 Getter Parameter Value for Document Action	*/
	protected String getDocAction() {
		return docAction;
	}

	/**	 Setter Parameter Value for Document Action	*/
	protected void setDocAction(String docAction) {
		this.docAction = docAction;
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