package com.example.medinajhordan_s07;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.ArrayList;
public class BaseDatos extends SQLiteOpenHelper {
 public BaseDatos(Context c) { super(c,"BD_SAIYAJIN",null,1); }
 @Override public void onCreate(SQLiteDatabase db) {
  db.execSQL("CREATE TABLE IF NOT EXISTS Saiyajin(id INTEGER PRIMARY KEY AUTOINCREMENT,nombre VARCHAR NOT NULL,planeta VARCHAR NOT NULL,transformacion VARCHAR NOT NULL)");
 }
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { }
 private ContentValues valores(String n,String a,String d) {
  ContentValues v=new ContentValues(); v.put("nombre",n); v.put("planeta",a); v.put("transformacion",d); return v;
 }
 public long registrar(String n,String a,String d) { return getWritableDatabase().insertOrThrow("Saiyajin",null,valores(n,a,d)); }
 public int actualizar(long id,String n,String a,String d) { return getWritableDatabase().update("Saiyajin",valores(n,a,d),"id = ?",new String[]{String.valueOf(id)}); }
 public int eliminar(long id) { return getWritableDatabase().delete("Saiyajin","id = ?",new String[]{String.valueOf(id)}); }
 public ArrayList<Saiyajin> listar() {
  ArrayList<Saiyajin> result=new ArrayList<>();
  try(Cursor c=getReadableDatabase().query("Saiyajin",new String[]{"id","nombre","planeta","transformacion"},null,null,null,null,"id DESC")) {
   while(c.moveToNext()) result.add(new Saiyajin(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)));
  } return result;
 }
 public Saiyajin buscar(long id) {
  try(Cursor c=getReadableDatabase().query("Saiyajin",new String[]{"id","nombre","planeta","transformacion"},"id = ?",new String[]{String.valueOf(id)},null,null,null)) {
   return c.moveToFirst()?new Saiyajin(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)):null;
  }
 }
}