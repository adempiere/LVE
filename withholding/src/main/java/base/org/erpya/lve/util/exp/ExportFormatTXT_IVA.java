/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2015 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpcya.com                                 *
 *****************************************************************************/
package org.erpya.lve.util.exp;

import java.io.File;
import java.io.Writer;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.compiere.print.MPrintFormat;
import org.compiere.print.MPrintFormatItem;
import org.compiere.print.PrintData;
import org.compiere.print.PrintDataElement;
import org.compiere.print.ReportEngine;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.spin.util.ExportFormatCSV;

/**
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 * 		<a href="https://github.com/adempiere/adempiere/issues/1400">
 * 		@see FR [ 1400 ] Dynamic report export</a>
 */
public class ExportFormatTXT_IVA extends ExportFormatCSV {
	
	public ExportFormatTXT_IVA(Properties ctx, ReportEngine reportEngine) {
		super(ctx, reportEngine);
	}
	
	/**	Static Logger	*/
	private static CLogger	log	= CLogger.getCLogger (ExportFormatTXT_IVA.class);
	
	private final static char CR  = (char) 0x0D;
	private final static char LF  = (char) 0x0A; 
	private final static String CRLF  = "" + CR + LF; 
	
	@Override
	public String getExtension() {
		return "txt";
	}

	@Override
	public String getName() {
		return Msg.getMsg(Env.getCtx(), "FileIVATXT");
	}
	
	@Override
	public boolean exportToFile(File file) {
		if(getReportEngine() == null
				|| getCtx() == null) {
			return false;
		}
		//	
		return createTXTFile(convertFile(file), getReportEngine());
	}
	
	/**
	 * 	Write delimited file to writer
	 * 	@param writer writer
	 *  @param engine
	 *  try
	 * 	@return true if success
	 */
	private boolean createTXTFile (Writer writer, ReportEngine engine) {
		try {
			int startAt = 0;
			PrintData printData = (engine != null? engine.getPrintData(): getPrintData());
			MPrintFormat printFormat = (engine != null? engine.getPrintFormat(): getPrintFormat());
			boolean firstLine = true;
			//	for all rows (-1 = header row)
			for (int row = startAt; row < printData.getRowCount(); row++) {
				StringBuffer buffer = new StringBuffer();
				if (row != -1) {
					printData.setRowIndex(row);
				}
				AtomicBoolean isFirstColumn = new AtomicBoolean(true);
				Arrays.asList(printFormat.getItems())
					.stream()
					.filter(printFormatItem -> printFormatItem.isPrinted())
					.forEach(printFormatItem -> {
						Object valueOfItem = printData.getNode(new Integer(printFormatItem.getAD_Column_ID()));
						String data = "";
						if (valueOfItem == null) {
							if(printFormatItem.getPrintFormatType().equals(MPrintFormatItem.PRINTFORMATTYPE_Text)) {
								if(!Util.isEmpty(printFormatItem.getPrintName())) {
									data = printFormatItem.getPrintName() + (!Util.isEmpty(printFormatItem.getPrintNameSuffix())? printFormatItem.getPrintNameSuffix(): "");
								}
							}
						} else if (valueOfItem instanceof PrintDataElement) {
							PrintDataElement dataElement = (PrintDataElement) valueOfItem;
							if (dataElement.isPKey()) {
								data = dataElement.getValueAsString();
							} else {
								if(!Util.isEmpty(printFormatItem.getFormatPattern())) {
									dataElement.setM_formatPattern(printFormatItem.getFormatPattern());
								}
								data = dataElement.getValueDisplay(getLanguage());	//	formatted
								//	Only for IVA
								if(!Util.isEmpty(data)) {
									if(DisplayType.isNumeric(dataElement.getDisplayType())) {
										data = data.replaceAll(",", ".");
									} else if(DisplayType.isText(dataElement.getDisplayType())
											&& (printFormatItem.getColumnName().equals("InvoiceNo") 
													|| printFormatItem.getColumnName().equals("AffectedDocumentNo")
													|| printFormatItem.getColumnName().equals("ControlNo")
													|| printFormatItem.getColumnName().equals("DocumentNo"))) {
										data = data.trim();
									}
								}
							}
						} else {
							log.log(Level.SEVERE, "Element not PrintData(Element) " + valueOfItem.getClass());
						}
						//	Set default
						if(Util.isEmpty(data)) {
							data = "0";
						}
						//	column delimiter
						if (isFirstColumn.get()) {
							isFirstColumn.set(false);
						} else {
							buffer.append('\t');
						}
						createCSVvalue (buffer, data);
				});
				//	Validate first line
				if(firstLine) {
					firstLine = false;
				} else if(buffer.length() > 0) {
					writer.write(CRLF);
				}
				writer.write(buffer.toString());
			}	//	for all rows
			//
			writer.flush();
		} catch (Exception e) {
			log.log(Level.SEVERE, "(w)", e);
		}
		return true;
	}	//	createDelimitedFile

	/**
	 * 	Add Content to CSV string.
	 *  Encapsulate/mask content in " if required
	 * 	@param sb StringBuffer to add to
	 * 	@param content column value
	 */
	private void createCSVvalue (StringBuffer sb, String content) {
		//	nothing to add
		if (content == null || content.length() == 0)
			return;
		sb.append(content);
	}	//	addCSVColumnValue
}	//	AbstractBatchImport
