package com.mobilsignandroid;

import android.os.Environment;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.DERPrintableString;
import org.spongycastle.asn1.DERUTF8String;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

import static com.mobilsignandroid.Consts.SERIAL_NUMBER;

public final class CertificateHelper {

    private static final String PIPE = "//**||";
    private static final String DIVIDER = ";?;!;?";

    private X509Certificate mCertificate;

    public CertificateHelper() {
        mCertificate = dajCertifikat();
    }

    public String getCertificateIds() {
        final ArrayList<Long> certificateIds = new ArrayList<>();
        certificateIds.add(1l);
        return "1;";
    }

    public boolean hasCertificate(String stringCertificate) {
        String key;
        String value;
        StringTokenizer tokenizer = new StringTokenizer(stringCertificate, DIVIDER);
        String nextToken;
        while (tokenizer.hasMoreTokens()) {
            nextToken = tokenizer.nextToken(DIVIDER);
            if (!nextToken.contains(PIPE)) {
                System.err.println("### Nebola pipe: nextToken: " + nextToken);
                System.err.println("### Nebola pipe: stringCertificate: " + stringCertificate);
                return false;
            }
            StringTokenizer tokenizer2 = new StringTokenizer(nextToken, PIPE);
            if (!tokenizer2.hasMoreTokens()) {
                System.err.println("### Nebolo viac tokenov pre key");
                return false;
            }
            key = tokenizer2.nextToken(PIPE);
            if (!tokenizer2.hasMoreTokens()) {
                System.err.println("### Nebolo viac tokenov pre value");
                return false;
            }
            value = tokenizer2.nextToken(PIPE);
            final boolean zhodujeSaAtribut = overAtributNaCertifikate(key, value);
            System.out.println("### ZHODUJU SA: " + zhodujeSaAtribut);
            if (!zhodujeSaAtribut) {
                return false;
            }
        }
        return true;
    }

    private boolean overAtributNaCertifikate(String key, String value) {
        System.out.println("### OVERUJEM atribut key: " + key + " s hodnotou: " + Arrays.toString(value.getBytes(Charset.forName("UTF-8"))) + " na certifikate");
        switch (key) {
            case Consts.CKA_SERIAL_NUMBER:
                return overASNObjektSerialNumberNaCertifikate(key, value);
            case Consts.CKA_TOKEN:
                return true;
            case Consts.CKA_CLASS:
                return value.equals(Consts.CKO_CERTIFICATE);
            case Consts.CKA_SUBJECT:
                return overASNObjektSubjectNaCertifikate(key, value);
            case Consts.CKA_ISSUER:
                return overASNObjektIssuerNaCertifikate(key, value);
            default:
                System.err.println("### Neimplementovane");
                return false;
        }
    }

    private boolean overASNObjektSerialNumberNaCertifikate(String key, String value) {
        final ASN1Primitive asn1Primitive = vytvorDerObject(value.getBytes());
        if (asn1Primitive == null || asn1Primitive.toString() == null) {
            System.out.println("### NIE JE ASN1 Primitive");
            return false;
        } else {
            System.out.println("### Overujem ASNObjekt SERIAL NUMBER: key: " + key + ", value: " + asn1Primitive.toString());
            if (asn1Primitive instanceof ASN1Integer) {
                System.out.println("### ASN1Integer value: " + ((ASN1Integer) asn1Primitive).getValue());
                System.out.println("### Certificate serial number value: " + mCertificate.getSerialNumber());
                // TODO: ziskat serial number z nasho certifikatu a porovname
                return mCertificate.getSerialNumber().equals(((ASN1Integer) asn1Primitive).getValue());
            } else {
                System.out.println("### INA INSTANCE");
                return false;
            }
        }
    }

