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
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.compiere.model.I_C_Invoice;
import org.compiere.model.MColumn;
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
import org.spin.util.AbstractExportFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 * 		<a href="https://github.com/adempiere/adempiere/issues/1400">
 * 		@see FR [ 1400 ] Dynamic report export</a>
 */
public class ExportFormatXML_ISLR extends AbstractExportFormat {
	
	public ExportFormatXML_ISLR(Properties ctx, ReportEngine reportEngine) {
		setCtx(ctx);
		setReportEngine(reportEngine);
	}
	
	/**	Static Logger	*/
	private static CLogger	log	= CLogger.getCLogger (ExportFormatXML_ISLR.class);
	
	@Override
	public String getExtension() {
		return "xml";
	}

	@Override
	public String getName() {
		return Msg.getMsg(Env.getCtx(), "FileISLRXML");
	}
	
	@Override
	public boolean exportToFile(File file) {
		if(getReportEngine() == null
				|| getCtx() == null) {
			return false;
		}
		//	
		return createXML(convertFile(file));
	}
	
	
	/**
	 * 	Write XML to writer
	 * 	@param writer writer
	 * 	@return true if success
	 */
	public boolean createXML(Writer writer) {
		if(writer == null) {
			return false;
		}
		try {
			createXML(new StreamResult(writer));
			writer.flush();
			writer.close();
			return true;
		} catch (Exception e) {
			log.log(Level.SEVERE, "(w)", e);
		}
		return false;
	}	//	createXML
	
	
	/**************************************************************************
	 * 	Get XML Document representation
	 * 	@return XML document
	 * @throws ParserConfigurationException 
	 */
	public Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		//	
		PrintData printData = getPrintData();
		MPrintFormat printFormat = getPrintFormat();
		//	Root
		Element root = document.createElement(printFormat.getName());
		int orgPrintFormatItemId = 0;
		int periodPrintFormatItemId = 0;
		for (int row = 0; row < printData.getRowCount(); row++) {
			printData.setRowIndex(row);
			//	Is first
			if(row == 0) {
				String orgPrintName = "";
				String orgTaxId = "";
				String periodPrintName = "";
				String period = "";
				for(int col = 0; col < printFormat.getItemCount(); col++) {
					MPrintFormatItem item = printFormat.getItem(col);
					//	Only for column
					if(item.getAD_Column_ID() == 0) {
						continue;
					}
					MColumn column = MColumn.get(getCtx(), item.getAD_Column_ID());
					if(column == null) {
						continue;
					}
					//	
					if(column.getColumnName().equals("OrgValue")
							&& orgPrintFormatItemId == 0) {
						orgPrintFormatItemId = item.getAD_PrintFormatItem_ID();
						orgPrintName = item.getPrintName();
						Object orgValue = printData.getNode(new Integer(item.getAD_Column_ID()));
						if(orgValue != null
								&& orgValue instanceof PrintDataElement) {
							PrintDataElement orgDataElement = (PrintDataElement) orgValue;
							orgTaxId = orgDataElement.getValueDisplay(getLanguage());	//	formatted
						}
					} else if(column.getColumnName().equals(I_C_Invoice.COLUMNNAME_DateAcct)
							&& periodPrintFormatItemId == 0) {
						periodPrintFormatItemId = item.getAD_PrintFormatItem_ID();
						periodPrintName = item.getPrintName();
						Object periodValue = printData.getNode(new Integer(item.getAD_Column_ID()));
						if(periodValue != null
								&& periodValue instanceof PrintDataElement) {
							PrintDataElement periodDataElement = (PrintDataElement) periodValue;
							period = periodDataElement.getValueDisplay(getLanguage());	//	formatted
						}
					}
				}
				//	Set
				if(Util.isEmpty(orgPrintName)) {
					orgPrintName = "RifAgente";
				}
				if(Util.isEmpty(orgTaxId)) {
					orgTaxId = "";
				}
				if(Util.isEmpty(periodPrintName)) {
					periodPrintName = "Periodo";
				}
				if(Util.isEmpty(period)) {
					period = "";
				}
				root.setAttribute(periodPrintName, period);
				root.setAttribute(orgPrintName, orgTaxId);
				document.appendChild(root);
			}
			Element withholdingElement = document.createElement(printFormat.getDescription());
			//	Add to root
			root.appendChild(withholdingElement);
			int orgPrintId = orgPrintFormatItemId;
			int periodPrintId = periodPrintFormatItemId;
			//	for all columns
			Arrays.asList(printFormat.getItems())
				.stream()
				.filter(printFormatItem -> 
					printFormatItem.isPrinted() 
						&& printFormatItem.getAD_PrintFormatItem_ID() != orgPrintId 
						&& printFormatItem.getAD_PrintFormatItem_ID() != periodPrintId)
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
							//	Only for ISLR
							if(!Util.isEmpty(data)) {
								if(DisplayType.isNumeric(dataElement.getDisplayType())) {
									data = data.replaceAll(",", ".");
								} else if(DisplayType.isText(dataElement.getDisplayType())
										&& (printFormatItem.getColumnName().equals("InvoiceNo") 
												|| printFormatItem.getColumnName().equals("AffectedDocumentNo")
												|| printFormatItem.getColumnName().equals("ControlNo"))) {
									data = data.replaceAll("-", "");
									//	Change data length
									if(data.length() > 10) {
										data = data.substring(data.length() - 10, data.length());
									}
								}
							}
						}
					} else {
						log.log(Level.SEVERE, "Element not PrintData(Element) " + valueOfItem.getClass());
					}
					//	Write
					Element withholdingDetail = document.createElement(printFormatItem.getPrintName());
					withholdingDetail.appendChild(document.createTextNode(data));
					withholdingElement.appendChild(withholdingDetail);
				});
		}
		return document;
	}	//	getDocument

	/**
	 * 	Create XML representation to StreamResult
	 * 	@param result StreamResult
	 * 	@return true if success
	 */
	public boolean createXML(StreamResult result) {
		try {
			DOMSource source = new DOMSource(getDocument());
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform (source, result);
		} catch (Exception e) {
			log.log(Level.SEVERE, "(StreamResult)", e);
			return false;
		}
		return true;
	}	//	createXML
	
}	//	AbstractBatchImport
