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

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.compiere.model.I_C_Order;
import org.compiere.model.MBPartner;
import org.compiere.model.MCharge;
import org.compiere.model.MCurrency;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrg;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.erpya.lve.model.MLVEList;
import org.erpya.lve.model.MLVEListLine;
import org.erpya.lve.model.MLVEListVersion;
import org.erpya.lve.model.MLVEWithholdingTax;
import org.erpya.lve.model.X_LVE_ListVersion;
import org.spin.model.I_WH_Withholding;
import org.spin.model.MWHSetting;
import org.spin.model.MWHWithholding;
import org.spin.util.AbstractWithholdingSetting;

/**
 * 	Implementación de retención de I.S.L.R para la locacización de Venezuela
 * 	Note que básicamente se realiza una validación del documento en cuestión y luego se procesa
 * 	la generación del documento de retención o el log del documento queda a libertad de la clase abstracta
 * 	@author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *  @contributor Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 */
public class OrderISLR extends AbstractWithholdingSetting {

	public OrderISLR(MWHSetting setting) {
		super(setting);
	}
	/**	Current Order	*/
	private MOrder order;
	/**	Current Business Partner	*/
	private MBPartner businessPartner;
	/**Person Type*/
	private String bpartnerPersonType = null;
	/**	Withholding Rental Exempt for Business Partner	*/
	private HashMap<MLVEList,WHConceptSettingOrder> conceptsToApply = new HashMap<MLVEList,WHConceptSettingOrder>();
	/**Tribute Unit Amount */
	BigDecimal tributeUnitAmount = Env.ZERO;
	/**Factor*/
	private static BigDecimal FACTOR = new BigDecimal(83.3334);
	/**Currency Precision */
	int curPrecision = 0 ;
	/**Manual Withholding*/
	private boolean isManual = false;
	
