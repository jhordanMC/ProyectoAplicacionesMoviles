package com.example.medinajhordan_s07;
public class Saiyajin {
 public final long id;
 public final String nombre, planeta, transformacion;
 public Saiyajin(long id, String nombre, String planeta, String transformacion) {
  this.id=id; this.nombre=nombre; this.planeta=planeta; this.transformacion=transformacion;
 }
 @Override public String toString() { return id+" · "+nombre+"\nPlaneta: "+planeta+"\nTransformación: "+transformacion; }
}
