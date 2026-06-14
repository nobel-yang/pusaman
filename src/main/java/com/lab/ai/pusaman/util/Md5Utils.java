package com.lab.ai.pusaman.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.pff.PSTGlobalObjectId.bytesToHex;

/**
 * @author yang.nobel
 * @since 2026-06-13 19:04
 **/
public class Md5Utils {

    public static String encrypt(String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("MD5 algorithm not found", e);
        }
    }
}