	@Override
	public boolean isValid() {
		boolean isValid = true;
		//	Validate Document
		if(getDocument().get_Table_ID() != I_C_Order.Table_ID) {
			addLog("@C_Order_ID@ @NotFound@");
			isValid = false;
		}
		order = (MOrder) getDocument();
		
		if (order!=null) {
			MCurrency currency = (MCurrency) order.getC_Currency();
			curPrecision = currency.getStdPrecision();
		}
		
		MLVEWithholdingTax currentWHTax = MLVEWithholdingTax.getFromClient(getContext(), getDocument().getAD_Org_ID(),MLVEWithholdingTax.TYPE_ISLR);
		//	Validate if exists Withholding Tax Definition for client
		if(currentWHTax == null) {
			addLog("@LVE_WithholdingTax_ID@ @NotFound@");
			isValid = false;
		}
		
		//	Validate if withholding if exclude for client
		if(currentWHTax!=null 
				&& currentWHTax.isClientExcluded()) {
			addLog("@IsClientExcluded@ " + currentWHTax.getName());
			isValid = false;
		}
	
		//	Validate Reversal
		if(!order.getDocStatus().equals(MOrder.DOCSTATUS_Completed)) {
			addLog("@Invalid@ @C_Order_ID@ @DocStatus@");
			isValid = false;
		}
		MDocType documentType = MDocType.get(getContext(), order.getC_DocTypeTarget_ID());
		if(documentType == null) {
			addLog("@C_DocType_ID@ @NotFound@");
			isValid = false;
		}
		//	Validate Purchase Order only
		if(documentType!=null && 
				!documentType.getDocBaseType().equals(MDocType.DOCBASETYPE_PurchaseOrder)) {
			addLog("@NotFound@ @DocBaseType@");
			isValid = false;
		}
		//	Validate Person Type
		businessPartner = (MBPartner) order.getC_BPartner();
		
		//Valid Business Partner
		Optional.ofNullable(order)
				.ifPresent(order ->{
					//Add reference
					setReturnValue(I_WH_Withholding.COLUMNNAME_SourceOrder_ID, order.getC_Order_ID());
					setReturnValue(I_WH_Withholding.COLUMNNAME_AD_Org_ID, order.getAD_Org_ID());
					if (order.isSOTrx()) {
						isManual = true;
						Optional.ofNullable(MOrg.get(getContext(), order.getAD_Org_ID()))
								.ifPresent(org ->{
								businessPartner = MBPartner.get(getContext(), org.getLinkedC_BPartner_ID(order.get_TrxName()));
						});
					}else
						isManual = false;
				});
		
		if (businessPartner==null) {
			addLog("@C_BPartner_ID@ @NotFound@");
			isValid = false;
		}else {		
			bpartnerPersonType = businessPartner.get_ValueAsString(ColumnsAdded.COLUMNNAME_PersonType);
			if(Util.isEmpty(bpartnerPersonType)) {
				addLog("@" + ColumnsAdded.COLUMNNAME_PersonType + "@ @NotFound@ @C_BPartner_ID@ " + businessPartner.getValue() + " - " + businessPartner.getName());
				isValid = false;
			}
			//	Validate Exempt Business Partner
			if(businessPartner.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsWithholdingRentalExempt)) {
				isValid = false;
				addLog("@C_BPartner_ID@ @IsWithholdingRentalExempt@");
			}
			//	Validate Withholding Definition
			setConcepts();
			if (conceptsToApply.size()==0) {
				isValid = false;
				addLog("@NotFound@ @WithholdingRentalConcept_ID@");
			}
			
			//	Validate Tribute Unit
			//MLVEWithholdingTax withholdingTaxDefinition = MLVEWithholdingTax.getFromClient(getContext(), order.getAD_Org_ID());
			if (currentWHTax!=null)
				tributeUnitAmount = currentWHTax.getValidTributeUnitAmount(order.getDateOrdered());
			
			if(tributeUnitAmount.equals(Env.ZERO)) {
				addLog("@TributeUnit@ (@Rate@ @NotFound@)");
				isValid = false;
			}
			
			String errorMessage = setRates();
			
			if (errorMessage!=null
					&& !errorMessage.isEmpty()) {
				if (!conceptsToApply.entrySet().stream().anyMatch((rateToApply) -> rateToApply.getValue().isGenerateDocument()))
					isValid = false;
				
				addLog(errorMessage);
			}
	
			//Validate if Document is Generated
			if (isGenerated()) {
				isValid = false;
			}
		}
		return isValid;
	}

	@Override
	public String run() {
		conceptsToApply.entrySet()
						.stream()
						.filter((rateToApply) -> rateToApply.getValue().isGenerateDocument())
						.forEach(rateToApply  -> {
						
						WHConceptSettingOrder conceptSetting = rateToApply.getValue();
						BigDecimal rate = conceptSetting.getRate();
						setReturnValue(ColumnsAdded.COLUMNNAME_WithholdingRentalRate_ID, conceptSetting.getRateToApply().getLVE_ListVersion_ID());
						if (conceptSetting.isVarRate()) 
							setReturnValue(ColumnsAdded.COLUMNNAME_WithholdingVariableRate_ID, conceptSetting.getVarRateToApply().getLVE_ListLine_ID());
						
						if (rate==null) 
							rate = Env.ZERO;
						
						if (rate.compareTo(Env.ZERO)!=0) {
							setWithholdingRate(rate);
							rate = getWithholdingRate(true);
							addBaseAmount(conceptSetting.getAmtBase());
							if (conceptSetting.isValid())
								addWithholdingAmount(conceptSetting.getAmtBase().multiply(rate,MathContext.DECIMAL128)
															.setScale(curPrecision,BigDecimal.ROUND_HALF_UP)
															.subtract(conceptSetting.getAmtSubtract()));
							else
								addWithholdingAmount(Env.ZERO);
							
							addDescription(rateToApply.getKey().getName());
							setReturnValue(ColumnsAdded.COLUMNNAME_Subtrahend, conceptSetting.getAmtSubtract());
							setReturnValue(ColumnsAdded.COLUMNNAME_IsCumulativeWithholding, conceptSetting.isCumulative());
							setReturnValue(ColumnsAdded.COLUMNNAME_IsSimulation, !conceptSetting.isValid());
							setReturnValue(MWHWithholding.COLUMNNAME_IsManual, isManual);
							
							int WHThirdParty_ID = order.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WHThirdParty_ID);
							if (WHThirdParty_ID != 0)
								setReturnValue(ColumnsAdded.COLUMNNAME_WHThirdParty_ID, WHThirdParty_ID);
							
							setReturnValue(MWHWithholding.COLUMNNAME_IsSimulation, true);
							
							saveResult();
						}
							
		});
		conceptsToApply.clear();
		curPrecision = 0;
		return null;
	}
	
	/**
	 * Set concepts from order document
	 */
	private void setConcepts() {
		if (order!=null) {
			if (order.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)!=0) {
				conceptsToApply.put(MLVEList.get(getContext(), order.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)),
												 new WHConceptSettingOrder(order.getTotalLines()));
				return;
			}
			
			MOrderLine[] oLines = order.getLines();
			for (MOrderLine line : oLines) {
				//Search concept for product
				MLVEList list = null;
				if (line.getM_Product_ID()!=0) {
					MProduct product = (MProduct)line.getM_Product();
					if (product.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)!=0) 
						list = MLVEList.get(getContext(), product.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID));
				}
				
				if (line.getC_Charge_ID()!=0) {
					MCharge charge = (MCharge)line.getC_Charge();
					if (charge.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID)!=0) 
						list = MLVEList.get(getContext(), charge.get_ValueAsInt(ColumnsAdded.COLUMNNAME_WithholdingRentalConcept_ID));
				}
				
				if (list!=null)
					conceptsToApply.compute(list, (concept, rateToApply) -> 
						rateToApply == null ? new WHConceptSettingOrder(line.getLineNetAmt()): rateToApply.addAmtBase(line.getLineNetAmt()));
			}
		}
	}
	
	/**
	 * Set Rates to calculate
	 * @return
	 */
	private String setRates() {

	    AtomicReference<String> resultMessage = new AtomicReference<>();
	    resultMessage.set("");
	    conceptsToApply.forEach((whConcept,whConceptSetting) ->{
	    	
	    	MLVEListVersion rateToApply = whConcept.getValidVersionInstance(order.getDateOrdered(), ColumnsAdded.COLUMNNAME_PersonType, bpartnerPersonType);
	    	MLVEListLine varRateToApply = null;
	    	BigDecimal subtractAmt = Env.ZERO;
			if (rateToApply!=null) {
				
				if (rateToApply.get_ValueAsBoolean(ColumnsAdded.COLUMNNAME_IsVariableRate)) {
					List<MLVEListLine> varRate= rateToApply.getListLine();
					if (varRate!=null) {
						varRateToApply = varRate.stream()
								.filter(listLine -> (
										whConceptSetting.getAmtBase().compareTo(listLine.getMinValue().multiply(tributeUnitAmount))>=0 && 
											(whConceptSetting.getAmtBase().compareTo(listLine.getMaxValue().multiply(tributeUnitAmount))<=0 
												|| listLine.getMaxValue().compareTo(Env.ZERO)==0))
										)
								.sorted(Comparator.comparing(MLVEListLine::getSeqNo))
								.findFirst()
								.orElse(null);
						
						if (varRateToApply==null)
							varRate.stream()
									.filter(listLine -> !(
											whConceptSetting.getAmtBase().compareTo(listLine.getMinValue().multiply(tributeUnitAmount))>=0 && 
												(whConceptSetting.getAmtBase().compareTo(listLine.getMaxValue().multiply(tributeUnitAmount))<=0 
													|| listLine.getMaxValue().compareTo(Env.ZERO)==0))
											)
									.sorted(Comparator.comparing(MLVEListLine::getSeqNo))
									.forEach(listLine -> {
										String minValue =NumberFormat.getInstance().format(listLine.getMinValue().multiply(tributeUnitAmount,MathContext.DECIMAL128));
										String maxValue =NumberFormat.getInstance().format(listLine.getMaxValue().multiply(tributeUnitAmount,MathContext.DECIMAL128));
										String baseAmt =NumberFormat.getInstance().format(whConceptSetting.getAmtBase());
										resultMessage.set(resultMessage.get()  + (resultMessage.get().isEmpty() ? "" :  "- ") + 
												"@A_Base_Amount@ < @MinAmt@ @OR@ > @MaxAmt@ (@MinAmt@ = " + minValue+ " - @MaxAmt@ = " + maxValue + "  - @A_Base_Amount@ = " + baseAmt + ") \n");
									});
						else
							whConceptSetting.setGenerateDocument(true);
					}
				}else {
					if (bpartnerPersonType.equals(X_LVE_ListVersion.PERSONTYPE_ResidentNaturalPerson)) {
						BigDecimal minValue = tributeUnitAmount.multiply(FACTOR,MathContext.DECIMAL128).setScale(curPrecision,BigDecimal.ROUND_HALF_UP);
						subtractAmt = minValue.multiply(rateToApply.getAmount()
												.divide(Env.ONEHUNDRED,MathContext.DECIMAL128)
												,MathContext.DECIMAL128)
												.setScale(curPrecision,BigDecimal.ROUND_HALF_UP);
						if (whConceptSetting.getAmtBase().compareTo(minValue)>=0) {
							whConceptSetting.setGenerateDocument(true);
						}else {
							if (rateToApply.get_ValueAsBoolean("IsCumulativeWithholding")){
								subtractAmt = minValue.multiply(rateToApply.getAmount()
																			.divide(Env.ONEHUNDRED,MathContext.DECIMAL128)
																			,MathContext.DECIMAL128)
																			.setScale(curPrecision,BigDecimal.ROUND_HALF_UP);
								whConceptSetting.setGenerateDocument(true);
								whConceptSetting.setValid(false);
							}
							resultMessage.set("@A_Base_Amount@ < @MinimumAmt@ ( @MinimumAmt@ = " + minValue + " @A_Base_Amount@ = " + whConceptSetting.getAmtBase() + ") \n");
						}
					}else 
						whConceptSetting.setGenerateDocument(true);
				}
				whConceptSetting.setRateToApply(rateToApply);
				whConceptSetting.setVarRateToApply(varRateToApply);	
				whConceptSetting.setAmtSubtract(subtractAmt);
				whConceptSetting.setCumulative(rateToApply.isCumulativeWithholding());
				if (varRateToApply!=null)
					whConceptSetting.setRate((BigDecimal)varRateToApply.get_Value(ColumnsAdded.COLUMNNAME_VariableRate));
				else
					whConceptSetting.setRate(rateToApply.getAmount());
			}else
				resultMessage.set(resultMessage.get() + "- "  + "@NotFound@ @WithholdingRentalRate_ID@");
	    });
		
		return resultMessage.get();
	}
	
	/**
	 * Validate if the document has withholding allocated
	 * @return
	 */
	private boolean isGenerated() {
		if (order!=null) 
			return new Query(getContext(), MWHWithholding.Table_Name, "SourceOrder_ID = ? "
																	+ "AND WH_Definition_ID = ? "
																	+ "AND WH_Setting_ID = ? "
																	+ "AND Processed = 'Y' "
																	+ "AND IsSimulation='Y'"
																	+ "AND DocStatus IN (?,?)" , getTransactionName())
						.setParameters(order.get_ID(),getDefinition().get_ID(),getSetting().get_ID(),MWHWithholding.DOCSTATUS_Completed,MWHWithholding.DOCSTATUS_Closed)
						.match();
		return false;
	}
}

