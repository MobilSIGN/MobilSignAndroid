package com.mobilsignandroid;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import static com.mobilsignandroid.Consts.SERIAL_NUMBER;

public final class CertificateHelper {

    private static final char PIPE = '|';
    private static final char DIVIDER = ';';

    public boolean hasCertificate(String stringCertificate) {
        final CertificateFactory certFactory;
        X509Certificate x509Certificate = null;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
            /**
             * TODO: tu dat do kontruktora certifikat z databazy
             */
            final InputStream in = new ByteArrayInputStream(new byte[0]);
            x509Certificate = (X509Certificate) certFactory.generateCertificate(in);
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        if (x509Certificate != null) {
            return certificatesEquals(x509Certificate, stringCertificate);
        } else {
            return false;
        }
    }

    private boolean certificatesEquals(X509Certificate x509Certificate, String stringCertificate) {
        final HashMap<String, String> certAtributes = new HashMap<>();
        String key;
        String value;
        int pipePosition;
        while (!stringCertificate.isEmpty()) {
            pipePosition = stringCertificate.indexOf(PIPE);
            key = stringCertificate.substring(0, PIPE - 1);
            stringCertificate = stringCertificate.replace(key + "" + PIPE, "");
            value = stringCertificate.substring(0, DIVIDER - 1);
            stringCertificate = stringCertificate.replace(value + "" + DIVIDER, "");
            certAtributes.put(key, value);
        }
        /**
         * TODO: porovnavat vsetky parametre
         */
        return x509Certificate.getSerialNumber().equals(certAtributes.get(SERIAL_NUMBER));
    }
}
