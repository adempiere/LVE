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
package org.erpya.lve.util;

import java.text.DecimalFormat;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MMovement;
import org.compiere.model.MNote;
import org.compiere.model.MOrder;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * 	Class added for handle control number for sequence
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class DocumentTypeSequence {

	private String trxName = null;
	/**
	 * From documentType
	 * @param documentType
	 */
	public DocumentTypeSequence(MDocType documentType, String trxName) {
		this.documentType = documentType;
		setTrxName(trxName);
	}
	
	/**	Document Type	*/
	private MDocType documentType = null;
	/**
	 * Get current control no and save new sequence
	 * @return
	 */
	public String getControlNo() {
		//	Get Control No Sequence by User
		int sequenceId = documentType.get_ValueAsInt(LVEUtil.COLUMNNAME_ControlNoSequence_ID);
		//	Load Sequence
		if(sequenceId != 0) {
			MSequence seqControlNo = new MSequence(Env.getCtx(), sequenceId, getTrxName());
			String prefix = seqControlNo.getPrefix();
			String suffix = seqControlNo.getSuffix();
			String decimalPattern = seqControlNo.getDecimalPattern();
			int next = seqControlNo.getNextID();
			if (validate(seqControlNo, next)) {
				//	
				StringBuffer doc = new StringBuffer();
				if (decimalPattern != null && decimalPattern.length() > 0) {
					doc.append(new DecimalFormat(decimalPattern).format(next));
				} else {
					doc.append(next);
				}
				//	Set
				if(prefix == null 
						|| prefix.length() == 0)
					prefix = "";
	
				if(suffix == null 
						|| suffix.length() == 0)
					suffix = "";
				//	Save
				seqControlNo.saveEx();
				//	Return valid sequence
				String validSequence = prefix + doc + suffix;
				validateControlNo(validSequence, seqControlNo);
				return validSequence;
			}
		}
		//	Nothing
		return null;
	}
	
	/**
	 * Get Transaction Name
	 * @return
	 */
	public String getTrxName() {
		return trxName;
	}
	
	/**
	 * Set Transaction Name
	 * @param trxName
	 */
	public void setTrxName(String trxName) {
		this.trxName = trxName;
	}
	
	/**
	 * Validate Control Number Sequence 
	 * @param sequence
	 * @param nextId
	 * @return
	 */
	private boolean validate(MSequence sequence, int nextId) {
		boolean valid = false;
		if (sequence != null) {
			int endNumber = sequence.get_ValueAsInt(LVEUtil.COLUMNNAME_LVE_SequenceEndNo);
			if (nextId <= endNumber) {
				valid = true;
				checkControlNumberUsedLevel(sequence, nextId, endNumber);
			}
			else
				throw new AdempiereException("@AD_Sequence_ID@ -> @ControlNo@ @LVE_SequenceEndNo@ (" + endNumber + ")> @CurrentNext@ (" + nextId + ")");
		}
		return valid;
	}
	
	/**
	 * Create System Note
	 * @param sequence
	 * @param nextId
	 * @param endNumber
	 */
	private void checkControlNumberUsedLevel(MSequence sequence, int nextId, int endNumber) {
		int controlNumberAvailable = endNumber - nextId;
		int levelControlNumberWarning = MSysConfig.getIntValue(LVEUtil.SYSCONFIG_LVE_WarningControlNumberAvailable, 20, sequence.getAD_Client_ID(), sequence.getAD_Org_ID());
		if (levelControlNumberWarning > controlNumberAvailable) {
			MNote note = new MNote (sequence.getCtx(), LVEUtil.MESSAGE_LVE_WarningControlNumber , Env.getAD_User_ID(sequence.getCtx()), sequence.getAD_Client_ID(), sequence.getAD_Org_ID(), sequence.get_TrxName());
				note.setRecord(sequence.get_Table_ID(), sequence.get_ID());
				note.setReference(sequence.getName());
				String textValue = sequence.getName().concat(": ")
								   .concat(Env.NL)
								   .concat("@CurrentNext@ : ").concat(String.valueOf(nextId))
								   .concat(Env.NL)
								   .concat("@LVE_SequenceEndNo@ : ").concat(String.valueOf(endNumber));
				note.setTextMsg(Msg.parseTranslation(sequence.getCtx(), textValue));
				note.saveEx();
		}
	}
	
	/***
	 * Validate Control Number for Duplicate
	 * @param controlNumber
	 * @param documentTypeId
	 * @param sequence
	 */
	public static void validateControlNo(String controlNumber, MSequence sequence ) {
		
		String whereClause = "";
		boolean controlNumberValidateOnInvoice = MSysConfig.getBooleanValue(LVEUtil.SYSCONFIG_LVE_ValidateControlNumberOnInvoice, true, sequence.getAD_Client_ID(), sequence.getAD_Org_ID());
		
		// Validate Invoice Duplicate Control Number with the same Document Control Number Sequence Definition
		if (controlNumberValidateOnInvoice) {
			whereClause =  " IsFiscalDocument = 'Y' AND "
								+ " IsSOTrx = 'Y' AND "
								+ " ControlNo = ? AND "
								+ " DocStatus IN ('CO', 'CL') AND "
								+ " EXISTS(SELECT 1 "
										+ "	FROM C_DocType dt "
										+ "	INNER JOIN C_DocType dt_control ON (dt.ControlNoSequence_ID = dt_control.ControlNoSequence_ID) "
										+ "	WHERE dt_control.C_DocType_ID = C_Invoice.C_DocTypeTarget_ID)";
			Optional<MInvoice> maybeInvoice = Optional.ofNullable(new Query(sequence.getCtx(), MInvoice.Table_Name, whereClause , sequence.get_TrxName())
																.setParameters(controlNumber)
																.setClient_ID()
																.setOnlyActiveRecords(true)
																.first());
										
			maybeInvoice.ifPresent(invoice -> {
				if (invoice.get_ID() > 0) 
					throw new AdempiereException("@AlreadyExists@ @ControlNo@ ".concat(invoice.get_ValueAsString(LVEUtil.COLUMNNAME_ControlNo)).concat(" -> @C_Invoice_ID@ ").concat(invoice.getDocumentNo()));
			});
		}
		
		boolean controlNumberValidateOnSalesOrder = MSysConfig.getBooleanValue(LVEUtil.SYSCONFIG_LVE_ValidateControlNumberOnSalesOrder, true, sequence.getAD_Client_ID(), sequence.getAD_Org_ID());
		
		// Validate Order Duplicate Control Number with the same Document Control Number Sequence Definition
		if (controlNumberValidateOnSalesOrder) {
			whereClause =  " IsFiscalDocument = 'Y' AND "
							+ " IsSOTrx = 'Y' AND "
							+ " ControlNo = ? AND "
							+ " DocStatus IN ('CO', 'CL') AND "
							+ " EXISTS(SELECT 1 "
									+ "	FROM C_DocType dt "
									+ "	INNER JOIN C_DocType dt_control ON (dt.ControlNoSequence_ID = dt_control.ControlNoSequence_ID) "
									+ "	WHERE dt_control.C_DocType_ID = C_Order.C_DocTypeTarget_ID)";
			Optional<MOrder> maybeOrder = Optional.ofNullable(new Query(sequence.getCtx(), MOrder.Table_Name, whereClause , sequence.get_TrxName())
																.setParameters(controlNumber)
																.setClient_ID()
																.setOnlyActiveRecords(true)
																.first());
										
			maybeOrder.ifPresent(order -> {
				if (order.get_ID() > 0) 
					throw new AdempiereException("@AlreadyExists@ @ControlNo@ ".concat(order.get_ValueAsString(LVEUtil.COLUMNNAME_ControlNo)).concat(" -> @C_Order_ID@ ").concat(order.getDocumentNo()));
			});
		}
		
		boolean controlNumberValidateOnInventoryMovement = MSysConfig.getBooleanValue(LVEUtil.SYSCONFIG_LVE_ValidateControlNumberOnInventoryMovement, true, sequence.getAD_Client_ID(), sequence.getAD_Org_ID());

		// Validate Inventory Movement Duplicate Control Number with the same Document Control Number Sequence Definition
		if (controlNumberValidateOnInventoryMovement) {
			whereClause =  " IsFiscalDocument = 'Y' AND "
							+ " ControlNo = ? AND "
							+ " DocStatus IN ('CO', 'CL') AND "
							+ " EXISTS(SELECT 1 "
									+ "	FROM C_DocType dt "
									+ "	INNER JOIN C_DocType dt_control ON (dt.ControlNoSequence_ID = dt_control.ControlNoSequence_ID) "
									+ "	WHERE dt_control.C_DocType_ID = M_Movement.C_DocType_ID)";
			Optional<MMovement> maybeMovement = Optional.ofNullable(new Query(sequence.getCtx(), MMovement.Table_Name, whereClause , sequence.get_TrxName())
																.setParameters(controlNumber)
																.setClient_ID()
																.setOnlyActiveRecords(true)
																.first());
										
			maybeMovement.ifPresent(movement -> {
				if (movement.get_ID() > 0) 
					throw new AdempiereException("@AlreadyExists@ @ControlNo@ ".concat(movement.get_ValueAsString(LVEUtil.COLUMNNAME_ControlNo)).concat(" -> @M_Movement_ID@ ").concat(movement.getDocumentNo()));
			});
		}

	}
}