/**
 * 
 * @author Carlos Parada, cparada@erpya.com 
 * Withholding Concept Settings to apply
 */
class WHConceptSettingOrder{
	private MLVEListVersion rateToApply = null;
	private MLVEListLine varRateToApply = null;
	private BigDecimal amtBase = Env.ZERO;
	private BigDecimal amtSubtract = Env.ZERO;
	private boolean isCumulative = false;
	private boolean isValid = true;
	private boolean generateDocument = false;
	private BigDecimal rate = Env.ZERO;
	
	/**
	 * Constructor
	 * @param amtBase
	 */
	public WHConceptSettingOrder(BigDecimal amtBase) {
		setAmtBase(amtBase);
	}
	
	/**
	 * Set Amount base
	 * @param amtBase
	 */
	public void setAmtBase(BigDecimal amtBase) {
		if (amtBase!=null)
			this.amtBase = amtBase;
	}
	
	/**
	 * Add to Amount Base
	 * @param amt
	 * @return
	 */
	public WHConceptSettingOrder addAmtBase(BigDecimal amt) {
		if (amt!=null)
			this.amtBase = (this.amtBase == null ? Env.ZERO : this.amtBase).add(amt);
		return this;
	}
	
	/**
	 * get Amount Base
	 * @return
	 */
	public BigDecimal getAmtBase() {
		return amtBase;
	}
	
