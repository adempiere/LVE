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
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_C_Invoice;
import org.compiere.model.MBPartner;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MOrgInfo;
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
import org.erpya.lve.util.LVEUtil;
import org.spin.util.ExportFormatCSV;

/**
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 * 		@see Txt de Impuesto Municipal para Araure Portuguesa
 */
public class PaezImpuestoMunicipal_TXT extends ExportFormatCSV {
	
	public PaezImpuestoMunicipal_TXT(Properties ctx, ReportEngine reportEngine) {
		super(ctx, reportEngine);
	}
	
	/**	Static Logger	*/
	private static CLogger	log	= CLogger.getCLogger (PaezImpuestoMunicipal_TXT.class);
	
	private final static char CR  = (char) 0x0D;
	private final static char LF  = (char) 0x0A; 
	private final static String CRLF  = "" + CR + LF; 
	
	@Override
	public String getExtension() {
		return "txt";
	}

	@Override
	public String getName() {
		return Msg.getMsg(Env.getCtx(), "FileIMPaezTXT");
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
			List<MPrintFormatItem> printFormatItems = Arrays.asList(printFormat.getItems());
			boolean firstLine = true;
			//	for all rows (-1 = header row)
			for (int row = startAt; row < printData.getRowCount(); row++) {
				StringBuffer buffer = new StringBuffer();
				if (row != -1) {
					printData.setRowIndex(row);
				}
				AtomicBoolean isFirstColumn = new AtomicBoolean(true);
				printFormatItems.stream()
					.filter(printFormatItem -> printFormatItem.isPrinted())
					.forEach(printFormatItem -> {
						String columnName = Optional.ofNullable(printFormatItem.getColumnName()).orElse("");
						Object valueOfItem = printData.getNode(Integer.valueOf(printFormatItem.getAD_Column_ID()));
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
								//	Only for IM
								if(!Util.isEmpty(data)) {
									if(DisplayType.isNumeric(dataElement.getDisplayType())) {
										if(columnName.equals("WithholdingRate")) {
											data = data.replace(".", ",") + "%";
										} else {
											data = data.replace(".", ",");
										}
									} else if(DisplayType.isText(dataElement.getDisplayType())) {
										if(columnName.equals("TaxID") 
												|| columnName.equals("OrgValue")) {
											data = data.trim();
											if(data.length() > 4) {
												data = data.substring(0, 1) + "-" + data.substring(1, data.length() - 1) + "-" + data.substring(data.length() -1);
											}
										}
									} else if(DisplayType.isLookup(dataElement.getDisplayType())) {
										if(columnName.equals(I_C_Invoice.COLUMNNAME_C_Invoice_ID)) {
											String value = dataElement.getValueKey();
											if(!Util.isEmpty(value)) {
												String transactionName = null;
												if(engine.getProcessInfo() != null) {
													transactionName = engine.getProcessInfo().getTransactionName();
												}
												if(printFormatItem.getName().equals("TipoContribuyente")) {
													MInvoice invoice = getInvoice(value, transactionName);
													if(invoice != null) {
														MOrgInfo organizationInfo = MOrgInfo.get(getCtx(), invoice.getAD_Org_ID(), null);
														MLocation invoiceLocation = MLocation.get(getCtx(), invoice.getC_BPartner_Location_ID(), transactionName);
														MLocation organizationLocation = MLocation.get(getCtx(), organizationInfo.getC_Location_ID(), transactionName);
														if(invoiceLocation.getC_City_ID() == organizationLocation.getC_City_ID()) {
															data = "D";
														} else {
															data = "T";
														}
													}
												} else if(printFormatItem.getName().equals("CodigoAgente")) {
													MInvoice invoice = getInvoice(value, transactionName);
													if(invoice != null) {
														MOrgInfo organizationInfo = MOrgInfo.get(getCtx(), invoice.getAD_Org_ID(), null);
														String businessPartnerCode = null;
														if(organizationInfo.get_ValueAsInt("WH_BPartner_ID") > 0) {
															MBPartner businessPartner = new MBPartner(getCtx(), organizationInfo.get_ValueAsInt("WH_BPartner_ID"), transactionName);
															businessPartnerCode = businessPartner.get_ValueAsString(LVEUtil.COLUMNNAME_LVE_CommercialActivityLicense);
														}
														data = String.format("%1$" + 5 + "s", Optional.ofNullable(businessPartnerCode).orElse("")).replace(" ", "0");
													}
												} else if(printFormatItem.getName().equals("CodigoProveedor")) {
													MInvoice invoice = getInvoice(value, transactionName);
													if(invoice != null) {
														MBPartner businessPartner = new MBPartner(getCtx(), invoice.getC_BPartner_ID(), transactionName);
														String businessPartnerCode = Optional.ofNullable(businessPartner.get_ValueAsString(LVEUtil.COLUMNNAME_LVE_CommercialActivityLicense)).orElse("");
														data = String.format("%1$" + 5 + "s", businessPartnerCode).replace(" ", "0");
													}
												} else if(printFormatItem.getName().equals("NombreProveedor")) {
													MInvoice invoice = getInvoice(value, transactionName);
													if(invoice != null) {
														data = invoice.getC_BPartner().getName();
													}
												}
											}
										} else {
											data = data.replace(".", ",");
										}
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
	 * Get Invoice from value
	 * @param value
	 * @param transactionName
	 * @return
	 */
	private MInvoice getInvoice(String value, String transactionName) {
		if(!Util.isEmpty(value)) {
			int invoiceId = 0;
			try {
				invoiceId = Integer.parseInt(value);
			} catch (Exception e) {
				log.warning(e.getLocalizedMessage());
			}
			if(invoiceId > 0) {
				return new MInvoice(getCtx(), invoiceId, transactionName);
			}
		}
		return null;
	}

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
