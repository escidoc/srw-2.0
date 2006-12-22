/*
   Copyright 2006 OCLC Online Computer Library Center, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
/*
 * Utilities.java
 *
 * Created on October 17, 2005, 3:13 PM
 */

package ORG.oclc.os.SRW;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLTermNode;

/**
 *
 * @author levan
 */
public class Utilities {
    static final Log log=LogFactory.getLog(Utilities.class);
    static final String newLine = System.getProperty("line.separator");

    public static String byteArrayToString(byte array[]) {
        return byteArrayToString(array, 0, array.length);
    }

    public static String byteArrayToString(byte array[], int offset, int length) {
        StringBuffer str = new StringBuffer();
        StringBuffer alpha = new StringBuffer();
        int stopat = length + offset;
        char c;
        int type;

        for (int i=1; offset < stopat; offset++,i++) {
            if ((array[offset]&0xff)<16)
                str.append(" 0");
            else
                str.append(" ");
            str.append(Integer.toString(array[offset]&0xff,16));

            c = (char)array[offset];
            type = Character.getType(c);

            //      if (Character.isLetterOrDigit(c) || (c > )
            if (type == Character.CONTROL || type == Character.LINE_SEPARATOR)
                alpha.append('.');
            else
                alpha.append(c);


            if ((i%16)==0) {
                str.append("  " + alpha + newLine);
                alpha.setLength(0);
            }
        }
        offset = 0;

        str.append("  " + alpha + newLine);
        str.append(newLine);

        return str.toString();
    }


