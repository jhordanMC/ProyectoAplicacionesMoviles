package com.example.medinajhordan_s07;
import android.os.Bundle;
import android.widget.*;
import android.database.sqlite.SQLiteException;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
public abstract class FormularioActivity extends AppCompatActivity {
 protected EditText nombre,planeta,transformacion;
 protected BaseDatos db;
 protected void preparar(int layout) {
  setContentView(layout); db=new BaseDatos(this);
  nombre=findViewById(R.id.name); planeta=findViewById(R.id.course); transformacion=findViewById(R.id.fee);
  ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),(v,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
  });
 }
 protected boolean validar() {
  for(EditText campo:new EditText[]{nombre,planeta,transformacion}) {
   if(campo.getText().toString().trim().isEmpty()) { campo.setError("Campo obligatorio"); campo.requestFocus(); return false; }
  } return true;
 }
 protected String valor(EditText campo) { return campo.getText().toString().trim(); }
 protected void mensaje(String texto) { Toast.makeText(this,texto,Toast.LENGTH_SHORT).show(); }
 @Override protected void onDestroy() { if(db!=null) db.close(); super.onDestroy(); }
}