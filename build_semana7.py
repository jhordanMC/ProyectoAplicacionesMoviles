from pathlib import Path
p=Path(r'D:\SaaSVilcas\semana7-preparado')
j=p/'app/src/main/java/com/example/medinajhordan_s07'
def java(name,code):
 (j/(name+'.java')).write_text('package com.example.medinajhordan_s07;\n'+code,encoding='utf-8')
java('Estudiante','''public class Estudiante {
 public final long id;
 public final String nombre, apellido, distrito;
 public Estudiante(long id, String nombre, String apellido, String distrito) {
  this.id=id; this.nombre=nombre; this.apellido=apellido; this.distrito=distrito;
 }
 @Override public String toString() { return id+" · "+nombre+" "+apellido+"\\nDistrito: "+distrito; }
}''')
java('BaseDatos','''import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.ArrayList;
public class BaseDatos extends SQLiteOpenHelper {
 public BaseDatos(Context c) { super(c,"BD_UPN",null,1); }
 @Override public void onCreate(SQLiteDatabase db) {
  db.execSQL("CREATE TABLE IF NOT EXISTS Estudiante(id INTEGER PRIMARY KEY AUTOINCREMENT,nombre VARCHAR NOT NULL,apellido VARCHAR NOT NULL,distrito VARCHAR NOT NULL)");
 }
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { }
 private ContentValues valores(String n,String a,String d) {
  ContentValues v=new ContentValues(); v.put("nombre",n); v.put("apellido",a); v.put("distrito",d); return v;
 }
 public long registrar(String n,String a,String d) { return getWritableDatabase().insertOrThrow("Estudiante",null,valores(n,a,d)); }
 public int actualizar(long id,String n,String a,String d) { return getWritableDatabase().update("Estudiante",valores(n,a,d),"id = ?",new String[]{String.valueOf(id)}); }
 public int eliminar(long id) { return getWritableDatabase().delete("Estudiante","id = ?",new String[]{String.valueOf(id)}); }
 public ArrayList<Estudiante> listar() {
  ArrayList<Estudiante> result=new ArrayList<>();
  try(Cursor c=getReadableDatabase().query("Estudiante",new String[]{"id","nombre","apellido","distrito"},null,null,null,null,"id DESC")) {
   while(c.moveToNext()) result.add(new Estudiante(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)));
  } return result;
 }
 public Estudiante buscar(long id) {
  try(Cursor c=getReadableDatabase().query("Estudiante",new String[]{"id","nombre","apellido","distrito"},"id = ?",new String[]{String.valueOf(id)},null,null,null)) {
   return c.moveToFirst()?new Estudiante(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)):null;
  }
 }
}''')
java('FormularioActivity','''import android.os.Bundle;
import android.widget.*;
import android.database.sqlite.SQLiteException;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
public abstract class FormularioActivity extends AppCompatActivity {
 protected EditText nombre,apellido,distrito;
 protected BaseDatos db;
 protected void preparar(int layout) {
  setContentView(layout); db=new BaseDatos(this);
  nombre=findViewById(R.id.name); apellido=findViewById(R.id.course); distrito=findViewById(R.id.fee);
  ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),(v,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
  });
 }
 protected boolean validar() {
  for(EditText campo:new EditText[]{nombre,apellido,distrito}) {
   if(campo.getText().toString().trim().isEmpty()) { campo.setError("Campo obligatorio"); campo.requestFocus(); return false; }
  } return true;
 }
 protected String valor(EditText campo) { return campo.getText().toString().trim(); }
 protected void mensaje(String texto) { Toast.makeText(this,texto,Toast.LENGTH_SHORT).show(); }
 @Override protected void onDestroy() { if(db!=null) db.close(); super.onDestroy(); }
}''')
java('MainActivity','''import android.os.Bundle;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
public class MainActivity extends FormularioActivity {
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); preparar(R.layout.activity_main);
  findViewById(R.id.bt1).setOnClickListener(v->{
   if(!validar()) return;
   try { db.registrar(valor(nombre),valor(apellido),valor(distrito)); mensaje("Estudiante registrado");
    nombre.setText(""); apellido.setText(""); distrito.setText(""); nombre.requestFocus();
   } catch(SQLiteException e) { mensaje("No se pudo guardar el estudiante"); }
  });
  findViewById(R.id.bt2).setOnClickListener(v->startActivity(new Intent(this,listar.class)));
 }
}''')
java('listar','''import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.database.sqlite.SQLiteException;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import java.util.ArrayList;
public class listar extends AppCompatActivity {
 private BaseDatos db;
 private ArrayAdapter<Estudiante> adapter;
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); setContentView(R.layout.activity_listar); db=new BaseDatos(this);
  ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),(v,insets)->{
   androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom); return insets;
  });
  ListView lista=findViewById(R.id.lst1); lista.setEmptyView(findViewById(R.id.empty));
  adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>()); lista.setAdapter(adapter);
  lista.setOnItemClickListener((parent,view,position,id)->{
   Estudiante e=adapter.getItem(position);
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
}''')
java('editar','''import android.os.Bundle;
import android.widget.EditText;
import android.database.sqlite.SQLiteException;
import androidx.appcompat.app.AlertDialog;
public class editar extends FormularioActivity {
 private long estudianteId;
 @Override protected void onCreate(Bundle state) {
  super.onCreate(state); preparar(R.layout.activity_editar);
  estudianteId=getIntent().getLongExtra("id",-1);
  try {
   Estudiante e=db.buscar(estudianteId);
   if(e==null) { mensaje("El estudiante ya no existe"); finish(); return; }
   ((EditText)findViewById(R.id.id)).setText(String.valueOf(e.id));
   if(state==null) { nombre.setText(e.nombre); apellido.setText(e.apellido); distrito.setText(e.distrito); }
  } catch(SQLiteException e) { mensaje("No se pudo cargar el estudiante"); finish(); return; }
  findViewById(R.id.bt1).setOnClickListener(v->{
   if(!validar()) return;
   try { if(db.actualizar(estudianteId,valor(nombre),valor(apellido),valor(distrito))==1) { mensaje("Registro actualizado"); finish(); }
    else { mensaje("El estudiante ya no existe"); finish(); }
   } catch(SQLiteException e) { mensaje("No se pudo actualizar el registro"); }
  });
  findViewById(R.id.bt2).setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar estudiante")
   .setMessage("¿Eliminar este registro? Esta acción no se puede deshacer.")
   .setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(dialog,which)->{
    try { int count=db.eliminar(estudianteId); mensaje(count==1?"Registro eliminado":"El estudiante ya no existe"); finish(); }
    catch(SQLiteException e) { mensaje("No se pudo eliminar el registro"); }
   }).show());
  findViewById(R.id.bt3).setOnClickListener(v->finish());
 }
}''')
strings={'app_name':'Medina SQLite','registro':'Registro de estudiantes','editar':'Editar estudiante','lista':'Estudiantes registrados','nombre':'Nombre','apellido':'Apellido','distrito':'Distrito','guardar':'Registrar','ver_lista':'Ver estudiantes','actualizar':'Actualizar','eliminar':'Eliminar','volver':'Volver','agregar':'Agregar estudiante','vacio':'Todavía no hay estudiantes. Agrega el primero.','ayuda':'Toca un estudiante para editar o eliminar su registro.'}
import xml.etree.ElementTree as ET
root=ET.Element('resources')
for k,v in strings.items(): ET.SubElement(root,'string',name=k).text=v
ET.ElementTree(root).write(p/'app/src/main/res/values/strings.xml',encoding='utf-8',xml_declaration=True)
def field(id,label,editable=True):
 return f'<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="{label}" android:labelFor="@+id/{id}" android:layout_marginTop="16dp"/><EditText android:id="@id/{id}" android:layout_width="match_parent" android:layout_height="wrap_content" android:minHeight="48dp" android:inputType="textPersonName" android:maxLines="1" android:singleLine="true" android:enabled="{str(editable).lower()}"/>'
