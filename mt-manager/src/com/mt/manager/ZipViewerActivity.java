package com.mt.manager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
import android.graphics.Typeface;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class ZipViewerActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0E0E0E"));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1E1E1E"));
        String path=getIntent().getStringExtra("path");
        TextView t=new TextView(this); t.setText(path!=null?new File(path).getName():"Arsiv");
        t.setTextColor(Color.WHITE); t.setTextSize(17); t.setTypeface(Typeface.DEFAULT_BOLD); t.setPadding(24,28,24,8);
        root.addView(t);
        ListView lv=new ListView(this); lv.setBackgroundColor(Color.parseColor("#1E1E1E"));
        root.addView(lv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        List<String> items=new ArrayList<>();
        if(path!=null){
            try{
                ZipFile z=new ZipFile(path);
                Enumeration<? extends ZipEntry> e=z.entries();
                while(e.hasMoreElements()){ ZipEntry en=e.nextElement(); items.add(en.getName()+"  ("+FileUtils.formatSize(en.getSize())+")"); }
                z.close();
                if(items.isEmpty()) items.add("(bos)");
            }catch(Exception ex){ items.add("Acilamadi: "+ex.getMessage()); }
        }
        ArrayAdapter<String> ad=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,items){
            @Override public View getView(int p, View v, ViewGroup parent){
                TextView tv=(TextView)super.getView(p,v,parent);
                tv.setTextColor(Color.parseColor("#D9D9D9")); tv.setBackgroundColor(Color.parseColor("#1E1E1E"));
                return tv;
            }
        };
        lv.setAdapter(ad);
        lv.setOnItemClickListener((a,v,p,i)->Toast.makeText(this,items.get(p),0).show());
    }
}
