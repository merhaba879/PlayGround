package com.mt.manager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.pm.*;
import java.io.File;

public class ApkEditorActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0E0E0E"));
        ScrollView sv=new ScrollView(this); sv.setBackgroundColor(Color.parseColor("#1E1E1E"));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24,24,24,24);
        sv.addView(root); setContentView(sv);
        String path=getIntent().getStringExtra("path");
        TextView title=new TextView(this); title.setText("APK Duzenle"); title.setTextColor(Color.WHITE); title.setTextSize(18); title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);
        TextView info=new TextView(this); info.setTextColor(Color.parseColor("#D4D4D4")); info.setTextSize(13);
        StringBuilder sb=new StringBuilder();
        sb.append("Dosya: ").append(path).append("\n");
        if(path!=null){
            try{
                PackageManager pm=getPackageManager();
                PackageInfo pi=pm.getPackageArchiveInfo(path,PackageManager.GET_ACTIVITIES);
                if(pi!=null){
                    sb.append("Paket: ").append(pi.packageName).append("\n");
                    sb.append("Surum: ").append(pi.versionName).append(" (").append(pi.versionCode).append(")\n");
                    ApplicationInfo ai=pi.applicationInfo;
                    if(ai!=null) sb.append("Etiket: ").append(pm.getApplicationLabel(ai)).append("\n");
                } else sb.append("Paket bilgisi alinamadi (sistem uygulamasi degil).\n");
            }catch(Exception e){ sb.append("Hata: ").append(e.getMessage()).append("\n"); }
            File f=new File(path);
            sb.append("Boyut: ").append(FileUtils.formatSize(f.length())).append(" (").append(f.length()).append(")\n");
            sb.append("Tarih: ").append(FileUtils.formatFullDate(f.lastModified())).append("\n");
        }
        sb.append("\nNot: tam XML incelemesi icin APK'yi cikar + Apktool kullan (kisaltildi).\n");
        sb.append("Ikon degistirme: /sdcard/yeni_ikon.png yolu girin.\n");
        info.setText(sb.toString()); root.addView(info);
        String[] ops={"AndroidManifest.xml Goruntule","Kaynaklar (res/) Listele","DEX / Smali Incele","Yeniden Paketle + Imzala","Cikar (/storage/emulated/0/MTClone_cikan/)"};
        for(String o:ops){
            Button btn=new Button(this); btn.setText(o); btn.setTextColor(Color.WHITE); btn.setBackgroundColor(Color.parseColor("#2E2E2E"));
            btn.setOnClickListener(v->Toast.makeText(this,o+" : yakinda / su an kopyalandi",0).show());
            root.addView(btn);
        }
        TextView hint=new TextView(this);
        hint.setText("Not: Smali duzenleme icin dex'i cikar, editorle duzenle, Yeniden Paketle.\nKurulum icin imzalamak gerekir (apksigner).");
        hint.setTextColor(Color.GRAY); hint.setTextSize(11); hint.setPadding(0,16,0,0);
        root.addView(hint);
    }
}
