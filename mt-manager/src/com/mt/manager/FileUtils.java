package com.mt.manager;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileUtils {
    public static String formatSize(long size) {
        if (size <= 0) return "0B";
        if (size < 1024) return size + "B";
        double v = size;
        String[] u = {"B","K","M","G","T"};
        int i = 0;
        while (v >= 1024 && i < u.length-1) { v/=1024; i++; }
        DecimalFormat df = new DecimalFormat(i==0?"#":"#,##0.00");
        String s = df.format(v).replace(".",",").replace(",00","");
        // screenshot style: 44,86K  3,94K  1,32K
        return s + u[i];
    }
    public static String formatDate(long t) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault());
            return f.format(new Date(t));
        } catch (Exception e){ return ""; }
    }
    public static String formatFullDate(long t) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return f.format(new Date(t));
        } catch (Exception e){ return ""; }
    }
    public static String permString(File f) {
        boolean r = f.canRead(), w = f.canWrite(), x = f.canExecute();
        String s = (f.isDirectory()?"d":"-") + (r?"r":"-") + (w?"w":"-") + (x?"x":"-")
                + (r?"r":"-") + (w?"w":"-") + "-"
                + (r?"-":"-") + (w?"-":"-") + (x?"-":"-");
        // realistic default like -rw-rw----(660)
        int oct = (r?6:0)*100 + (w?2:0)*10 + 0;
        // better: rw=6
        if (r && w) oct = f.isDirectory()?775:660;
        else if (r) oct = 440;
        return s + "(" + oct + ")";
    }
    public static void copyRec(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            File[] fs = src.listFiles();
            if (fs!=null) for (File c: fs) copyRec(c, new File(dst, c.getName()));
        } else {
            InputStream in = new FileInputStream(src);
            dst.getParentFile().mkdirs();
            OutputStream out = new FileOutputStream(dst);
            byte[] b = new byte[8192]; int n;
            while((n=in.read(b))>0) out.write(b,0,n);
            in.close(); out.close();
        }
    }
    public static void deleteRec(File f) throws IOException {
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            if (fs!=null) for (File c: fs) deleteRec(c);
        }
        if (!f.delete()) { f.deleteOnExit(); }
    }
    public static String md5(File f) throws Exception {
        java.security.MessageDigest d = java.security.MessageDigest.getInstance("MD5");
        InputStream in = new FileInputStream(f); byte[] b=new byte[8192]; int n;
        while((n=in.read(b))>0) d.update(b,0,n); in.close();
        byte[] h=d.digest(); StringBuilder s=new StringBuilder();
        for(byte x:h) s.append(String.format("%02x",x));
        return s.toString();
    }
    public static String sha1(File f) throws Exception {
        java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-1");
        InputStream in = new FileInputStream(f); byte[] b=new byte[8192]; int n;
        while((n=in.read(b))>0) d.update(b,0,n); in.close();
        byte[] h=d.digest(); StringBuilder s=new StringBuilder();
        for(byte x:h) s.append(String.format("%02X",x));
        return s.toString();
    }
    public static String sha256(File f) throws Exception {
        java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-256");
        InputStream in = new FileInputStream(f); byte[] b=new byte[8192]; int n;
        while((n=in.read(b))>0) d.update(b,0,n); in.close();
        byte[] h=d.digest(); StringBuilder s=new StringBuilder();
        for(byte x:h) s.append(String.format("%02x",x));
        return s.toString();
    }
}
