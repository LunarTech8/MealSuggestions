package com.romanbrunner.apps.mealsuggestions;


public interface Ingredient
{
    String getName();
    String getBrand();
    double getAmount();
    String getSpoonCountString();
    String getWeightString();
    String getSugarPercentageString();
}