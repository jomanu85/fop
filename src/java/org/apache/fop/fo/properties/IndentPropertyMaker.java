/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.properties;

import org.apache.fop.apps.FOPException;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FOPropertyMapping;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.NumericOp;

/**
 * This property maker handles the calculations described in 5.3.2 which
 * involves the sizes of the corresponding margin-* properties and the
 * padding-* and border-*-width properties.
 */
public class IndentPropertyMaker extends CorrespondingPropertyMaker {
    /**
     * The corresponding padding-* propIds 
     */
    private int[] paddingCorresponding = null;    

    /**
     * The corresponding border-*-width propIds 
     */
    private int[] borderWidthCorresponding = null;
    
    /**
     * Create a start-indent or end-indent property maker.
     * @param baseMaker
     */
    public IndentPropertyMaker(PropertyMaker baseMaker) {
        super(baseMaker);
    }

    /**
     * Set the corresponding values for the padding-* properties.
     * @param paddingCorresponding the corresping propids.
     */
    public void setPaddingCorresponding(int[] paddingCorresponding) {
        this.paddingCorresponding = paddingCorresponding;
    }
    
    /**
     * Set the corresponding values for the border-*-width properties.
     * @param borderWidthCorresponding the corresping propids.
     */
    public void setBorderWidthCorresponding(int[] borderWidthCorresponding) {
        this.borderWidthCorresponding = borderWidthCorresponding;
    }
    
    /**
     * Calculate the corresponding value for start-indent and end-indent.  
     * @see CorrespondingPropertyMaker#compute(PropertyList)
     */
    public Property compute(PropertyList propertyList) throws FOPException {
        PropertyList pList = getWMPropertyList(propertyList);
        // Calculate the values as described in 5.3.2.
        try {
            int marginProp = pList.getWritingMode(lr_tb, rl_tb, tb_rl);
            Numeric margin;
//          Calculate the absolute margin.
            if (propertyList.getExplicitOrShorthand(marginProp) == null) {
                Property indent = propertyList.getExplicit(baseMaker.propId);
                if (indent == null) {
                    margin = new FixedLength(0);
                } else {
                    margin = propertyList.getExplicit(baseMaker.propId).getNumeric();
                    margin = NumericOp.subtraction(margin, propertyList.getInherited(baseMaker.propId).getNumeric());
                }
                margin = NumericOp.subtraction(margin, getCorresponding(paddingCorresponding, propertyList).getNumeric());
                margin = NumericOp.subtraction(margin, getCorresponding(borderWidthCorresponding, propertyList).getNumeric());
            } else {
                margin = propertyList.get(marginProp).getNumeric();
            }
            
            Numeric v = new FixedLength(0);
            if (!propertyList.getFObj().generatesReferenceAreas()) {
                // The inherited_value_of([start|end]-indent)
                v = NumericOp.addition(v, propertyList.getInherited(baseMaker.propId).getNumeric());
            }
            // The corresponding absolute margin-[right|left}.
            v = NumericOp.addition(v, margin);
            v = NumericOp.addition(v, getCorresponding(paddingCorresponding, propertyList).getNumeric());
            v = NumericOp.addition(v, getCorresponding(borderWidthCorresponding, propertyList).getNumeric());
            return (Property) v;
        } catch (org.apache.fop.fo.expr.PropertyException propEx) {
           String propName = FOPropertyMapping.getPropertyName(baseMaker.getPropId());
           throw new FOPException("Error in " + propName 
                   + " calculation " + propEx);
        }    
    }
    
    private Property getCorresponding(int[] corresponding, PropertyList propertyList) {
        PropertyList pList = getWMPropertyList(propertyList);
        int wmcorr = pList.getWritingMode(corresponding[0], corresponding[1], corresponding[2]);
        return propertyList.get(wmcorr);
    }
}
