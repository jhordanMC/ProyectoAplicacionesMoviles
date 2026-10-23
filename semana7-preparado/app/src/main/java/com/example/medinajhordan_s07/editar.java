package com.example.medinajhordan_s07;
import android.os.Bundle;
import android.widget.EditText;
import android.database.sqlite.SQLiteException;
import androidx.appcompat.app.AlertDialog;
public class editar extends FormularioActivity {
 private long saiyajinId;
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); preparar(R.layout.activity_editar);
  saiyajinId=getIntent().getLongExtra("id",-1);
  try {
   Saiyajin e=db.buscar(saiyajinId);
   if(e==null) { mensaje("El saiyajin ya no existe"); finish(); return; }
   ((EditText)findViewById(R.id.id)).setText(String.valueOf(e.id));
   if(state==null) { nombre.setText(e.nombre); planeta.setText(e.planeta); transformacion.setText(e.transformacion); }
  } catch(SQLiteException e) { mensaje("No se pudo cargar el saiyajin"); finish(); return; }
  findViewById(R.id.bt1).setOnClickListener(v->{
   if(!validar()) return;
   try { if(db.actualizar(saiyajinId,valor(nombre),valor(planeta),valor(transformacion))==1) { mensaje("Registro actualizado"); finish(); }
    else { mensaje("El saiyajin ya no existe"); finish(); }
   } catch(SQLiteException e) { mensaje("No se pudo actualizar el registro"); }
  });
  findViewById(R.id.bt2).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar saiyajin")
   .setMessage("¿Eliminar este registro? Esta acción no se puede deshacer.")
   .setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(dialog,which)->{
    try { int count=db.eliminar(saiyajinId); mensaje(count==1?"Registro eliminado":"El saiyajin ya no existe"); finish(); }
    catch(SQLiteException e) { mensaje("No se pudo eliminar el registro"); }
   }).show());
  findViewById(R.id.bt3).setOnClickListener(v->finish());
 }
}