    public static String escapeBackslash(String s) {
        boolean      changed=false;
        char         c;
        StringBuffer sb=null;
        for(int i=0; i<s.length(); i++) {
            c=s.charAt(i);
            if(c=='\\') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("\\\\");
            }
            else
                if(changed)
                    sb.append(c);
        }
        if(!changed)
            return s;
        return sb.toString();
    }


    public static File findFile(final String fileName, String directory1,
      String directory2) {
        // try the current directory
        File f=new File(fileName);
        if(f.exists())
            return f;

        if(directory1!=null) {
            if(directory1.endsWith("/"))
                f=new File(directory1+fileName);
            else
                f=new File(directory1+"/"+fileName);
            if(f.exists())
                return f;
        }

        if(directory2!=null) {
            if(directory2.endsWith("/"))
                f=new File(directory2+fileName);
            else
                f=new File(directory2+"/"+fileName);
            if(f.exists())
                return f;
        }

        // finally, let's see if we can find it on the classpath
        URL url=Thread.currentThread().getContextClassLoader().getResource(fileName);
        if(url!=null) {
            f=new File(unUrlEncode(url.getFile()));
            if(f.exists())
                return f;
        }

        log.error("Couldn't find \""+fileName+"\" in the CWD or in \""+
            directory1+"\" or \""+directory2+"\" or on the classpath");
        return null;
    }

    
    public static CQLTermNode getFirstTerm(CQLNode node) {
        if(node instanceof CQLTermNode)
            return (CQLTermNode)node;
        if(node instanceof CQLBooleanNode)
            return getFirstTerm(((CQLBooleanNode)node).left);
        log.error("processing node of type: "+node);
        return null;
    }

    public static String hex07Encode(String s) {
        boolean      changed=false;
        char         c;
        StringBuffer sb=null;
        for(int i=0; i<s.length(); i++) {
            c=s.charAt(i);
            if(c<0xa) {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("&#x").append(Integer.toHexString(c)).append(';');
            }
            else
                if(changed)
                    sb.append(c);
        }
        if(!changed)
            return s;
        return sb.toString();
    }

    public static InputStream openInputStream(final String fileName,
      String directory1, String directory2) throws FileNotFoundException {
        File f=findFile(fileName, directory1, directory2);
        if(f==null) {
            throw new FileNotFoundException(fileName);
        }
        try {
            return new FileInputStream(f);
        }
        catch(Exception e) {
            log.error(e, e);
        }
        throw new FileNotFoundException(fileName);
    }


    public static String unUrlEncode(String s) {
        boolean      changed=false;
        char         c;
        StringBuffer sb=null;
        for(int i=0; i<s.length(); i++) {
            c=s.charAt(i);
            if(c=='+') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append(' ');
            }
            else if(c=='%') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append((char)Integer.parseInt(s.substring(i+1, i+3), 16));
                i+=2;
            }
            else
                if(changed)
                    sb.append(c);
        }
        if(!changed)
            return s;
        return sb.toString();
    }
 

    public static String unXmlEncode(String s) {
        boolean      changed=false;
        char         c, c1;
        StringBuffer sb=null;
        for(int i=0; i<s.length(); i++) {
            c=s.charAt(i);
            if(c=='&') {
                c1=s.charAt(i+1);
                switch(c1) {
                    case '#':
                        if(s.charAt(i+2)=='x') {
                            if(!changed) {
                                if(i>0)
                                    sb=new StringBuffer(s.substring(0, i));
                                else
                                    sb=new StringBuffer();
                                changed=true;
                            }
                            sb.append((char)Integer.parseInt(s.substring(i+3, i+5), 16));
                            i+=5;
                        }
                        break;
                    case 'a':
                        if(s.charAt(i+2)=='p' && s.charAt(i+3)=='o' &&
                           s.charAt(i+4)=='s' && s.charAt(i+5)==';') {
                            if(!changed) {
                                if(i>0)
                                    sb=new StringBuffer(s.substring(0, i));
                                else
                                    sb=new StringBuffer();
                                changed=true;
                            }
                            sb.append('\'');
                            i+=5;
                        }
                        else if(s.charAt(i+2)=='m' && s.charAt(i+3)=='p' &&
                          s.charAt(i+4)==';') {
                            if(!changed) {
                                if(i>0)
                                    sb=new StringBuffer(s.substring(0, i));
                                else
                                    sb=new StringBuffer();
                                changed=true;
                            }
                            sb.append('&');
                            i+=5;
                        }
                        break;
                    case 'g':
                        if(s.charAt(i+2)=='t' && s.charAt(i+3)==';') {
                            if(!changed) {
                                if(i>0)
                                    sb=new StringBuffer(s.substring(0, i));
                                else
                                    sb=new StringBuffer();
                                changed=true;
                            }
                            sb.append('>');
                            i+=3;
                        }
                        break;
                    case 'l':
                        if(s.charAt(i+2)=='t' && s.charAt(i+3)==';') {
                            if(!changed) {
                                if(i>0)
                                    sb=new StringBuffer(s.substring(0, i));
                                else
                                    sb=new StringBuffer();
                                changed=true;
                            }
                            sb.append('<');
                            i+=3;
                        }
                        break;
                    case 'q':
                        if(s.charAt(i+2)=='u' && s.charAt(i+3)=='o' &&
                           s.charAt(i+4)=='t' && s.charAt(i+5)==';') {
                            if(!changed) {
                                if(i>0)
                                    sb=new StringBuffer(s.substring(0, i));
                                else
                                    sb=new StringBuffer();
                                changed=true;
                            }
                            sb.append('"');
                            i+=5;
                        }
                        break;
                    default:
                        if(changed)
                            sb.append(c);
                }
            }
            else
                if(changed)
                    sb.append(c);
        }
        if(changed)
            return sb.toString();
        return s;
    }


    public static String urlEncode(String s) {
        boolean      changed=false;
        char         c;
        StringBuffer sb=null;
        for(int i=0; i<s.length(); i++) {
            c=s.charAt(i);
            if(c==' ') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append('+');
            }
            else if(c=='+' || c=='<' || c=='&' || c=='>' || c=='"' || c=='\'' ||
              c>0x7f) {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append('%').append(Integer.toHexString(c));
            }
            else
                if(changed)
                    sb.append(c);
        }
        if(!changed)
            return s;
        return sb.toString();
    }
 

    public static void writeEncoded(java.io.Writer writer, String xmlString)
            throws java.io.IOException {
        if (xmlString == null) {
            return;
        }
        char[] characters = xmlString.toCharArray();
        char character;
        for (int i = 0; i < characters.length; i++) {
            character = characters[i];
            switch (character) {
                // we don't care about single quotes since axis will
                // use double quotes anyway
                case '&':
                case '"':
                case '<':
                case '>':
                case '\n':
                case '\r':
                case '\t':
                    writer.write("&#x");
                    writer.write(Integer.toHexString(character).toUpperCase());
                    writer.write(";");
                    break;
                default:
                    if (character < 0x20) {
                        throw new IllegalArgumentException(
                          "Invalid Xml Character 00"+
                          Integer.toHexString(character));
                    } else if (character > 0x7F) {
                        writer.write("&#x");
                        writer.write(Integer.toHexString(character).toUpperCase());
                        writer.write(";");
                        /*
                        TODO: Try fixing this block instead of code above.
                        if (character < 0x80) {
                            writer.write(character);
                        } else if (character < 0x800) {
                            writer.write((0xC0 | character >> 6));
                            writer.write((0x80 | character & 0x3F));
                        } else if (character < 0x10000) {
                            writer.write((0xE0 | character >> 12));
                            writer.write((0x80 | character >> 6 & 0x3F));
                            writer.write((0x80 | character & 0x3F));
                        } else if (character < 0x200000) {
                            writer.write((0xF0 | character >> 18));
                            writer.write((0x80 | character >> 12 & 0x3F));
                            writer.write((0x80 | character >> 6 & 0x3F));
                            writer.write((0x80 | character & 0x3F));
                        }
                        */
                    } else {
                        writer.write(character);
                    }
                    break;
            }
        }
    }


    public static String xmlEncode(String s) {
        boolean      changed=false;
        char         c;
        StringBuffer sb=null;
        for(int i=0; i<s.length(); i++) {
            c=s.charAt(i);
            if(c<0xa) {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("&#x").append(Integer.toHexString(c)).append(';');
            }
            else if(c=='<') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("&lt;");
            }
            else if(c=='>') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("&gt;");
            }
            else if(c=='"') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("&quot;");
            }
            else if(c=='&') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("&amp;");
            }
            else  if(c=='\'') {
                if(!changed) {
                    if(i>0)
                        sb=new StringBuffer(s.substring(0, i));
                    else
                        sb=new StringBuffer();
                    changed=true;
                }
                sb.append("&apos;");
            }
            else
                if(changed)
                    sb.append(c);
        }
        if(!changed)
            return s;
        return sb.toString();
    }
}
