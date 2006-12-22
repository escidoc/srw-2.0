/**
 * XSortKeysType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package gov.loc.www.zing.srw.xsortkeys;

public class XSortKeysType  implements java.io.Serializable {
    private gov.loc.www.zing.srw.xsortkeys.SortKeyType[] sortKey;

    public XSortKeysType() {
    }

    public gov.loc.www.zing.srw.xsortkeys.SortKeyType[] getSortKey() {
        return sortKey;
    }

    public void setSortKey(gov.loc.www.zing.srw.xsortkeys.SortKeyType[] sortKey) {
        this.sortKey = sortKey;
    }

    public gov.loc.www.zing.srw.xsortkeys.SortKeyType getSortKey(int i) {
        return sortKey[i];
    }

    public void setSortKey(int i, gov.loc.www.zing.srw.xsortkeys.SortKeyType value) {
        this.sortKey[i] = value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof XSortKeysType)) return false;
        XSortKeysType other = (XSortKeysType) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.sortKey==null && other.getSortKey()==null) || 
             (this.sortKey!=null &&
              java.util.Arrays.equals(this.sortKey, other.getSortKey())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getSortKey() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSortKey());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSortKey(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(XSortKeysType.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.loc.gov/zing/srw/xsortkeys/", "xSortKeysType"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sortKey");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.loc.gov/zing/srw/xsortkeys/", "sortKey"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.loc.gov/zing/srw/xsortkeys/", "sortKeyType"));
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
