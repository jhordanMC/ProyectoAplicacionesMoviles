package com.example.medinajhordan_s07;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.database.sqlite.SQLiteException;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import java.util.ArrayList;
public class listar extends AppCompatActivity {
 private BaseDatos db;
 private ArrayAdapter<Saiyajin> adapter;
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); setContentView(R.layout.activity_listar); db=new BaseDatos(this);
  ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),(v,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
  });
  ListView lista=findViewById(R.id.lst1); lista.setEmptyView(findViewById(R.id.empty));
  adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>()); lista.setAdapter(adapter);
  lista.setOnItemClickListener((parent,view,position,id)->{
   Saiyajin e=adapter.getItem(position);
   if(e!=null) startActivity(new Intent(this,editar.class).putExtra("id",e.id));
  });
  findViewById(R.id.imageButton).setOnClickListener(v->{
   startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)); finish();
  });
 }
 @Override protected void onResume() {
  super.onResume();
  try { adapter.clear(); adapter.addAll(db.listar()); } catch(SQLiteException e) { Toast.makeText(this,"No se pudo cargar la lista",Toast.LENGTH_SHORT).show(); }
 }
 @Override protected void onDestroy() { db.close(); super.onDestroy(); }
}