	/**
	 * Set Rate to Apply
	 * @param rateToApply
	 */
	public void setRateToApply(MLVEListVersion rateToApply) {
		this.rateToApply = rateToApply;
	}
	
	/**
	 * Set Variable Rate to Apply
	 * @param varRateToApply
	 */
	public void setVarRateToApply(MLVEListLine varRateToApply) {
		this.varRateToApply = varRateToApply;
	}
	
	/**
	 * Set Amount to Subtract
	 * @param amtSubtract
	 */
	public void setAmtSubtract(BigDecimal amtSubtract) {
		this.amtSubtract = amtSubtract;
	}
	
	/**
	 * set IsValid 
	 * @param isValid
	 */
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
	
	/**
	 * Get is Valid
	 * @return
	 */
	public boolean isValid() {
		return isValid;
	}
	
	/**
	 * Set Generated Document
	 * @param generateDocument
	 */
	public void setGenerateDocument(boolean generateDocument) {
		this.generateDocument = generateDocument;
	}
	
	/**
	 * Get Generate Document
	 * @return
	 */
	public boolean isGenerateDocument() {
		return generateDocument;
	}
	
	/**
	 * Get Rate to Apply
	 * @return
	 */
	public MLVEListVersion getRateToApply() {
		return rateToApply;
	}
	
	/**
	 * Get Variable Rate to Apply
	 * @return
	 */
	public MLVEListLine getVarRateToApply() {
		return varRateToApply;
	}
	
	/**
	 * Get Amount to Subtract
	 * @return
	 */
	public BigDecimal getAmtSubtract() {
		return amtSubtract;
	}
	
	/**
	 * Get is Variable Rate
	 * @return
	 */
	public boolean isVarRate() {
		return varRateToApply!=null;
	}
	
	/**
	 * Set Rate
	 * @param rate
	 */
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}
	
	/**
	 * Get Rate
	 * @return
	 */
	public BigDecimal getRate() {
		return rate;
	}
	
	/**
	 * Set Cumulative
	 * @return
	 */
	public boolean isCumulative() {
		return isCumulative;
	}
	
	/**
	 * Get Cumulative
	 * @param isCumulative
	 */
	public void setCumulative(boolean isCumulative) {
		this.isCumulative = isCumulative;
	}
	
	@Override
	public String toString() {
		return "Rate = " + getRate()
				+ ",AmtBase = " + getAmtBase()
				+ ",Subtract = " + getAmtSubtract()
				+ ",GenerateDocument = " + isGenerateDocument()
				+ ",IsValid = " + isValid()
				
		;
	}
}