    private boolean overASNObjektSubjectNaCertifikate(String key, String value) {
        final ASN1Primitive asn1Primitive = vytvorDerObject(value.getBytes());
        if (asn1Primitive == null || asn1Primitive.toString() == null) {
            System.out.println("### NIE JE ASN1 Primitive");
            return false;
        } else {
            if (asn1Primitive instanceof ASN1Sequence) {
                Enumeration obj = ((ASN1Sequence) asn1Primitive).getObjects();
                Object element;
                while (obj.hasMoreElements()) {
                    element = obj.nextElement();
                    if (element instanceof ASN1Set) {
                        final Enumeration enumeration = ((ASN1Set) element).getObjects();
                        Object iter;
                        while (enumeration.hasMoreElements()) {
                            iter = enumeration.nextElement();
                            if (iter instanceof ASN1Sequence) {
                                final Enumeration objects = ((ASN1Sequence) iter).getObjects();
                                Object object;
                                ASN1ObjectIdentifier identifier = null;
                                String extensionValue = null;
                                while (objects.hasMoreElements()) {
                                    object = objects.nextElement();
                                    if (object instanceof ASN1ObjectIdentifier) {
                                        identifier = (ASN1ObjectIdentifier) object;
                                    } else if (object instanceof DERPrintableString) {
                                        System.out.println("### DERPrintableString");
                                        extensionValue = ((DERPrintableString) object).toString();
                                    } else if (object instanceof DERUTF8String) {
                                        System.out.println("### DERUTF8String");
                                        extensionValue = ((DERUTF8String) object).toString();
                                    } else {
                                        System.err.println("### Unknown instance: " + object.getClass() + "; POVODNY asn1Primitive: " + asn1Primitive);
                                    }
                                }
                                overExtensionNaCertifikate(identifier, extensionValue);
                            } else {
                                System.out.println("### ASN1SET objekt nie je ASN!Sequence");
                            }
                        }
                    } else if (element instanceof ASN1Sequence) {
                        System.out.println("### SEQUENCE V SEQUENCE: ");
                    } else {
                        System.out.println("### NIECO INE V SEQUENCE: " + element.getClass());
                    }
                }
                // TODO:
                return true;
            } else {
                System.out.println("### ASN1Sequence CHYBA");
                return false;
            }
        }
    }

    private boolean overASNObjektIssuerNaCertifikate(String key, String value) {
        final ASN1Primitive asn1Primitive = vytvorDerObject(value.getBytes());
        if (asn1Primitive == null || asn1Primitive.toString() == null) {
            System.out.println("### NIE JE ASN1 Primitive");
            return false;
        } else {
            if (asn1Primitive instanceof ASN1Sequence) {
                Enumeration obj = ((ASN1Sequence) asn1Primitive).getObjects();
                Object element;
                while (obj.hasMoreElements()) {
                    element = obj.nextElement();
                    if (element instanceof ASN1Set) {
                        final Enumeration enumeration = ((ASN1Set) element).getObjects();
                        Object iter;
                        while (enumeration.hasMoreElements()) {
                            iter = enumeration.nextElement();
                            if (iter instanceof ASN1Sequence) {
                                final Enumeration objects = ((ASN1Sequence) iter).getObjects();
                                Object object;
                                ASN1ObjectIdentifier identifier = null;
                                String extensionValue = null;
                                while (objects.hasMoreElements()) {
                                    object = objects.nextElement();
                                    if (object instanceof ASN1ObjectIdentifier) {
                                        identifier = (ASN1ObjectIdentifier) object;
                                    } else if (object instanceof DERPrintableString) {
                                        System.out.println("### DERPrintableString");
                                        extensionValue = ((DERPrintableString) object).toString();
                                    } else if (object instanceof DERUTF8String) {
                                        System.out.println("### DERUTF8String");
                                        extensionValue = ((DERUTF8String) object).toString();
                                    } else {
                                        System.err.println("### Unknown instance: " + object.getClass() + "; POVODNY asn1Primitive: " + asn1Primitive);
                                    }
                                }
                                overExtensionNaCertifikate(identifier, extensionValue);
                            } else {
                                System.out.println("### ASN1SET objekt nie je ASN!Sequence");
                            }
                        }
                    } else if (element instanceof ASN1Sequence) {
                        System.out.println("### SEQUENCE V SEQUENCE: ");
                    } else {
                        System.out.println("### NIECO INE V SEQUENCE: " + element.getClass());
                    }
                }
                // TODO:
                return true;
            } else {
                System.out.println("### ASN1Sequence CHYBA");
                return false;
            }
        }
    }

