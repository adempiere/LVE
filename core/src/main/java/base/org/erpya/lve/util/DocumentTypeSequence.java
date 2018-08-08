/**************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                               *
 * This program is free software; you can redistribute it and/or modify it    		  *
 * under the terms version 2 or later of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope           *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                   *
 * See the GNU General Public License for more details.                               *
 * You should have received a copy of the GNU General Public License along            *
 * with this program; if not, printLine to the Free Software Foundation, Inc.,        *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                             *
 * For the text or an alternative of this public license, you may reach us            *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, S.A. All Rights Reserved.  *
 * Contributor: Yamel Senih ysenih@erpya.com                                          *
 * Contributor: Carlos Parada cparada@erpya.com                                       *
 * See: www.erpya.com                                                                 *
 *************************************************************************************/
package org.erpya.lve.util;

import java.text.DecimalFormat;

import org.compiere.model.MDocType;
import org.compiere.model.MSequence;
import org.compiere.util.Env;

/**
 * 	Class added from standard values
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<a href="https://github.com/adempiere/LVE/issues/2>
 * 		@see FR [ 2 ] Add standard LVE values</a>
 */
public class DocumentTypeSequence {

	/**
	 * From documentType
	 * @param documentType
	 */
	public DocumentTypeSequence(MDocType documentType) {
		this.documentType = documentType;
	}
	
	/**	Document Type	*/
	private MDocType documentType = null;
	/**
	 * Get current control no and save new sequence
	 * @return
	 */
	public String getControlNo() {
		//	Get Control No Sequence by User
		int sequenceId = documentType.get_ValueAsInt("ControlNoSequence_ID");
		//	Load Sequence
		if(sequenceId != 0) {
			MSequence seqControlNo = new MSequence(Env.getCtx(), sequenceId, documentType.get_TrxName());
			String prefix = seqControlNo.getPrefix();
			String suffix = seqControlNo.getSuffix();
			String decimalPattern = seqControlNo.getDecimalPattern();
			int next = seqControlNo.getNextID();
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
		//	Nothing
		return null;
	}
}
