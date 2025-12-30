/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.                                     *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net                                                  *
 * or https://github.com/adempiere/adempiere/blob/develop/license.html        *
 *****************************************************************************/

package org.erpya.lve.process;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.erpya.lve.util.LVEUtil;

/** Generated Process for (Cancel Invoice)
 *  @author Jesús Albujas 
 *  @version Release 3.9.4
 */
public class CancelInvoice extends CancelInvoiceAbstract
{
    @Override
    protected void prepare()
    {
        super.prepare();
    }

    @Override
    protected String doIt() throws Exception
    {
        // source invoice
        MInvoice invoiceFrom = new MInvoice(getCtx(), getSourceInvoiceId(), get_TrxName());
        
        if (invoiceFrom.get_ID() == 0) {
            throw new AdempiereException("@SourceInvoice_ID@ @NotFound@");
        }
        
        if (!MInvoice.DOCSTATUS_Completed.equals(invoiceFrom.getDocStatus())) {
            throw new AdempiereException("@SQLErrorReferenced@ " + " @C_Invoice_ID@: " + invoiceFrom.getDocumentNo() + ("@NoCompleted@") );
        }
        
        // Validate if invoice has payments allocated
        boolean hasPayment = new Query(
                getCtx(),
                "C_AllocationLine",
                "C_Invoice_ID = ?",
                get_TrxName()
            )
            .setParameters(invoiceFrom.getC_Invoice_ID())
            .match();

        if (hasPayment) {
        	throw new AdempiereException("@SQLErrorReferenced@ " + "@C_Payment_ID@ @IsAllocated@");
        }
        
		String whereClause = "InvoiceToAllocate_ID = ? AND DocStatus = 'CO'";

		MInvoice existingCancelled = new Query(getCtx(), MInvoice.Table_Name, whereClause, get_TrxName())
			    .setParameters(invoiceFrom.getC_Invoice_ID())
			    .first();

		if (existingCancelled != null) {
			throw new AdempiereException("@SQLErrorReferenced@ " 
					+ " @C_Invoice_ID@: " + existingCancelled.getDocumentNo());
		}

        MInvoice invoiceTo = new MInvoice(getCtx(), 0, get_TrxName());
        copyInvoiceFrom(invoiceTo, invoiceFrom);

        int linesCopied = copyInvoiceFromLines(invoiceTo, invoiceFrom);
        
        log.info("Lines copied: " + linesCopied);

        invoiceTo.processIt(MInvoice.DOCACTION_Complete);
        invoiceTo.saveEx();

        return "@C_Invoice_ID@: " + invoiceTo.getDocumentNo();
    }
    
    /**
     * Copy the header data from the source invoice to the destination invoice
     * Following the pattern of MInvoice copyFrom method
     * @param invoiceTo Destination invoice (must already have ID)
     * @param invoiceFrom Source invoice
     */
    private void copyInvoiceFrom(MInvoice invoiceTo, MInvoice invoiceFrom)
    {
        if (invoiceFrom.get_ID() == 0) {
            log.log(Level.SEVERE, "Source invoice must have valid ID");
            return;
        }
        
        int invoiceToId = invoiceTo.getC_Invoice_ID();

        // Copy values from source to destination
        PO.copyValues(invoiceFrom, invoiceTo);
        
        // Reset to new record (no ID yet)
        invoiceTo.setC_Invoice_ID(invoiceToId);
        
        invoiceTo.setDocumentNo(null);
        invoiceTo.set_ValueOfColumn(LVEUtil.COLUMNNAME_ControlNo, null);
        invoiceTo.setProcessed(false);
        invoiceTo.setPosted(false);
        invoiceTo.setDocStatus(MInvoice.STATUS_Drafted);
        invoiceTo.setDocAction(MInvoice.DOCACTION_Complete);
        invoiceTo.setDateAcct(Env.getContextAsDate(getCtx(), "#Date"));

        if (getDocTypeId() > 0) {
            invoiceTo.setC_DocTypeTarget_ID(getDocTypeId());
            invoiceTo.setC_DocType_ID(getDocTypeId());
        }
        
        invoiceTo.set_ValueOfColumn("InvoiceToAllocate_ID", invoiceFrom.getC_Invoice_ID());
        
        // Now save with all required fields
        if (!invoiceTo.save(get_TrxName())) {
            log.log(Level.SEVERE, "Error when save invoice header");
        }
    }
    
    /**
     * Copy lines from source invoice to destination invoice
     * Uses the standard copyLinesFrom method and then sets custom columns
     * Following the same pattern as copyInvoiceFrom for the header
     * @param invoiceTo Destination invoice
     * @param invoiceFrom Source invoice
     * @return Number of lines copied
     */
    private int copyInvoiceFromLines(MInvoice invoiceTo, MInvoice invoiceFrom)
    {
        if (invoiceTo.get_ID() == 0) {
            log.log(Level.SEVERE, "Destination invoice must have valid ID");
            return 0;
        }
        
        // Copy lines using standard method
        int linesCopied = invoiceTo.copyLinesFrom(invoiceFrom, false, false);
        
        if (linesCopied == 0) {
            log.log(Level.WARNING, "No lines were copied");
            return 0;
        }
        
        MInvoiceLine[] linesFrom = invoiceFrom.getLines(false);
        MInvoiceLine[] linesTo = invoiceTo.getLines(false);
        
        if (linesTo == null || linesTo.length == 0) {
            log.log(Level.WARNING, "No lines found in destination invoice");
            return 0;
        }
        
        for (int i = 0; i < linesTo.length; i++) {
            MInvoiceLine lineTo = linesTo[i];
            
            if (linesFrom != null && i < linesFrom.length) {
                MInvoiceLine lineFrom = linesFrom[i];
                lineTo.set_ValueOfColumn("InvoiceLineToAllocate_ID", lineFrom.getC_InvoiceLine_ID());
            }
            
            lineTo.set_ValueOfColumn("InvoiceToAllocate_ID", invoiceFrom.getC_Invoice_ID());
            
            // Save line
            if (!lineTo.save(get_TrxName())) {
                log.log(Level.SEVERE, "@SaveError@ Line: " + lineTo.getC_InvoiceLine_ID());
            }
        }
        
        return linesCopied;
    }
    
}