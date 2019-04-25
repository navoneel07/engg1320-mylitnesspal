package com.checc.mylitnesspal;

public class Meal {
    private String name;
    private String kCal;

    public Meal(String name, String cal){
        this.name = name;
        this.kCal = cal;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setkCal(String kCal) {
        this.kCal = kCal;
    }

    public String getName() {
        return name;
    }

    public String getkCal() {
        return kCal;
    }

    public Meal(){

    }
}