def button(id,text): return f'<Button android:id="@+id/{id}" android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="12dp" android:text="@string/{text}"/>'
for mode in ['main','editar']:
 content='<TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/'+('registro' if mode=='main' else 'editar')+'" android:textSize="26sp" android:textStyle="bold"/>'
 if mode=='editar': content+=field('id','ID',False)
 for id,label in [('name','@string/nombre'),('course','@string/apellido'),('fee','@string/distrito')]: content+=field(id,label)
 content+=button('bt1','guardar' if mode=='main' else 'actualizar')+button('bt2','ver_lista' if mode=='main' else 'eliminar')
 if mode=='editar': content+=button('bt3','volver')
 xml='<?xml version="1.0" encoding="utf-8"?><ScrollView xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/main" android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true"><LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="24dp">'+content+'</LinearLayout></ScrollView>'
 (p/f'app/src/main/res/layout/activity_{mode}.xml').write_text(xml,encoding='utf-8')
(p/'app/src/main/res/layout/activity_listar.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/main" android:layout_width="match_parent" android:layout_height="match_parent" android:orientation="vertical">
 <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="24dp">
  <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/lista" android:textSize="26sp" android:textStyle="bold"/>
  <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:layout_marginTop="8dp" android:text="@string/ayuda"/>
  <Button android:id="@+id/imageButton" android:layout_width="match_parent" android:layout_height="wrap_content" android:text="@string/agregar"/>
 </LinearLayout>
 <FrameLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1">
  <ListView android:id="@+id/lst1" android:layout_width="match_parent" android:layout_height="match_parent"/>
  <TextView android:id="@+id/empty" android:layout_width="match_parent" android:layout_height="match_parent" android:gravity="center" android:padding="24dp" android:text="@string/vacio"/>
 </FrameLayout>
</LinearLayout>''',encoding='utf-8')
manifest=p/'app/src/main/AndroidManifest.xml'
manifest.write_text(manifest.read_text().replace('    </application>','        <activity android:name=".listar" android:exported="false" />\n        <activity android:name=".editar" android:exported="false" android:windowSoftInputMode="adjustResize" />\n    </application>'),encoding='utf-8')
s=p/'settings.gradle.kts'; s.write_text(s.read_text().replace('MedinaJhordan_S07','Medina_SQLite'),encoding='utf-8')
(p/'LEEME.md').write_text('''# Medina SQLite · Semana 7

Ejemplo de la guía: registro, listado, edición y eliminación de estudiantes.
Base local: BD_UPN. Tabla: Estudiante (id autoincremental, nombre, apellido, distrito).

Abre este proyecto en Android Studio y ejecuta app en un emulador o teléfono (Android 7 o superior).
Registra un estudiante, pulsa Ver estudiantes, toca el registro, actualiza sus datos y comprueba el listado. Cierra y abre la app para comprobar la persistencia. Prueba campos vacíos y cancelar/confirmar la eliminación.

Para inspeccionar la base: View > Tool Windows > App Inspection > Database Inspector, con la app en ejecución en Android 8 o superior; selecciona BD_UPN y Estudiante.

La última parte de la guía requiere los nombres de clase, base y tabla asignados por el docente; esta versión implementa el ejemplo de estudiantes.
''',encoding='utf-8')
print('Proyecto preparado')
