package com.dappley.android.sdk;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.dappley.android.sdk.chain.TransactionManager;
import com.dappley.android.sdk.net.ProtocalProvider;
import com.dappley.android.sdk.protobuf.TransactionProto;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.web3j.crypto.ECKeyPair;

import java.math.BigInteger;

@RunWith(AndroidJUnit4.class)
public class TransactionTest {

    @Test
    public void testTransaction() {
        Context context = InstrumentationRegistry.getTargetContext();
        DappleyClient.init(context, ProtocalProvider.ProviderType.RPC);

        BigInteger privateKey = new BigInteger("bb23d2ff19f5b16955e8a24dca34dd520980fe3bddca2b3e1b56663f0ec1aa7e", 16);
        ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
        String fromAddress = "1BpXBb3uunLa9PL8MmkMtKNd3jzb5DHFkG";
        String toAddress = "1BpXBb3uunLa9PL8MmkMtKNd3jzb5DHFkG";
        int amount = 2;
        TransactionProto.Transaction transaction = TransactionManager.newTransaction(fromAddress, toAddress, amount, ecKeyPair);
        System.out.println(transaction.toByteArray());

        Assert.assertEquals(transaction.toString(), "");
    }
}
