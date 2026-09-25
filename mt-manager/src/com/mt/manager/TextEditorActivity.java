package com.mt.manager;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.Color;
import android.graphics.Typeface;
import java.io.*;

public class TextEditorActivity extends Activity {
    EditText edit; File file; TextView title;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0E0E0E"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1E1E1E"));
        String path = getIntent().getStringExtra("path");
        file = path!=null?new File(path):null;
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(Color.parseColor("#0E0E0E"));
        top.setPadding(16,24,16,12);
        title = new TextView(this);
        title.setText(file!=null?file.getName():"Yeni dosya");
        title.setTextColor(Color.WHITE); title.setTextSize(16); title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Button save = new Button(this); save.setText("Kaydet");
        save.setTextColor(Color.WHITE); save.setBackgroundColor(Color.parseColor("#2196F3"));
        save.setOnClickListener(v->save());
        top.addView(title); top.addView(save);
        root.addView(top);
        edit = new EditText(this);
        edit.setBackgroundColor(Color.parseColor("#1E1E1E"));
        edit.setTextColor(Color.parseColor("#D4D4D4"));
        edit.setTypeface(Typeface.MONOSPACE); edit.setTextSize(13);
        edit.setGravity(Gravity.TOP|Gravity.START);
        edit.setSingleLine(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1);
        edit.setLayoutParams(lp);
        edit.setPadding(16,16,16,16);
        root.addView(edit);
        setContentView(root);
        if (file!=null && file.exists()) {
            try {
                if (file.length()>1024*1024) { edit.setText("Dosya cok buyuk ("+FileUtils.formatSize(file.length())+"), onizleme kapali."); }
                else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
                    StringBuilder sb=new StringBuilder(); String l;
                    while((l=br.readLine())!=null) sb.append(l).append("\n");
                    br.close(); edit.setText(sb.toString());
                }
            } catch(Exception e){ edit.setText("Acilamadi: "+e.getMessage()); }
        }
    }
    void save(){
        try{
            if(file==null){ Toast.makeText(this,"Yol yok",0).show(); return; }
            file.getParentFile().mkdirs();
            OutputStreamWriter w=new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
            w.write(edit.getText().toString()); w.close();
            Toast.makeText(this,"Kaydedildi: "+file.getName(),0).show();
        }catch(Exception e){ Toast.makeText(this,"Hata: "+e.getMessage(),0).show(); }
    }
}
