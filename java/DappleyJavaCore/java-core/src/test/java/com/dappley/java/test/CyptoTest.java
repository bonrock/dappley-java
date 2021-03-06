package com.dappley.java.test;

import com.dappley.java.core.crypto.AesCipher;
import com.dappley.java.core.crypto.ShaDigest;
import com.dappley.java.core.util.HashUtil;
import com.dappley.java.core.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

@Slf4j
public class CyptoTest {
    private static final String TAG = "CyptoTest";

    @Test
    public void secp256k1() throws UnsupportedEncodingException {
        String privateKey = "300c0338c4b0d49edc66113e3584e04c6b907f9ded711d396d522aae6a79be1a";
        byte[] privateKeyBytes = HashUtil.fromECDSAPrivateKey(new BigInteger(privateKey, 16));
        log.debug("secp256k1: privateKeyBytes：" + HexUtil.toHex(privateKeyBytes));
        String string = "hello world";
        byte[] data = string.getBytes("UTF-8");
        data = ShaDigest.sha256(data);
        log.debug("secp256k1: sha256：" + HexUtil.toHex(data));
        byte[] sign = HashUtil.secp256k1Sign(data, privateKeyBytes);
        String signValue = HexUtil.toHex(sign);
        log.debug("secp256k1: signValue：" + signValue);

        Assert.assertTrue(signValue != null && signValue.length() > 0);
    }

    @Test
    public void aes() {
        String data = "I am the one.";
        byte[] encoded = AesCipher.encrypt(data.getBytes(), "aesKey");
        String decoded = new String(AesCipher.decrypt(encoded, "aesKey"));

        Assert.assertEquals(data, decoded);
    }
}
