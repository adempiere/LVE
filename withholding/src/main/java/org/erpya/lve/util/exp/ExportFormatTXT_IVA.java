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
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MQuery;
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
		return createTXTFile(convertFile(file), '\t', getReportEngine());
	}
	
	/**
	 * 	Write delimited file to writer
	 * 	@param writer writer
	 *  @param delimiter delimiter, e.g. comma, tab
	 *  @param language translation language
	 *  @param printHeader if you want a row with column names included
	 *  @param engine
	 *  try
	 * 	@return true if success
	 */
	public boolean createTXTFile (Writer writer, char delimiter, ReportEngine engine) {
		
		if (delimiter == 0)
			delimiter = '\t';
		try {
			int startAt = 0;
			PrintData printData = (engine != null? engine.getPrintData(): getPrintData());
			MPrintFormat printFormat = (engine != null? engine.getPrintFormat(): getPrintFormat());
			//	for all rows (-1 = header row)
			for (int row = startAt; row < printData.getRowCount(); row++) {
				StringBuffer sb = new StringBuffer();
				if (row != -1)
					printData.setRowIndex(row);

				//	for all columns
				boolean first = true;	//	first column to print
				for (int col = 0; col < printFormat.getItemCount(); col++) {
					MPrintFormatItem item = printFormat.getItem(col);
					if (item.isPrinted()) {
						//	column delimiter (comma or tab)
						if (first)
							first = false;
						else
							sb.append(delimiter);
						//	header row
						if (row == -1) {
							createCSVvalue (sb, delimiter,
									printFormat.getItem(col).getPrintName(getLanguage()));
						} else {
							Object obj = printData.getNode(new Integer(item.getAD_Column_ID()));
							String data = "";
							if (obj == null) {
								if(item.getPrintFormatType().equals(MPrintFormatItem.PRINTFORMATTYPE_Text)) {
									if(!Util.isEmpty(item.getPrintName())) {
										data = item.getPrintName() + (!Util.isEmpty(item.getPrintNameSuffix())? item.getPrintNameSuffix(): "");
									}
								}
							} else if (obj instanceof PrintDataElement) {
								PrintDataElement pde = (PrintDataElement)obj;
								if (item.isTypePrintFormat()) {
									writer.write(sb.toString());
									sb = new StringBuffer();
									writer.write(CRLF);
									MPrintFormat format = MPrintFormat.get (getCtx(), item.getAD_PrintFormatChild_ID(), false);
									format.setLanguage(getLanguage());
									int AD_Column_ID = item.getAD_Column_ID();
									log.info(format + " - Item=" + item.getName() + " (" + AD_Column_ID + ")");
									//
									String recordString = pde.getValueKey();
									int Record_ID = 0;
									try {
										Record_ID = Integer.parseInt(recordString);
									} catch (Exception e) {
										log.log(Level.SEVERE, "Invalid Record Key - " + recordString
												+ " (" + e.getMessage()
												+ ") - AD_Column_ID=" + AD_Column_ID + " - " + item);
									}
									MQuery query = new MQuery (format.getAD_Table_ID());
									query.addRestriction(item.getColumnName(), MQuery.EQUAL, new Integer(Record_ID));
									format.setTranslationViewQuery(query);
									log.fine(query.toString());
								}
								else if (pde.isPKey()) {
									data = pde.getValueAsString();
								} else {
									data = pde.getValueDisplay(getLanguage());	//	formatted
									//	Only for IVA
									if(!Util.isEmpty(data)
											&& DisplayType.isNumeric(pde.getDisplayType())) {
										data = data.replaceAll(",", ".");
									}
								}
							} else {
								log.log(Level.SEVERE, "Element not PrintData(Element) " + obj.getClass());
							}
							createCSVvalue (sb, delimiter, data);
						}
					}	//	printed
				}	//	for all columns
				writer.write(sb.toString());
				writer.write(Env.NL);
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
	 * 	@param delimiter delimiter
	 * 	@param content column value
	 */
	private void createCSVvalue (StringBuffer sb, char delimiter, String content)
	{
		//	nothing to add
		if (content == null || content.length() == 0)
			return;

		// don't quote tab-delimited file
		if ( delimiter == '\t' )
		{
			sb.append(content);
			return;
		}		
		//
		boolean needMask = false;
		StringBuffer buff = new StringBuffer();
		char chars[] = content.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			char c = chars[i];
			if (c == '"')
			{
				needMask = true;
				buff.append(c);		//	repeat twice
			}	//	mask if any control character
			else if (!needMask && (c == delimiter || !Character.isLetterOrDigit(c)))
				needMask = true;
			buff.append(c);
		}

		//	Optionally mask value
		if (needMask)
			sb.append('"').append(buff).append('"');
		else
			sb.append(buff);
	}	//	addCSVColumnValue
}	//	AbstractBatchImport
