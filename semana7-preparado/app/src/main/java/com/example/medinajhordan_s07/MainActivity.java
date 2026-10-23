package com.example.medinajhordan_s07;
import android.os.Bundle;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
public class MainActivity extends FormularioActivity {
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); preparar(R.layout.activity_main);
  findViewById(R.id.bt1).setOnClickListener(v->{
   if(!validar()) return;
   try { db.registrar(valor(nombre),valor(planeta),valor(transformacion)); mensaje("Saiyajin registrado");
    nombre.setText(""); planeta.setText(""); transformacion.setText(""); nombre.requestFocus();
   } catch(SQLiteException e) { mensaje("No se pudo guardar el saiyajin"); }
  });
  findViewById(R.id.bt2).setOnClickListener(v->startActivity(new Intent(this,listar.class)));
 }
}