package com.mt.manager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.StatFs;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    static class Item {
        File f; String name; boolean dir; long size; long time;
        Item(File f){ this.f=f; this.name=f.getName(); this.dir=f.isDirectory();
            try{ this.size=f.isDirectory()?0:f.length(); }catch(Exception e){size=0;}
            try{ this.time=f.lastModified(); }catch(Exception e){time=0;}
        }
    }

    File curL, curR;
    int activePane = 0;
    Stack<File> backL=new Stack<>(), fwdL=new Stack<>(), backR=new Stack<>(), fwdR=new Stack<>();
    List<Item> listL=new ArrayList<>(), listR=new ArrayList<>();
    MtAdapter adL, adR;
    TextView pathTv, infoTv;
    ListView lvL, lvR;
    View paneWrapL, paneWrapR;
    boolean showHidden=false;
    int sortMode=0; //0 name 1 date 2 size
    // clipboard
    List<File> bufSrc=null; String bufMode=null;
    File bookmarksFile = new File("/storage/emulated/0/MTClone_etiketler.txt");

    ExecutorService searchExec = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0E0E0E"));
        requestPerms();
        curL = new File("/storage/emulated/0");
        curR = new File("/storage/emulated/0");
        if(!curL.exists()) curL=new File("/sdcard");
        if(!curR.exists()) curR=new File("/sdcard");
        buildUI();
        refreshPane(0); refreshPane(1);
        updateTop();
    }

    void requestPerms(){
        try{
            if(Build.VERSION.SDK_INT>=30){
                if(!Environment.isExternalStorageManager()){
                    try{
                        Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        i.setData(Uri.parse("package:"+getPackageName()));
                        startActivity(i);
                    }catch(Exception e){}
                }
            } else {
                requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            }
        }catch(Exception e){}
    }

    void buildUI(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0E0E0E"));
        // top bar
        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setBackgroundColor(Color.parseColor("#0E0E0E"));
        top.setPadding(8,20,8,8);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button ham=new Button(this); ham.setText("☰"); ham.setTextColor(Color.parseColor("#8A8A8A"));
        ham.setBackgroundColor(Color.TRANSPARENT); ham.setTextSize(20);
        ham.setOnClickListener(v->showBookmarks());
        LinearLayout mid=new LinearLayout(this); mid.setOrientation(LinearLayout.VERTICAL);
        mid.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        pathTv=new TextView(this); pathTv.setTextColor(Color.parseColor("#6E6E6E"));
        pathTv.setTextSize(19); pathTv.setTypeface(Typeface.DEFAULT_BOLD);
        pathTv.setSingleLine(true); pathTv.setEllipsize(android.text.TextUtils.TruncateAt.START);
        infoTv=new TextView(this); infoTv.setTextColor(Color.parseColor("#6E6E6E")); infoTv.setTextSize(12);
        mid.addView(pathTv); mid.addView(infoTv);
        Button more=new Button(this); more.setText("⋮"); more.setTextColor(Color.parseColor("#8A8A8A"));
        more.setBackgroundColor(Color.TRANSPARENT); more.setTextSize(20);
        more.setOnClickListener(v->showTopMenu());
        top.addView(ham); top.addView(mid); top.addView(more);
        root.addView(top);
        // panes
        LinearLayout panes=new LinearLayout(this); panes.setOrientation(LinearLayout.HORIZONTAL);
        panes.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1));
        paneWrapL=makePane(0); paneWrapR=makePane(1);
        panes.addView(paneWrapL,new LinearLayout.LayoutParams(0,-1,1));
        panes.addView(paneWrapR,new LinearLayout.LayoutParams(0,-1,1));
        root.addView(panes);
        // bottom bar
        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setBackgroundColor(Color.parseColor("#141414"));
        bottom.setPadding(0,6,0,6);
        String[] icons={"<",">","+","⇄","↑"};
        for(int i=0;i<5;i++){
            final int idx=i;
            Button bb=new Button(this); bb.setText(icons[i]);
            bb.setTextColor(Color.parseColor("#454545")); bb.setTextSize(22);
            bb.setBackgroundColor(Color.TRANSPARENT);
            bb.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            bb.setOnClickListener(v->onBottom(idx));
            bottom.addView(bb);
        }
        root.addView(bottom);
        setContentView(root);
    }

    View makePane(final int pane){
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setBackgroundColor(Color.parseColor("#1E1E1E"));
        // thin divider handled by parent
        ListView lv=new ListView(this);
        lv.setBackgroundColor(Color.parseColor("#1E1E1E"));
        lv.setDivider(new ColorDrawable(Color.parseColor("#141414")));
        lv.setDividerHeight(1);
        lv.setLayoutParams(new LinearLayout.LayoutParams(-1,-1,1));
        if(pane==0){ lvL=lv; } else { lvR=lv; }
        MtAdapter ad=new MtAdapter(pane==0?listL:listR);
        lv.setAdapter(ad);
        if(pane==0) adL=ad; else adR=ad;
        lv.setOnItemClickListener((a,v,p,i)->{
            activePane=pane; highlightPanes(); updateTop();
            List<Item> ls = pane==0?listL:listR;
            if(p>=0&&p<ls.size()){ Item it=ls.get(p); openItem(pane,it); }
        });
        lv.setOnItemLongClickListener((a,v,p,i)->{
            activePane=pane; highlightPanes(); updateTop();
            List<Item> ls = pane==0?listL:listR;
            if(p>=0&&p<ls.size()){ showContextMenu(ls.get(p).f); }
            return true;
        });
        // long-press empty -> paste / new
        lv.setOnLongClickListener(v->{ showPaneLongMenu(pane); return true; });
        wrap.addView(lv);
        return wrap;
    }

    void highlightPanes(){
        try{
            paneWrapL.setBackgroundColor(activePane==0?Color.parseColor("#2B2B2B"):Color.parseColor("#1E1E1E"));
            paneWrapR.setBackgroundColor(activePane==1?Color.parseColor("#2B2B2B"):Color.parseColor("#1E1E1E"));
        }catch(Exception e){}
    }

    void onBottom(int idx){
        if(idx==0){ goBack(activePane); }
        else if(idx==1){ goFwd(activePane); }
        else if(idx==2){ showNewMenu(); }
        else if(idx==3){ // swap
            File t=curL; curL=curR; curR=t;
            refreshPane(0); refreshPane(1); updateTop();
            Toast.makeText(this,"Pencereleri değiştir",0).show();
        }
        else if(idx==4){ goUp(activePane); }
    }

    File curOf(int pane){ return pane==0?curL:curR; }
    void setCur(int pane, File f, boolean pushHist){
        if(pane==0){
            if(pushHist&&curL!=null) backL.push(curL);
            curL=f; if(pushHist) fwdL.clear();
        }else{
            if(pushHist&&curR!=null) backR.push(curR);
            curR=f; if(pushHist) fwdR.clear();
        }
        refreshPane(pane); updateTop();
    }
    void goBack(int pane){
        try{
            if(pane==0){ if(!backL.isEmpty()){ fwdL.push(curL); curL=backL.pop(); refreshPane(0); updateTop(); } }
            else { if(!backR.isEmpty()){ fwdR.push(curR); curR=backR.pop(); refreshPane(1); updateTop(); } }
        }catch(Exception e){}
    }
    void goFwd(int pane){
        try{
            if(pane==0){ if(!fwdL.isEmpty()){ backL.push(curL); curL=fwdL.pop(); refreshPane(0); updateTop(); } }
            else { if(!fwdR.isEmpty()){ backR.push(curR); curR=fwdR.pop(); refreshPane(1); updateTop(); } }
        }catch(Exception e){}
    }
    void goUp(int pane){
        File c=curOf(pane);
        if(c!=null&&c.getParentFile()!=null) setCur(pane,c.getParentFile(),true);
    }

    void refreshPane(int pane){
        File cur=curOf(pane);
        List<Item> out = pane==0?listL:listR;
        out.clear();
        if(cur!=null){
            // ".." navigation helper like screenshot right pane
            // add parent entries for convenience
            try{
                File[] fs=cur.listFiles();
                if(fs!=null){
                    for(File f:fs){
                        if(!showHidden&&f.getName().startsWith(".")) continue;
                        out.add(new Item(f));
                    }
                    // sort
                    if(sortMode==0) Collections.sort(out,(a,b2)->{
                        if(a.dir!=b2.dir) return a.dir?-1:1;
                        return a.name.compareToIgnoreCase(b2.name);
                    });
                    else if(sortMode==1) Collections.sort(out,(a,b2)->Long.compare(b2.time,a.time));
                    else Collections.sort(out,(a,b2)->Long.compare(b2.size,a.size));
                }
            }catch(Exception e){}
        }
        if(pane==0&&adL!=null) adL.notifyDataSetChanged();
        if(pane==1&&adR!=null) adR.notifyDataSetChanged();
        updateTop();
    }

    void updateTop(){
        try{
            File cur=curOf(activePane);
            String p=cur!=null?cur.getAbsolutePath():"/storage/emulated/0";
            pathTv.setText(p+"/");
            int dc=0,fc=0;
            List<Item> ls=activePane==0?listL:listR;
            for(Item it:ls){ if(it.dir)dc++; else fc++; }
            String disk="--";
            try{
                StatFs st=new StatFs(cur!=null?cur.getAbsolutePath():"/storage/emulated/0");
                long avail=st.getAvailableBytes(), total=st.getTotalBytes();
                disk=FileUtils.formatSize(total-avail)+"/"+FileUtils.formatSize(total);
            }catch(Exception e){}
            infoTv.setText("Klasörler: "+dc+"  Dosyalar: "+fc+"  Disk: "+disk);
        }catch(Exception e){}
    }

    class MtAdapter extends BaseAdapter {
        List<Item> data;
        MtAdapter(List<Item> d){data=d;}
        public int getCount(){return data.size();}
        public Object getItem(int p){return data.get(p);}
        public long getItemId(int p){return p;}
        public View getView(int p, View cv, ViewGroup parent){
            Item it=data.get(p);
            LinearLayout row=new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBackgroundColor(Color.parseColor("#1E1E1E"));
            row.setPadding(8,8,8,8); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView icon=new TextView(MainActivity.this);
            icon.setText(it.dir?"📁":"📄");
            icon.setTextSize(20); icon.setGravity(Gravity.CENTER);
            icon.setWidth(110); icon.setHeight(110);
            GradientDrawable gd=new GradientDrawable();
            gd.setCornerRadius(12);
            String ext=it.name.contains(".")?it.name.substring(it.name.lastIndexOf(".")+1).toLowerCase():"";
            if(it.dir){ gd.setColor(Color.parseColor("#2E2E2E")); }
            else if(ext.equals("txt")||ext.equals("php")||ext.equals("py")||ext.equals("java")||ext.equals("js")){ gd.setColor(Color.parseColor("#1A4D8F")); icon.setTextColor(Color.WHITE); }
            else if(ext.equals("bak")){ gd.setColor(Color.parseColor("#3A3A3A")); }
            else if(ext.equals("zip")||ext.equals("apk")||ext.equals("rar")){ gd.setColor(Color.parseColor("#6A5A2E")); }
            else if(ext.equals("png")||ext.equals("jpg")||ext.equals("jpeg")||ext.equals("webp")){ gd.setColor(Color.parseColor("#2E6B4F")); }
            else { gd.setColor(Color.parseColor("#2B2B2B")); }
            icon.setBackground(gd);
            LinearLayout col=new LinearLayout(MainActivity.this); col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1)); col.setPadding(16,0,0,0);
            TextView nm=new TextView(MainActivity.this); nm.setText(it.name);
            nm.setTextColor(it.dir?Color.parseColor("#D9D9D9"):Color.parseColor("#EFEFEF"));
            nm.setTextSize(15); nm.setSingleLine(true); nm.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            TextView dt=new TextView(MainActivity.this);
            String detail=FileUtils.formatDate(it.time)+"  "+(it.dir?"":FileUtils.formatSize(it.size));
            if(it.dir&&it.name.startsWith(".")){ /* hidden style */ }
            dt.setText(detail); dt.setTextColor(Color.parseColor("#808080")); dt.setTextSize(12);
            col.addView(nm); col.addView(dt);
            row.addView(icon); row.addView(col);
            return row;
        }
    }

    void openItem(int pane, Item it){
        File f=it.f;
        if(f.isDirectory()){ setCur(pane,f,true); return; }
        String n=f.getName().toLowerCase();
        if(n.endsWith(".txt")||n.endsWith(".py")||n.endsWith(".php")||n.endsWith(".java")||n.endsWith(".js")||n.endsWith(".json")||n.endsWith(".xml")||n.endsWith(".html")||n.endsWith(".css")||n.endsWith(".sh")||n.endsWith(".log")||n.endsWith(".smali")){
            Intent i=new Intent(this,TextEditorActivity.class); i.putExtra("path",f.getAbsolutePath()); startActivity(i);
        } else if(n.endsWith(".zip")||n.endsWith(".jar")){
            Intent i=new Intent(this,ZipViewerActivity.class); i.putExtra("path",f.getAbsolutePath()); startActivity(i);
        } else if(n.endsWith(".apk")){
            Intent i=new Intent(this,ApkEditorActivity.class); i.putExtra("path",f.getAbsolutePath()); startActivity(i);
        } else if(n.endsWith(".png")||n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".webp")||n.endsWith(".mp3")||n.endsWith(".mp4")||n.endsWith(".pdf")){
            openWith(f);
        } else {
            // default: try text editor for small, else open-with
            if(f.length()<512*1024){ Intent i=new Intent(this,TextEditorActivity.class); i.putExtra("path",f.getAbsolutePath()); startActivity(i); }
            else openWith(f);
        }
    }

    void openWith(File f){
        try{
            String mime="*/*";
            String n=f.getName().toLowerCase();
            if(n.endsWith(".txt")||n.endsWith(".log")) mime="text/plain";
            else if(n.endsWith(".html")) mime="text/html";
            else if(n.endsWith(".png")) mime="image/png";
            else if(n.endsWith(".jpg")||n.endsWith(".jpeg")) mime="image/jpeg";
            else if(n.endsWith(".mp3")) mime="audio/*";
            else if(n.endsWith(".mp4")) mime="video/*";
            else if(n.endsWith(".pdf")) mime="application/pdf";
            else if(n.endsWith(".apk")) mime="application/vnd.android.package-archive";
            else if(n.endsWith(".zip")) mime="application/zip";
            Intent i=new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(f),mime);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(i,"İle aç..."));
        }catch(Exception e){ Toast.makeText(this,"Acilamadi: "+e.getMessage(),0).show(); }
    }

    // ===== TOP MENU =====
    void showTopMenu(){
        String[] ops={"Ara (bul)","Yenile","Sırala: İsme/Tarihe/Boyuta","Gizli dosyalar: "+(showHidden?"Kapalı yap":"Açık yap"),"Yapıştır"+(bufSrc!=null?" ("+bufSrc.size()+")":""),"Yerlerim","Hakkında"};
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Seçenekler");
        styleDialog(b);
        b.setItems(ops,(d,w)->{
            if(w==0) showSearchInput();
            else if(w==1){ refreshPane(0); refreshPane(1); }
            else if(w==2) cycleSort();
            else if(w==3){ showHidden=!showHidden; refreshPane(0); refreshPane(1); Toast.makeText(this,"Gizli dosyalar: "+(showHidden?"Açık":"Kapalı"),0).show(); }
            else if(w==4) doPaste();
            else if(w==5) showBookmarks();
            else showAbout();
        });
        b.setNegativeButton("Kapat",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }

    void cycleSort(){
        sortMode=(sortMode+1)%3;
        String s=sortMode==0?"İsme göre":sortMode==1?"Tarihe göre":"Boyuta göre";
        Toast.makeText(this,"Sırala: "+s,0).show();
        refreshPane(0); refreshPane(1);
    }
    void showAbout(){
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("MT Manager Clone 3.2");
        b.setMessage("Çift panelli dosya yöneticisi\nArama, Özellik, Sağlama toplamı, Zip/APK/Text düzenleyici içerir.\nKoyu tema MT Manager benzeri.");
        b.setPositiveButton("Tamam",null);
        AlertDialog d=b.create(); d.show(); darkButtons(d);
    }

    void showBookmarks(){
        List<String> marks=new ArrayList<>();
        marks.add("/storage/emulated/0");
        marks.add("/storage/emulated/0/Download");
        marks.add("/storage/emulated/0/Android/obb");
        marks.add("/data/data/"+getPackageName());
        try{
            if(bookmarksFile.exists()){
                BufferedReader br=new BufferedReader(new FileReader(bookmarksFile));
                String l; while((l=br.readLine())!=null){ l=l.trim(); if(!l.isEmpty()&&!marks.contains(l)) marks.add(l); }
                br.close();
            }
        }catch(Exception e){}
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Yerlerim");
        styleDialog(b);
        b.setItems(marks.toArray(new String[0]),(d,w)->{
            File f=new File(marks.get(w));
            if(!f.exists()){ Toast.makeText(this,"Bulunamadı: "+marks.get(w),0).show(); return; }
            if(f.isFile()) openWith(f); else setCur(activePane,f,true);
        });
        b.setNegativeButton("Kapat",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }

    void showNewMenu(){
        String[] ops={"Yeni Dosya","Yeni Klasör","Yapıştır"+(bufSrc!=null?" ("+bufSrc.size()+")":"")};
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Oluştur");
        styleDialog(b);
        b.setItems(ops,(d,w)->{
            if(w==0) createNew(false);
            else if(w==1) createNew(true);
            else doPaste();
        });
        b.setNegativeButton("Vazgeç",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }
    void createNew(boolean isDir){
        final EditText et=new EditText(this); et.setHint(isDir?"yeni_klasor":"yeni_dosya.txt");
        et.setTextColor(Color.WHITE); et.setHintTextColor(Color.GRAY);
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle(isDir?"Yeni Klasör":"Yeni Dosya"); b.setView(et);
        b.setPositiveButton("Tamam",(d,w)->{
            String n=et.getText().toString().trim();
            if(n.isEmpty()) return;
            File cur=curOf(activePane);
            File nf=new File(cur,n);
            try{
                if(isDir) nf.mkdirs(); else { nf.createNewFile(); }
                refreshPane(activePane); updateTop();
            }catch(Exception e){ Toast.makeText(this,"Hata: "+e.getMessage(),0).show(); }
        });
        b.setNegativeButton("Vazgeç",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }
    void showPaneLongMenu(int pane){
        activePane=pane; highlightPanes(); updateTop();
        String[] ops={"Yapıştır"+(bufSrc!=null?" ("+bufSrc.size()+")":""),"Yeni Dosya","Yeni Klasör","Ara (bul)","Yenile"};
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle(curOf(pane).getAbsolutePath());
        styleDialog(b);
        b.setItems(ops,(d,w)->{
            if(w==0) doPaste();
            else if(w==1) createNew(false);
            else if(w==2) createNew(true);
            else if(w==3) showSearchInput();
            else { refreshPane(0); refreshPane(1); }
        });
        b.setNegativeButton("Kapat",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }

    // ===== CONTEXT MENU (screenshot 3) =====
    void showContextMenu(final File f){
        final android.app.Dialog dlg=new android.app.Dialog(this);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#1E1E1E")));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1E1E1E")); root.setPadding(12,12,12,12);
        GridLayout grid=new GridLayout(this); grid.setColumnCount(2); grid.setRowCount(5);
        // {text, icon, enabled}
        String[][] rows={
            {"Kopyala ->","⧉"},{"Taşı ->","✂"},
            {"Sil","🗑"},{"Adlandır","✎"},
            {"Araçlar","🔧"},{"Sıkıştır","⬇"},
            {"Özellik","ⓘ"},{"Paylaş","⎙"},
            {"İle aç...","✓"},{"Yerimi ekle","🔖"}
        };
        for(int i=0;i<rows.length;i++){
            final int idx=i;
            LinearLayout cell=new LinearLayout(this); cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(Gravity.CENTER_VERTICAL); cell.setPadding(18,22,18,22);
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
            lp.width=0; lp.columnSpec=GridLayout.spec(idx%2,1f); lp.rowSpec=GridLayout.spec(idx/2);
            cell.setLayoutParams(lp);
            // Taşı disabled look like screenshot? keep enabled
            TextView ic=new TextView(this); ic.setText(rows[i][1]); ic.setTextColor(Color.parseColor("#8A8A8A")); ic.setTextSize(18); ic.setPadding(0,0,18,0);
            TextView tx=new TextView(this); tx.setText(rows[i][0]); tx.setTextColor(Color.parseColor("#D4D4D4")); tx.setTextSize(16);
            cell.addView(ic); cell.addView(tx);
            cell.setOnClickListener(v->{ dlg.dismiss(); onContextAction(idx,f); });
            // divider effect
            cell.setBackgroundColor(Color.parseColor("#1E1E1E"));
            grid.addView(cell);
        }
        root.addView(grid);
        dlg.setContentView(root);
        // width match parent-ish
        try{ dlg.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.92),ViewGroup.LayoutParams.WRAP_CONTENT); }catch(Exception e){}
        dlg.show();
    }

    void onContextAction(int idx, File f){
        if(idx==0){ // Kopyala
            bufSrc=Arrays.asList(f); bufMode="copy";
            Toast.makeText(this,"Kopyalandı: "+f.getName()+" - hedefte Yapıştır",0).show();
        } else if(idx==1){ // Taşı
            bufSrc=Arrays.asList(f); bufMode="cut";
            Toast.makeText(this,"Taşınacak: "+f.getName()+" - hedefte Yapıştır",0).show();
        } else if(idx==2){ confirmDelete(f); }
        else if(idx==3){ showRename(f); }
        else if(idx==4){ showTools(f); }
        else if(idx==5){ showZip(f); }
        else if(idx==6){ showProperties(f); }
        else if(idx==7){ shareFile(f); }
        else if(idx==8){ openWith(f); }
        else if(idx==9){ addBookmark(f); }
    }

    void confirmDelete(final File f){
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Silinsin mi?");
        b.setMessage(f.getAbsolutePath());
        b.setPositiveButton("Sil",(d,w)->{
            try{ FileUtils.deleteRec(f); refreshPane(0); refreshPane(1); updateTop(); Toast.makeText(this,"Silindi",0).show(); }
            catch(Exception e){ Toast.makeText(this,"Hata: "+e.getMessage(),0).show(); }
        });
        b.setNegativeButton("Vazgeç",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }
    void showRename(final File f){
        final EditText et=new EditText(this); et.setText(f.getName()); et.setTextColor(Color.WHITE);
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Yeniden Adlandır"); b.setView(et);
        b.setPositiveButton("Tamam",(d,w)->{
            String nn=et.getText().toString().trim();
            if(nn.isEmpty()||nn.equals(f.getName())) return;
            File nf=new File(f.getParentFile(),nn);
            if(f.renameTo(nf)){ refreshPane(0); refreshPane(1); updateTop(); }
            else Toast.makeText(this,"Adlandırılamadı",0).show();
        });
        b.setNegativeButton("Vazgeç",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }
    void showTools(final File f){
        String[] ops={"Ara (bul)","Sırala","Gizli dosyalar","Terminal aç","Metin Düzenleyici","APK Düzenleyici","ZIP Görüntüleyici","Yapıştır","Yerlerim"};
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Araçlar");
        styleDialog(b);
        b.setItems(ops,(d,w)->{
            if(w==0) showSearchInput();
            else if(w==1) cycleSort();
            else if(w==2){ showHidden=!showHidden; refreshPane(0); refreshPane(1); }
            else if(w==3) Toast.makeText(this,"Terminal: (demo) "+curOf(activePane).getAbsolutePath(),0).show();
            else if(w==4){ Intent i=new Intent(this,TextEditorActivity.class); i.putExtra("path",f.isFile()?f.getAbsolutePath():new File(f,"yeni.txt").getAbsolutePath()); startActivity(i); }
            else if(w==5){ Intent i=new Intent(this,ApkEditorActivity.class); i.putExtra("path",f.getAbsolutePath()); startActivity(i); }
            else if(w==6){ Intent i=new Intent(this,ZipViewerActivity.class); i.putExtra("path",f.getAbsolutePath()); startActivity(i); }
            else if(w==7) doPaste();
            else showBookmarks();
        });
        b.setNegativeButton("Kapat",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }
    void showZip(final File f){
        final EditText et=new EditText(this);
        et.setText(f.getParent()+"/"+f.getName()+".zip"); et.setTextColor(Color.WHITE);
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Sıkıştır"); b.setMessage("ZIP yolu girin:"); b.setView(et);
        b.setPositiveButton("Tamam",(d,w)->{
            String out=et.getText().toString().trim();
            try{
                ZipOutput(f,new File(out));
                Toast.makeText(this,"Paketlendi: "+out,0).show();
                refreshPane(0); refreshPane(1);
            }catch(Exception e){ Toast.makeText(this,"Hata: "+e.getMessage(),0).show(); }
        });
        b.setNegativeButton("Vazgeç",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }
    void ZipOutput(File src, File out) throws Exception{
        java.util.zip.ZipOutputStream z=new java.util.zip.ZipOutputStream(new FileOutputStream(out));
        zipRec(z,src,src.getName()); z.close();
    }
    void zipRec(java.util.zip.ZipOutputStream z, File f, String base) throws Exception{
        if(f.isDirectory()){
            File[] fs=f.listFiles();
            if(fs!=null) for(File c:fs) zipRec(z,c,base+"/"+c.getName());
            else z.putNextEntry(new java.util.zip.ZipEntry(base+"/"));
        }else{
            z.putNextEntry(new java.util.zip.ZipEntry(base));
            FileInputStream in=new FileInputStream(f); byte[] b=new byte[8192]; int n;
            while((n=in.read(b))>0) z.write(b,0,n); in.close(); z.closeEntry();
        }
    }
    void shareFile(File f){
        try{
            Intent i=new Intent(Intent.ACTION_SEND);
            i.setType("*/*"); i.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(f));
            startActivity(Intent.createChooser(i,"Paylaş"));
        }catch(Exception e){ Toast.makeText(this,"Paylaşılamadı: "+e.getMessage(),0).show(); }
    }
    void addBookmark(File f){
        try{
            bookmarksFile.getParentFile().mkdirs();
            FileWriter fw=new FileWriter(bookmarksFile,true);
            fw.write(f.getAbsolutePath()+"\n"); fw.close();
            Toast.makeText(this,"Etiket kaydedildi: "+f.getName(),0).show();
        }catch(Exception e){ Toast.makeText(this,"Hata: "+e.getMessage(),0).show(); }
    }
    void doPaste(){
        if(bufSrc==null||bufSrc.isEmpty()){ Toast.makeText(this,"Önce Kopyala / Taşı",0).show(); return; }
        File targetDir=curOf(activePane);
        if(!targetDir.isDirectory()) targetDir=targetDir.getParentFile();
        try{
            for(File s:bufSrc){
                File d=new File(targetDir,s.getName());
                if(s.getCanonicalPath().equals(d.getCanonicalPath())) continue;
                FileUtils.copyRec(s,d);
                if("cut".equals(bufMode)) FileUtils.deleteRec(s);
            }
            Toast.makeText(this,"Yapıştırıldı",0).show();
            if("cut".equals(bufMode)){ bufSrc=null; bufMode=null; }
            refreshPane(0); refreshPane(1); updateTop();
        }catch(Exception e){ Toast.makeText(this,"Hata: "+e.getMessage(),0).show(); }
    }

    // ===== ÖZELLİK DIALOG (screenshot 2) =====
    void showProperties(final File f){
        final android.app.Dialog dlg=new android.app.Dialog(this);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#333333")));
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#333333")); root.setPadding(36,28,36,28);
        TextView title=new TextView(this); title.setText("Özellik"); title.setTextColor(Color.WHITE);
        title.setTextSize(22); title.setTypeface(Typeface.DEFAULT_BOLD); title.setPadding(0,0,0,18);
        root.addView(title);
        // rows
        String[][] rows={
            {"Adı",f.getName()},
            {"Üstöğe",f.getParent()!=null?f.getParent()+"/":"/"},
            {"Tür",f.isDirectory()?"Klasör":"Dosya"},
            {"Boyut",FileUtils.formatSize(f.isDirectory()?0:f.length())+" ("+(f.isDirectory()?"-":String.valueOf(f.length()))+")"},
            {"Değiştirilen",FileUtils.formatFullDate(f.lastModified())},
            {"İzinler",FileUtils.permString(f)},
            {"Sahip","u0_a273"},
            {"Grup","media_rw"}
        };
        for(String[] r:rows){
            LinearLayout line=new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); line.setPadding(0,6,0,6);
            TextView k=new TextView(this); k.setText(r[0]); k.setTextColor(Color.parseColor("#9A9A9A")); k.setTextSize(16);
            k.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            TextView v=new TextView(this); v.setText(r[1]); v.setTextColor(Color.WHITE); v.setTextSize(16);
            v.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1.4f));
            line.addView(k); line.addView(v); root.addView(line);
        }
        // buttons right aligned stacked
        String[] btns={"KAPAT","SAĞLAMA TOPLAMI","DAHA FAZLA"};
        for(int i=0;i<btns.length;i++){
            final int bi=i;
            Button bb=new Button(this); bb.setText(btns[i]);
            bb.setTextColor(Color.parseColor("#2196F3")); bb.setBackgroundColor(Color.TRANSPARENT);
            bb.setGravity(Gravity.END); bb.setTextSize(15); bb.setTypeface(Typeface.DEFAULT_BOLD);
            bb.setPadding(0,18,8,8);
            final File ff=f;
            bb.setOnClickListener(v->{
                if(bi==0) dlg.dismiss();
                else if(bi==1) showChecksum(ff);
                else showMore(ff);
            });
            root.addView(bb);
        }
        dlg.setContentView(root);
        try{ dlg.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.92),ViewGroup.LayoutParams.WRAP_CONTENT); }catch(Exception e){}
        dlg.show();
    }

    void showChecksum(File f){
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("SAĞLAMA TOPLAMI");
        String msg;
        try{
            if(f.isDirectory()) msg="Klasör: "+f.getName()+"\nKlasör için dosya sağlaması hesaplanamaz.\nBoyut: -\n";
            else{
                msg="Adı: "+f.getName()+"\n\nMD5:\n"+FileUtils.md5(f)+"\n\nSHA-1:\n"+FileUtils.sha1(f)+"\n\nSHA-256:\n"+FileUtils.sha256(f)+"\n\nBoyut: "+f.length();
            }
        }catch(Exception e){ msg="Hata: "+e.getMessage(); }
        b.setMessage(msg);
        b.setPositiveButton("KAPAT",null);
        AlertDialog d=b.create(); d.show(); darkButtons(d);
    }
    void showMore(File f){
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("DAHA FAZLA");
        StringBuilder sb=new StringBuilder();
        try{
            sb.append("Tam yol:\n").append(f.getAbsolutePath()).append("\n\n");
            sb.append("Canonical:\n").append(f.getCanonicalPath()).append("\n\n");
            sb.append("Gizli: ").append(f.isHidden()?"Evet":"Hayır").append("\n");
            sb.append("Okunabilir: ").append(f.canRead()?"Evet":"Hayır").append("\n");
            sb.append("Yazılabilir: ").append(f.canWrite()?"Evet":"Hayır").append("\n");
            sb.append("Çalıştırılabilir: ").append(f.canExecute()?"Evet":"Hayır").append("\n");
            sb.append("Son erişim: ").append(FileUtils.formatFullDate(f.lastModified())).append("\n");
            if(f.isDirectory()){
                File[] fs=f.listFiles(); sb.append("İçerik: ").append(fs!=null?fs.length:0).append(" öğe\n");
            }
            sb.append("Sahip: u0_a273\nGrup: media_rw\nİzin: ").append(FileUtils.permString(f)).append("\n");
        }catch(Exception e){ sb.append("Hata: "+e.getMessage()); }
        b.setMessage(sb.toString());
        b.setPositiveButton("KAPAT",null);
        AlertDialog d=b.create(); d.show(); darkButtons(d);
    }

    // ===== SEARCH (screenshot 1) =====
    void showSearchInput(){
        LinearLayout lay=new LinearLayout(this); lay.setOrientation(LinearLayout.VERTICAL); lay.setPadding(20,10,20,10);
        final EditText et=new EditText(this); et.setHint("aranacak kelime"); et.setTextColor(Color.WHITE); et.setHintTextColor(Color.GRAY);
        lay.addView(et);
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Ara (bul)"); b.setView(lay);
        b.setPositiveButton("Tamam",(d,w)->{
            String q=et.getText().toString().trim();
            if(!q.isEmpty()) startSearch(curOf(activePane),q);
        });
        b.setNegativeButton("Vazgeç",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }

    void startSearch(final File rootDir, final String keyword){
        final android.app.Dialog dlg=new android.app.Dialog(this);
        dlg.setCancelable(false);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#333333")));
        LinearLayout lay=new LinearLayout(this); lay.setOrientation(LinearLayout.VERTICAL);
        lay.setBackgroundColor(Color.parseColor("#333333")); lay.setPadding(36,28,36,28);
        lay.setMinimumWidth((int)(getResources().getDisplayMetrics().widthPixels*0.85));
        TextView t1=new TextView(this); t1.setText("Aranıyor..."); t1.setTextColor(Color.WHITE); t1.setTextSize(22);
        t1.setPadding(0,0,0,40);
        final TextView tPath=new TextView(this); tPath.setText("./"+rootDir.getName()+"/"); tPath.setTextColor(Color.parseColor("#9A9A9A"));
        tPath.setTextSize(15); tPath.setGravity(Gravity.CENTER); tPath.setPadding(0,0,0,10);
        final TextView tFound=new TextView(this); tFound.setText("Bulunan: 0"); tFound.setTextColor(Color.WHITE);
        tFound.setTextSize(16); tFound.setGravity(Gravity.END); tFound.setPadding(0,0,8,20);
        Button stop=new Button(this); stop.setText("ARAMAYI DURDUR"); stop.setTextColor(Color.WHITE);
        stop.setBackgroundColor(Color.parseColor("#2196F3")); stop.setTypeface(Typeface.DEFAULT_BOLD);
        stop.setPadding(20,16,20,16);
        LinearLayout center=new LinearLayout(this); center.setGravity(Gravity.CENTER); center.addView(stop);
        lay.addView(t1); lay.addView(tPath); lay.addView(tFound); lay.addView(center);
        dlg.setContentView(lay);
        try{ dlg.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.88),ViewGroup.LayoutParams.WRAP_CONTENT); }catch(Exception e){}
        dlg.show();

        final List<File> results=Collections.synchronizedList(new ArrayList<>());
        final AtomicBoolean cancelled=new AtomicBoolean(false);
        stop.setOnClickListener(v->{ cancelled.set(true); });
        final Handler h=new Handler(Looper.getMainLooper());
        searchExec.execute(()->{
            try{ searchRec(rootDir,keyword.toLowerCase(),tPath,tFound,results,cancelled,h); }catch(Exception e){}
            h.post(()->{
                try{ dlg.dismiss(); }catch(Exception e){}
                showSearchResults(keyword,results);
            });
        });
    }

    void searchRec(File dir, String kw, final TextView tPath, final TextView tFound, List<File> out, AtomicBoolean cancel, Handler h){
        if(cancel.get()) return;
        String tmpPath;
        try{ tmpPath="."+dir.getAbsolutePath().replace("/storage/emulated/0","")+"/"; }catch(Exception e){ tmpPath=dir.getAbsolutePath(); }
        final String showPath = tmpPath;
        h.post(()->{ try{ tPath.setText(showPath.length()>48?("..."+showPath.substring(showPath.length()-45)):showPath); tFound.setText("Bulunan: "+out.size()); }catch(Exception e){} });
        File[] fs=null;
        try{ fs=dir.listFiles(); }catch(Exception e){ return; }
        if(fs==null) return;
        for(File f:fs){
            if(cancel.get()) return;
            try{
                if(f.getName().toLowerCase().contains(kw)){
                    out.add(f);
                    h.post(()->{ try{ tFound.setText("Bulunan: "+out.size()); }catch(Exception e){} });
                }
                if(f.isDirectory()&&!f.getName().equals("Android")||f.isDirectory()&&out.size()<500){
                    // recurse all, but avoid huge loops; still allow Android/obb like screenshot
                    if(!cancel.get()) searchRec(f,kw,tPath,tFound,out,cancel,h);
                } else if(f.isDirectory()){
                    if(!cancel.get()) searchRec(f,kw,tPath,tFound,out,cancel,h);
                }
            }catch(Exception e){}
            if(out.size()>=1000) return;
        }
    }

    void showSearchResults(String kw, List<File> res){
        if(res.isEmpty()){
            AlertDialog.Builder b=new AlertDialog.Builder(this);
            b.setTitle("Arama Sonuçları");
            b.setMessage("Bulunamadı: "+kw);
            b.setPositiveButton("KAPAT",null);
            AlertDialog d=b.create(); d.show(); darkButtons(d);
            return;
        }
        String[] arr=new String[res.size()];
        for(int i=0;i<res.size();i++) arr[i]=res.get(i).getAbsolutePath();
        AlertDialog.Builder b=new AlertDialog.Builder(this);
        b.setTitle("Arama Sonuçları ("+res.size()+")");
        styleDialog(b);
        b.setItems(arr,(d,w)->{
            File f=res.get(w);
            if(f.isDirectory()) setCur(activePane,f,true);
            else{
                // open parent and open file
                setCur(activePane,f.getParentFile(),true);
                openItem(activePane,new Item(f));
            }
        });
        b.setNegativeButton("KAPAT",null);
        AlertDialog dlg=b.create(); dlg.show(); darkButtons(dlg);
    }

    void styleDialog(AlertDialog.Builder b){ /* keep default dark via theme */ }
    void darkButtons(AlertDialog d){
        try{
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#2196F3"));
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#2196F3"));
            d.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.parseColor("#2196F3"));
        }catch(Exception e){}
    }
}
