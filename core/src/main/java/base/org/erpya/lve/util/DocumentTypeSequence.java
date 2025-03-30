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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MSequence;
import org.compiere.util.Env;

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
				return prefix + doc + suffix;
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
			if (endNumber <= nextId) 
				valid = true;
			else
				throw new AdempiereException("@AD_Sequence_ID@ -> @ControlNo@ @LVE_SequenceEndNo@ (" + endNumber + ")> @CurrentNext@ (" + nextId + ")");
		}
		return valid;
	}
}