    private X509Certificate dajCertifikat() {
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "certificate.p12";
        String path = baseDir + "/Download/" + fileName;
        KeyStore p12;
        try {
            p12 = KeyStore.getInstance("pkcs12");
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
        if (p12 == null) {
            return null;
        }
        try {
            p12.load(new FileInputStream(path), "ponorka90".toCharArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            e.printStackTrace();
            return null;
        }
        try {
            if (p12.aliases().hasMoreElements()) {
                return (X509Certificate) p12.getCertificate(p12.aliases().nextElement());
            } else {
                return null;
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ASN1Primitive vytvorDerObject(byte[] bytes) {
        final ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        final ASN1InputStream asn1InputStream = new ASN1InputStream(stream);
        try {
            return asn1InputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void showCertificate() throws Exception {
        System.out.println("### Show Certificate");
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String fileName = "certificate.p12";
        String path = baseDir + "/Download/" + fileName;
        KeyStore p12 = KeyStore.getInstance("pkcs12");
        p12.load(new FileInputStream(path), "ponorka90".toCharArray());
        Enumeration e = p12.aliases();
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            X509Certificate c = (X509Certificate) p12.getCertificate(alias);
            Principal subject = c.getSubjectDN();
            String subjectArray[] = subject.toString().split(",");
            for (String s : subjectArray) {
                String[] str = s.trim().split("=");
                String key = str[0];
                String value = str[1];
                System.out.println("### " + key + " - " + value);
            }
            final X509Certificate certificate = ((X509Certificate) p12.getCertificate(alias));
            System.out.println("### ISSUER = " + certificate.getIssuerDN().getName());
            System.out.println("### SUBJECT = " + certificate.getSubjectDN().getName());
            System.out.println("### SERIAL NUMBER = " + certificate.getSerialNumber());
            System.out.println("### CRITICAL OIDS: ");
            Set<String> criticalExtensionOIDs = certificate.getCriticalExtensionOIDs();
            for (String s : criticalExtensionOIDs) {
                System.out.println("### key: " + s);
                System.out.println("### EXTENSION VALUE: " + vytvorDerObject(certificate.getExtensionValue(s)).toString());
            }
            System.out.println("### NON CRITICAL OIDS: ");
            Set<String> noncriticalExtensionOIDs = certificate.getNonCriticalExtensionOIDs();
            for (String s : noncriticalExtensionOIDs) {
                System.out.println("### key: " + s);
                System.out.println("### EXTENSION VALUE: " + vytvorDerObject(certificate.getExtensionValue(s)).toString());
            }
        }
    }

    private boolean overExtensionNaCertifikate(ASN1ObjectIdentifier oid, String extension) {
        if (oid == null) {
            System.out.println("### OID je NULL");
            return false;
        } else if (extension == null) {
            System.out.println("### EXTENSION je NULL");
            return false;
        } else {
            System.out.println("### Overujem extension: OID: " + oid + ", EXTENSION: " + extension);
            final byte[] extensionValue = mCertificate.getExtensionValue(oid.toString());
            if (extensionValue != null) {
                System.out.println("### Nas Certifikat ma: OID: " + oid + ", EXTENSION: " + vytvorDerObject(extensionValue));
            } else {
                System.out.println("### Nas Certifikat ma EXTENSION NULL");
                return false;
            }
        }
        return true;
    }
}
